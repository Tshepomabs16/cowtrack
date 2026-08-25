import React from 'react';
import './AlertFeed.css';

const AlertFeed = () => {
  const alerts = [
    {
      id: 1,
      cow: 'Milky',
      type: 'geofence_breach',
      message: 'Left geofence area',
      time: '10:30 AM',
      resolved: false,
    },
    {
      id: 2,
      cow: 'Butter',
      type: 'no_signal',
      message: 'No GPS signal for 24h',
      time: '08:15 AM',
      resolved: false,
    },
    {
      id: 3,
      cow: 'Milky',
      type: 'health_alert',
      message: 'Temperature high',
      time: 'Yesterday',
      resolved: true,
    },
    {
      id: 4,
      cow: 'Daisy',
      type: 'reminder',
      message: 'Vaccination due tomorrow',
      time: 'Yesterday',
      resolved: false,
    },
  ];

  const getTypeIcon = (type) => {
    switch (type) {
      case 'geofence_breach': return '📍';
      case 'no_signal': return '📡';
      case 'health_alert': return '🏥';
      case 'reminder': return '⏰';
      default: return 'ℹ️';
    }
  };

  const getTypeColor = (type) => {
    switch (type) {
      case 'geofence_breach': return '#ff4444';
      case 'no_signal': return '#ffaa00';
      case 'health_alert': return '#0088ff';
      case 'reminder': return '#00ff88';
      default: return '#b0b0d0';
    }
  };

  return (
    <div className="alert-feed">
      {alerts.map((alert) => (
        <div
          key={alert.id}
          className={`alert-item ${alert.resolved ? 'resolved' : ''}`}
          style={{ borderLeftColor: getTypeColor(alert.type) }}
        >
          <div className="alert-icon" style={{ color: getTypeColor(alert.type) }}>
            {getTypeIcon(alert.type)}
          </div>
          <div className="alert-content">
            <div className="alert-header">
              <span className="alert-cow">{alert.cow}</span>
              <span className="alert-time">{alert.time}</span>
            </div>
            <div className="alert-message">{alert.message}</div>
            {alert.resolved && (
              <div className="alert-resolved">✅ Resolved</div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default AlertFeed;