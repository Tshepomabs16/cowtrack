import React from 'react';
import { Circle, Polygon, Tooltip } from 'react-leaflet';
import { describeFence, fenceStyle, polygonPositions } from '../geofenceShapes';

/** The farm's saved camps and restricted areas. */
const FenceLayer = ({ fences }) => (
  <>
    {fences.map(fence => {
      const style = fenceStyle(fence);
      const label = (
        <Tooltip sticky>
          <strong>{fence.name}</strong>
          <br />
          {describeFence(fence)}
        </Tooltip>
      );

      if (fence.shape === 'CIRCLE') {
        if (fence.centerLatitude == null || fence.centerLongitude == null) return null;
        return (
          <Circle
            key={fence.geofenceId}
            center={[Number(fence.centerLatitude), Number(fence.centerLongitude)]}
            radius={Number(fence.radiusMeters)}
            pathOptions={style}
          >
            {label}
          </Circle>
        );
      }

      return (
        <Polygon key={fence.geofenceId} positions={polygonPositions(fence)} pathOptions={style}>
          {label}
        </Polygon>
      );
    })}
  </>
);

export default FenceLayer;
