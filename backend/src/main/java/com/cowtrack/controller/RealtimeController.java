package com.cowtrack.controller;

import com.cowtrack.config.RealtimeProperties;
import com.cowtrack.realtime.SseHub;
import com.cowtrack.realtime.StreamTicketService;
import com.cowtrack.security.FarmContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * The live channel behind the map's "Live" badge.
 *
 * <p>Server-sent events rather than WebSockets. Everything here travels one way,
 * server to client, which is the case SSE exists for: no new dependency on
 * either side, reconnection is the browser's job rather than ours, and — the
 * reason that mattered most here — the stream is an ordinary HTTP GET, so it
 * passes through the same security filter chain and the same farm scoping as
 * every other endpoint. A WebSocket upgrade bypasses that chain and would have
 * needed its own parallel authentication, which is where multi-tenant leaks in
 * this codebase have come from before.
 */
@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
public class RealtimeController extends BaseController {

    private final StreamTicketService ticketService;
    private final SseHub hub;
    private final FarmContext farmContext;
    private final RealtimeProperties properties;

    /**
     * Trades the caller's JWT for a short-lived ticket to open a stream with.
     *
     * <p>Authenticated normally, from the {@code Authorization} header.
     */
    @PostMapping("/ticket")
    public ResponseEntity<?> issueTicket() {
        String ticket = ticketService.issue(farmContext.getCurrentFarmId());
        return success("Stream ticket issued", Map.of(
                "ticket", ticket,
                "expiresInSeconds", properties.getTicketTtl().toSeconds()));
    }

    /**
     * Opens the stream.
     *
     * <p>The ticket is the credential: {@code EventSource} cannot send an
     * {@code Authorization} header, so this path is exempt from the JWT filter
     * and authenticates here instead — the same arrangement as device ingestion.
     * The farm comes from redeeming the ticket and never from the request, so a
     * caller cannot ask for a farm that is not theirs.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String ticket) {
        Long farmId = ticketService.redeem(ticket);
        return hub.subscribe(farmId);
    }
}
