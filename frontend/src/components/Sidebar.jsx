import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  FiHome,
  FiMap,
  FiBell,
  FiActivity,
  FiCalendar,
  FiPieChart,
  FiSettings,
  FiLogOut,
  FiMenu
} from 'react-icons/fi';
import { GiCow } from 'react-icons/gi';
import { useAuth } from '../context/AuthContext';
import './Sidebar.css';

const Sidebar = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [isOpen, setIsOpen] = React.useState(true);

  // Clear the stored token and user before navigating. Without this the guard on
  // "/" still sees an authenticated session and sends you straight back in.
  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const menuItems = [
    { icon: <FiHome />, label: 'Dashboard', path: '/dashboard' },
    { icon: <GiCow />, label: 'Cattle', path: '/cows' },
    { icon: <FiMap />, label: 'Live Map', path: '/live-map' },
    { icon: <FiBell />, label: 'Alerts', path: '/alerts' },
    { icon: <FiActivity />, label: 'Health', path: '/health' },
    { icon: <FiCalendar />, label: 'Reminders', path: '/reminders' },
    { icon: <FiPieChart />, label: 'Analytics', path: '/analytics' },
    { icon: <FiSettings />, label: 'Settings', path: '/settings' }
  ];

  return (
    <div className={`sidebar ${isOpen ? 'open' : ''}`}>
      <div className="sidebar-header">
        <div className="logo" onClick={() => navigate('/dashboard')} style={{ cursor: 'pointer' }}>
          <GiCow className="logo-icon" />
          <h2>CowTrack</h2>
        </div>
        <button className="menu-toggle" onClick={() => setIsOpen(!isOpen)} aria-label={isOpen ? 'Close sidebar' : 'Open sidebar'}>
          <FiMenu />
        </button>
      </div>

      <nav className="sidebar-nav">
        {menuItems.map((item, index) => (
          <NavLink
            key={index}
            to={item.path}
            className={({ isActive }) =>
              `nav-item ${isActive ? 'active' : ''}`
            }
            end={item.path === '/dashboard'}
          >
            <span className="nav-icon">{item.icon}</span>
            <span className="nav-label">{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        {/* Settings already appears in menuItems above; a second link here was a
            duplicate. Only the sign-out control belongs in the footer. */}
        <button type="button" className="nav-item nav-item-button" onClick={handleLogout}>
          <FiLogOut className="nav-icon" />
          <span className="nav-label">Logout</span>
        </button>
      </div>
    </div>
  );
};

export default Sidebar;