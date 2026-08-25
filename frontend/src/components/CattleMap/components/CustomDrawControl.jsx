import { useEffect } from 'react';
import { useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet-draw';
import 'leaflet-draw/dist/leaflet.draw.css';

const CustomDrawControl = ({ onCreated, onEdited, onDeleted }) => {
  const map = useMap();

  useEffect(() => {
    if (!map) return;

    // Create a FeatureGroup for drawing
    const drawnItems = new L.FeatureGroup();
    map.addLayer(drawnItems);

    // Initialize draw control
    const drawControl = new L.Control.Draw({
      position: 'topright',
      draw: {
        polygon: {
          allowIntersection: false,
          showArea: true,
          shapeOptions: {
            color: '#3388ff',
            fillColor: '#3388ff',
            fillOpacity: 0.2,
            weight: 2
          }
        },
        polyline: {
          shapeOptions: {
            color: '#3388ff',
            weight: 4
          }
        },
        rectangle: {
          shapeOptions: {
            color: '#3388ff',
            fillColor: '#3388ff',
            fillOpacity: 0.2,
            weight: 2
          }
        },
        circle: false,
        marker: false,
        circlemarker: false
      },
      edit: {
        featureGroup: drawnItems
      }
    });

    map.addControl(drawControl);

    // Handle draw events
    map.on(L.Draw.Event.CREATED, function(event) {
      const layer = event.layer;
      drawnItems.addLayer(layer);

      // Extract coordinates based on shape type
      let coordinates;
      let type = event.layerType;

      if (type === 'polygon' || type === 'polyline') {
        coordinates = layer.getLatLngs();
      } else if (type === 'rectangle') {
        coordinates = layer.getBounds();
      }

      if (onCreated) {
        onCreated({
          type,
          coordinates,
          layer,
          id: Date.now(),
          color: '#3388ff'
        });
      }
    });

    map.on(L.Draw.Event.EDITED, function(event) {
      if (onEdited) {
        onEdited(event);
      }
    });

    map.on(L.Draw.Event.DELETED, function(event) {
      if (onDeleted) {
        onDeleted(event);
      }
    });

    // Cleanup
    return () => {
      if (map) {
        map.removeControl(drawControl);
        map.removeLayer(drawnItems);
      }
    };
  }, [map, onCreated, onEdited, onDeleted]);

  return null;
};

export default CustomDrawControl;