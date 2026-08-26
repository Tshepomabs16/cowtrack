import React, { useState, useEffect } from 'react';
import { cowsAPI } from '../services/api';
import { FiFilter, FiSearch, FiPlus, FiEdit2, FiTrash2 } from 'react-icons/fi';
import { FaTemperatureHigh, FaHeartbeat } from 'react-icons/fa';
import { GiCow } from 'react-icons/gi';
import './Cows.css';

/** The API returns a full ISO timestamp; only the date and time are useful here. */
const formatCheck = (timestamp) => {
  if (!timestamp) return 'never';
  const when = new Date(timestamp);
  if (Number.isNaN(when.getTime())) return 'unknown';
  return when.toLocaleString(undefined, {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
  });
};

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

  // The request already applies the status filter server-side. Narrowing by search
  // term locally keeps typing responsive without a round trip per keystroke.
  const filteredCows = cows.filter(cow => {
    if (!searchTerm) return true;
    const query = searchTerm.toLowerCase();
    return cow.name?.toLowerCase().includes(query)
        || cow.tagId?.toLowerCase().includes(query);
  });

  return (
    <div className="cows-page page-enter">
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
            aria-label="Search cattle by name or tag"
          />
        </div>

        <div className="filters">
          <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)} aria-label="Filter by status">
            <option value="all">All Status</option>
            <option value="healthy">Healthy</option>
            <option value="feeding">Feeding</option>
            <option value="inactive">Inactive</option>
            <option value="pregnant">Pregnant</option>
            <option value="alert">Alert</option>
          </select>
          <button className="btn-primary" aria-label="Open more filters">
            <FiFilter /> More Filters
          </button>
          <button className="btn-success" aria-label="Add new cow">
            <FiPlus /> Add New Cow
          </button>
        </div>
      </div>

      <div className="cows-grid">
        {loading ? (
          Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="cow-card" style={{ cursor: 'default' }}>
              <div className="cow-header">
                <div className="skeleton" style={{ width: 40, height: 40, borderRadius: '50%' }} />
                <div style={{ flex: 1 }}>
                  <div className="skeleton" style={{ width: '50%', height: 14, marginBottom: 6 }} />
                  <div className="skeleton" style={{ width: '30%', height: 10 }} />
                </div>
              </div>
              <div className="cow-details">
                <div className="skeleton" style={{ height: 80, borderRadius: 6 }} />
              </div>
              <div className="cow-health">
                <div className="skeleton" style={{ width: 40, height: 40, borderRadius: 6 }} />
                <div className="skeleton" style={{ width: 40, height: 40, borderRadius: 6 }} />
                <div className="skeleton" style={{ width: 60, height: 14, borderRadius: 4 }} />
              </div>
              <div className="skeleton" style={{ height: 36, borderRadius: 8 }} />
            </div>
          ))
        ) : (
          filteredCows.map(cow => (
            <div key={cow.cowId} className="cow-card" data-stagger style={{ '--stagger-i': 0 }}>
              <div className="cow-header">
                <div className="cow-icon">
                  <GiCow />
                </div>
                <div className="cow-info">
                  <h3>{cow.name}</h3>
                  <p className="cow-tag">Tag: {cow.tagId}</p>
                </div>
                <span className={`status-badge ${cow.status || ''}`}>
                  {cow.status || 'unknown'}
                </span>
              </div>

              <div className="cow-details">
                <div className="detail-row">
                  <span>Breed:</span>
                  <strong>{cow.breed}</strong>
                </div>
                <div className="detail-row">
                  <span>Age:</span>
                  <strong>{cow.age ?? '—'}</strong>
                </div>
                <div className="detail-row">
                  <span>Weight:</span>
                  <strong>{cow.weight ? `${cow.weight} kg` : '—'}</strong>
                </div>
                <div className="detail-row">
                  <span>Location:</span>
                  <strong>{cow.location}</strong>
                </div>
              </div>

              <div className="cow-health">
                <div className="health-metric">
                  <FaTemperatureHigh />
                  <span>{cow.temperature ? `${cow.temperature}°C` : '—'}</span>
                </div>
                <div className="health-metric">
                  <FaHeartbeat />
                  <span>{cow.heartRate ? `${cow.heartRate} bpm` : '—'}</span>
                </div>
                <div className="last-check">
                  Last check: {formatCheck(cow.lastCheck)}
                </div>
              </div>

              <div className="cow-actions">
                <button className="btn-view" onClick={() => window.location.href = `/cows/${cow.cowId}`} aria-label={`View details for ${cow.name}`}>
                  View Details
                </button>
                <button className="btn-edit" aria-label={`Edit ${cow.name}`}>
                  <FiEdit2 />
                </button>
                <button className="btn-delete" onClick={() => handleDeleteCow(cow.cowId)} aria-label={`Delete ${cow.name}`}>
                  <FiTrash2 />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      <div className="summary-stats">
        <div className="stat-card" data-stagger style={{ '--stagger-i': 0 }}>
          <h3>Total Cattle</h3>
          <p className="stat-number">{cows.length}</p>
        </div>
        <div className="stat-card" data-stagger style={{ '--stagger-i': 1 }}>
          <h3>Healthy</h3>
          <p className="stat-number" style={{color: '#10b981'}}>
            {cows.filter(c => c.status === 'healthy').length}
          </p>
        </div>
        <div className="stat-card" data-stagger style={{ '--stagger-i': 2 }}>
          <h3>Pregnant</h3>
          <p className="stat-number" style={{color: '#8b5cf6'}}>
            {cows.filter(c => c.status === 'pregnant').length}
          </p>
        </div>
        <div className="stat-card" data-stagger style={{ '--stagger-i': 3 }}>
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