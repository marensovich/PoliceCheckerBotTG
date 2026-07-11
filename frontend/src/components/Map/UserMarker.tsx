/**
 * Leaflet marker component for the current user's location.
 *
 * <p>Renders a pulsing blue dot that is added to the map on mount
 * and repositioned whenever {@code lat} or {@code lon} changes.
 * The marker is removed from the map when the component unmounts.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useRef } from 'react';
import L from 'leaflet';

const USER_ICON = L.divIcon({
  className: '',
  html: `<div style="
    width: 20px; height: 20px; border-radius: 50%;
    background: #1976D2; border: 3px solid white;
    box-shadow: 0 0 0 4px rgba(25,118,210,0.3);
    animation: pulse 2s infinite;
  "></div>
  <style>
    @keyframes pulse {
      0% { box-shadow: 0 0 0 0 rgba(25,118,210,0.5); }
      70% { box-shadow: 0 0 0 12px rgba(25,118,210,0); }
      100% { box-shadow: 0 0 0 0 rgba(25,118,210,0); }
    }
  </style>`,
  iconSize: [20, 20],
  iconAnchor: [10, 10],
});

interface UserMarkerProps {
  map: L.Map;
  lat: number;
  lon: number;
}

/** User marker with a pulsing animation. Repositions automatically when coordinates change. */
export default function UserMarker({ map, lat, lon }: UserMarkerProps) {
  const markerRef = useRef<L.Marker | null>(null);

  useEffect(() => {
    if (!markerRef.current) {
      markerRef.current = L.marker([lat, lon], {
        icon: USER_ICON,
        zIndexOffset: 1000,
      }).addTo(map);
    } else {
      markerRef.current.setLatLng([lat, lon]);
    }

    return () => {
      markerRef.current?.remove();
      markerRef.current = null;
    };
  }, [map, lat, lon]);

  return null;
}
