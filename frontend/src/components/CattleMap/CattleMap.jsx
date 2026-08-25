import React, { useState, useRef } from 'react';
import { MapContainer, TileLayer, FeatureGroup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import CowMarker from './components/CowMarker';
import CustomDrawControl from './components/CustomDrawControl';
import CowInfoPanel from './components/CowInfoPanel';
import './CattleMap.css';

const CattleMap = () => {
  const [cows, setCows] = useState([
    { id: 1, name: "Bessie-001", lat: -1.2921, lng: 36.8219, status: "healthy", lastSeen: "10 min ago" },
    { id: 2, name: "Daisy-002", lat: -1.2915, lng: 36.8225, status: "feeding", lastSeen: "5 min ago" },
    { id: 3, name: "Moo-003", lat: -1.2930, lng: 36.8205, status: "inactive", lastSeen: "15 min ago" },
  ]);

  const [selectedCow, setSelectedCow] = useState(null);
  const [geofences, setGeofences] = useState([]);
  const featureGroupRef = useRef();

  const center = [-1.2921, 36.8219];

  const handleCowClick = (cow) => {
    setSelectedCow(cow);
  };

  const handleGeofenceCreated = (geofenceData) => {
    const newGeofence = {
      id: geofenceData.id,
      type: geofenceData.type,
      coordinates: geofenceData.coordinates,
      color: geofenceData.color,
      name: `Geofence ${geofences.length + 1}`
    };
    setGeofences([...geofences, newGeofence]);
  };

  const handleGeofenceEdited = (e) => {
    console.log('Geofence edited:', e);
    // Update your state here
  };

  const handleGeofenceDeleted = (e) => {
    console.log('Geofence deleted:', e);
    // Update your state here
  };

  return (
    <div className="cattle-map-container">
      <MapContainer
        center={center}
        zoom={16}
        style={{ height: "600px", width: "100%" }}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        />

        {/* FeatureGroup for drawing */}
        <FeatureGroup ref={featureGroupRef}>
          {/* Existing geofences */}
          {geofences.map(geofence => (
            <CowMarker
              key={geofence.id}
              geofence={geofence}
              // Render geofence shapes here if needed
            />
          ))}
        </FeatureGroup>

        {/* Cow Markers */}
        {cows.map(cow => (
          <CowMarker
            key={cow.id}
            cow={cow}
            onClick={() => handleCowClick(cow)}
          />
        ))}

        {/* Custom Draw Control */}
        <CustomDrawControl
          onCreated={handleGeofenceCreated}
          onEdited={handleGeofenceEdited}
          onDeleted={handleGeofenceDeleted}
        />
      </MapContainer>

      {/* Cow Info Panel */}
      {selectedCow && (
        <CowInfoPanel
          cow={selectedCow}
          onClose={() => setSelectedCow(null)}
          onViewInfo={() => console.log(`View info for ${selectedCow.id}`)}
        />
      )}

      {/* Map Controls */}
      <div className="map-controls">
        <div className="controls-panel">
          <h4>Drawing Tools</h4>
          <p>Use the toolbar on the map to draw custom shapes</p>
          <div className="geofence-list">
            <h5>Geofences ({geofences.length})</h5>
            {geofences.map(gf => (
              <div key={gf.id} className="geofence-item">
                <span style={{color: gf.color}}>●</span>
                <span>{gf.name}</span>
                <button
                  onClick={() => setGeofences(geofences.filter(g => g.id !== gf.id))}
                  className="remove-btn"
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CattleMap;