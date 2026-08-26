import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { cowsAPI, healthAPI, productionAPI } from '../services/api';
import {
  FiArrowLeft,
  FiEdit,
  FiPrinter,
  FiDownload,
  FiShare2,
  FiCalendar,
  FiMapPin,
  FiActivity,
  FiThermometer,
  FiHeart,
  FiTrendingUp
} from 'react-icons/fi';
import {
  FaWeight,
  FaBirthdayCake,
  FaSyringe,
  FaUtensils,
  FaWater
} from 'react-icons/fa';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';
import './CowDetail.css';

const CowDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [cow, setCow] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');

  const [production, setProduction] = useState([]);
  const [metrics, setMetrics] = useState([]);
  const [records, setRecords] = useState([]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    const load = async () => {
      try {
        const [cowResponse, productionResponse, metricResponse, recordResponse] =
          await Promise.all([
            cowsAPI.getById(id),
            productionAPI.getForCow(id),
            healthAPI.getCowHealth(id),
            healthAPI.getCowRecords(id),
          ]);
        if (cancelled) return;

        setCow(cowResponse.data);
        setProduction(Array.isArray(productionResponse.data) ? productionResponse.data : []);
        setMetrics(Array.isArray(metricResponse.data) ? metricResponse.data : []);
        setRecords(Array.isArray(recordResponse.data) ? recordResponse.data : []);
      } catch (error) {
        console.error('Error loading cow:', error);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, [id]);

  // Charts read oldest-to-newest; the API returns newest first.
  const productionAsc = production.slice().reverse();
  const metricsAsc = metrics.slice().reverse();

  const weightData = productionAsc
    .filter(entry => entry.weightKg !== null && entry.weightKg !== undefined)
    .map(entry => ({
      month: new Date(entry.recordDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
      weight: Number(entry.weightKg),
    }));

  const milkData = productionAsc
    .filter(entry => entry.milkLitres !== null && entry.milkLitres !== undefined)
    .map(entry => ({
      day: new Date(entry.recordDate).toLocaleDateString(undefined, { weekday: 'short' }),
      milk: Number(entry.milkLitres),
    }));

  const healthData = metricsAsc.map(metric => ({
    date: new Date(metric.recordedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
    temp: Number(metric.temperature),
    heart: Number(metric.heartRate),
  }));

  const activityData = metricsAsc.map(metric => ({
    time: new Date(metric.recordedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    activity: Number(metric.activityLevel),
  }));

  const medicalHistory = records.map(record => ({
    date: record.recordDate,
    procedure: record.diagnosis,
    vet: record.vetName,
    notes: record.treatment,
  }));

  const latestMetric = metrics[0];
  const latestProduction = production[0];

  if (loading) {
    return (
      <div className="cow-detail-page page-enter">
        <div className="cow-detail-header">
          <div className="skeleton" style={{ width: 140, height: 36, borderRadius: 8 }} />
          <div style={{ display: 'flex', gap: 20, alignItems: 'center', marginTop: 20 }}>
            <div className="skeleton" style={{ width: 80, height: 80, borderRadius: '50%' }} />
            <div>
              <div className="skeleton" style={{ width: 180, height: 24, marginBottom: 8 }} />
              <div className="skeleton" style={{ width: 100, height: 14 }} />
            </div>
          </div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16, marginTop: 24 }}>
          {[1, 2, 3].map(i => (
            <div key={i} className="skeleton" style={{ height: 200, borderRadius: 'var(--radius)' }} />
          ))}
        </div>
      </div>
    );
  }

  if (!cow) {
    return (
      <div className="cow-detail-error">
        <h2>Cow not found</h2>
        <p>The cow with ID {id} does not exist.</p>
        <button onClick={() => navigate('/cows')} className="btn-primary">
          <FiArrowLeft /> Back to Cattle
        </button>
      </div>
    );
  }

  return (
    <div className="cow-detail-page page-enter">
      {/* Header Section */}
      <div className="cow-detail-header">
        <button onClick={() => navigate('/cows')} className="back-btn" aria-label="Back to cattle list">
          <FiArrowLeft /> Back to Cattle
        </button>

        <div className="header-content">
          <div className="cow-title">
            <h1>{cow.name}</h1>
            <span className="cow-tag">{cow.tagId}</span>
            <span className={`status-badge status-${cow.status || 'unknown'}`}>
              {(cow.status || 'unknown').charAt(0).toUpperCase() + (cow.status || 'unknown').slice(1)}
            </span>
          </div>

          <div className="header-actions">
            <button className="action-btn" aria-label="Edit cow">
              <FiEdit /> Edit
            </button>
            <button className="action-btn" aria-label="Print cow details">
              <FiPrinter /> Print
            </button>
            <button className="action-btn" aria-label="Export cow data">
              <FiDownload /> Export
            </button>
            <button className="action-btn" aria-label="Share cow profile">
              <FiShare2 /> Share
            </button>
          </div>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="detail-tabs">
        <button
          className={activeTab === 'overview' ? 'active' : ''}
          onClick={() => setActiveTab('overview')}
        >
          Overview
        </button>
        <button
          className={activeTab === 'health' ? 'active' : ''}
          onClick={() => setActiveTab('health')}
        >
          Health
        </button>
        <button
          className={activeTab === 'feeding' ? 'active' : ''}
          onClick={() => setActiveTab('feeding')}
        >
          Feeding
        </button>
        <button
          className={activeTab === 'medical' ? 'active' : ''}
          onClick={() => setActiveTab('medical')}
        >
          Medical History
        </button>
        <button
          className={activeTab === 'activity' ? 'active' : ''}
          onClick={() => setActiveTab('activity')}
        >
          Activity
        </button>
      </div>

      {/* Content based on active tab */}
      <div className="detail-content">

        {/* OVERVIEW TAB */}
        {activeTab === 'overview' && (
          <div className="overview-content">
            <div className="overview-grid">
              {/* Left Column - Basic Info */}
              <div className="info-card">
                <h3>Basic Information</h3>
                <div className="info-grid">
                  <div className="info-item">
                    <span className="info-label">Breed</span>
                    <span className="info-value">{cow.breed || '—'}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Age</span>
                    <span className="info-value">{cow.age === null || cow.age === undefined ? '—' : `${cow.age} years`}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Weight</span>
                    <span className="info-value">{cow.weight ? `${cow.weight} kg` : '—'}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Caretaker</span>
                    <span className="info-value">{cow.caretakerName || 'Unassigned'}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Birth Date</span>
                    <span className="info-value">
                      <FiCalendar /> {cow.dateOfBirth || '—'}
                    </span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Location</span>
                    <span className="info-value">
                      <FiMapPin /> {cow.location || 'No GPS fix'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Middle Column - Health Metrics */}
              <div className="info-card">
                <h3>Current Health Metrics</h3>
                <div className="metrics-grid">
                  <div className="metric-card">
                    <div className="metric-icon temp">
                      <FiThermometer />
                    </div>
                    <div className="metric-content">
                      <h4>Temperature</h4>
                      <p className="metric-value">{cow.temperature ? `${cow.temperature}°C` : '—'}</p>
                      <p className="metric-status normal">Normal</p>
                    </div>
                  </div>
                  <div className="metric-card">
                    <div className="metric-icon heart">
                      <FiHeart />
                    </div>
                    <div className="metric-content">
                      <h4>Heart Rate</h4>
                      <p className="metric-value">{cow.heartRate ? `${cow.heartRate} bpm` : '—'}</p>
                      <p className="metric-status normal">Normal</p>
                    </div>
                  </div>
                  <div className="metric-card">
                    <div className="metric-icon milk">
                      <FaUtensils />
                    </div>
                    <div className="metric-content">
                      <h4>Milk Production</h4>
                      <p className="metric-value">{latestProduction?.milkLitres ? `${latestProduction.milkLitres} L/day` : '—'}</p>
                      <p className="metric-status good">Good</p>
                    </div>
                  </div>
                  <div className="metric-card">
                    <div className="metric-icon score">
                      <FiTrendingUp />
                    </div>
                    <div className="metric-content">
                      <h4>Health Score</h4>
                      <p className="metric-value">{latestMetric ? (latestMetric.status === 'Warning' ? 'Warning' : 'Normal') : '—'}</p>
                      <div className="score-bar">
                        <div
                          className="score-fill"
                          style={{ width: latestMetric ? (latestMetric.status === 'Warning' ? '45%' : '90%') : '0%' }}
                        ></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Right Column - Quick Stats */}
              <div className="info-card">
                <h3>Quick Stats</h3>
                <div className="stats-list">
                  <div className="stat-item">
                    <span className="stat-label">Last Check</span>
                    <span className="stat-value">{cow.lastCheck ? new Date(cow.lastCheck).toLocaleString() : 'Never'}</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Owner</span>
                    <span className="stat-value">{cow.caretakerName || 'Unassigned'}</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Assigned Vet</span>
                    <span className="stat-value">{records[0]?.vetName || '—'}</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Daily Feed Cost</span>
                    <span className="stat-value">$8.50</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Monthly Milk Revenue</span>
                    <span className="stat-value">$420</span>
                  </div>
                </div>

                <div className="notes-section">
                  <h4>Notes</h4>
                  <p className="notes-text">{records[0]?.treatment || 'No notes recorded'}</p>
                </div>
              </div>
            </div>

            {/* Charts Row */}
            <div className="charts-row">
              <div className="chart-card">
                <h4>Weight Progression</h4>
                <ResponsiveContainer width="100%" height={250}>
                  <LineChart data={weightData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                    <XAxis dataKey="month" />
                    <YAxis label={{ value: 'kg', angle: -90, position: 'insideLeft' }} />
                    <Tooltip />
                    <Line type="monotone" dataKey="weight" stroke="#3b82f6" strokeWidth={2} />
                  </LineChart>
                </ResponsiveContainer>
              </div>

              <div className="chart-card">
                <h4>Milk Production (Last 7 Days)</h4>
                <ResponsiveContainer width="100%" height={250}>
                  <BarChart data={milkData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                    <XAxis dataKey="day" />
                    <YAxis label={{ value: 'Liters', angle: -90, position: 'insideLeft' }} />
                    <Tooltip />
                    <Bar dataKey="milk" fill="#10b981" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        )}

        {/* HEALTH TAB */}
        {activeTab === 'health' && (
          <div className="health-content">
            <div className="health-charts">
              <div className="chart-card large">
                <h4>Temperature & Heart Rate Trends</h4>
                <ResponsiveContainer width="100%" height={300}>
                  <AreaChart data={healthData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                    <XAxis dataKey="date" />
                    <YAxis yAxisId="left" />
                    <YAxis yAxisId="right" orientation="right" />
                    <Tooltip />
                    <Legend />
                    <Area yAxisId="left" type="monotone" dataKey="temp" fill="#ef4444" stroke="#ef4444" name="Temperature (°C)" />
                    <Area yAxisId="right" type="monotone" dataKey="heart" fill="#3b82f6" stroke="#3b82f6" name="Heart Rate (bpm)" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>

              <div className="health-metrics">
                <div className="metric-summary">
                  <h4>Health Summary</h4>
                  <div className="summary-grid">
                    <div className="summary-item good">
                      <span className="summary-label">Respiratory Rate</span>
                      <span className="summary-value">24/min</span>
                    </div>
                    <div className="summary-item normal">
                      <span className="summary-label">Rumination</span>
                      <span className="summary-value">8.2 hours</span>
                    </div>
                    <div className="summary-item warning">
                      <span className="summary-label">Body Condition</span>
                      <span className="summary-value">3.5/5</span>
                    </div>
                    <div className="summary-item good">
                      <span className="summary-label">Hydration</span>
                      <span className="summary-value">Excellent</span>
                    </div>
                  </div>
                </div>

                <div className="health-alerts">
                  <h4>Health Alerts</h4>
                  <div className="alerts-list">
                    <div className="alert-item info">
                      <span>🔄</span>
                      <span>Regular checkup due in 3 days</span>
                    </div>
                    <div className="alert-item warning">
                      <span>⚠️</span>
                      <span>Milk production slightly decreased</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* FEEDING TAB */}
        {activeTab === 'feeding' && (
          <div className="feeding-content">
            <div className="feeding-schedule">
              <h3>Daily Feeding Schedule</h3>
              {/*
                Feeding is not modelled in the backend: there is no feed entity,
                schedule or intake record. Showing an empty state rather than
                invented figures, until that domain exists.
              */}
              <p className="empty-row">
                Feeding is not tracked yet. Once feed schedules and intake are
                modelled, this tab will show them.
              </p>
            </div>
          </div>
        )}

        {/* MEDICAL HISTORY TAB */}
        {activeTab === 'medical' && (
          <div className="medical-content">
            <div className="medical-history">
              <h3>Medical History</h3>
              <table className="medical-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Procedure</th>
                    <th>Veterinarian</th>
                    <th>Notes</th>
                    <th>Documents</th>
                  </tr>
                </thead>
                <tbody>
                  {medicalHistory.map((record, index) => (
                    <tr key={index}>
                      <td>{record.date}</td>
                      <td>
                        <FaSyringe /> {record.procedure}
                      </td>
                      <td>{record.vet}</td>
                      <td>{record.notes}</td>
                      <td>
                        <button className="btn-small">View</button>
                        <button className="btn-small">Download</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="upcoming-procedures">
              <h3>Upcoming Procedures</h3>
              <div className="procedures-list">
                <div className="procedure-item">
                  <div className="procedure-date">
                    <span className="date-day">15</span>
                    <span className="date-month">JAN</span>
                  </div>
                  <div className="procedure-details">
                    <h4>Pregnancy Check</h4>
                    <p>Ultrasound examination</p>
                    <span className="procedure-status scheduled">Scheduled</span>
                  </div>
                </div>
                <div className="procedure-item">
                  <div className="procedure-date">
                    <span className="date-day">28</span>
                    <span className="date-month">JAN</span>
                  </div>
                  <div className="procedure-details">
                    <h4>Vaccination</h4>
                    <p>Annual booster shots</p>
                    <span className="procedure-status pending">Pending</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ACTIVITY TAB */}
        {activeTab === 'activity' && (
          <div className="activity-content">
            <div className="activity-chart">
              <h4>Daily Activity Pattern</h4>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={activityData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                  <XAxis dataKey="time" />
                  <YAxis label={{ value: 'Activity Level %', angle: -90, position: 'insideLeft' }} />
                  <Tooltip />
                  <Bar dataKey="activity" fill="#8b5cf6" />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="activity-summary">
              <h4>Activity Summary</h4>
              <div className="summary-cards">
                <div className="summary-card">
                  <FiActivity className="summary-icon" />
                  <h5>Daily Average</h5>
                  <p className="summary-value">62%</p>
                </div>
                <div className="summary-card">
                  <FaBirthdayCake className="summary-icon" />
                  <h5>Resting Time</h5>
                  <p className="summary-value">9.2 hours</p>
                </div>
                <div className="summary-card">
                  <FiMapPin className="summary-icon" />
                  <h5>Distance Traveled</h5>
                  <p className="summary-value">3.8 km</p>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CowDetail;