package com.cowtrack.realtime;

/**
 * Something worth telling a farm's open clients about.
 *
 * <p>Carries its own {@code farmId} rather than reading one from
 * {@link com.cowtrack.security.FarmContext} at delivery time. Delivery happens
 * after the transaction commits, potentially past the point where the request
 * that produced the event still has a security context, and a fan-out that
 * guessed the wrong farm would put one farm's cattle on another's map. The farm
 * is fixed when the event is raised, by the code that already knows it.
 *
 * @param farmId  the only farm whose subscribers may receive this
 * @param type    event name the client dispatches on; see {@link EventTypes}
 * @param payload the response DTO the client renders
 */
public record RealtimeEvent(Long farmId, String type, Object payload) {

    public RealtimeEvent {
        if (farmId == null) {
            throw new IllegalArgumentException("A realtime event must belong to a farm");
        }
    }
}
