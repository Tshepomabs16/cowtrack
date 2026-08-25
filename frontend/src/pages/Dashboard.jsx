import React from 'react';
import CattleMap from '../components/CattleMap/CattleMap';
import StatsCard from '../components/StatsCard';
import AlertFeed from '../components/AlertFeed';
import './Dashboard.css';

const Dashboard = () => {
  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1>Dashboard</h1>
        <p>Real-time cattle monitoring and tracking</p>
      </div>

      <div className="dashboard-grid">
        <div className="map-section">
          <h2>Cattle Locations</h2>
          <CattleMap />
        </div>

        <div className="stats-section">
          <div className="stats-row">
            <StatsCard title="Total Cattle" value="247" change="+5%" />
            <StatsCard title="Active Now" value="218" change="+2%" />
            <StatsCard title="Health Alerts" value="3" change="-1" alert />
            <StatsCard title="Avg Temperature" value="38.4°C" change="0.2°" />
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