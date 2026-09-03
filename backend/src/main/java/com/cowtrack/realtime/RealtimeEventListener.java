package com.cowtrack.realtime;

import com.cowtrack.config.RealtimeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers raised events once the work behind them is durable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeEventListener {

    private final SseHub hub;
    private final RealtimeProperties properties;

    /**
     * {@code AFTER_COMMIT} is the whole point: a client must not be shown a
     * position or an alert that a rollback then erased.
     *
     * <p>{@code fallbackExecution} covers events raised with no transaction in
     * progress — there is nothing to roll back in that case, so dropping them,
     * which is the default, would silently lose them.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRealtimeEvent(RealtimeEvent event) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            hub.publish(event);
        } catch (Exception e) {
            // This runs after commit, so throwing cannot undo the write it
            // followed; it would only produce an alarming stack trace for a
            // failure the caller can do nothing about.
            log.warn("Failed to fan out realtime event {} for farm {}", event.type(), event.farmId(), e);
        }
    }
}
