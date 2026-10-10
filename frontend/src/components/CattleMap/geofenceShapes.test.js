import {
  assignableCamps,
  describeFence,
  fenceStyle,
  polygonPositions,
  shapeFromLayer,
} from './geofenceShapes';

/** The parts of a Leaflet layer the helpers read, without Leaflet. */
const circleLayer = (lat, lng, radius) => ({
  getLatLng: () => ({ lat, lng }),
  getRadius: () => radius,
});
const polygonLayer = (points) => ({
  getLatLngs: () => [points.map(([lat, lng]) => ({ lat, lng }))],
});

describe('shapeFromLayer', () => {
  test('a drawn circle becomes a centre and a whole-metre radius', () => {
    expect(shapeFromLayer('circle', circleLayer(-23.90451234567, 29.4689, 812.6))).toEqual({
      shape: 'CIRCLE',
      centerLatitude: -23.904512,
      centerLongitude: 29.4689,
      radiusMeters: 813,
    });
  });

  test('a drawn polygon becomes its corners as [lat, lng]', () => {
    const shape = shapeFromLayer('polygon', polygonLayer([[-23.9, 29.46], [-23.9, 29.47], [-23.91, 29.47]]));
    expect(shape).toEqual({
      shape: 'POLYGON',
      vertices: [[-23.9, 29.46], [-23.9, 29.47], [-23.91, 29.47]],
    });
  });

  test('a rectangle is stored as a four-cornered polygon', () => {
    const corners = [[-23.9, 29.46], [-23.9, 29.47], [-23.91, 29.47], [-23.91, 29.46]];
    expect(shapeFromLayer('rectangle', polygonLayer(corners)).vertices).toHaveLength(4);
  });

  test('a line cannot be a fence', () => {
    expect(shapeFromLayer('polyline', polygonLayer([[-23.9, 29.46], [-23.91, 29.47]]))).toBeNull();
  });
});

test('stored corners become numeric map positions', () => {
  expect(polygonPositions({ vertices: [['-23.9', '29.46']] })).toEqual([[-23.9, 29.46]]);
  expect(polygonPositions({})).toEqual([]);
});

describe('fenceStyle', () => {
  test('a restricted area is dashed and a camp is solid', () => {
    expect(fenceStyle({ fenceType: 'KEEP_OUT', isActive: true }).dashArray).toBeDefined();
    expect(fenceStyle({ fenceType: 'KEEP_IN', isActive: true }).dashArray).toBeUndefined();
  });

  test('a switched-off fence is drawn faded whatever its kind', () => {
    expect(fenceStyle({ fenceType: 'KEEP_OUT', isActive: false }).color).toBe('#9e9e9e');
  });
});

describe('describeFence', () => {
  test('a camp says how many animals it has and how many are out', () => {
    expect(describeFence({ fenceType: 'KEEP_IN', animalCount: 12, animalsOutside: 1 }))
      .toBe('12 animals · 1 outside');
    expect(describeFence({ fenceType: 'KEEP_IN', animalCount: 1, animalsOutside: 0 })).toBe('1 animal');
  });

  test('a restricted area applies to the whole herd', () => {
    expect(describeFence({ fenceType: 'KEEP_OUT' })).toBe('Restricted area · whole herd');
  });
});

test('only live camps can take animals', () => {
  const fences = [
    { geofenceId: 1, fenceType: 'KEEP_IN', isActive: true },
    { geofenceId: 2, fenceType: 'KEEP_IN', isActive: false },
    { geofenceId: 3, fenceType: 'KEEP_OUT', isActive: true },
    { geofenceId: 4, fenceType: 'KEEP_IN', isActive: true, retiredAt: '2026-10-01 10:00:00' },
  ];
  expect(assignableCamps(fences).map(fence => fence.geofenceId)).toEqual([1]);
});
