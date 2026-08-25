import React, { useState } from 'react';
import { FiCalendar, FiClock, FiCheckCircle, FiPlus, FiBell } from 'react-icons/fi';
import { format, addDays, isToday, isTomorrow } from 'date-fns';
import './Reminders.css';

const Reminders = () => {
  const [reminders, setReminders] = useState([
    { id: 1, title: 'Morning Feeding', description: 'Feed all cattle in Pasture A', time: '08:00', recurring: 'daily', completed: true, priority: 'high', assigned: 'John Doe' },
    { id: 2, title: 'Health Check - Bessie', description: 'Routine checkup for pregnant cow', time: '10:00', date: format(addDays(new Date(), 0), 'yyyy-MM-dd'), completed: false, priority: 'high', assigned: 'Dr. Smith' },
    { id: 3, title: 'Vaccination Schedule', description: 'Administer vaccines to new calves', time: '14:00', date: format(addDays(new Date(), 2), 'yyyy-MM-dd'), completed: false, priority: 'medium', assigned: 'Vet Team' },
    { id: 4, title: 'Barn Cleaning', description: 'Clean and disinfect Barn 1', time: '16:00', recurring: 'weekly', completed: false, priority: 'medium', assigned: 'Maintenance' },
    { id: 5, title: 'Weighing Session', description: 'Monthly weight check for all cattle', time: '09:00', date: format(addDays(new Date(), 7), 'yyyy-MM-dd'), completed: false, priority: 'low', assigned: 'Farm Hands' },
  ]);

  const [newReminder, setNewReminder] = useState({ title: '', description: '', time: '09:00', priority: 'medium' });
  const [showForm, setShowForm] = useState(false);

  const handleToggleComplete = (id) => {
    setReminders(reminders.map(reminder =>
      reminder.id === id ? { ...reminder, completed: !reminder.completed } : reminder
    ));
  };

  const handleAddReminder = () => {
    if (!newReminder.title.trim()) return;

    const newRem = {
      id: Date.now(),
      ...newReminder,
      date: format(new Date(), 'yyyy-MM-dd'),
      completed: false,
      assigned: 'Unassigned'
    };

    setReminders([...reminders, newRem]);
    setNewReminder({ title: '', description: '', time: '09:00', priority: 'medium' });
    setShowForm(false);
  };

  const getDateLabel = (date) => {
    if (!date) return 'Today';
    const reminderDate = new Date(date);
    if (isToday(reminderDate)) return 'Today';
    if (isTomorrow(reminderDate)) return 'Tomorrow';
    return format(reminderDate, 'MMM d');
  };

  return (
    <div className="reminders-page">
      <div className="page-header">
        <div className="header-left">
          <h1><FiCalendar /> Reminders & Tasks</h1>
          <p>Manage daily operations and scheduled tasks</p>
        </div>
        <button
          className="btn-primary"
          onClick={() => setShowForm(!showForm)}
        >
          <FiPlus /> Add New Reminder
        </button>
      </div>

      {showForm && (
        <div className="reminder-form">
          <h3>Add New Reminder</h3>
          <div className="form-grid">
            <input
              type="text"
              placeholder="Task title"
              value={newReminder.title}
              onChange={(e) => setNewReminder({...newReminder, title: e.target.value})}
            />
            <input
              type="text"
              placeholder="Description"
              value={newReminder.description}
              onChange={(e) => setNewReminder({...newReminder, description: e.target.value})}
            />
            <input
              type="time"
              value={newReminder.time}
              onChange={(e) => setNewReminder({...newReminder, time: e.target.value})}
            />
            <select
              value={newReminder.priority}
              onChange={(e) => setNewReminder({...newReminder, priority: e.target.value})}
            >
              <option value="low">Low Priority</option>
              <option value="medium">Medium Priority</option>
              <option value="high">High Priority</option>
            </select>
          </div>
          <div className="form-actions">
            <button className="btn-secondary" onClick={() => setShowForm(false)}>
              Cancel
            </button>
            <button className="btn-success" onClick={handleAddReminder}>
              Add Reminder
            </button>
          </div>
        </div>
      )}

      <div className="reminders-overview">
        <div className="overview-card">
          <h3>Today's Tasks</h3>
          <p className="overview-number">
            {reminders.filter(r => r.date === format(new Date(), 'yyyy-MM-dd') || r.recurring).length}
          </p>
        </div>
        <div className="overview-card">
          <h3>Pending</h3>
          <p className="overview-number">
            {reminders.filter(r => !r.completed).length}
          </p>
        </div>
        <div className="overview-card">
          <h3>High Priority</h3>
          <p className="overview-number">
            {reminders.filter(r => r.priority === 'high' && !r.completed).length}
          </p>
        </div>
        <div className="overview-card">
          <h3>Completed Today</h3>
          <p className="overview-number">
            {reminders.filter(r => r.completed && (r.date === format(new Date(), 'yyyy-MM-dd') || r.recurring)).length}
          </p>
        </div>
      </div>

      <div className="reminders-container">
        <div className="todays-reminders">
          <h3><FiBell /> Today's Schedule</h3>
          <div className="reminders-list">
            {reminders
              .filter(r => r.date === format(new Date(), 'yyyy-MM-dd') || r.recurring === 'daily')
              .sort((a, b) => a.time.localeCompare(b.time))
              .map(reminder => (
                <div key={reminder.id} className={`reminder-item ${reminder.priority} ${reminder.completed ? 'completed' : ''}`}>
                  <div className="reminder-checkbox">
                    <input
                      type="checkbox"
                      checked={reminder.completed}
                      onChange={() => handleToggleComplete(reminder.id)}
                    />
                  </div>
                  <div className="reminder-content">
                    <h4>{reminder.title}</h4>
                    <p>{reminder.description}</p>
                    <div className="reminder-meta">
                      <span className="time">
                        <FiClock /> {reminder.time}
                      </span>
                      <span className="assigned">Assigned to: {reminder.assigned}</span>
                      {reminder.recurring && (
                        <span className="recurring">{reminder.recurring}</span>
                      )}
                    </div>
                  </div>
                  <div className="reminder-actions">
                    <button className="btn-small">Edit</button>
                    <button className="btn-small">Snooze</button>
                  </div>
                </div>
              ))}
          </div>
        </div>

        <div className="upcoming-reminders">
          <h3><FiCalendar /> Upcoming Tasks</h3>
          <div className="reminders-list">
            {reminders
              .filter(r => r.date && r.date !== format(new Date(), 'yyyy-MM-dd') && !r.recurring)
              .sort((a, b) => a.date.localeCompare(b.date))
              .map(reminder => (
                <div key={reminder.id} className={`reminder-item ${reminder.priority}`}>
                  <div className="reminder-date">
                    <span className="date-label">
                      {getDateLabel(reminder.date)}
                    </span>
                  </div>
                  <div className="reminder-content">
                    <h4>{reminder.title}</h4>
                    <p>{reminder.description}</p>
                    <div className="reminder-meta">
                      <span className="time">
                        <FiClock /> {reminder.time}
                      </span>
                      <span className="assigned">{reminder.assigned}</span>
                    </div>
                  </div>
                  <div className="reminder-actions">
                    <button className="btn-small">Reschedule</button>
                  </div>
                </div>
              ))}
          </div>
        </div>
      </div>

      <div className="completed-tasks">
        <h3>Recently Completed</h3>
        <div className="tasks-grid">
          {reminders
            .filter(r => r.completed)
            .slice(0, 4)
            .map(reminder => (
              <div key={reminder.id} className="task-card completed">
                <div className="task-header">
                  <FiCheckCircle className="completed-icon" />
                  <h4>{reminder.title}</h4>
                </div>
                <p>{reminder.description}</p>
                <div className="task-footer">
                  <span>Completed by: {reminder.assigned}</span>
                  <span>{reminder.time}</span>
                </div>
              </div>
            ))}
        </div>
      </div>
    </div>
  );
};

export default Reminders;