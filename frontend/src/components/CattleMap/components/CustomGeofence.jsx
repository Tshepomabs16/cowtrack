import React, { useState } from 'react';
import { Polygon, Polyline, Rectangle } from 'react-leaflet';

const CustomGeofence = ({ geofence, onEdit }) => {
  const [color, setColor] = useState(geofence.color || '#3388ff');

  const handleColorChange = (newColor) => {
    setColor(newColor);
    onEdit({ ...geofence, color: newColor });
  };

  const renderShape = () => {
    const pathOptions = {
      color: color,
      fillColor: color,
      fillOpacity: 0.2,
      weight: 2
    };

    switch(geofence.type) {
      case 'polygon':
        return <Polygon pathOptions={pathOptions} positions={geofence.coordinates} />;
      case 'polyline':
        return <Polyline pathOptions={pathOptions} positions={geofence.coordinates} />;
      case 'rectangle':
        return <Rectangle pathOptions={pathOptions} bounds={geofence.coordinates} />;
      default:
        return null;
    }
  };

  return (
    <>
      {renderShape()}
      {/* Color picker could be added here */}
    </>
  );
};

export default CustomGeofence;