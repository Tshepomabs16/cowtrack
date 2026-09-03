import React, { useState, useEffect, useCallback } from 'react';
import { realtimeService, EVENTS } from '../services/realtime';
import { FiAlertCircle, FiCheckCircle, FiMapPin } from 'react-icons/fi';
import CattleMap from '../components/CattleMap/CattleMap';
import './LiveMap.css';

const LiveMap = () => {
  // Seeded from the service rather than false: the stream is opened by the
  // layout and is normally already up by the time this page mounts, so waiting
  // for the next `connected` event would show "Disconnected" over a live map.
  const [isConnected, setIsConnected] = useState(() => realtimeService.isLive());
  const [tracked, setTracked] = useState(0);

  // Stable, so the child's effect does not re-run on every render of this one.
  const handleTrackedChange = useCallback((count) => setTracked(count), []);

  useEffect(() => {
    const onConnected = () => setIsConnected(true);
    const onDisconnected = () => setIsConnected(false);

    realtimeService.on(EVENTS.CONNECTED, onConnected);
    realtimeService.on(EVENTS.DISCONNECTED, onDisconnected);

    return () => {
      realtimeService.off(EVENTS.CONNECTED, onConnected);
      realtimeService.off(EVENTS.DISCONNECTED, onDisconnected);
    };
  }, []);

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
          <span className="tracked-count">{tracked} tracked</span>
        </div>
      </div>

      <CattleMap onTrackedChange={handleTrackedChange} />
    </div>
  );
};

export default LiveMap;
