import React, { useState } from 'react';
import React, { useState, useEffect, useCallback } from 'react';
import { wsService, WS_EVENTS } from '../services/websocket';
import { cowsAPI, locationsAPI } from '../services/api';
import { FiBell, FiCheckCircle, FiAlertCircle, FiClock } from 'react-icons/fi';
import './LiveMap.css';

const LiveMap = () => {
  const [cows, setCows] = useState([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    // Fetch initial cow locations
    fetchCowLocations();

    // Connect to WebSocket
    wsService.connect();

    // Set up WebSocket listeners
    wsService.on(WS_EVENTS.CONNECTED, () => setIsConnected(true));
    wsService.on(WS_EVENTS.DISCONNECTED, () => setIsConnected(false));
    wsService.on(WS_EVENTS.LOCATION_UPDATE, handleLocationUpdate);
    wsService.on(WS_EVENTS.HEALTH_ALERT, handleHealthAlert);

    // Clean up on unmount
    return () => {
      wsService.off(WS_EVENTS.CONNECTED);
      wsService.off(WS_EVENTS.DISCONNECTED);
      wsService.off(WS_EVENTS.LOCATION_UPDATE);
      wsService.off(WS_EVENTS.HEALTH_ALERT);
      wsService.disconnect();
    };
  }, []);

  const fetchCowLocations = async () => {
    try {
      const response = await locationsAPI.getLiveLocations();
      setCows(response.data);
    } catch (error) {
      console.error('Error fetching cow locations:', error);
    }
  };

  const handleLocationUpdate = useCallback((update) => {
    setCows(prevCows =>
      prevCows.map(cow =>
        cow.id === update.cowId ? { ...cow, ...update.location } : cow
      )
    );
  }, []);

  const handleHealthAlert = useCallback((alert) => {
    // Show notification for health alert
    console.log('Health alert received:', alert);
    // You can add a notification system here
  }, []);

const Alerts = () => {
  const [alerts, setAlerts] = useState([
    { id: 1, type: 'health', severity: 'high', title: 'High Temperature Alert', message: 'Cow CT-003 shows elevated temperature (39.8°C)', time: '10 minutes ago', resolved: false, cowId: 3 },
    { id: 2, type: 'location', severity: 'medium', title: 'Geofence Breach', message: 'Cow CT-005 left designated area', time: '45 minutes ago', resolved: false, cowId: 5 },
    { id: 3, type: 'health', severity: 'low', title: 'Irregular Feeding Pattern', message: 'Cow CT-002 missed scheduled feeding', time: '2 hours ago', resolved: true, cowId: 2 },
    { id: 4, type: 'system', severity: 'medium', title: 'GPS Signal Lost', message: 'Lost connection with collar #CT-007', time: '3 hours ago', resolved: false, cowId: 7 },
    { id: 5, type: 'health', severity: 'high', title: 'Heart Rate Spike', message: 'Cow CT-001 heart rate at 120 bpm', time: '5 hours ago', resolved: true, cowId: 1 },
  ]);

  const [filter, setFilter] = useState('all');

  const handleResolveAlert = (id) => {
    setAlerts(alerts.map(alert =>
      alert.id === id ? { ...alert, resolved: true } : alert
    ));
  };

  const filteredAlerts = alerts.filter(alert => {
    if (filter === 'all') return true;
    if (filter === 'unresolved') return !alert.resolved;
    if (filter === 'resolved') return alert.resolved;
    return alert.type === filter;
  });

  const getSeverityIcon = (severity) => {
    switch(severity) {
      case 'high': return <FiAlertCircle className="severity-high" />;
      case 'medium': return <FiAlertCircle className="severity-medium" />;
      case 'low': return <FiAlertCircle className="severity-low" />;
      default: return <FiBell />;
    }
  };

  return (
    <div className="alerts-page">
      <div className="page-header">
        <div className="header-left">
          <h1><FiBell /> Alert Management</h1>
          <p>Monitor and respond to system alerts</p>
        </div>
        <div className="header-right">
          <button className="btn-primary">Configure Alerts</button>
          <button className="btn-secondary">Export Logs</button>
        </div>
      </div>

      <div className="alerts-stats">
        <div className="stat-card total">
          <h3>Total Alerts</h3>
          <p className="stat-number">{alerts.length}</p>
        </div>
        <div className="stat-card unresolved">
          <h3>Unresolved</h3>
          <p className="stat-number">{alerts.filter(a => !a.resolved).length}</p>
        </div>
        <div className="stat-card high">
          <h3>High Priority</h3>
          <p className="stat-number">{alerts.filter(a => a.severity === 'high').length}</p>
        </div>
        <div className="stat-card health">
          <h3>Health Alerts</h3>
          <p className="stat-number">{alerts.filter(a => a.type === 'health').length}</p>
        </div>
      </div>

      <div className="alerts-filters">
        <div className="filter-buttons">
          <button
            className={filter === 'all' ? 'active' : ''}
            onClick={() => setFilter('all')}
          >
            All Alerts
          </button>
          <button
            className={filter === 'unresolved' ? 'active' : ''}
            onClick={() => setFilter('unresolved')}
          >
            Unresolved
          </button>
          <button
            className={filter === 'health' ? 'active' : ''}
            onClick={() => setFilter('health')}
          >
            Health
          </button>
          <button
            className={filter === 'location' ? 'active' : ''}
            onClick={() => setFilter('location')}
          >
            Location
          </button>
          <button
            className={filter === 'resolved' ? 'active' : ''}
            onClick={() => setFilter('resolved')}
          >
            Resolved
          </button>
        </div>
      </div>

      <div className="alerts-list">
        {filteredAlerts.map(alert => (
          <div key={alert.id} className={`alert-card ${alert.severity} ${alert.resolved ? 'resolved' : ''}`}>
            <div className="alert-icon">
              {getSeverityIcon(alert.severity)}
            </div>

            <div className="alert-content">
              <div className="alert-header">
                <h4>{alert.title}</h4>
                <span className={`severity-badge ${alert.severity}`}>
                  {alert.severity.toUpperCase()}
                </span>
              </div>

              <p className="alert-message">{alert.message}</p>

              <div className="alert-footer">
                <span className="alert-time">
                  <FiClock /> {alert.time}
                </span>
                <span className="alert-cow">
                  Cow: <strong>CT-00{alert.cowId}</strong>
                </span>
              </div>
            </div>

            <div className="alert-actions">
              {!alert.resolved ? (
                <>
                  <button
                    className="btn-resolve"
                    onClick={() => handleResolveAlert(alert.id)}
                  >
                    <FiCheckCircle /> Mark Resolved
                  </button>
                  <button className="btn-view">View Details</button>
                  <button className="btn-ignore">Ignore</button>
                </>
              ) : (
                <span className="resolved-badge">
                  <FiCheckCircle /> Resolved
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Alerts;