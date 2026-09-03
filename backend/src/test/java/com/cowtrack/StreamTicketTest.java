package com.cowtrack;

import com.cowtrack.config.RealtimeProperties;
import com.cowtrack.exception.UnauthorizedException;
import com.cowtrack.realtime.StreamTicketService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The stream ticket is the one credential in this system that travels in a URL,
 * where it will be written to access logs. What keeps that acceptable is that it
 * is worth nothing a moment later, so the single use and the expiry are the
 * security property here rather than incidental details.
 */
class StreamTicketTest {

    private StreamTicketService serviceWithTtl(Duration ttl) {
        RealtimeProperties properties = new RealtimeProperties();
        properties.setTicketTtl(ttl);
        return new StreamTicketService(properties);
    }

    @Test
    void aFreshTicketAdmitsToTheFarmItWasIssuedFor() {
        StreamTicketService tickets = serviceWithTtl(Duration.ofSeconds(60));

        String ticket = tickets.issue(7L);

        assertThat(tickets.redeem(ticket)).isEqualTo(7L);
    }

    @Test
    void aTicketCannotBeRedeemedTwice() {
        StreamTicketService tickets = serviceWithTtl(Duration.ofSeconds(60));
        String ticket = tickets.issue(7L);

        tickets.redeem(ticket);

        assertThatThrownBy(() -> tickets.redeem(ticket))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void anExpiredTicketIsRefused() throws Exception {
        StreamTicketService tickets = serviceWithTtl(Duration.ofMillis(1));
        String ticket = tickets.issue(7L);

        Thread.sleep(20);

        assertThatThrownBy(() -> tickets.redeem(ticket))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void anInventedTicketIsRefused() {
        StreamTicketService tickets = serviceWithTtl(Duration.ofSeconds(60));

        assertThatThrownBy(() -> tickets.redeem("not-a-real-ticket"))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> tickets.redeem(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    /**
     * Tickets that are issued and never used must not accumulate: a client that
     * fails to open the stream still leaves one behind every retry.
     */
    @Test
    void unredeemedTicketsAreNotRetainedForever() throws Exception {
        StreamTicketService tickets = serviceWithTtl(Duration.ofMillis(1));
        tickets.issue(1L);
        tickets.issue(2L);
        assertThat(tickets.outstanding()).isEqualTo(2);

        Thread.sleep(20);
        tickets.issue(3L);

        assertThat(tickets.outstanding()).isEqualTo(1);
    }

    /**
     * A ticket is only ever minted from a farm the caller is already authenticated
     * for, so a null farm means something upstream failed to establish one. Issuing
     * anyway would produce a ticket admitting to no farm in particular.
     */
    @Test
    void aTicketCannotBeIssuedWithoutAFarm() {
        StreamTicketService tickets = serviceWithTtl(Duration.ofSeconds(60));

        assertThatThrownBy(() -> tickets.issue(null))
                .isInstanceOf(UnauthorizedException.class);
    }
}
