import React from 'react';
import { Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { FaCow } from 'react-icons/fa';

// Custom cow icon
const createCowIcon = (status) => {
  let color = '#4CAF50'; // green for healthy

  switch(status) {
    case 'inactive': color = '#FF9800'; break; // orange
    case 'feeding': color = '#2196F3'; break; // blue
    case 'alert': color = '#F44336'; break; // red
  }

  return L.divIcon({
    html: `<div style="
      background: ${color};
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      border: 2px solid white;
      box-shadow: 0 2px 5px rgba(0,0,0,0.3);
    ">
      <svg style="width: 18px; height: 18px;" viewBox="0 0 24 24">
        <path fill="currentColor" d="M18.5,12A2.5,2.5 0 0,0 21,9.5A2.5,2.5 0 0,0 18.5,7A2.5,2.5 0 0,0 16,9.5A2.5,2.5 0 0,0 18.5,12M12,10A2,2 0 0,0 14,12A2,2 0 0,0 16,10A2,2 0 0,0 14,8A2,2 0 0,0 12,10M6.5,12A2.5,2.5 0 0,0 9,9.5A2.5,2.5 0 0,0 6.5,7A2.5,2.5 0 0,0 4,9.5A2.5,2.5 0 0,0 6.5,12M18.5,14C16.8,14 15.5,15.3 15.5,17C15.5,18.7 16.8,20 18.5,20C20.2,20 21.5,18.7 21.5,17C21.5,15.3 20.2,14 18.5,14M12,14C10.9,14 10,14.9 10,16C10,17.1 10.9,18 12,18C13.1,18 14,17.1 14,16C14,14.9 13.1,14 12,14M6.5,14C4.8,14 3.5,15.3 3.5,17C3.5,18.7 4.8,20 6.5,20C8.2,20 9.5,18.7 9.5,17C9.5,15.3 8.2,14 6.5,14Z" />
      </svg>
    </div>`,
    className: '',
    iconSize: [32, 32],
    iconAnchor: [16, 16]
  });
};

const CowMarker = ({ cow, onClick }) => {
  return (
    <Marker
      position={[cow.lat, cow.lng]}
      icon={createCowIcon(cow.status)}
      eventHandlers={{
        click: onClick
      }}
    >
      <Popup>
        <div className="cow-popup">
          <h4>{cow.name}</h4>
          <p>Status: <span className={`status-${cow.status}`}>{cow.status}</span></p>
          <p>Last seen: {cow.lastSeen}</p>
          <button
            className="view-info-btn"
            onClick={(e) => {
              e.stopPropagation();
              // This will trigger from parent
            }}
          >
            View Details
          </button>
        </div>
      </Popup>
    </Marker>
  );
};

export default CowMarker;