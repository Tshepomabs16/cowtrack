import React, { useState, useEffect, useCallback } from 'react';
import { cowsAPI } from '../services/api';
import { FiFilter, FiSearch, FiPlus, FiEdit2, FiTrash2, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
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

const PAGE_SIZE = 12;

const Cows = () => {
  const [cows, setCows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');

  // Page numbers are 0-based, as the API returns them, so nothing has to
  // translate between two conventions.
  const [page, setPage] = useState(0);
  const [pageInfo, setPageInfo] = useState({
    totalPages: 1,
    totalItems: 0,
    hasNext: false,
    hasPrevious: false,
  });

  // Applied to the query only once typing pauses. The search now runs against
  // the whole herd rather than the page in hand, so it is a real request and
  // should not fire on every keystroke.
  const [appliedSearch, setAppliedSearch] = useState('');
  useEffect(() => {
    const timer = setTimeout(() => {
      setAppliedSearch(searchTerm.trim());
      setPage(0); // a narrower result set makes the old page number meaningless
    }, 300);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  const fetchCows = useCallback(async () => {
    try {
      setLoading(true);
      const response = await cowsAPI.getPage({
        page,
        size: PAGE_SIZE,
        search: appliedSearch || undefined,
      });

      // A page, not a list: the envelope carries the herd total, which is what
      // the page count is built from. It used to be inferred from the length of
      // the array itself, so there was always exactly one page.
      const body = response.data || {};
      setCows(Array.isArray(body.content) ? body.content : []);
      setPageInfo({
        totalPages: body.totalPages ?? 1,
        totalItems: body.totalItems ?? 0,
        hasNext: Boolean(body.hasNext),
        hasPrevious: Boolean(body.hasPrevious),
      });
    } catch (error) {
      console.error('Error fetching cows:', error);
      alert('Failed to load cattle data');
    } finally {
      setLoading(false);
    }
  }, [page, appliedSearch]);

  useEffect(() => { fetchCows(); }, [fetchCows]);

  const handleDeleteCow = async (id) => {
      if (window.confirm('Are you sure you want to remove this cow?')) {
        try {
          await cowsAPI.delete(id);
          // Refetched rather than spliced out locally: removing a row from a
          // page leaves it a row short, and the herd total stale.
          fetchCows();
          alert('Cow removed successfully');
        } catch (error) {
          console.error('Error deleting cow:', error);
          alert('Failed to delete cow');
        }
      }
    };

  const filteredCows = cows;

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

      {!loading && pageInfo.totalPages > 1 && (
        <nav className="pagination" aria-label="Cattle list pages">
          <button
            className="page-btn"
            onClick={() => setPage(current => Math.max(0, current - 1))}
            disabled={!pageInfo.hasPrevious}
            aria-label="Previous page"
          >
            <FiChevronLeft /> Previous
          </button>

          <span className="page-status" aria-live="polite">
            Page {page + 1} of {pageInfo.totalPages}
          </span>

          <button
            className="page-btn"
            onClick={() => setPage(current => current + 1)}
            disabled={!pageInfo.hasNext}
            aria-label="Next page"
          >
            Next <FiChevronRight />
          </button>
        </nav>
      )}

      <div className="summary-stats">
        <div className="stat-card" data-stagger style={{ '--stagger-i': 0 }}>
          <h3>Total Cattle</h3>
          {/* The herd total from the API, not the length of the page. */}
          <p className="stat-number">{pageInfo.totalItems}</p>
        </div>
        {/* These three count the page in front of you, not the herd: status is
            derived per animal after loading, so it cannot be totalled without
            fetching every animal — the thing paging is here to avoid. */}
        <div className="stat-card" data-stagger style={{ '--stagger-i': 1 }}>
          <h3>Healthy on this page</h3>
          <p className="stat-number" style={{color: '#10b981'}}>
            {cows.filter(c => c.status === 'healthy').length}
          </p>
        </div>
        <div className="stat-card" data-stagger style={{ '--stagger-i': 2 }}>
          <h3>Inactive on this page</h3>
          <p className="stat-number" style={{color: '#8b5cf6'}}>
            {cows.filter(c => c.status === 'inactive').length}
          </p>
        </div>
        <div className="stat-card" data-stagger style={{ '--stagger-i': 3 }}>
          <h3>Need attention on this page</h3>
          <p className="stat-number" style={{color: '#ef4444'}}>
            {cows.filter(c => c.status === 'alert').length}
          </p>
        </div>
      </div>
    </div>
  );
};

export default Cows;