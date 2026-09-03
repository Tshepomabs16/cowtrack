import React, { useState, useEffect, useRef, useCallback } from 'react';
import { MapContainer, TileLayer, FeatureGroup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import CowMarker from './components/CowMarker';
import CustomDrawControl from './components/CustomDrawControl';
import CowInfoPanel from './components/CowInfoPanel';
import { locationsAPI, cowsAPI } from '../../services/api';
import { realtimeService, EVENTS } from '../../services/realtime';
import './CattleMap.css';

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

  // Shapes drawn in this session. Drawn geofences are not persisted yet, so this
  // is deliberately local rather than loaded from the API.
  const [geofences, setGeofences] = useState([]);
  const featureGroupRef = useRef();

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

    realtimeService.on(EVENTS.LOCATION_UPDATE, onUpdate);
    realtimeService.on(EVENTS.CONNECTED, onConnected);

    load();

    return () => {
      cancelled = true;
      realtimeService.off(EVENTS.LOCATION_UPDATE, onUpdate);
      realtimeService.off(EVENTS.CONNECTED, onConnected);
    };
  }, [load, applyLocationUpdate]);

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
          }
          : current
      ));
    } catch (err) {
      console.error('Could not load full record for cow', marker.id, err);
    }
  }, []);

  const handleGeofenceCreated = (geofenceData) => {
    setGeofences(previous => [...previous, {
      id: geofenceData.id,
      type: geofenceData.type,
      coordinates: geofenceData.coordinates,
      color: geofenceData.color,
      name: `Geofence ${previous.length + 1}`,
    }]);
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

        <FeatureGroup ref={featureGroupRef} />

        {cows.map(cow => (
          <CowMarker key={cow.id} cow={cow} onClick={() => openCow(cow)} />
        ))}

        <CustomDrawControl
          onCreated={handleGeofenceCreated}
          onEdited={() => {}}
          onDeleted={() => {}}
        />
      </MapContainer>

      {selectedCow && (
        <CowInfoPanel cow={selectedCow} onClose={() => setSelectedCow(null)} />
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
          {geofences.length > 0 && (
            <div className="geofence-list">
              <h5>Drawn shapes ({geofences.length})</h5>
              {geofences.map(gf => (
                <div key={gf.id} className="geofence-item">
                  <span style={{ color: gf.color }}>●</span>
                  <span>{gf.name}</span>
                  <button
                    className="remove-btn"
                    onClick={() => setGeofences(geofences.filter(g => g.id !== gf.id))}
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CattleMap;
