import React, { useState, useEffect } from 'react';
import { cowsAPI } from '../services/api';
import { FiFilter, FiSearch, FiPlus, FiEdit2, FiTrash2 } from 'react-icons/fi';
import { FaTemperatureHigh, FaHeartbeat } from 'react-icons/fa';
import { GiCow } from 'react-icons/gi';
import './Cows.css';

const Cows = () => {
  const [cows, setCows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [pagination, setPagination] = useState({
    page: 1,
    limit: 10,
    total: 0,
    totalPages: 1
  });

  useEffect(() => {
      fetchCows();
    }, [pagination.page, filterStatus]);

  const fetchCows = async () => {
      try {
        setLoading(true);
        const params = {
          page: pagination.page,
          limit: pagination.limit,
          status: filterStatus !== 'all' ? filterStatus : undefined,
          search: searchTerm || undefined
        };

        // The API returns a plain list; the axios interceptor has already
        // unwrapped the ApiResponse envelope.
        const response = await cowsAPI.getAll(params);
        const list = Array.isArray(response.data) ? response.data : [];
        setCows(list);
        setPagination(prev => ({
          ...prev,
          total: list.length,
          totalPages: Math.max(1, Math.ceil(list.length / prev.limit))
        }));
      } catch (error) {
        console.error('Error fetching cows:', error);
        alert('Failed to load cattle data');
      } finally {
        setLoading(false);
      }
    };

  const handleDeleteCow = async (id) => {
      if (window.confirm('Are you sure you want to remove this cow?')) {
        try {
          await cowsAPI.delete(id);
          setCows(cows.filter(cow => cow.cowId !== id));
          alert('Cow removed successfully');
        } catch (error) {
          console.error('Error deleting cow:', error);
          alert('Failed to delete cow');
        }
      }
    };

  const statusColors = {
    healthy: '#10b981',
    feeding: '#3b82f6',
    inactive: '#f59e0b',
    pregnant: '#8b5cf6',
    alert: '#ef4444'
  };

  // The request already applies the status filter server-side. Narrowing by search
  // term locally keeps typing responsive without a round trip per keystroke.
  const filteredCows = cows.filter(cow => {
    if (!searchTerm) return true;
    const query = searchTerm.toLowerCase();
    return cow.name?.toLowerCase().includes(query)
        || cow.tagId?.toLowerCase().includes(query);
  });

  return (
    <div className="cows-page">
      <div className="page-header">
        <h1>Cattle Management</h1>
        <p>Monitor and manage your herd</p>
      </div>

      <div className="cows-controls">
        <div className="search-box">
          <FiSearch className="search-icon" />
          <input
            type="text"
            placeholder="Search by name or tag..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        <div className="filters">
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
            <option value="all">All Status</option>
            <option value="healthy">Healthy</option>
            <option value="feeding">Feeding</option>
            <option value="inactive">Inactive</option>
            <option value="pregnant">Pregnant</option>
            <option value="alert">Alert</option>
          </select>
          <button className="btn-primary">
            <FiFilter /> More Filters
          </button>
          <button className="btn-success">
            <FiPlus /> Add New Cow
          </button>
        </div>
      </div>

      <div className="cows-grid">
        {filteredCows.map(cow => (
          <div key={cow.cowId} className="cow-card">
            <div className="cow-header">
              <div className="cow-icon">
                <GiCow />
              </div>
              <div className="cow-info">
                <h3>{cow.name}</h3>
                <p className="cow-tag">Tag: {cow.tagId}</p>
              </div>
              <span
                className="status-badge"
                style={{ backgroundColor: statusColors[cow.status] || '#6b7280' }}
              >
                {cow.status}
              </span>
            </div>

            <div className="cow-details">
              <div className="detail-row">
                <span>Breed:</span>
                <strong>{cow.breed}</strong>
              </div>
              <div className="detail-row">
                <span>Age:</span>
                <strong>{cow.age}</strong>
              </div>
              <div className="detail-row">
                <span>Weight:</span>
                <strong>{cow.weight}</strong>
              </div>
              <div className="detail-row">
                <span>Location:</span>
                <strong>{cow.location}</strong>
              </div>
            </div>

            <div className="cow-health">
              <div className="health-metric">
                <FaTemperatureHigh />
                <span>{cow.temperature}</span>
              </div>
              <div className="health-metric">
                <FaHeartbeat />
                <span>65 bpm</span>
              </div>
              <div className="last-check">
                Last check: {cow.lastCheck}
              </div>
            </div>

            <div className="cow-actions">
              <button className="btn-view" onClick={() => window.location.href = `/cows/${cow.cowId}`}>
                View Details
              </button>
              <button className="btn-edit">
                <FiEdit2 />
              </button>
              <button className="btn-delete" onClick={() => handleDeleteCow(cow.cowId)}>
                <FiTrash2 />
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="summary-stats">
        <div className="stat-card">
          <h3>Total Cattle</h3>
          <p className="stat-number">{cows.length}</p>
        </div>
        <div className="stat-card">
          <h3>Healthy</h3>
          <p className="stat-number" style={{color: '#10b981'}}>
            {cows.filter(c => c.status === 'healthy').length}
          </p>
        </div>
        <div className="stat-card">
          <h3>Pregnant</h3>
          <p className="stat-number" style={{color: '#8b5cf6'}}>
            {cows.filter(c => c.status === 'pregnant').length}
          </p>
        </div>
        <div className="stat-card">
          <h3>Need Attention</h3>
          <p className="stat-number" style={{color: '#ef4444'}}>
            {cows.filter(c => c.status === 'alert').length}
          </p>
        </div>
      </div>
    </div>
  );
};

export default Cows;