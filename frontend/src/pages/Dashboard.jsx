import React, { useState, useEffect } from 'react';
import CattleMap from '../components/CattleMap/CattleMap';
import StatsCard from '../components/StatsCard';
import AlertFeed from '../components/AlertFeed';
import { analyticsAPI, healthAPI } from '../services/api';
import './Dashboard.css';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      try {
        const [dashboard, metrics] = await Promise.all([
          analyticsAPI.getDashboardStats(),
          healthAPI.getMetrics({ range: '7d' }),
        ]);
        if (cancelled) return;
        setStats(dashboard.data);
        setHealth(metrics.data);
      } catch (error) {
        console.error('Error loading dashboard:', error);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, []);

  // Mean of the most recent reading per animal.
  const averageTemperature = () => {
    const readings = (health?.cows || [])
      .map(cow => Number(cow.temperature))
      .filter(Number.isFinite);

    if (!readings.length) return null;
    const mean = readings.reduce((sum, value) => sum + value, 0) / readings.length;
    return `${mean.toFixed(1)}°C`;
  };

  // A dash is more honest than a zero when nothing has been recorded yet.
  const show = (value, suffix = '') => {
    if (loading) return '…';
    if (value === null || value === undefined) return '—';
    return `${value}${suffix}`;
  };

  return (
    <div className="dashboard page-enter">
      <div className="dashboard-header">
        <h1>Dashboard</h1>
        <p>Real-time cattle monitoring and tracking</p>
      </div>

      <div className="dashboard-grid">
        <div className="map-section">
          <h2>Cattle Locations</h2>
          {loading ? (
            <div className="skeleton" style={{ height: 300, borderRadius: 'var(--radius)' }} />
          ) : (
            <CattleMap />
          )}
        </div>

        <div className="stats-section">
          <div className="stats-row">
            {loading ? (
              <>
                <div className="skeleton" style={{ height: 100, borderRadius: 'var(--radius)' }} />
                <div className="skeleton" style={{ height: 100, borderRadius: 'var(--radius)' }} />
                <div className="skeleton" style={{ height: 100, borderRadius: 'var(--radius)' }} />
                <div className="skeleton" style={{ height: 100, borderRadius: 'var(--radius)' }} />
              </>
            ) : (
              <>
                <StatsCard title="Total Cattle" value={show(stats?.totalCows)} icon="🐄" />
                <StatsCard title="Monitored" value={show(health?.monitored)} icon="📡" />
                <StatsCard
                  title="Health Alerts"
                  value={show(stats?.activeAlerts)}
                  icon="⚠️"
                  color={stats?.activeAlerts ? '#ef4444' : undefined}
                />
                <StatsCard title="Avg Temperature" value={show(averageTemperature())} icon="🌡️" />
              </>
            )}
          </div>

          <div className="alerts-section">
            <h3>Recent Alerts</h3>
            <AlertFeed />
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
