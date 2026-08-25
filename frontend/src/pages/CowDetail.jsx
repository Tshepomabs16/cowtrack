import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
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

  // Mock data - replace with API call
  useEffect(() => {
    // Simulate API call
    setTimeout(() => {
      setCow({
        id: id || '1',
        name: 'Bessie-001',
        tag: 'CT-001',
        breed: 'Holstein',
        age: '3 years 2 months',
        weight: '450 kg',
        status: 'healthy',
        birthDate: '2020-10-15',
        purchaseDate: '2021-01-20',
        location: 'Pasture A',
        temperature: '38.5°C',
        heartRate: '65 bpm',
        milkProduction: '28L/day',
        healthScore: 92,
        lastCheck: '2 hours ago',
        color: 'Black & White',
        owner: 'John Doe',
        vet: 'Dr. Smith',
        notes: 'Pregnant, due in 3 months. Requires special diet.',
      });
      setLoading(false);
    }, 500);
  }, [id]);

  // Mock chart data
  const weightData = [
    { month: 'Jan', weight: 420 },
    { month: 'Feb', weight: 425 },
    { month: 'Mar', weight: 430 },
    { month: 'Apr', weight: 435 },
    { month: 'May', weight: 440 },
    { month: 'Jun', weight: 445 },
    { month: 'Jul', weight: 450 },
  ];

  const milkData = [
    { day: 'Mon', milk: 26 },
    { day: 'Tue', milk: 28 },
    { day: 'Wed', milk: 27 },
    { day: 'Thu', milk: 29 },
    { day: 'Fri', milk: 28 },
    { day: 'Sat', milk: 27 },
    { day: 'Sun', milk: 28 },
  ];

  const healthData = [
    { date: 'Week 1', temp: 38.3, heart: 64 },
    { date: 'Week 2', temp: 38.4, heart: 65 },
    { date: 'Week 3', temp: 38.2, heart: 66 },
    { date: 'Week 4', temp: 38.5, heart: 65 },
    { date: 'Week 5', temp: 38.3, heart: 64 },
  ];

  const activityData = [
    { time: '6 AM', activity: 20 },
    { time: '9 AM', activity: 85 },
    { time: '12 PM', activity: 60 },
    { time: '3 PM', activity: 75 },
    { time: '6 PM', activity: 40 },
    { time: '9 PM', activity: 15 },
  ];

  const medicalHistory = [
    { date: '2024-01-15', procedure: 'Routine Checkup', vet: 'Dr. Smith', notes: 'All normal' },
    { date: '2023-11-20', procedure: 'Vaccination', vet: 'Dr. Johnson', notes: 'Annual vaccines administered' },
    { date: '2023-08-10', procedure: 'Hoof Trimming', vet: 'Dr. Smith', notes: 'Routine maintenance' },
    { date: '2023-05-05', procedure: 'Deworming', vet: 'Dr. Williams', notes: 'Preventive treatment' },
  ];

  const feedingSchedule = [
    { time: '06:00', food: 'Hay', quantity: '5 kg', supplements: 'Mineral mix' },
    { time: '12:00', food: 'Silage', quantity: '8 kg', supplements: 'Salt lick' },
    { time: '18:00', food: 'Grains', quantity: '3 kg', supplements: 'Calcium' },
  ];

  if (loading) {
    return (
      <div className="cow-detail-loading">
        <div className="loading-spinner"></div>
        <p>Loading cow details...</p>
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
    <div className="cow-detail-page">
      {/* Header Section */}
      <div className="cow-detail-header">
        <button onClick={() => navigate('/cows')} className="back-btn">
          <FiArrowLeft /> Back to Cattle
        </button>

        <div className="header-content">
          <div className="cow-title">
            <h1>{cow.name}</h1>
            <span className="cow-tag">{cow.tag}</span>
            <span className={`status-badge status-${cow.status}`}>
              {cow.status.charAt(0).toUpperCase() + cow.status.slice(1)}
            </span>
          </div>

          <div className="header-actions">
            <button className="action-btn">
              <FiEdit /> Edit
            </button>
            <button className="action-btn">
              <FiPrinter /> Print
            </button>
            <button className="action-btn">
              <FiDownload /> Export
            </button>
            <button className="action-btn">
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
                    <span className="info-value">{cow.breed}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Age</span>
                    <span className="info-value">{cow.age}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Weight</span>
                    <span className="info-value">{cow.weight}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Color</span>
                    <span className="info-value">{cow.color}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Birth Date</span>
                    <span className="info-value">
                      <FiCalendar /> {cow.birthDate}
                    </span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">Location</span>
                    <span className="info-value">
                      <FiMapPin /> {cow.location}
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
                      <p className="metric-value">{cow.temperature}</p>
                      <p className="metric-status normal">Normal</p>
                    </div>
                  </div>
                  <div className="metric-card">
                    <div className="metric-icon heart">
                      <FiHeart />
                    </div>
                    <div className="metric-content">
                      <h4>Heart Rate</h4>
                      <p className="metric-value">{cow.heartRate}</p>
                      <p className="metric-status normal">Normal</p>
                    </div>
                  </div>
                  <div className="metric-card">
                    <div className="metric-icon milk">
                      <FaUtensils />
                    </div>
                    <div className="metric-content">
                      <h4>Milk Production</h4>
                      <p className="metric-value">{cow.milkProduction}</p>
                      <p className="metric-status good">Good</p>
                    </div>
                  </div>
                  <div className="metric-card">
                    <div className="metric-icon score">
                      <FiTrendingUp />
                    </div>
                    <div className="metric-content">
                      <h4>Health Score</h4>
                      <p className="metric-value">{cow.healthScore}/100</p>
                      <div className="score-bar">
                        <div
                          className="score-fill"
                          style={{ width: `${cow.healthScore}%` }}
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
                    <span className="stat-value">{cow.lastCheck}</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Owner</span>
                    <span className="stat-value">{cow.owner}</span>
                  </div>
                  <div className="stat-item">
                    <span className="stat-label">Assigned Vet</span>
                    <span className="stat-value">{cow.vet}</span>
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
                  <p className="notes-text">{cow.notes}</p>
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
              <table className="feeding-table">
                <thead>
                  <tr>
                    <th>Time</th>
                    <th>Food Type</th>
                    <th>Quantity</th>
                    <th>Supplements</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {feedingSchedule.map((meal, index) => (
                    <tr key={index}>
                      <td>
                        <FiCalendar /> {meal.time}
                      </td>
                      <td>{meal.food}</td>
                      <td><strong>{meal.quantity}</strong></td>
                      <td>{meal.supplements}</td>
                      <td>
                        <button className="btn-small">Edit</button>
                        <button className="btn-small">Skip</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="feeding-stats">
              <div className="stat-card">
                <FaUtensils className="stat-icon" />
                <h4>Daily Intake</h4>
                <p className="stat-value">16 kg</p>
              </div>
              <div className="stat-card">
                <FaWater className="stat-icon" />
                <h4>Water Consumption</h4>
                <p className="stat-value">45 L/day</p>
              </div>
              <div className="stat-card">
                <FaWeight className="stat-icon" />
                <h4>Feed Efficiency</h4>
                <p className="stat-value">85%</p>
              </div>
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