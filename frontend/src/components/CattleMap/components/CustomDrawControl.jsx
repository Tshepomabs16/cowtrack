import { useEffect, useRef } from 'react';
import { useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet-draw';
import 'leaflet-draw/dist/leaflet.draw.css';

const DRAFT_STYLE = {
  color: '#f9a825',
  fillColor: '#f9a825',
  fillOpacity: 0.15,
  weight: 2,
  dashArray: '4 4',
};

/**
 * leaflet-draw's toolbar, limited to the shapes a fence can be: polygons,
 * rectangles and circles. A finished shape is handed to `onCreated` as
 * `(layerType, layer)` and not kept on the map; the saved fence is drawn from
 * the API once it exists, so an unsaved draft never looks like a real fence.
 */
const CustomDrawControl = ({ onCreated }) => {
  const map = useMap();

  // Held in a ref so a new callback from the parent does not tear the toolbar
  // down and rebuild it on every render.
  const onCreatedRef = useRef(onCreated);
  onCreatedRef.current = onCreated;

  useEffect(() => {
    if (!map) return undefined;

    const drawControl = new L.Control.Draw({
      position: 'topleft',
      draw: {
        polygon: {
          // A camp drawn as a figure eight has no sensible inside; the backend
          // refuses one too.
          allowIntersection: false,
          showArea: true,
          shapeOptions: DRAFT_STYLE,
        },
        rectangle: { shapeOptions: DRAFT_STYLE },
        circle: { shapeOptions: DRAFT_STYLE },
        polyline: false,
        marker: false,
        circlemarker: false,
      },
    });

    const handleCreated = (event) => {
      if (onCreatedRef.current) onCreatedRef.current(event.layerType, event.layer);
    };

    map.addControl(drawControl);
    map.on(L.Draw.Event.CREATED, handleCreated);

    return () => {
      // Removed by reference: the previous version left its handlers attached,
      // so every remount added another and one drawing fired several saves.
      map.off(L.Draw.Event.CREATED, handleCreated);
      map.removeControl(drawControl);
    };
  }, [map]);

  return null;
};

export default CustomDrawControl;
