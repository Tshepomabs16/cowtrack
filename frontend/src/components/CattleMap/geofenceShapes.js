// Pure helpers between what Leaflet draws and what the geofence API stores.
// Kept free of Leaflet and React so they can be tested on their own.

export const FENCE_TYPES = {
  KEEP_IN: 'KEEP_IN',
  KEEP_OUT: 'KEEP_OUT',
};

/** Six decimal places is about 10 cm, finer than any collar fix. */
const round = (degrees) => Math.round(degrees * 1e6) / 1e6;

/**
 * The shape part of a geofence request, from a layer leaflet-draw just created.
 * Rectangles are stored as four-cornered polygons: the backend only knows
 * circles and polygons, and a rectangle is one.
 *
 * Returns null for anything that cannot be a fence, such as a line.
 */
export const shapeFromLayer = (layerType, layer) => {
  if (layerType === 'circle') {
    const centre = layer.getLatLng();
    return {
      shape: 'CIRCLE',
      centerLatitude: round(centre.lat),
      centerLongitude: round(centre.lng),
      radiusMeters: Math.max(1, Math.round(layer.getRadius())),
    };
  }

  if (layerType === 'polygon' || layerType === 'rectangle') {
    // Leaflet nests a simple polygon's ring one level down.
    const rings = layer.getLatLngs();
    const ring = Array.isArray(rings[0]) ? rings[0] : rings;
    return {
      shape: 'POLYGON',
      vertices: ring.map(point => [round(point.lat), round(point.lng)]),
    };
  }

  return null;
};

/** The positions react-leaflet's Polygon expects, from a stored fence. */
export const polygonPositions = (fence) =>
  (fence.vertices || []).map(([lat, lng]) => [Number(lat), Number(lng)]);

/**
 * How a fence is drawn. A camp is a solid green line, a restricted zone a
 * dashed red one, so the kind reads from the line itself as well as its colour;
 * a camp being rested is faded and dotted.
 */
export const fenceStyle = (fence) => {
  const keepOut = fence.fenceType === FENCE_TYPES.KEEP_OUT;
  const colour = keepOut ? '#d32f2f' : '#2e7d32';

  if (fence.isActive === false) {
    return { color: '#9e9e9e', fillColor: '#9e9e9e', fillOpacity: 0.05, weight: 2, dashArray: '2 6' };
  }
  return {
    color: colour,
    fillColor: colour,
    fillOpacity: keepOut ? 0.18 : 0.08,
    weight: keepOut ? 2 : 3,
    dashArray: keepOut ? '8 6' : undefined,
  };
};

/** One line under a fence's name in the list: what it is and who is in it. */
export const describeFence = (fence) => {
  if (fence.isActive === false) return 'Switched off';
  if (fence.fenceType === FENCE_TYPES.KEEP_OUT) return 'Restricted area · whole herd';

  const count = fence.animalCount || 0;
  const animals = `${count} animal${count === 1 ? '' : 's'}`;
  return fence.animalsOutside > 0 ? `${animals} · ${fence.animalsOutside} outside` : animals;
};

/** Camps an animal can be moved into: switched on and not removed. */
export const assignableCamps = (fences) =>
  fences.filter(fence => fence.fenceType === FENCE_TYPES.KEEP_IN
    && fence.isActive !== false
    && !fence.retiredAt);
