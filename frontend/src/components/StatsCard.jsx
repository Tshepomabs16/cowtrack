import React from 'react';
import './StatsCard.css';

const StatsCard = ({ title, value, icon, color, trend, trendUp }) => {
  return (
    <div className="stats-card" style={{ borderColor: color }}>
      <div className="stats-content">
        <div className="stats-left">
          <h3>{title}</h3>
          <div className="stats-value">{value}</div>
          <div className={`stats-trend ${trendUp ? 'up' : 'down'}`}>
            {trendUp ? '📈' : '📉'} {trend}
          </div>
        </div>
        <div className="stats-icon" style={{ color: color }}>
          {icon}
        </div>
      </div>
    </div>
  );
};

export default StatsCard;