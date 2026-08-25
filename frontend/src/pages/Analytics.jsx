import React, { useState, useEffect } from 'react';
import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { FiTrendingUp, FiUsers, FiDollarSign, FiPieChart } from 'react-icons/fi';
import { analyticsAPI } from '../services/api';
import './Analytics.css';

// Palette for the breed pie chart. Breeds come from the data, so colours are
// assigned by position rather than hardcoded per breed name.
const SLICE_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4'];

const Analytics = () => {
  const [timeRange, setTimeRange] = useState('month');
  const [financials, setFinancials] = useState(null);
  const [production, setProduction] = useState(null);
  const [dashboard, setDashboard] = useState(null);
  const [predictions, setPredictions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    const load = async () => {
      try {
        const [fin, prod, dash, pred] = await Promise.all([
          analyticsAPI.getFinancials({ range: timeRange }),
          analyticsAPI.getProductionTrends({ range: timeRange }),
          analyticsAPI.getDashboardStats(),
          analyticsAPI.getPredictions(),
        ]);
        if (cancelled) return;
        setFinancials(fin.data);
        setProduction(prod.data);
        setDashboard(dash.data);
        setPredictions(Array.isArray(pred.data) ? pred.data : []);
      } catch (error) {
        console.error('Error loading analytics:', error);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => { cancelled = true; };
  }, [timeRange]);

  const revenueData = (financials?.series || []).map((point) => ({
    month: point.month,
    revenue: Number(point.revenue) || 0,
    cost: Number(point.cost) || 0,
  }));

  const breedDistribution = (production?.breedDistribution || []).map((slice, index) => ({
    name: slice.name,
    value: Number(slice.value) || 0,
    color: SLICE_COLORS[index % SLICE_COLORS.length],
  }));

  const productivityData = (production?.series || []).map((point) => ({
    day: point.day,
    milk: Number(point.milk) || 0,
    weight: Number(point.weight) || 0,
  }));

  const money = (value) =>
    value === null || value === undefined ? '—' : `R${Number(value).toLocaleString()}`;

  const topProducers = production?.topProducers || [];
  const costBreakdown = financials?.costBreakdown || [];

  const milkTrend = predictions.find((p) => p.metric === 'Milk production');

  const kpis = loading ? [] : [
    {
      title: 'Total Revenue',
      value: money(financials?.totalRevenue),
      change: `Net ${money(financials?.netProfit)}`,
      icon: <FiDollarSign />, color: '#10b981',
    },
    {
      title: 'Avg Milk Production',
      value: dashboard?.averageMilkPerDay ? `${dashboard.averageMilkPerDay} L/day` : '—',
      change: milkTrend?.trend || 'No history',
      icon: <FiTrendingUp />, color: '#3b82f6',
    },
    {
      title: 'Herd Size',
      value: dashboard?.totalCows ?? '—',
      change: `${breedDistribution.length} breeds`,
      icon: <FiUsers />, color: '#8b5cf6',
    },
    {
      title: 'Open Alerts',
      value: dashboard?.activeAlerts ?? '—',
      change: dashboard?.activeAlerts ? 'Needs attention' : 'All clear',
      icon: <FiPieChart />, color: '#f59e0b',
    },
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
              <Tooltip formatter={(value) => [`R${Number(value).toLocaleString()}`, '']} />
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
              <Tooltip formatter={(value) => [`${value} head`, '']} />
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
                <th>Avg Milk</th>
                <th>Weight Change</th>
                <th>Total Milk</th>
              </tr>
            </thead>
            <tbody>
              {topProducers.length === 0 ? (
                <tr>
                  <td colSpan={4} className="empty-row">
                    {loading ? 'Loading…' : 'No production records for this period'}
                  </td>
                </tr>
              ) : topProducers.map((cow) => (
                <tr key={cow.cowId}>
                  <td><strong>{cow.name}</strong></td>
                  <td>{cow.averageMilk ?? '—'} L/day</td>
                  <td className={Number(cow.weightGain) >= 0 ? 'positive' : ''}>
                    {cow.weightGain === null || cow.weightGain === undefined
                      ? '—'
                      : `${Number(cow.weightGain) >= 0 ? '+' : ''}${cow.weightGain}kg`}
                  </td>
                  <td>{cow.totalMilk ?? '—'} L</td>
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
                <th>Cost</th>
                <th>% of Total</th>
              </tr>
            </thead>
            <tbody>
              {costBreakdown.length === 0 ? (
                <tr>
                  <td colSpan={3} className="empty-row">
                    {loading ? 'Loading…' : 'No costs recorded for this period'}
                  </td>
                </tr>
              ) : costBreakdown.map((item) => (
                <tr key={item.category}>
                  <td>{item.category}</td>
                  <td><strong>{money(item.amount)}</strong></td>
                  <td>
                    <div className="percent-bar">
                      <div
                        className="percent-fill"
                        style={{ width: `${item.percent}%` }}
                      ></div>
                      <span>{item.percent}%</span>
                    </div>
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
          {predictions.map((prediction) => (
            <div className="insight-card" key={prediction.metric}>
              <h4>📈 {prediction.metric}</h4>
              <p>
                Currently {prediction.current ?? '—'}, projected {prediction.projected ?? '—'}.
                {' '}{prediction.trend}
              </p>
            </div>
          ))}
          {!loading && predictions.length === 0 && (
            <div className="insight-card">
              <h4>No projections yet</h4>
              <p>Record production and financial data to see trends here.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Analytics;