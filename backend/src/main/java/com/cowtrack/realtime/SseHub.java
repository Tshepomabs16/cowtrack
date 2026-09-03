package com.cowtrack.realtime;

import com.cowtrack.config.RealtimeProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The open streams, grouped by the farm they belong to.
 *
 * <p>This is a tenancy boundary in its own right, and a less obvious one than a
 * query: nothing here goes near the database, so the farm scoping that repository
 * methods enforce does not apply. A subscriber is filed under the farm its ticket
 * named and is only ever written to from that farm's bucket, which is what stops
 * one farm's positions appearing on another's map. The registry is keyed by farm
 * rather than filtered by it so there is no per-event predicate to forget.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseHub {

    /** farm id -> subscription id -> the open response. */
    private final Map<Long, Map<String, SseEmitter>> subscribers = new ConcurrentHashMap<>();

    private final RealtimeProperties properties;

    /** What a subscriber receives: the client dispatches on {@code type}. */
    private record Envelope(String type, Object payload) {
    }

    /**
     * Opens a stream for one farm.
     *
     * <p>The caller must have established the farm itself — by redeeming a
     * ticket — because everything delivered afterwards is decided by the key
     * used here and nothing downstream re-checks it.
     */
    public SseEmitter subscribe(Long farmId) {
        SseEmitter emitter = new SseEmitter(properties.getStreamTimeout().toMillis());
        String subscriptionId = UUID.randomUUID().toString();

        subscribers.computeIfAbsent(farmId, id -> new ConcurrentHashMap<>())
                .put(subscriptionId, emitter);

        emitter.onCompletion(() -> remove(farmId, subscriptionId));
        emitter.onError(error -> remove(farmId, subscriptionId));
        emitter.onTimeout(() -> {
            remove(farmId, subscriptionId);
            emitter.complete();
        });

        // Sent inline so the client learns the stream is live from the stream
        // itself, and so the response is committed before this returns.
        send(farmId, subscriptionId, emitter, new RealtimeEvent(farmId, EventTypes.CONNECTED, Map.of()));

        log.debug("Opened stream {} for farm {}", subscriptionId, farmId);
        return emitter;
    }

    /**
     * Delivers an event to every open stream belonging to its farm.
     *
     * <p>Never throws. A subscriber that has gone away must not be able to fail
     * the work that produced the event — a browser tab closing is not a reason
     * for a collar's upload to fail.
     */
    public void publish(RealtimeEvent event) {
        Map<String, SseEmitter> forFarm = subscribers.get(event.farmId());
        if (forFarm == null || forFarm.isEmpty()) {
            return;
        }
        forFarm.forEach((subscriptionId, emitter) ->
                send(event.farmId(), subscriptionId, emitter, event));
    }

    /**
     * Keeps idle connections open, and is how a client that vanished without
     * closing the connection gets noticed: the write fails and the emitter is
     * dropped. Positions can be many minutes apart, which is exactly the idle
     * period proxies and mobile networks reclaim.
     */
    @Scheduled(fixedRateString = "${cowtrack.realtime.heartbeat-interval:25s}")
    public void heartbeat() {
        subscribers.forEach((farmId, forFarm) -> forFarm.forEach((subscriptionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (Exception e) {
                remove(farmId, subscriptionId);
            }
        }));
    }

    private void send(Long farmId, String subscriptionId, SseEmitter emitter, RealtimeEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(new Envelope(event.type(), event.payload()), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            // Routine: the client navigated away or the connection was reclaimed.
            log.debug("Dropping stream {} for farm {}: {}", subscriptionId, farmId, e.getMessage());
            remove(farmId, subscriptionId);
        }
    }

    private void remove(Long farmId, String subscriptionId) {
        // Compute rather than get-then-remove so the farm's empty bucket is
        // discarded atomically; otherwise a farm that once connected leaks a map
        // entry for the lifetime of the process.
        subscribers.computeIfPresent(farmId, (id, forFarm) -> {
            forFarm.remove(subscriptionId);
            return forFarm.isEmpty() ? null : forFarm;
        });
    }

    /**
     * Closes every open stream on the way down.
     *
     * <p>Without this the server holds each connection until it times out, which
     * blocks shutdown and leaves clients waiting on a socket that will never
     * produce anything again. Completing them makes each client see the stream
     * end and reconnect, so a restart costs a reconnection rather than minutes
     * of silence.
     */
    @PreDestroy
    public void closeAll() {
        subscribers.values().forEach(forFarm -> forFarm.values().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // Already gone; nothing to close.
            }
        }));
        subscribers.clear();
    }

    /** Open streams for a farm. For tests and diagnostics. */
    public int subscriberCount(Long farmId) {
        Map<String, SseEmitter> forFarm = subscribers.get(farmId);
        return forFarm == null ? 0 : forFarm.size();
    }

    /** Open streams across all farms. For tests and diagnostics. */
    public int totalSubscribers() {
        return subscribers.values().stream().mapToInt(Map::size).sum();
    }
}
