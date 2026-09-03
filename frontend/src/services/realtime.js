// Live updates from the server.
//
// Server-sent events, not a WebSocket. Everything on this channel travels one
// way, so the extra machinery of a duplex protocol would buy nothing: this needs
// no client library, and the browser gives us the connection handling.
//
// This module keeps the small event-emitter surface the pages were already
// written against, so subscribing looks the same as it did.

import api from './api';

export const EVENTS = {
  LOCATION_UPDATE: 'location_update',
  NEW_ALERT: 'new_alert',
  CONNECTED: 'connected',
  DISCONNECTED: 'disconnected',
};

const BASE_URL = process.env.REACT_APP_API_URL || '/api';
const MAX_RECONNECT_DELAY_MS = 30000;

class RealtimeService {
  constructor() {
    this.source = null;
    this.listeners = new Map();
    this.retryTimer = null;
    this.reconnectAttempts = 0;
    // Distinguishes a dropped connection, which should be retried, from one we
    // closed on purpose, which should not.
    this.stopped = true;
    this.live = false;
  }

  // A component mounting into an already-open stream has missed the `connected`
  // event and would otherwise show itself as disconnected until the next drop.
  isLive() {
    return this.live;
  }

  connect() {
    this.stopped = false;
    return this.open();
  }

  async open() {
    if (this.stopped) return;
    this.closeSource();

    let ticket;
    try {
      // EventSource cannot send an Authorization header, so the JWT is exchanged
      // here — over a normal authenticated request — for a single-use ticket that
      // is safe enough to put in the stream URL.
      const response = await api.post('/realtime/ticket');
      ticket = response.data?.ticket;
    } catch (error) {
      this.scheduleReconnect();
      return;
    }

    // The caller may have disconnected while the ticket request was in flight.
    if (this.stopped || !ticket) return;

    const source = new EventSource(
      `${BASE_URL}/realtime/stream?ticket=${encodeURIComponent(ticket)}`
    );
    this.source = source;

    source.onmessage = (event) => {
      let message;
      try {
        message = JSON.parse(event.data);
      } catch (error) {
        console.error('Discarding malformed realtime message', error);
        return;
      }

      if (message.type === EVENTS.CONNECTED) {
        this.reconnectAttempts = 0;
        this.live = true;
      }
      this.emit(message.type, message.payload);
    };

    source.onerror = () => {
      // EventSource retries on its own, but it would retry the same URL — and
      // the ticket in it has already been spent, so every attempt would be
      // refused. Close it and reconnect ourselves with a fresh ticket.
      this.closeSource();
      this.live = false;
      this.emit(EVENTS.DISCONNECTED, null);
      this.scheduleReconnect();
    };
  }

  scheduleReconnect() {
    if (this.stopped || this.retryTimer) return;

    this.reconnectAttempts += 1;
    const delay = Math.min(
      1000 * 2 ** (this.reconnectAttempts - 1),
      MAX_RECONNECT_DELAY_MS
    );

    // Retried indefinitely rather than giving up after a few attempts. A phone
    // in a paddock loses signal for long stretches, and a map that stopped
    // trying would go quietly stale with no sign it had stopped.
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      this.open();
    }, delay);
  }

  closeSource() {
    if (this.source) {
      this.source.onmessage = null;
      this.source.onerror = null;
      this.source.close();
      this.source = null;
    }
  }

  disconnect() {
    this.stopped = true;
    if (this.retryTimer) {
      clearTimeout(this.retryTimer);
      this.retryTimer = null;
    }
    this.closeSource();
    this.listeners.clear();
    this.reconnectAttempts = 0;
    this.live = false;
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event).push(callback);
  }

  // Omitting the callback removes every listener for the event. The pages call
  // it that way, and the previous version silently removed nothing.
  off(event, callback) {
    if (!this.listeners.has(event)) return;

    if (!callback) {
      this.listeners.delete(event);
      return;
    }

    const remaining = this.listeners.get(event).filter((fn) => fn !== callback);
    if (remaining.length) {
      this.listeners.set(event, remaining);
    } else {
      this.listeners.delete(event);
    }
  }

  emit(event, data) {
    const callbacks = this.listeners.get(event);
    if (!callbacks) return;

    // A copy, so a handler that unsubscribes during dispatch cannot make the
    // loop skip the handler after it.
    [...callbacks].forEach((callback) => {
      try {
        callback(data);
      } catch (error) {
        console.error(`Realtime listener for "${event}" failed`, error);
      }
    });
  }
}

export const realtimeService = new RealtimeService();
