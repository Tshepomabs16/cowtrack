import React, { useState } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { FiActivity, FiThermometer, FiHeart, FiTrendingUp } from 'react-icons/fi';
import './Health.css';

const Health = () => {
  const [timeRange, setTimeRange] = useState('7d');

  const healthData = [
    { day: 'Mon', temp: 38.4, heart: 65, activity: 85 },
    { day: 'Tue', temp: 38.2, heart: 68, activity: 82 },
    { day: 'Wed', temp: 38.6, heart: 70, activity: 78 },
    { day: 'Thu', temp: 38.3, heart: 66, activity: 88 },
    { day: 'Fri', temp: 38.5, heart: 64, activity: 90 },
    { day: 'Sat', temp: 38.7, heart: 72, activity: 76 },
    { day: 'Sun', temp: 38.4, heart: 65, activity: 84 },
  ];

  const cowHealthData = [
    { name: 'Bessie-001', temp: 38.5, heart: 65, status: 'Normal', score: 92 },
    { name: 'Daisy-002', temp: 38.2, heart: 68, status: 'Normal', score: 88 },
    { name: 'Moo-003', temp: 39.1, heart: 72, status: 'Warning', score: 65 },
    { name: 'Buttercup-004', temp: 38.3, heart: 64, status: 'Normal', score: 90 },
    { name: 'Clover-005', temp: 38.6, heart: 70, status: 'Normal', score: 85 },
  ];

  const alerts = [
    { id: 1, cow: 'Moo-003', metric: 'Temperature', value: '39.1°C', threshold: '38.5°C', time: '2h ago' },
    { id: 2, cow: 'Bessie-001', metric: 'Heart Rate', value: '72 bpm', threshold: '70 bpm', time: '5h ago' },
    { id: 3, cow: 'Daisy-002', metric: 'Activity', value: '45%', threshold: '50%', time: '1d ago' },
  ];

  return (
    <div className="health-page">
      <div className="page-header">
        <h1>Health Monitoring</h1>
        <p>Track and analyze herd health metrics</p>
      </div>

      <div className="time-range-selector">
        <button
          className={timeRange === '24h' ? 'active' : ''}
          onClick={() => setTimeRange('24h')}
        >
          24 Hours
        </button>
        <button
          className={timeRange === '7d' ? 'active' : ''}
          onClick={() => setTimeRange('7d')}
        >
          7 Days
        </button>
        <button
          className={timeRange === '30d' ? 'active' : ''}
          onClick={() => setTimeRange('30d')}
        >
          30 Days
        </button>
      </div>

      <div className="health-overview">
        <div className="overview-card">
          <div className="overview-icon temp">
            <FiThermometer />
          </div>
          <div className="overview-content">
            <h3>Avg Temperature</h3>
            <p className="overview-value">38.4°C</p>
            <p className="overview-change positive">+0.2° from avg</p>
          </div>
        </div>

        <div className="overview-card">
          <div className="overview-icon heart">
            <FiHeart />
          </div>
          <div className="overview-content">
            <h3>Avg Heart Rate</h3>
            <p className="overview-value">67 bpm</p>
            <p className="overview-change">Normal range</p>
          </div>
        </div>

        <div className="overview-card">
          <div className="overview-icon activity">
            <FiActivity />
          </div>
          <div className="overview-content">
            <h3>Activity Level</h3>
            <p className="overview-value">82%</p>
            <p className="overview-change positive">+5% from yesterday</p>
          </div>
        </div>

        <div className="overview-card">
          <div className="overview-icon trend">
            <FiTrendingUp />
          </div>
          <div className="overview-content">
            <h3>Health Score</h3>
            <p className="overview-value">84/100</p>
            <p className="overview-change positive">Good condition</p>
          </div>
        </div>
      </div>

      <div className="charts-grid">
        <div className="chart-card">
          <h3>Temperature Trends</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={healthData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="day" />
              <YAxis label={{ value: '°C', angle: -90, position: 'insideLeft' }} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="temp" stroke="#ef4444" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="chart-card">
          <h3>Heart Rate Distribution</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={healthData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="day" />
              <YAxis label={{ value: 'bpm', angle: -90, position: 'insideLeft' }} />
              <Tooltip />
              <Legend />
              <Bar dataKey="heart" fill="#3b82f6" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="health-details">
        <div className="cow-health-table">
          <h3>Individual Cow Health</h3>
          <table>
            <thead>
              <tr>
                <th>Cow Name</th>
                <th>Temperature</th>
                <th>Heart Rate</th>
                <th>Status</th>
                <th>Health Score</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {cowHealthData.map((cow, index) => (
                <tr key={index}>
                  <td><strong>{cow.name}</strong></td>
                  <td>
                    <span className={cow.temp > 38.5 ? 'warning-value' : 'normal-value'}>
                      {cow.temp}°C
                    </span>
                  </td>
                  <td>{cow.heart} bpm</td>
                  <td>
                    <span className={`status-badge ${cow.status.toLowerCase()}`}>
                      {cow.status}
                    </span>
                  </td>
                  <td>
                    <div className="score-bar">
                      <div
                        className="score-fill"
                        style={{ width: `${cow.score}%` }}
                      ></div>
                      <span>{cow.score}/100</span>
                    </div>
                  </td>
                  <td>
                    <button className="btn-small">View</button>
                    <button className="btn-small">Report</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="health-alerts">
          <h3>Current Health Alerts</h3>
          <div className="alerts-list">
            {alerts.map(alert => (
              <div key={alert.id} className="alert-item">
                <div className="alert-icon">⚠️</div>
                <div className="alert-content">
                  <h4>{alert.cow} - {alert.metric}</h4>
                  <p>Current: {alert.value} | Threshold: {alert.threshold}</p>
                  <span className="alert-time">{alert.time}</span>
                </div>
                <button className="btn-small">Investigate</button>
              </div>
            ))}
          </div>
          <button className="btn-primary full-width">View All Alerts</button>
        </div>
      </div>
    </div>
  );
};

export default Health;