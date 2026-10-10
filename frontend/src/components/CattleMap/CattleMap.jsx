import React, { useState, useEffect, useCallback } from 'react';
import { MapContainer, TileLayer } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import CowMarker from './components/CowMarker';
import CustomDrawControl from './components/CustomDrawControl';
import CowInfoPanel from './components/CowInfoPanel';
import FenceLayer from './components/FenceLayer';
import { assignableCamps, describeFence, fenceStyle, shapeFromLayer, FENCE_TYPES } from './geofenceShapes';
import { locationsAPI, cowsAPI, geofencesAPI } from '../../services/api';
import { realtimeService, EVENTS } from '../../services/realtime';
import { useAuth } from '../../context/AuthContext';
import './CattleMap.css';

/** Mirrors the backend: only a farmer or an admin may draw or change fences. */
const MANAGER_ROLES = ['FARMER', 'ADMIN'];

/** The server's own explanation when it gave one, which is usually the useful part. */
const errorMessage = (err, fallback) => err?.response?.data?.message || fallback;

/** Used only when no animal has reported a position yet. */
const FALLBACK_CENTER = [-23.9045, 29.4689];

const relativeTime = (timestamp) => {
  if (!timestamp) return 'never';
  const then = new Date(timestamp);
  if (Number.isNaN(then.getTime())) return 'unknown';

  const minutes = Math.round((Date.now() - then.getTime()) / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes} min ago`;

  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hr ago`;
  return `${Math.round(hours / 24)} d ago`;
};

/** A marker from the live endpoint, which carries what the map draws. */
const toMarker = (live) => ({
  id: live.cowId,
  name: live.cowName,
  tagId: live.tagId,
  status: live.status || 'unknown',
  lat: Number(live.latitude),
  lng: Number(live.longitude),
  lastSeen: relativeTime(live.recordedAt),
});

