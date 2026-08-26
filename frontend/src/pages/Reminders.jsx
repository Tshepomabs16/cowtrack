import React, { useState, useEffect, useCallback } from 'react';
import { FiCalendar, FiCheckCircle, FiPlus, FiBell } from 'react-icons/fi';
import { format, isToday, isTomorrow, isPast } from 'date-fns';
import { remindersAPI, cowsAPI } from '../services/api';
import './Reminders.css';

const FREQUENCIES = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'ONCE'];

/**
 * Reminders are scheduled per animal, with a type and a recurrence. There is no
 * stored priority, so urgency is inferred from how the due date sits relative to
 * today.
 */
const priorityOf = (reminder) => {
  if (reminder.isOverdue) return 'high';
  const days = reminder.daysUntilDue;
  if (days === null || days === undefined) return 'low';
  if (days <= 1) return 'high';
  if (days <= 7) return 'medium';
  return 'low';
};

const Reminders = () => {
  const [reminders, setReminders] = useState([]);
  const [cows, setCows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [newReminder, setNewReminder] = useState({
    cowId: '',
    reminderType: '',
    notes: '',
    frequency: 'WEEKLY',
    startDate: format(new Date(), 'yyyy-MM-dd'),
  });

  const load = useCallback(async () => {
    try {
      const [reminderResponse, cowResponse] = await Promise.all([
        remindersAPI.getAll(),
        cowsAPI.getAll(),
      ]);
      setReminders(Array.isArray(reminderResponse.data) ? reminderResponse.data : []);
      setCows(Array.isArray(cowResponse.data) ? cowResponse.data : []);
      setError(null);
    } catch (err) {
      console.error('Error loading reminders:', err);
      setError('Could not load reminders');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleToggleComplete = async (reminder) => {
    if (reminder.isCompleted) return;
    try {
      await remindersAPI.markComplete(reminder.reminderId);
      // Re-read rather than patching locally: completing a recurring reminder
      // rolls its due date forward on the server.
      await load();
    } catch (err) {
      console.error('Error completing reminder:', err);
      setError('Could not update the reminder');
    }
  };

  const handleAddReminder = async () => {
    if (!newReminder.cowId || !newReminder.reminderType.trim()) return;

    setSaving(true);
    try {
      await remindersAPI.create({
        cowId: Number(newReminder.cowId),
        reminderType: newReminder.reminderType.trim(),
        frequency: newReminder.frequency,
        startDate: newReminder.startDate,
        notes: newReminder.notes.trim() || undefined,
      });
      setNewReminder({
        cowId: '',
        reminderType: '',
        notes: '',
        frequency: 'WEEKLY',
        startDate: format(new Date(), 'yyyy-MM-dd'),
      });
      setShowForm(false);
      await load();
    } catch (err) {
      console.error('Error creating reminder:', err);
      setError(err.response?.data?.message || 'Could not create the reminder');
    } finally {
      setSaving(false);
    }
  };

  const getDateLabel = (date) => {
    if (!date) return '—';
    const parsed = new Date(date);
    if (Number.isNaN(parsed.getTime())) return '—';
    if (isToday(parsed)) return 'Today';
    if (isTomorrow(parsed)) return 'Tomorrow';
    return format(parsed, 'MMM d');
  };

  const isDueToday = (reminder) => {
    if (!reminder.startDate) return false;
    const parsed = new Date(reminder.startDate);
    return isToday(parsed) || (isPast(parsed) && !reminder.isCompleted);
  };

  const pending = reminders.filter(r => !r.isCompleted);
  const dueToday = pending.filter(isDueToday);
  const upcoming = pending.filter(r => !isDueToday(r));
  const completed = reminders.filter(r => r.isCompleted);

  if (loading) {
    return (
      <div className="reminders-page page-enter">
        <div className="page-header">
          <div className="header-left">
            <div className="skeleton" style={{ width: 240, height: 28, marginBottom: 8 }} />
            <div className="skeleton" style={{ width: 180, height: 14 }} />
          </div>
        </div>
        <div className="reminders-overview">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="skeleton" style={{ height: 90, borderRadius: 'var(--radius)' }} />
          ))}
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 30 }}>
          <div className="skeleton" style={{ height: 300, borderRadius: 'var(--radius)' }} />
          <div className="skeleton" style={{ height: 300, borderRadius: 'var(--radius)' }} />
        </div>
      </div>
    );
  }

  return (
    <div className="reminders-page page-enter">
      <div className="page-header">
        <div className="header-left">
          <h1><FiCalendar /> Reminders & Tasks</h1>
          <p>Manage daily operations and scheduled tasks</p>
        </div>
        <button className="btn-primary" onClick={() => setShowForm(!showForm)} aria-label={showForm ? 'Close form' : 'Add new reminder'}>
          <FiPlus /> Add New Reminder
        </button>
      </div>

      {error && <div className="form-error">{error}</div>}

      {showForm && (
        <div className="reminder-form">
          <h3>Add New Reminder</h3>
          <div className="form-grid">
            <select
              value={newReminder.cowId}
              onChange={(e) => setNewReminder({ ...newReminder, cowId: e.target.value })}
            >
              <option value="">Select a cow…</option>
              {cows.map(cow => (
                <option key={cow.cowId} value={cow.cowId}>
                  {cow.name} ({cow.tagId})
                </option>
              ))}
            </select>
            <input
              type="text"
              placeholder="Reminder type, e.g. Vaccination"
              value={newReminder.reminderType}
              onChange={(e) => setNewReminder({ ...newReminder, reminderType: e.target.value })}
            />
            <input
              type="text"
              placeholder="Notes"
              value={newReminder.notes}
              onChange={(e) => setNewReminder({ ...newReminder, notes: e.target.value })}
            />
            <input
              type="date"
              value={newReminder.startDate}
              onChange={(e) => setNewReminder({ ...newReminder, startDate: e.target.value })}
            />
            <select
              value={newReminder.frequency}
              onChange={(e) => setNewReminder({ ...newReminder, frequency: e.target.value })}
            >
              {FREQUENCIES.map(frequency => (
                <option key={frequency} value={frequency}>
                  {frequency.charAt(0) + frequency.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>
          <div className="form-actions">
            <button className="btn-secondary" onClick={() => setShowForm(false)}>
              Cancel
            </button>
            <button
              className="btn-success"
              onClick={handleAddReminder}
              disabled={saving || !newReminder.cowId || !newReminder.reminderType.trim()}
            >
              {saving ? 'Saving…' : 'Add Reminder'}
            </button>
          </div>
        </div>
      )}

      <div className="reminders-overview">
        <div className="overview-card">
          <h3>Due Today</h3>
          <p className="overview-number">{dueToday.length}</p>
        </div>
        <div className="overview-card">
          <h3>Pending</h3>
          <p className="overview-number">{pending.length}</p>
        </div>
        <div className="overview-card">
          <h3>Overdue</h3>
          <p className="overview-number">{pending.filter(r => r.isOverdue).length}</p>
        </div>
        <div className="overview-card">
          <h3>Completed</h3>
          <p className="overview-number">{completed.length}</p>
        </div>
      </div>

      <div className="reminders-container">
        <div className="todays-reminders">
          <h3><FiBell /> Due Now</h3>
          <div className="reminders-list">
            {dueToday.length === 0 && <p className="empty-row">Nothing due today.</p>}
            {dueToday.map(reminder => (
              <div
                key={reminder.reminderId}
                className={`reminder-item ${priorityOf(reminder)}`}
              >
                <div className="reminder-checkbox">
                  <input
                    type="checkbox"
                    checked={Boolean(reminder.isCompleted)}
                    onChange={() => handleToggleComplete(reminder)}
                  />
                </div>
                <div className="reminder-content">
                  <h4>{reminder.reminderType}</h4>
                  <p>{reminder.notes}</p>
                  <div className="reminder-meta">
                    <span className="assigned">{reminder.cowName}</span>
                    <span className="recurring">
                      {(reminder.frequency || '').toLowerCase()}
                    </span>
                    {reminder.isOverdue && <span className="time">Overdue</span>}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="upcoming-reminders">
          <h3><FiCalendar /> Upcoming</h3>
          <div className="reminders-list">
            {upcoming.length === 0 && <p className="empty-row">Nothing scheduled.</p>}
            {upcoming
              .slice()
              .sort((a, b) => String(a.startDate).localeCompare(String(b.startDate)))
              .map(reminder => (
                <div
                  key={reminder.reminderId}
                  className={`reminder-item ${priorityOf(reminder)}`}
                >
                  <div className="reminder-date">
                    <span className="date-label">{getDateLabel(reminder.startDate)}</span>
                  </div>
                  <div className="reminder-content">
                    <h4>{reminder.reminderType}</h4>
                    <p>{reminder.notes}</p>
                    <div className="reminder-meta">
                      <span className="assigned">{reminder.cowName}</span>
                      {reminder.daysUntilDue !== null &&
                        reminder.daysUntilDue !== undefined && (
                          <span className="time">in {reminder.daysUntilDue} days</span>
                        )}
                    </div>
                  </div>
                </div>
              ))}
          </div>
        </div>
      </div>

      <div className="completed-tasks">
        <h3>Recently Completed</h3>
        <div className="tasks-grid">
          {completed.length === 0 && <p className="empty-row">Nothing completed yet.</p>}
          {completed.slice(0, 4).map(reminder => (
            <div key={reminder.reminderId} className="task-card completed">
              <div className="task-header">
                <FiCheckCircle className="completed-icon" />
                <h4>{reminder.reminderType}</h4>
              </div>
              <p>{reminder.notes}</p>
              <div className="task-footer">
                <span>{reminder.cowName}</span>
                <span>{getDateLabel(reminder.startDate)}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Reminders;
