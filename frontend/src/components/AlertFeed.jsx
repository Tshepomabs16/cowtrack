import React, { useState, useEffect } from 'react';
import { alertsAPI } from '../services/api';
import './AlertFeed.css';

/** Compact list of the most recent alerts, for the dashboard. */
const AlertFeed = ({ limit = 6 }) => {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      try {
        // A feed, so one page of the most recent is all it ever shows.
        const response = await alertsAPI.getPage({ size: 20 });
        if (cancelled) return;
        const content = response.data?.content;
        setAlerts(Array.isArray(content) ? content : []);
      } catch (err) {
        console.error('Error loading alerts:', err);
        if (!cancelled) setError('Could not load alerts');
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, []);

  const getTypeIcon = (type) => {
    switch (type) {
      case 'GEOFENCE_BREACH': return '📍';
      case 'NO_SIGNAL': return '📡';
      case 'DEVICE_REMOVED': return '🔓';
      case 'NIGHT_MOVEMENT': return '🌙';
      default: return 'ℹ️';
    }
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'critical': return '#ff4444';
      case 'high': return '#ffaa00';
      case 'medium': return '#0088ff';
      case 'low': return '#00ff88';
      default: return '#b0b0d0';
    }
  };

  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) return '';

    const isToday = date.toDateString() === new Date().toDateString();
    return isToday
      ? date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : date.toLocaleDateString();
  };

  if (loading) {
    return (
      <div className="alert-feed">
        {[1, 2, 3].map(i => (
          <div key={i} className="alert-item" style={{ opacity: 1 }}>
            <div className="skeleton" style={{ width: 32, height: 32, borderRadius: 6, flexShrink: 0 }} />
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <div className="skeleton" style={{ width: '40%', height: 12 }} />
              <div className="skeleton" style={{ width: '80%', height: 10 }} />
              <div className="skeleton" style={{ width: '25%', height: 10 }} />
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return <div className="alert-feed"><p className="alert-empty">{error}</p></div>;
  }

  if (!alerts.length) {
    return <div className="alert-feed"><p className="alert-empty">No alerts. All quiet.</p></div>;
  }

  return (
    <div className="alert-feed">
      {alerts.slice(0, limit).map((alert) => (
        <div
          key={alert.alertId}
          className={`alert-item ${alert.isResolved ? 'resolved' : ''}`}
          style={{ borderLeftColor: getSeverityColor(alert.severity) }}
        >
          <div className="alert-icon" style={{ color: getSeverityColor(alert.severity) }}>
            {getTypeIcon(alert.alertType)}
          </div>
          <div className="alert-content">
            <div className="alert-header">
              <span className="alert-cow">{alert.cowName}</span>
              <span className="alert-time">{formatTime(alert.createdAt)}</span>
            </div>
            <div className="alert-message">{alert.message || alert.title}</div>
            {alert.isResolved && (
              <div className="alert-resolved">✅ Resolved</div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default AlertFeed;
