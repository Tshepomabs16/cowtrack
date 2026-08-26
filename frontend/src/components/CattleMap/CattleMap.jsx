import React, { useState, useEffect, useRef, useMemo } from 'react';
import { MapContainer, TileLayer, FeatureGroup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import CowMarker from './components/CowMarker';
import CustomDrawControl from './components/CustomDrawControl';
import CowInfoPanel from './components/CowInfoPanel';
import { locationsAPI, cowsAPI } from '../../services/api';
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

const CattleMap = ({ height = 600 }) => {
  const [cows, setCows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedCow, setSelectedCow] = useState(null);

  // Shapes drawn in this session. Drawn geofences are not persisted yet, so this
  // is deliberately local rather than loaded from the API.
  const [geofences, setGeofences] = useState([]);
  const featureGroupRef = useRef();

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      try {
        // Positions come from the location service, status and identity from the
        // cow record; joined on cowId so a marker can be coloured by status.
        const [locationResponse, cowResponse] = await Promise.all([
          locationsAPI.getLiveLocations(),
          cowsAPI.getAll(),
        ]);
        if (cancelled) return;

        const byId = new Map(
          (Array.isArray(cowResponse.data) ? cowResponse.data : []).map(c => [c.cowId, c])
        );

        const positioned = (Array.isArray(locationResponse.data) ? locationResponse.data : [])
          .filter(loc => loc.latitude != null && loc.longitude != null)
          .map(loc => {
            const cow = byId.get(loc.cowId) || {};
            return {
              id: loc.cowId,
              name: cow.name || loc.cowName,
              tagId: cow.tagId,
              breed: cow.breed,
              age: cow.age,
              lat: Number(loc.latitude),
              lng: Number(loc.longitude),
              status: cow.status || 'unknown',
              lastSeen: relativeTime(loc.recordedAt),
              temperature: cow.temperature,
              heartRate: cow.heartRate,
              weight: cow.weight,
            };
          });

        setCows(positioned);
        setError(null);
      } catch (err) {
        console.error('Error loading cattle positions:', err);
        if (!cancelled) setError('Could not load cattle positions');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, []);

  // Centre on the herd rather than a fixed point, so the map is useful wherever
  // the farm actually is.
  const center = useMemo(() => {
    if (!cows.length) return FALLBACK_CENTER;
    const lat = cows.reduce((sum, c) => sum + c.lat, 0) / cows.length;
    const lng = cows.reduce((sum, c) => sum + c.lng, 0) / cows.length;
    return [lat, lng];
  }, [cows]);

  const handleGeofenceCreated = (geofenceData) => {
    setGeofences(previous => [...previous, {
      id: geofenceData.id,
      type: geofenceData.type,
      coordinates: geofenceData.coordinates,
      color: geofenceData.color,
      name: `Geofence ${previous.length + 1}`,
    }]);
  };

  if (loading) {
    return (
      <div className="cattle-map-container">
        <div className="skeleton" style={{ height, borderRadius: 'var(--radius)' }} />
      </div>
    );
  }

  return (
    <div className="cattle-map-container">
      {/* key forces a remount when the centre resolves, since MapContainer
          ignores later changes to its center prop. */}
      <MapContainer
        key={center.join(',')}
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
          <CowMarker key={cow.id} cow={cow} onClick={() => setSelectedCow(cow)} />
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
