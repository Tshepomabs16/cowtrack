import React, { useState } from 'react';
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
import './Navbar.css';

const Navbar = () => {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);

  // Mock notifications
  const notifications = [
    { id: 1, title: 'Health Alert', message: 'Cow CT-003 has high temperature', time: '10 min ago', unread: true },
    { id: 2, title: 'Feeding Reminder', message: 'Morning feeding due in 30 minutes', time: '1 hour ago', unread: true },
    { id: 3, title: 'Location Alert', message: 'Cow CT-005 left designated area', time: '2 hours ago', unread: false },
    { id: 4, title: 'System Update', message: 'Farm sensors updated successfully', time: '1 day ago', unread: false },
  ];

  const unreadCount = notifications.filter(n => n.unread).length;

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery)}`);
      setSearchQuery('');
    }
  };

  const handleNotificationClick = (notificationId) => {
    // Mark as read and navigate
    navigate('/alerts');
    setShowNotifications(false);
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
      icon: isDarkMode ? <FiSun /> : <FiMoon />,
      label: isDarkMode ? 'Light Mode' : 'Dark Mode',
      action: () => setIsDarkMode(!isDarkMode)
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
              onClick={() => setIsDarkMode(!isDarkMode)}
              aria-label={isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}
            >
              {isDarkMode ? <FiSun /> : <FiMoon />}
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
                      onClick={() => console.log('Mark all as read')}
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
                          onClick={() => handleNotificationClick(notification.id)}
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
                      onClick={() => {
                        console.log('Logging out...');
                        navigate('/login');
                        setShowUserMenu(false);
                      }}
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