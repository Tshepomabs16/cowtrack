import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FiSearch,
  FiBell,
  FiUser,
  FiMenu,
  FiSettings,
  FiHelpCircle,
  FiMoon,
  FiSun
} from 'react-icons/fi';
import { GiCow } from 'react-icons/gi';
import { useTheme } from '../context/ThemeContext';
import { alertsAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

/** Short human-readable age of a timestamp, for the notification list. */
const relativeTime = (timestamp) => {
  if (!timestamp) return '';
  const then = new Date(timestamp);
  if (Number.isNaN(then.getTime())) return '';

  const minutes = Math.round((Date.now() - then.getTime()) / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes} min ago`;

  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hr ago`;
  return `${Math.round(hours / 24)} d ago`;
};

const Navbar = () => {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  // Theme lives in ThemeContext so the toggle, the Settings page and the
  // persisted preference all agree. A local useState here would only have
  // changed this icon.
  const { setTheme, isDark } = useTheme();
  const { logout } = useAuth();
  const toggleTheme = () => setTheme(isDark ? 'light' : 'dark');

  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);

  // Notifications are the alert feed: unresolved alerts are the unread ones.
  const [notifications, setNotifications] = useState([]);

  const loadAlerts = useCallback(async () => {
    try {
      const response = await alertsAPI.getAll();
      const alerts = Array.isArray(response.data) ? response.data : [];
      setNotifications(alerts.map(alert => ({
        id: alert.alertId,
        title: alert.title || 'Alert',
        message: alert.message,
        time: relativeTime(alert.createdAt),
        unread: !alert.isResolved,
      })));
    } catch (error) {
      console.error('Error loading notifications:', error);
      setNotifications([]);
    }
  }, []);

  useEffect(() => { loadAlerts(); }, [loadAlerts]);

  const unreadCount = notifications.filter(n => n.unread).length;

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery)}`);
      setSearchQuery('');
    }
  };

  const handleNotificationClick = () => {
    navigate('/alerts');
    setShowNotifications(false);
  };

  // Clears the stored session; without this the route guard still sees an
  // authenticated user and bounces straight back in.
  const handleLogout = async () => {
    setShowUserMenu(false);
    await logout();
    navigate('/login', { replace: true });
  };

  const handleMarkAllRead = async () => {
    try {
      await alertsAPI.markAllAsRead();
      await loadAlerts();
    } catch (error) {
      console.error('Error resolving alerts:', error);
    }
  };

  const toggleSidebar = () => {
    const sidebar = document.querySelector('.sidebar');
    if (sidebar) {
      sidebar.classList.toggle('open');
      // Add overlay
      const overlay = document.querySelector('.sidebar-overlay');
      if (sidebar.classList.contains('open')) {
        if (!overlay) {
          const newOverlay = document.createElement('div');
          newOverlay.className = 'sidebar-overlay';
          newOverlay.onclick = toggleSidebar;
          document.querySelector('.main-content')?.appendChild(newOverlay);
        }
      } else {
        overlay?.remove();
      }
    }
  };

  const userMenuItems = [
    { icon: <FiUser />, label: 'My Profile', action: () => navigate('/profile') },
    { icon: <FiSettings />, label: 'Settings', action: () => navigate('/settings') },
    { icon: <FiHelpCircle />, label: 'Help & Support', action: () => navigate('/help') },
    {
      icon: isDark ? <FiSun /> : <FiMoon />,
      label: isDark ? 'Light Mode' : 'Dark Mode',
      action: toggleTheme
    },
  ];

  return (
    <>
      <nav className="navbar">
        <div className="navbar-content">
          {/* Left section */}
          <div className="navbar-left">
            <button
              className="menu-toggle"
              onClick={toggleSidebar}
              aria-label="Toggle menu"
            >
              <FiMenu />
            </button>

            <div
              className="brand"
              onClick={() => navigate('/dashboard')}
              style={{ cursor: 'pointer' }}
            >
              <GiCow className="brand-icon" />
              <span className="brand-text">CowTrack</span>
            </div>
          </div>

          {/* Center section - Search */}
          <div className="navbar-center">
            <form className="search-bar" onSubmit={handleSearch}>
              <FiSearch className="search-icon" />
              <input
                type="text"
                placeholder="Search cows, alerts, reports..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                aria-label="Search"
              />
              {searchQuery && (
                <button
                  type="button"
                  className="clear-search"
                  onClick={() => setSearchQuery('')}
                  aria-label="Clear search"
                >
                  ×
                </button>
              )}
            </form>
          </div>

          {/* Right section - Actions */}
          <div className="navbar-right">
            {/* Dark Mode Toggle */}
            <button
              className="nav-action-btn theme-toggle"
              onClick={toggleTheme}
              aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
            >
              {isDark ? <FiSun /> : <FiMoon />}
            </button>

            {/* Notifications */}
            <div className="notification-wrapper">
              <button
                className="nav-action-btn notification-btn"
                onClick={() => setShowNotifications(!showNotifications)}
                aria-label="Notifications"
              >
                <FiBell />
                {unreadCount > 0 && (
                  <span className="notification-badge">{unreadCount}</span>
                )}
              </button>

              {/* Notifications Dropdown */}
              {showNotifications && (
                <div className="notifications-dropdown">
                  <div className="dropdown-header">
                    <h3>Notifications</h3>
                    <button
                      className="mark-all-read"
                      onClick={handleMarkAllRead}
                    >
                      Mark all as read
                    </button>
                  </div>

                  <div className="notifications-list">
                    {notifications.length > 0 ? (
                      notifications.map(notification => (
                        <div
                          key={notification.id}
                          className={`notification-item ${notification.unread ? 'unread' : ''}`}
                          onClick={handleNotificationClick}
                        >
                          <div className="notification-content">
                            <h4>{notification.title}</h4>
                            <p>{notification.message}</p>
                            <span className="notification-time">{notification.time}</span>
                          </div>
                          {notification.unread && (
                            <span className="unread-dot"></span>
                          )}
                        </div>
                      ))
                    ) : (
                      <div className="empty-notifications">
                        No new notifications
                      </div>
                    )}
                  </div>

                  <div className="dropdown-footer">
                    <button
                      className="view-all-btn"
                      onClick={() => {
                        navigate('/alerts');
                        setShowNotifications(false);
                      }}
                    >
                      View All Notifications
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* User Profile */}
            <div className="user-wrapper">
              <button
                className="user-btn"
                onClick={() => setShowUserMenu(!showUserMenu)}
                aria-label="User menu"
              >
                <div className="user-avatar">
                  <FiUser />
                </div>
                <div className="user-info">
                  <span className="user-name">Admin User</span>
                  <span className="user-role">Farm Manager</span>
                </div>
              </button>

              {/* User Menu Dropdown */}
              {showUserMenu && (
                <div className="user-dropdown">
                  <div className="user-dropdown-header">
                    <div className="dropdown-avatar">
                      <FiUser />
                    </div>
                    <div>
                      <h4>Admin User</h4>
                      <p>admin@cowtrack.com</p>
                    </div>
                  </div>

                  <div className="user-menu-items">
                    {userMenuItems.map((item, index) => (
                      <button
                        key={index}
                        className="user-menu-item"
                        onClick={() => {
                          item.action();
                          setShowUserMenu(false);
                        }}
                      >
                        <span className="menu-item-icon">{item.icon}</span>
                        <span className="menu-item-label">{item.label}</span>
                      </button>
                    ))}
                  </div>

                  <div className="user-dropdown-footer">
                    <button
                      className="logout-btn"
                      onClick={handleLogout}
                    >
                      <FiUser />
                      Log Out
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </nav>

      {/* Click outside to close dropdowns */}
      {(showNotifications || showUserMenu) && (
        <div
          className="dropdown-backdrop"
          onClick={() => {
            setShowNotifications(false);
            setShowUserMenu(false);
          }}
        />
      )}
    </>
  );
};

export default Navbar;