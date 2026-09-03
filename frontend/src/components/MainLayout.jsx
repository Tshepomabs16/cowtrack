import React, { useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Navbar from './Navbar';
import { useAuth } from '../context/AuthContext';
import { realtimeService } from '../services/realtime';
import './MainLayout.css';

const MainLayout = () => {
  const { user } = useAuth();

  // The stream belongs to the signed-in session, not to any one page. Opening it
  // on the Live Map instead meant alerts only arrived while that page happened
  // to be showing, and the notification bell never updated at all.
  useEffect(() => {
    if (!user) return undefined;

    realtimeService.connect();
    return () => realtimeService.disconnect();
  }, [user]);

  if (!user) {
    return null; // Or loading spinner
  }

  return (
    <div className="main-layout">
      <Sidebar />
      <div className="main-content">
        <Navbar />
        <div className="content-area">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default MainLayout;