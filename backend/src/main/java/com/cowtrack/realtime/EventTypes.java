package com.cowtrack.realtime;

/**
 * Event names shared with the web client's {@code WS_EVENTS} in
 * {@code services/websocket.js}. Both sides must agree, so changes here are
 * breaking changes for any client already connected.
 */
public final class EventTypes {

    /** A new position for one animal. Payload: {@code LocationResponse}. */
    public static final String LOCATION_UPDATE = "location_update";

    /** An alert has just been raised. Payload: {@code AlertResponse}. */
    public static final String NEW_ALERT = "new_alert";

    /**
     * An alert was closed by the system rather than by the farmer, e.g. an
     * animal walked back into its camp. Payload: {@code AlertResponse}.
     */
    public static final String ALERT_RESOLVED = "alert_resolved";

    /** Sent once when a stream opens, so the client can show it is live. */
    public static final String CONNECTED = "connected";

    private EventTypes() {
    }
}
