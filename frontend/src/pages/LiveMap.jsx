import React, { useState, useEffect, useCallback } from 'react';
import { wsService, WS_EVENTS } from '../services/websocket';
import { locationsAPI } from '../services/api';
import { FiAlertCircle, FiCheckCircle, FiMapPin } from 'react-icons/fi';
import CattleMap from '../components/CattleMap/CattleMap';
import './LiveMap.css';

const LiveMap = () => {
  const [cows, setCows] = useState([]);
  const [isConnected, setIsConnected] = useState(false);

  const handleLocationUpdate = useCallback((update) => {
    setCows(prevCows =>
      prevCows.map(cow =>
        cow.id === update.cowId ? { ...cow, ...update.location } : cow
      )
    );
  }, []);

  const handleHealthAlert = useCallback((alert) => {
    // Show notification for health alert
    console.log('Health alert received:', alert);
    // You can add a notification system here
  }, []);

  useEffect(() => {
    const fetchCowLocations = async () => {
      try {
        const response = await locationsAPI.getLiveLocations();
        setCows(response.data);
      } catch (error) {
        console.error('Error fetching cow locations:', error);
      }
    };

    fetchCowLocations();

    wsService.connect();

    wsService.on(WS_EVENTS.CONNECTED, () => setIsConnected(true));
    wsService.on(WS_EVENTS.DISCONNECTED, () => setIsConnected(false));
    wsService.on(WS_EVENTS.LOCATION_UPDATE, handleLocationUpdate);
    wsService.on(WS_EVENTS.HEALTH_ALERT, handleHealthAlert);

    return () => {
      wsService.off(WS_EVENTS.CONNECTED);
      wsService.off(WS_EVENTS.DISCONNECTED);
      wsService.off(WS_EVENTS.LOCATION_UPDATE);
      wsService.off(WS_EVENTS.HEALTH_ALERT);
      wsService.disconnect();
    };
  }, [handleLocationUpdate, handleHealthAlert]);

  return (
    <div className="live-map-page page-enter">
      <div className="page-header">
        <div className="header-left">
          <h1><FiMapPin /> Live Map</h1>
          <p>Real-time herd positions and geofences</p>
        </div>
        <div className="header-right">
          <span className={`connection-status ${isConnected ? 'online' : 'offline'}`}>
            {isConnected
              ? <><FiCheckCircle /> Live</>
              : <><FiAlertCircle /> Disconnected</>}
          </span>
          <span className="tracked-count">{cows.length} tracked</span>
        </div>
      </div>

      <CattleMap />
    </div>
  );
};

export default LiveMap;
