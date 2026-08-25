import React from 'react';
import { FaTimes, FaHeartbeat, FaTemperatureHigh, FaWeight, FaMapMarkerAlt } from 'react-icons/fa';
import './CowInfoPanel.css';

const CowInfoPanel = ({ cow, onClose, onViewInfo }) => {
  // Mock data - replace with API call
  const cowDetails = {
    temperature: "38.5°C",
    heartRate: "65 bpm",
    weight: "450 kg",
    breed: "Holstein",
    age: "3 years",
    lastFeeding: "2 hours ago",
    healthScore: 92
  };

  return (
    <div className="cow-info-panel">
      <div className="panel-header">
        <h3>{cow.name} - Details</h3>
        <button className="close-btn" onClick={onClose}>
          <FaTimes />
        </button>
      </div>

      <div className="panel-body">
        <div className="cow-stats">
          <div className="stat-item">
            <FaTemperatureHigh className="stat-icon" />
            <span className="stat-value">{cowDetails.temperature}</span>
            <span className="stat-label">Temperature</span>
          </div>

          <div className="stat-item">
            <FaHeartbeat className="stat-icon" />
            <span className="stat-value">{cowDetails.heartRate}</span>
            <span className="stat-label">Heart Rate</span>
          </div>

          <div className="stat-item">
            <FaWeight className="stat-icon" />
            <span className="stat-value">{cowDetails.weight}</span>
            <span className="stat-label">Weight</span>
          </div>

          <div className="stat-item">
            <FaMapMarkerAlt className="stat-icon" />
            <span className="stat-value">Active</span>
            <span className="stat-label">Status</span>
          </div>
        </div>

        <div className="cow-details">
          <div className="detail-row">
            <span className="detail-label">Breed:</span>
            <span className="detail-value">{cowDetails.breed}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Age:</span>
            <span className="detail-value">{cowDetails.age}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Last Feeding:</span>
            <span className="detail-value">{cowDetails.lastFeeding}</span>
          </div>
          <div className="detail-row">
            <span className="detail-label">Health Score:</span>
            <span className="detail-value health-score">{cowDetails.healthScore}/100</span>
          </div>
        </div>

        <div className="panel-actions">
          <button className="btn-primary" onClick={onViewInfo}>
            View Full History
          </button>
          <button className="btn-secondary">
            Generate Health Report
          </button>
          <button className="btn-alert">
            Send Alert
          </button>
        </div>
      </div>
    </div>
  );
};

export default CowInfoPanel;