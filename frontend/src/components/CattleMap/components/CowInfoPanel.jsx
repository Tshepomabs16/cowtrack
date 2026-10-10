import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaTimes, FaHeartbeat, FaTemperatureHigh, FaWeight, FaMapMarkerAlt } from 'react-icons/fa';
import './CowInfoPanel.css';

/** Renders a dash rather than a partial string when a reading is missing. */
const value = (reading, suffix) =>
  reading === null || reading === undefined || reading === '' ? '—' : `${reading}${suffix}`;

const CowInfoPanel = ({ cow, camps = [], canMove = false, onMove, onClose }) => {
  const navigate = useNavigate();
  const [moving, setMoving] = useState(false);
  const [moveError, setMoveError] = useState(null);

  if (!cow) return null;

  const handleCampChange = async (event) => {
    const campId = event.target.value ? Number(event.target.value) : null;
    setMoving(true);
    setMoveError(null);
    const problem = await onMove(cow, campId);
    setMoveError(problem);
    setMoving(false);
  };

  // The animal's camp may be switched off, and so missing from the choices,
  // while it is still in it; it is listed anyway so the select shows the truth.
  const campChoices = cow.campId && !camps.some(camp => camp.geofenceId === cow.campId)
    ? [{ geofenceId: cow.campId, name: cow.campName }, ...camps]
    : camps;

  return (
    <div className="cow-info-panel">
      <div className="panel-header">
        <h3>{cow.name} - Details</h3>
        <button className="close-btn" onClick={onClose} aria-label="Close details">
          <FaTimes />
        </button>
      </div>

      <div className="panel-body">
        <div className="cow-stats">
          <div className="stat-item">
            <FaTemperatureHigh className="stat-icon" />
            <span className="stat-value">{value(cow.temperature, '°C')}</span>
            <span className="stat-label">Temperature</span>
          </div>

          <div className="stat-item">
            <FaHeartbeat className="stat-icon" />
            <span className="stat-value">{value(cow.heartRate, ' bpm')}</span>
            <span className="stat-label">Heart Rate</span>
          </div>

          <div className="stat-item">
            <FaWeight className="stat-icon" />
            <span className="stat-value">{value(cow.weight, ' kg')}</span>
            <span className="stat-label">Weight</span>
          </div>

          <div className="stat-item">
            <FaMapMarkerAlt className="stat-icon" />
            <span className={`status-badge ${cow.status || ''}`}>
              {cow.status || 'unknown'}
            </span>
            <span className="stat-label">Status</span>
          </div>
        </div>

        <div className="cow-details">
          <div className="detail-row">
            <span className="detail-label">Tag:</span>
            <span className="detail-value">{cow.tagId || '—'}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Breed:</span>
            <span className="detail-value">{cow.breed || '—'}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Age:</span>
            <span className="detail-value">{value(cow.age, ' years')}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Camp:</span>
            {canMove && onMove ? (
              <select
                className="camp-select"
                value={cow.campId || ''}
                disabled={moving}
                onChange={handleCampChange}
                aria-label="Camp"
              >
                <option value="">Not in a camp</option>
                {campChoices.map(camp => (
                  <option key={camp.geofenceId} value={camp.geofenceId}>{camp.name}</option>
                ))}
              </select>
            ) : (
              <span className="detail-value">{cow.campName || 'Not in a camp'}</span>
            )}
          </div>
          {moveError && <p className="camp-error" role="alert">{moveError}</p>}
          <div className="detail-row">
            <span className="detail-label">Last seen:</span>
            <span className="detail-value">{cow.lastSeen || '—'}</span>
          </div>
        </div>

        <div className="panel-actions">
          <button className="btn-primary" onClick={() => navigate(`/cows/${cow.id}`)}>
            View Full History
          </button>
        </div>
      </div>
    </div>
  );
};

export default CowInfoPanel;
