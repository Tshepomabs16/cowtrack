import React from 'react';
import './StatsCard.css';

const StatsCard = ({ title, value, icon, color, trend, trendUp }) => {
  return (
    <div className="stats-card" style={{ borderColor: color }}>
      <div className="stats-content">
        <div className="stats-left">
          <h3>{title}</h3>
          <div className="stats-value">{value}</div>
          {/* Only rendered when a trend was actually supplied. Rendering it
              unconditionally put a red downward arrow with no value on every
              card, implying a decline that had never been measured. */}
          {trend && (
            <div className={`stats-trend ${trendUp ? 'up' : 'down'}`}>
              {trendUp ? '📈' : '📉'} {trend}
            </div>
          )}
        </div>
        <div className="stats-icon" style={{ color: color }}>
          {icon}
        </div>
      </div>
    </div>
  );
};

export default StatsCard;