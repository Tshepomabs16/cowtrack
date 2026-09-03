import React, { useState, useEffect } from 'react';
import { useFetch } from '../hooks/useFetch';
import { alertsAPI } from '../services/api';
import { realtimeService, EVENTS } from '../services/realtime';
import {
  FiBell,
  FiCheckCircle,
  FiAlertCircle,
  FiClock,
  FiFilter,
  FiDownload,
  FiEye,
  FiTrash2,
  FiRefreshCw,
  FiXCircle
} from 'react-icons/fi';
import { FaTemperatureHigh, FaHeartbeat, FaMapMarkerAlt } from 'react-icons/fa';
import './Alerts.css';

/**
 * The alerts endpoint returns a page; this screen works on a plain list, so the
 * envelope is unwrapped here.
 *
 * Declared outside the component on purpose: useFetch takes this as a dependency,
 * and a function rebuilt on every render would refetch on every render.
 *
 * The size is a bound, not a page size — this screen has no paging controls, so
 * it shows the most recent alerts and would need them to go further back.
 */
const fetchAlerts = (params) =>
  alertsAPI.getPage(params).then(response => ({
    ...response,
    data: response.data?.content ?? [],
  }));

const Alerts = () => {
  const [filter, setFilter] = useState('all');
  const [selectedAlerts, setSelectedAlerts] = useState([]);
  const [showResolved, setShowResolved] = useState(false);

  // Use the custom hook for fetching alerts
  const {
    data: alerts,
    loading,
    error,
    refresh,
    setData: setAlerts
  } = useFetch(fetchAlerts, { size: 100 }, [], true);

  // Listen for real-time alerts
  useEffect(() => {
    const handleNewAlert = (newAlert) => {
      setAlerts(prev => {
        // A push can race the fetch that already included it, so the same alert
        // would otherwise appear twice with the same React key.
        if (prev.some(alert => alert.alertId === newAlert.alertId)) return prev;
        return [newAlert, ...prev];
      });
    };

    realtimeService.on(EVENTS.NEW_ALERT, handleNewAlert);

    return () => {
      realtimeService.off(EVENTS.NEW_ALERT, handleNewAlert);
    };
  }, [setAlerts]);

  const handleResolveAlert = async (id) => {
    try {
      await alertsAPI.markAsRead(id);
      setAlerts(alerts.map(alert =>
        alert.alertId === id ? { ...alert, isResolved: true } : alert
      ));
    } catch (error) {
      console.error('Error resolving alert:', error);
      alert('Failed to resolve alert');
    }
  };

  const handleResolveSelected = () => {
    setAlerts(alerts.map(alert =>
      selectedAlerts.includes(alert.alertId) ? { ...alert, isResolved: true } : alert
    ));
    setSelectedAlerts([]);
  };

  const handleDeleteAlert = (id) => {
    if (window.confirm('Are you sure you want to delete this alert?')) {
      setAlerts(alerts.filter(alert => alert.alertId !== id));
    }
  };

  const handleDeleteSelected = () => {
    if (window.confirm(`Delete ${selectedAlerts.length} selected alerts?`)) {
      setAlerts(alerts.filter(alert => !selectedAlerts.includes(alert.alertId)));
      setSelectedAlerts([]);
    }
  };

  const handleSelectAll = () => {
    if (selectedAlerts.length === filteredAlerts.length) {
      setSelectedAlerts([]);
    } else {
      setSelectedAlerts(filteredAlerts.map(alert => alert.alertId));
    }
  };

  const handleSelectAlert = (id) => {
    if (selectedAlerts.includes(id)) {
      setSelectedAlerts(selectedAlerts.filter(alertId => alertId !== id));
    } else {
      setSelectedAlerts([...selectedAlerts, id]);
    }
  };

  const getSeverityIcon = (severity) => {
    switch(severity) {
      case 'critical': return <FiAlertCircle className="severity-icon critical" />;
      case 'high': return <FiAlertCircle className="severity-icon high" />;
      case 'medium': return <FiAlertCircle className="severity-icon medium" />;
      case 'low': return <FiAlertCircle className="severity-icon low" />;
      default: return <FiBell className="severity-icon" />;
    }
  };

  const getTypeIcon = (type) => {
    switch(type) {
      case 'health': return <FaHeartbeat className="type-icon health" />;
      case 'location': return <FaMapMarkerAlt className="type-icon location" />;
      case 'system': return <FiAlertCircle className="type-icon system" />;
      case 'environment': return <FaTemperatureHigh className="type-icon environment" />;
      default: return <FiBell className="type-icon" />;
    }
  };

  const filteredAlerts = alerts.filter(alert => {
    if (filter === 'all') return true;
    if (filter === 'unresolved') return !alert.isResolved;
    if (filter === 'resolved') return alert.isResolved;
    if (filter === 'critical') return alert.severity === 'critical';
    if (filter === 'health') return alert.alertType === 'health';
    if (filter === 'location') return alert.alertType === 'location';
    return alert.alertType === filter;
  }).filter(alert => showResolved ? true : !alert.isResolved);

  const stats = {
    total: alerts.length,
    unresolved: alerts.filter(a => !a.isResolved).length,
    critical: alerts.filter(a => a.severity === 'critical' && !a.isResolved).length,
    health: alerts.filter(a => a.alertType === 'health' && !a.isResolved).length,
    today: alerts.filter(a => {
      // createdAt arrives as an ISO string, so it has to be parsed first.
      if (!a.createdAt) return false;
      const alertDate = new Date(a.createdAt);
      return alertDate.toDateString() === new Date().toDateString() && !a.isResolved;
    }).length,
  };

  if (loading) {
    return (
      <div className="alerts-page page-enter">
        <div className="alerts-header">
          <div className="header-left">
            <div className="skeleton" style={{ width: 220, height: 28, marginBottom: 8 }} />
            <div className="skeleton" style={{ width: 160, height: 14 }} />
          </div>
        </div>
        <div className="alerts-stats">
          {[1, 2, 3, 4, 5].map(i => (
            <div key={i} className="skeleton" style={{ height: 90, borderRadius: 'var(--radius)' }} />
          ))}
        </div>
        <div className="skeleton" style={{ height: 60, borderRadius: 'var(--radius)', marginBottom: 20 }} />
        <div className="skeleton" style={{ height: 300, borderRadius: 'var(--radius)' }} />
      </div>
    );
  }

  return (
    <div className="alerts-page page-enter">
      {/* Header */}
      <div className="alerts-header">
        <div className="header-left">
          <h1><FiBell /> Alert Management</h1>
          <p>Monitor and respond to system alerts</p>
        </div>
        <div className="header-actions">
          <button className="btn-primary" onClick={refresh} aria-label="Refresh alerts">
            <FiRefreshCw /> Refresh
          </button>
          <button className="btn-secondary" aria-label="Export alerts">
            <FiDownload /> Export
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="alerts-stats">
        <div className="stat-card total">
          <h3>Total Alerts</h3>
          <p className="stat-number">{stats.total}</p>
          <p className="stat-subtitle">All time</p>
        </div>
        <div className="stat-card unresolved">
          <h3>Active</h3>
          <p className="stat-number">{stats.unresolved}</p>
          <p className="stat-subtitle">Require attention</p>
        </div>
        <div className="stat-card critical">
          <h3>Critical</h3>
          <p className="stat-number">{stats.critical}</p>
          <p className="stat-subtitle">Immediate action</p>
        </div>
        <div className="stat-card health">
          <h3>Health Alerts</h3>
          <p className="stat-number">{stats.health}</p>
          <p className="stat-subtitle">Today</p>
        </div>
        <div className="stat-card today">
          <h3>Today</h3>
          <p className="stat-number">{stats.today}</p>
          <p className="stat-subtitle">New alerts</p>
        </div>
      </div>

      {/* Filters and Controls */}
      <div className="alerts-controls">
        <div className="filters-row">
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
              Active Only
            </button>
            <button
              className={filter === 'critical' ? 'active' : ''}
              onClick={() => setFilter('critical')}
            >
              Critical
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
          </div>

          <div className="view-controls">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={showResolved}
                onChange={(e) => setShowResolved(e.target.checked)}
              />
              <span>Show Resolved</span>
            </label>

            <div className="dropdown-filter">
              <FiFilter />
              <select
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                className="filter-select"
              >
                <option value="all">All Types</option>
                <option value="health">Health</option>
                <option value="location">Location</option>
                <option value="system">System</option>
                <option value="environment">Environment</option>
              </select>
            </div>
          </div>
        </div>

        {selectedAlerts.length > 0 && (
          <div className="bulk-actions">
            <div className="selected-count">
              <span>{selectedAlerts.length} alerts selected</span>
            </div>
            <div className="bulk-buttons">
              <button
                className="btn-success"
                onClick={handleResolveSelected}
              >
                <FiCheckCircle /> Mark as Resolved
              </button>
              <button
                className="btn-danger"
                onClick={handleDeleteSelected}
              >
                <FiTrash2 /> Delete Selected
              </button>
              <button
                className="btn-secondary"
                onClick={() => setSelectedAlerts([])}
              >
                <FiXCircle /> Clear Selection
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Alerts Table */}
      <div className="alerts-table-container">
        <table className="alerts-table">
          <thead>
            <tr>
              <th className="select-column">
                <input
                  type="checkbox"
                  checked={selectedAlerts.length === filteredAlerts.length && filteredAlerts.length > 0}
                  onChange={handleSelectAll}
                  indeterminate={selectedAlerts.length > 0 && selectedAlerts.length < filteredAlerts.length}
                />
              </th>
              <th>Type</th>
              <th>Severity</th>
              <th>Alert Details</th>
              <th>Cow</th>
              <th>Time</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredAlerts.length > 0 ? (
              filteredAlerts.map(alert => (
                <tr key={alert.alertId} className={`alert-row ${alert.severity} ${alert.isResolved ? 'resolved' : ''}`}>
                  <td className="select-column">
                    <input
                      type="checkbox"
                      checked={selectedAlerts.includes(alert.alertId)}
                      onChange={() => handleSelectAlert(alert.alertId)}
                    />
                  </td>
                  <td className="type-column">
                    <div className="type-cell">
                      {getTypeIcon(alert.alertType)}
                      <span className="type-label">{alert.alertType}</span>
                    </div>
                  </td>
                  <td className="severity-column">
                    <div className="severity-cell">
                      {getSeverityIcon(alert.severity)}
                      <span className={`severity-label ${alert.severity}`}>
                        {alert.severity}
                      </span>
                    </div>
                  </td>
                  <td className="details-column">
                    <div className="alert-details">
                      <h4>{alert.title}</h4>
                      <p className="alert-message">{alert.message}</p>
                      <div className="alert-metrics">
                        <span className="metric-item">
                          <strong>{alert.metric}:</strong> {alert.value}
                        </span>
                        <span className="metric-item">
                          <strong>Threshold:</strong> {alert.threshold}
                        </span>
                        <span className="metric-item">
                          <FaMapMarkerAlt /> {alert.location}
                        </span>
                      </div>
                    </div>
                  </td>
                  <td className="cow-column">
                    {alert.cowId ? (
                      <div className="cow-info">
                        <div className="cow-name">{alert.cowName}</div>
                        <div className="cow-id">ID: CT-00{alert.cowId}</div>
                      </div>
                    ) : (
                      <span className="system-alert">System</span>
                    )}
                  </td>
                  <td className="time-column">
                    <div className="time-cell">
                      <FiClock />
                      <span>{alert.createdAt}</span>
                    </div>
                  </td>
                  <td className="status-column">
                    <span className={`status-badge ${alert.isResolved ? 'resolved' : 'active'}`}>
                      {alert.isResolved ? 'Resolved' : 'Active'}
                    </span>
                  </td>
                  <td className="actions-column">
                    <div className="action-buttons">
                      {!alert.isResolved && (
                        <button
                          className="btn-small btn-resolve"
                          onClick={() => handleResolveAlert(alert.alertId)}
                          title="Mark as resolved"
                        >
                          <FiCheckCircle />
                        </button>
                      )}
                      <button
                        className="btn-small btn-view"
                        onClick={() => {
                          // Navigate to alert details or cow details
                          console.log('View alert details:', alert.alertId);
                        }}
                        title="View details"
                      >
                        <FiEye />
                      </button>
                      <button
                        className="btn-small btn-delete"
                        onClick={() => handleDeleteAlert(alert.alertId)}
                        title="Delete alert"
                      >
                        <FiTrash2 />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr className="empty-row">
                <td colSpan="8">
                  <div className="empty-state">
                    <FiBell className="empty-icon" />
                    <h3>No alerts found</h3>
                    <p>Try changing your filters or check back later.</p>
                    <button
                      className="btn-primary"
                      onClick={() => {
                        setFilter('all');
                        setShowResolved(true);
                      }}
                    >
                      Show All Alerts
                    </button>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Alert Summary */}
      <div className="alert-summary">
        <div className="summary-card">
          <h3>Alert Trends</h3>
          <div className="trend-chart">
            {/* Simple bar chart visualization */}
            <div className="chart-bars">
              {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day, i) => (
                <div key={day} className="chart-bar-container">
                  <div
                    className="chart-bar"
                    style={{ height: `${Math.random() * 80 + 20}%` }}
                  ></div>
                  <span className="chart-label">{day}</span>
                </div>
              ))}
            </div>
          </div>
          <div className="trend-info">
            <p>Average response time: <strong>2.4 hours</strong></p>
            <p>Most common alert: <strong>Health (42%)</strong></p>
          </div>
        </div>

        <div className="summary-card">
          <h3>Quick Actions</h3>
          <div className="quick-actions-list">
            <button className="action-btn full-width">
              <FiAlertCircle /> Configure Alert Rules
            </button>
            <button className="action-btn full-width">
              <FiBell /> Notification Settings
            </button>
            <button className="action-btn full-width">
              <FiDownload /> Generate Report
            </button>
            <button className="action-btn full-width">
              <FiFilter /> Advanced Filters
            </button>
          </div>
        </div>

        <div className="summary-card">
          <h3>Alert Types Distribution</h3>
          <div className="type-distribution">
            <div className="type-item">
              <span className="type-dot health"></span>
              <span>Health</span>
              <span className="type-count">42%</span>
            </div>
            <div className="type-item">
              <span className="type-dot location"></span>
              <span>Location</span>
              <span className="type-count">28%</span>
            </div>
            <div className="type-item">
              <span className="type-dot system"></span>
              <span>System</span>
              <span className="type-count">18%</span>
            </div>
            <div className="type-item">
              <span className="type-dot environment"></span>
              <span>Environment</span>
              <span className="type-count">12%</span>
            </div>
          </div>
          <div className="response-stats">
            <h4>Response Stats</h4>
            <div className="stat-row">
              <span>Avg. Response Time:</span>
              <strong>2.4 hours</strong>
            </div>
            <div className="stat-row">
              <span>Resolved Today:</span>
              <strong>8 alerts</strong>
            </div>
            <div className="stat-row">
              <span>Auto-resolved:</span>
              <strong>15%</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Alerts;