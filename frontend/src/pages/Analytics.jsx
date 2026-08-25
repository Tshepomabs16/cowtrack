import React, { useState } from 'react';
import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { FiTrendingUp, FiUsers, FiDollarSign, FiPieChart } from 'react-icons/fi';
import './Analytics.css';

const Analytics = () => {
  const [timeRange, setTimeRange] = useState('month');

  // Sample data
  const revenueData = [
    { month: 'Jan', revenue: 45000, cost: 32000 },
    { month: 'Feb', revenue: 52000, cost: 35000 },
    { month: 'Mar', revenue: 48000, cost: 33000 },
    { month: 'Apr', revenue: 61000, cost: 38000 },
    { month: 'May', revenue: 55000, cost: 36000 },
    { month: 'Jun', revenue: 68000, cost: 42000 },
  ];

  const breedDistribution = [
    { name: 'Holstein', value: 40, color: '#3b82f6' },
    { name: 'Jersey', value: 25, color: '#10b981' },
    { name: 'Angus', value: 20, color: '#f59e0b' },
    { name: 'Hereford', value: 15, color: '#8b5cf6' },
  ];

  const productivityData = [
    { day: 'Mon', milk: 420, weight: 450 },
    { day: 'Tue', milk: 450, weight: 455 },
    { day: 'Wed', milk: 410, weight: 452 },
    { day: 'Thu', milk: 480, weight: 460 },
    { day: 'Fri', milk: 460, weight: 458 },
    { day: 'Sat', milk: 430, weight: 453 },
    { day: 'Sun', milk: 440, weight: 456 },
  ];

  const kpis = [
    { title: 'Total Revenue', value: '$325,000', change: '+12.5%', icon: <FiDollarSign />, color: '#10b981' },
    { title: 'Avg Milk Production', value: '445 L/day', change: '+5.2%', icon: <FiTrendingUp />, color: '#3b82f6' },
    { title: 'Cattle Growth Rate', value: '8.2%', change: '+1.3%', icon: <FiUsers />, color: '#8b5cf6' },
    { title: 'Operational Efficiency', value: '92%', change: '+2.1%', icon: <FiPieChart />, color: '#f59e0b' },
  ];

  return (
    <div className="analytics-page">
      <div className="page-header">
        <h1>Farm Analytics</h1>
        <p>Comprehensive insights and performance metrics</p>
      </div>

      <div className="time-range-tabs">
        {['week', 'month', 'quarter', 'year'].map(range => (
          <button
            key={range}
            className={timeRange === range ? 'active' : ''}
            onClick={() => setTimeRange(range)}
          >
            {range.charAt(0).toUpperCase() + range.slice(1)}
          </button>
        ))}
      </div>

      <div className="kpi-cards">
        {kpis.map((kpi, index) => (
          <div key={index} className="kpi-card">
            <div className="kpi-icon" style={{ backgroundColor: kpi.color }}>
              {kpi.icon}
            </div>
            <div className="kpi-content">
              <h3>{kpi.title}</h3>
              <p className="kpi-value">{kpi.value}</p>
              <p className="kpi-change positive">{kpi.change}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="charts-row">
        <div className="chart-container large">
          <h3>Revenue vs Costs</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={revenueData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" />
              <YAxis />
              <Tooltip formatter={(value) => [`$${value.toLocaleString()}`, '']} />
              <Legend />
              <Line type="monotone" dataKey="revenue" stroke="#10b981" strokeWidth={2} />
              <Line type="monotone" dataKey="cost" stroke="#ef4444" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="chart-container medium">
          <h3>Breed Distribution</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={breedDistribution}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {breedDistribution.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip formatter={(value) => [`${value}%`, '']} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="charts-row">
        <div className="chart-container medium">
          <h3>Daily Productivity</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={productivityData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="day" />
              <YAxis yAxisId="left" />
              <YAxis yAxisId="right" orientation="right" />
              <Tooltip />
              <Legend />
              <Bar yAxisId="left" dataKey="milk" fill="#3b82f6" name="Milk (L)" />
              <Bar yAxisId="right" dataKey="weight" fill="#f59e0b" name="Avg Weight (kg)" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="chart-container medium">
          <h3>Health Metrics Trend</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={productivityData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="day" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="milk" stroke="#3b82f6" name="Milk Production" />
              <Line type="monotone" dataKey="weight" stroke="#10b981" name="Avg Weight" />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="analytics-tables">
        <div className="table-container">
          <h3>Top Performing Cattle</h3>
          <table>
            <thead>
              <tr>
                <th>Cow Name</th>
                <th>Milk Production</th>
                <th>Weight Gain</th>
                <th>Health Score</th>
                <th>Efficiency</th>
              </tr>
            </thead>
            <tbody>
              {[
                { name: 'Bessie-001', milk: '48L/day', weight: '+12kg', health: 92, efficiency: '94%' },
                { name: 'Daisy-002', milk: '45L/day', weight: '+10kg', health: 88, efficiency: '91%' },
                { name: 'Buttercup-004', milk: '42L/day', weight: '+8kg', health: 90, efficiency: '89%' },
                { name: 'Clover-005', milk: '40L/day', weight: '+9kg', health: 85, efficiency: '87%' },
                { name: 'Moo-003', milk: '38L/day', weight: '+7kg', health: 65, efficiency: '82%' },
              ].map((cow, index) => (
                <tr key={index}>
                  <td><strong>{cow.name}</strong></td>
                  <td>{cow.milk}</td>
                  <td className="positive">{cow.weight}</td>
                  <td>
                    <div className="score-bar">
                      <div
                        className="score-fill"
                        style={{ width: `${cow.health}%` }}
                      ></div>
                      <span>{cow.health}/100</span>
                    </div>
                  </td>
                  <td>{cow.efficiency}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="table-container">
          <h3>Cost Breakdown</h3>
          <table>
            <thead>
              <tr>
                <th>Category</th>
                <th>Monthly Cost</th>
                <th>% of Total</th>
                <th>Trend</th>
              </tr>
            </thead>
            <tbody>
              {[
                { category: 'Feed', cost: '$12,500', percent: '42%', trend: '▼ 2%' },
                { category: 'Veterinary', cost: '$3,200', percent: '11%', trend: '▲ 5%' },
                { category: 'Labor', cost: '$8,000', percent: '27%', trend: '▼ 1%' },
                { category: 'Maintenance', cost: '$2,500', percent: '8%', trend: '▲ 3%' },
                { category: 'Utilities', cost: '$1,800', percent: '6%', trend: '▼ 4%' },
                { category: 'Other', cost: '$1,000', percent: '3%', trend: '▬ 0%' },
              ].map((item, index) => (
                <tr key={index}>
                  <td>{item.category}</td>
                  <td><strong>{item.cost}</strong></td>
                  <td>
                    <div className="percent-bar">
                      <div
                        className="percent-fill"
                        style={{ width: item.percent }}
                      ></div>
                      <span>{item.percent}</span>
                    </div>
                  </td>
                  <td className={item.trend.includes('▲') ? 'negative' : item.trend.includes('▼') ? 'positive' : 'neutral'}>
                    {item.trend}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="insights-section">
        <h3>Key Insights & Recommendations</h3>
        <div className="insights-grid">
          <div className="insight-card">
            <h4>📈 Opportunity</h4>
            <p>Milk production increased by 5.2% this month. Consider expanding the high-performing herd.</p>
          </div>
          <div className="insight-card">
            <h4>⚠️ Alert</h4>
            <p>Veterinary costs up by 5%. Schedule preventive checkups to reduce emergency costs.</p>
          </div>
          <div className="insight-card">
            <h4>💡 Recommendation</h4>
            <p>Optimize feeding schedule for Barn 2 - current efficiency at 78% vs target 85%.</p>
          </div>
          <div className="insight-card">
            <h4>🎯 Target</h4>
            <p>Increase operational efficiency to 95% by optimizing labor allocation.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;