const CattleMap = ({ height = 600, onTrackedChange }) => {
  const [cows, setCows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedCow, setSelectedCow] = useState(null);

  // Fixed once the herd's first positions arrive. Recomputing it as cattle move
  // would drag the viewport around under the farmer while they are reading it,
  // and MapContainer only honours `center` on mount, so following the herd would
  // mean remounting the map on every position that arrives.
  const [center, setCenter] = useState(null);

  const { user } = useAuth();
  const canManage = MANAGER_ROLES.includes(user?.role);

  // Camps and restricted areas, as saved. A shape just drawn waits in `draft`
  // until it has a name and a kind; only saved fences are drawn on the map.
  const [fences, setFences] = useState([]);
  const [draft, setDraft] = useState(null);
  const [fenceError, setFenceError] = useState(null);
  const [savingFence, setSavingFence] = useState(false);

  const loadFences = useCallback(async () => {
    try {
      const response = await geofencesAPI.list();
      setFences(Array.isArray(response.data) ? response.data : []);
    } catch (err) {
      console.error('Error loading camps:', err);
    }
  }, []);

  const load = useCallback(async ({ isReconnect = false } = {}) => {
    try {
      // One request. This used to also fetch the entire herd to join identity
      // and status onto each position; the live endpoint now carries what a
      // marker needs, and the rest of an animal's detail is fetched only when
      // its marker is clicked.
      const response = await locationsAPI.getLiveLocations();

      const positioned = (Array.isArray(response.data) ? response.data : [])
        .filter(live => live.latitude != null && live.longitude != null)
        .map(toMarker);

      setCows(positioned);
      setCenter(previous => {
        if (previous) return previous;
        if (!positioned.length) return FALLBACK_CENTER;
        return [
          positioned.reduce((sum, c) => sum + c.lat, 0) / positioned.length,
          positioned.reduce((sum, c) => sum + c.lng, 0) / positioned.length,
        ];
      });
      setError(null);
    } catch (err) {
      console.error('Error loading cattle positions:', err);
      // A failed refetch after a reconnect leaves the markers we already have,
      // which are better than an error where a map used to be.
      if (!isReconnect) setError('Could not load cattle positions');
    } finally {
      setLoading(false);
    }
  }, []);

  const applyLocationUpdate = useCallback((location) => {
    if (!location || location.latitude == null || location.longitude == null) return;

    setCows(previous => {
      const index = previous.findIndex(cow => cow.id === location.cowId);

      const moved = {
        lat: Number(location.latitude),
        lng: Number(location.longitude),
        lastSeen: relativeTime(location.recordedAt),
      };

      // An animal reporting for the first time has no marker yet. It arrives
      // with only what a position carries; the rest fills in on the next load.
      if (index === -1) {
        return [...previous, {
          id: location.cowId,
          name: location.cowName,
          status: 'unknown',
          ...moved,
        }];
      }

      // Merged rather than replaced. A pushed position carries where the animal
      // is, not what it is, so overwriting the marker would blank out the tag
      // and the status that colours it.
      const next = [...previous];
      next[index] = { ...previous[index], ...moved };
      return next;
    });
  }, []);

  useEffect(() => {
    let cancelled = false;
    let hasConnectedBefore = false;

    const onUpdate = (location) => {
      if (!cancelled) applyLocationUpdate(location);
    };

    // A dropped stream means positions were missed while it was down, so the
    // snapshot is refetched on every reconnection. The first `connected` is the
    // initial one, which the load below already covers.
    const onConnected = () => {
      if (cancelled) return;
      if (hasConnectedBefore) load({ isReconnect: true });
      hasConnectedBefore = true;
    };

    // A breach raised or cleared changes how many animals a camp has outside.
    const onBreachChange = (alert) => {
      if (!cancelled && alert?.alertType === 'GEOFENCE_BREACH') loadFences();
    };

    realtimeService.on(EVENTS.LOCATION_UPDATE, onUpdate);
    realtimeService.on(EVENTS.CONNECTED, onConnected);
    realtimeService.on(EVENTS.NEW_ALERT, onBreachChange);
    realtimeService.on(EVENTS.ALERT_RESOLVED, onBreachChange);

    load();
    loadFences();

    return () => {
      cancelled = true;
      realtimeService.off(EVENTS.LOCATION_UPDATE, onUpdate);
      realtimeService.off(EVENTS.CONNECTED, onConnected);
      realtimeService.off(EVENTS.NEW_ALERT, onBreachChange);
      realtimeService.off(EVENTS.ALERT_RESOLVED, onBreachChange);
    };
  }, [load, applyLocationUpdate, loadFences]);

  useEffect(() => {
    if (onTrackedChange) onTrackedChange(cows.length);
  }, [cows.length, onTrackedChange]);

  /**
   * Opens the detail panel, then fills in the rest of the animal's record.
   *
   * Breed, age, weight and vital signs are fetched here rather than loaded with
   * the map, because they are only ever read for the one animal a farmer clicks.
   * Fetching them for the herd was most of the cost of drawing it.
   */
  const openCow = useCallback(async (marker) => {
    setSelectedCow(marker);

    try {
      const { data } = await cowsAPI.getById(marker.id);
      setSelectedCow(current => (
        // The farmer may have closed the panel or picked another animal while
        // this was in flight.
        current && current.id === marker.id
          ? {
            ...current,
            breed: data?.breed,
            age: data?.age,
            weight: data?.weight,
            temperature: data?.temperature,
            heartRate: data?.heartRate,
            campId: data?.campId,
            campName: data?.campName,
          }
          : current
      ));
    } catch (err) {
      console.error('Could not load full record for cow', marker.id, err);
    }
  }, []);

  const handleShapeDrawn = useCallback((layerType, layer) => {
    const shape = shapeFromLayer(layerType, layer);
    if (!shape) return;
    setFenceError(null);
    setDraft({ ...shape, name: '', fenceType: FENCE_TYPES.KEEP_IN });
  }, []);

  const saveDraft = async (event) => {
    event.preventDefault();
    if (!draft.name.trim()) {
      setFenceError('Give it a name the farm will recognise');
      return;
    }
    setSavingFence(true);
    try {
      const { data } = await geofencesAPI.create({ ...draft, name: draft.name.trim() });
      setFences(previous => [data, ...previous]);
      setDraft(null);
      setFenceError(null);
    } catch (err) {
      setFenceError(errorMessage(err, 'Could not save that shape'));
    } finally {
      setSavingFence(false);
    }
  };

  /** Runs a change to one fence and puts the server's answer in its place. */
  const changeFence = async (action, fence) => {
    setFenceError(null);
    try {
      if (action === 'remove') {
        const label = fence.fenceType === FENCE_TYPES.KEEP_OUT ? 'restricted area' : 'camp';
        if (!window.confirm(`Remove ${label} '${fence.name}'?`)) return;
        await geofencesAPI.remove(fence.geofenceId);
        setFences(previous => previous.filter(f => f.geofenceId !== fence.geofenceId));
        return;
      }
      const { data } = action === 'activate'
        ? await geofencesAPI.activate(fence.geofenceId)
        : await geofencesAPI.deactivate(fence.geofenceId);
      setFences(previous => previous.map(f => (f.geofenceId === data.geofenceId ? data : f)));
    } catch (err) {
      setFenceError(errorMessage(err, 'That change could not be made'));
    }
  };

  /** Moves the open animal into a camp, or out of its current one. */
  const moveCow = async (cow, campId) => {
    try {
      if (campId) {
        await geofencesAPI.moveAnimals(campId, [cow.id]);
      } else if (cow.campId) {
        await geofencesAPI.takeOutAnimal(cow.campId, cow.id);
      }
      const camp = fences.find(f => f.geofenceId === campId);
      setSelectedCow(current => (current && current.id === cow.id
        ? { ...current, campId: campId || null, campName: camp ? camp.name : null }
        : current));
      loadFences();
      return null;
    } catch (err) {
      return errorMessage(err, 'Could not move this animal');
    }
  };

  if (loading || !center) {
    return (
      <div className="cattle-map-container">
        <div className="skeleton" style={{ height, borderRadius: 'var(--radius)' }} />
      </div>
    );
  }

  return (
    <div className="cattle-map-container">
      <MapContainer
        center={center}
        zoom={15}
        style={{ height: `${height}px`, width: '100%' }}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        />

        <FenceLayer fences={fences} />

        {cows.map(cow => (
          <CowMarker key={cow.id} cow={cow} onClick={() => openCow(cow)} />
        ))}

        {canManage && <CustomDrawControl onCreated={handleShapeDrawn} />}
      </MapContainer>

      {selectedCow && (
        <CowInfoPanel
          cow={selectedCow}
          camps={assignableCamps(fences)}
          canMove={canManage}
          onMove={moveCow}
          onClose={() => setSelectedCow(null)}
        />
      )}

      <div className="map-controls">
        <div className="controls-panel">
          <h4>Tracked</h4>
          <p>
            {error
              ? error
              : cows.length
                ? `${cows.length} animal${cows.length === 1 ? '' : 's'} reporting`
                : 'No positions reported yet'}
          </p>

          {draft && (
            <form className="fence-draft" onSubmit={saveDraft}>
              <h5>New {draft.shape === 'CIRCLE' ? 'circle' : 'shape'}</h5>
              <input
                type="text"
                placeholder="Name, e.g. North camp"
                value={draft.name}
                maxLength={120}
                autoFocus
                onChange={e => setDraft({ ...draft, name: e.target.value })}
              />
              <select
                value={draft.fenceType}
                onChange={e => setDraft({ ...draft, fenceType: e.target.value })}
              >
                <option value={FENCE_TYPES.KEEP_IN}>Camp: alert when an animal leaves</option>
                <option value={FENCE_TYPES.KEEP_OUT}>Restricted area: alert when one enters</option>
              </select>
              <div className="fence-draft-actions">
                <button type="submit" className="fence-btn primary" disabled={savingFence}>
                  {savingFence ? 'Saving…' : 'Save'}
                </button>
                <button type="button" className="fence-btn" onClick={() => setDraft(null)}>
                  Cancel
                </button>
              </div>
            </form>
          )}

          {fenceError && <p className="fence-error" role="alert">{fenceError}</p>}

          <div className="geofence-list">
            <h5>Camps &amp; areas ({fences.length})</h5>
            {fences.length === 0 && (
              <p className="fence-empty">
                {canManage
                  ? 'Draw a camp with the shape tools on the left of the map.'
                  : 'No camps have been drawn yet.'}
              </p>
            )}
            {fences.map(fence => (
              <div key={fence.geofenceId} className="geofence-item">
                <span className="fence-swatch" style={{ borderColor: fenceStyle(fence).color }} />
                <div className="fence-text">
                  <span className="fence-name">{fence.name}</span>
                  <span className={`fence-meta ${fence.animalsOutside > 0 ? 'warn' : ''}`}>
                    {describeFence(fence)}
                  </span>
                </div>
                {canManage && (
                  <div className="fence-actions">
                    <button
                      type="button"
                      className="fence-btn"
                      onClick={() => changeFence(fence.isActive === false ? 'activate' : 'deactivate', fence)}
                    >
                      {fence.isActive === false ? 'Switch on' : 'Switch off'}
                    </button>
                    <button type="button" className="fence-btn danger" onClick={() => changeFence('remove', fence)}>
                      Remove
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CattleMap;
