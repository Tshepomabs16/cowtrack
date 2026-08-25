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
import './Sidebar.css';

const Sidebar = () => {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = React.useState(true);

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
        <button className="menu-toggle" onClick={() => setIsOpen(!isOpen)}>
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
        <NavLink to="/settings" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <FiSettings className="nav-icon" />
          <span className="nav-label">Settings</span>
        </NavLink>
        <div
          className="nav-item"
          onClick={() => {
            // Handle logout logic here
            console.log('Logging out...');
            navigate('/');
          }}
          style={{ cursor: 'pointer' }}
        >
          <FiLogOut className="nav-icon" />
          <span className="nav-label">Logout</span>
        </div>
      </div>
    </div>
  );
};

export default Sidebar;