package com.cowtrack.realtime;

import com.cowtrack.config.RealtimeProperties;
import com.cowtrack.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, single-use credentials for opening a stream.
 *
 * <p>The browser's {@code EventSource} cannot set request headers, so the JWT
 * every other endpoint authenticates with cannot be sent on the stream request.
 * The obvious workaround — putting the JWT in the query string — writes a
 * credential that is valid for hours into access logs, proxy logs and browser
 * history.
 *
 * <p>So the client trades its JWT, over a normal authenticated request, for a
 * ticket that is worth almost nothing: valid for about a minute, usable once,
 * and good only for opening a read-only stream. That is still a credential in a
 * URL, but the window in which a leaked one is useful is small enough to accept.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamTicketService {

    private static final int TICKET_BYTES = 32;

    private final RealtimeProperties properties;
    private final SecureRandom random = new SecureRandom();

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    private record Ticket(Long farmId, Instant expiresAt) {
    }

    /** Issues a ticket admitting the bearer to one farm's stream. */
    public String issue(Long farmId) {
        if (farmId == null) {
            throw new UnauthorizedException("No farm associated with this user");
        }
        purgeExpired();

        byte[] bytes = new byte[TICKET_BYTES];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tickets.put(ticket, new Ticket(farmId, Instant.now().plus(properties.getTicketTtl())));
        return ticket;
    }

    /**
     * Redeems a ticket, returning the farm it admits to.
     *
     * <p>{@code remove} is what makes it single-use, and does so atomically: two
     * requests racing on the same ticket cannot both be handed a farm, because
     * only one of them gets a non-null result.
     */
    public Long redeem(String ticket) {
        Ticket found = ticket == null ? null : tickets.remove(ticket);

        if (found == null) {
            throw new UnauthorizedException("Invalid or already used stream ticket");
        }
        if (found.expiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Stream ticket has expired");
        }
        return found.farmId();
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        tickets.values().removeIf(ticket -> ticket.expiresAt().isBefore(now));
    }

    /** Outstanding unredeemed tickets. For tests and diagnostics. */
    public int outstanding() {
        return tickets.size();
    }
}
