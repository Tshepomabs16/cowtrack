package com.cowtrack.realtime;

import com.cowtrack.security.FarmContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * How the service layer announces something a farmer should see immediately.
 *
 * <p>Raising an event is not the same as delivering one. This only records that
 * something happened; {@link RealtimeEventListener} delivers it once the
 * surrounding transaction commits. Pushing at the call site instead would let a
 * client be told about a position or an alert that a later rollback erased,
 * leaving a marker on the map with nothing behind it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimePublisher {

    private final ApplicationEventPublisher events;
    private final FarmContext farmContext;

    /** Announces to the farm the current work belongs to. */
    public void publish(String type, Object payload) {
        Long farmId = farmContext.getCurrentFarmIdOrNull();
        if (farmId == null) {
            // Not an error worth failing on: something ran outside both a request
            // and an acting-farm scope, so there is no audience to send to.
            log.debug("Skipping realtime {}: no farm in scope", type);
            return;
        }
        publish(farmId, type, payload);
    }

    public void publish(Long farmId, String type, Object payload) {
        events.publishEvent(new RealtimeEvent(farmId, type, payload));
    }
}
