import React, { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { FiActivity, FiThermometer, FiHeart, FiTrendingUp } from 'react-icons/fi';
import { analyticsAPI, healthAPI } from '../services/api';
import './Health.css';

const num = (value) => (Number.isFinite(Number(value)) ? Number(value) : null);

/** Mean of a numeric field across rows, or null when there is nothing to average. */
const mean = (rows, pick) => {
  const values = rows.map(pick).filter(Number.isFinite);
  if (!values.length) return null;
  return values.reduce((sum, value) => sum + value, 0) / values.length;
};

const Health = () => {
  const [timeRange, setTimeRange] = useState('7d');
  const [trends, setTrends] = useState([]);
  const [herd, setHerd] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    const load = async () => {
      try {
        const [trendResponse, herdResponse] = await Promise.all([
          analyticsAPI.getHealthTrends({ range: timeRange }),
          healthAPI.getMetrics({ range: timeRange }),
        ]);
        if (cancelled) return;

        setTrends(
          (trendResponse.data?.series || []).map((point) => ({
            day: new Date(point.date).toLocaleDateString(undefined, { weekday: 'short' }),
            temp: num(point.temp),
            heart: num(point.heart),
            activity: num(point.activity),
          }))
        );
        setHerd(herdResponse.data);
      } catch (error) {
        console.error('Error loading health data:', error);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, [timeRange]);

  const healthData = trends;
  const thresholds = herd?.thresholds || {};

  const cowHealthData = (herd?.cows || []).map((cow) => ({
    cowId: cow.cowId,
    name: cow.cowName,
    temp: num(cow.temperature),
    heart: num(cow.heartRate),
    activity: num(cow.activityLevel),
    status: cow.status,
    // Presentation-only summary: start at 100 and deduct for each vital that is
    // outside its band, scaled by how far outside it sits.
    score: (() => {
      let score = 100;
      const temp = num(cow.temperature);
      const heart = num(cow.heartRate);
      const activity = num(cow.activityLevel);
      if (temp !== null && temp > thresholds.temperature) {
        score -= Math.min(35, (temp - thresholds.temperature) * 30);
      }
      if (heart !== null && heart > thresholds.heartRate) {
        score -= Math.min(30, heart - thresholds.heartRate);
      }
      if (activity !== null && activity < thresholds.activityLevel) {
        score -= Math.min(30, thresholds.activityLevel - activity);
      }
      return Math.max(0, Math.round(score));
    })(),
  }));

  // Each vital that is out of band becomes one row in the alerts panel.
  const alerts = (herd?.cows || []).flatMap((cow) => {
    const rows = [];
    const temp = num(cow.temperature);
    const heart = num(cow.heartRate);
    const activity = num(cow.activityLevel);

    if (temp !== null && temp > thresholds.temperature) {
      rows.push({ id: `${cow.cowId}-temp`, cow: cow.cowName, metric: 'Temperature',
        value: `${temp}°C`, threshold: `${thresholds.temperature}°C`, time: cow.recordedAt });
    }
    if (heart !== null && heart > thresholds.heartRate) {
      rows.push({ id: `${cow.cowId}-heart`, cow: cow.cowName, metric: 'Heart Rate',
        value: `${heart} bpm`, threshold: `${thresholds.heartRate} bpm`, time: cow.recordedAt });
    }
    if (activity !== null && activity < thresholds.activityLevel) {
      rows.push({ id: `${cow.cowId}-activity`, cow: cow.cowName, metric: 'Activity',
        value: `${activity}%`, threshold: `${thresholds.activityLevel}%`, time: cow.recordedAt });
    }
    return rows;
  });

  const avgTemp = mean(cowHealthData, (cow) => cow.temp);
  const avgHeart = mean(cowHealthData, (cow) => cow.heart);
  const avgActivity = mean(cowHealthData, (cow) => cow.activity);
  const avgScore = mean(cowHealthData, (cow) => cow.score);

  const show = (value, format) => {
    if (loading) return '…';
    if (value === null || value === undefined) return '—';
    return format(value);
  };

  const formatTime = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return Number.isNaN(date.getTime()) ? '' : date.toLocaleString();
  };

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
            <p className="overview-value">{show(avgTemp, v => `${v.toFixed(1)}°C`)}</p>
            <p className="overview-change">Across {cowHealthData.length} monitored</p>
          </div>
        </div>

        <div className="overview-card">
          <div className="overview-icon heart">
            <FiHeart />
          </div>
          <div className="overview-content">
            <h3>Avg Heart Rate</h3>
            <p className="overview-value">{show(avgHeart, v => `${Math.round(v)} bpm`)}</p>
            <p className="overview-change">
              {avgHeart !== null && avgHeart > thresholds.heartRate ? 'Above normal' : 'Normal range'}
            </p>
          </div>
        </div>

        <div className="overview-card">
          <div className="overview-icon activity">
            <FiActivity />
          </div>
          <div className="overview-content">
            <h3>Activity Level</h3>
            <p className="overview-value">{show(avgActivity, v => `${Math.round(v)}%`)}</p>
            <p className="overview-change">Herd average</p>
          </div>
        </div>

        <div className="overview-card">
          <div className="overview-icon trend">
            <FiTrendingUp />
          </div>
          <div className="overview-content">
            <h3>Health Score</h3>
            <p className="overview-value">{show(avgScore, v => `${Math.round(v)}/100`)}</p>
            <p className="overview-change">{herd?.warnings ? `${herd.warnings} need attention` : 'All within range'}</p>
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
              {cowHealthData.map((cow) => (
                <tr key={cow.cowId}>
                  <td><strong>{cow.name}</strong></td>
                  <td>
                    <span className={cow.temp > thresholds.temperature ? 'warning-value' : 'normal-value'}>
                      {cow.temp === null ? '—' : `${cow.temp}°C`}
                    </span>
                  </td>
                  <td>{cow.heart === null ? '—' : `${cow.heart} bpm`}</td>
                  <td>
                    <span className={`status-badge ${(cow.status || '').toLowerCase()}`}>
                      {cow.status || 'Unknown'}
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
                  <span className="alert-time">{formatTime(alert.time)}</span>
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