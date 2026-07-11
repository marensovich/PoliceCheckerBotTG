/**
 * Map page — the landing screen of the DPS Tracker Mini App.
 *
 * <p>Composes the full-screen {@link DpsMap} with the live-tracking toggle bar
 * at the bottom. Bridges geolocation data into the global store and activates
 * WebSocket live-tracking when enabled.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect } from 'react';
import DpsMap from '../components/Map/DpsMap';
import LiveTrackingToggle from '../components/LiveTracking/LiveTrackingToggle';
import { useAppStore } from '../store';
import { useGeolocation } from '../hooks/useGeolocation';
import { useLiveTracking } from '../hooks/useLiveTracking';

/** Main page with the DPS map, user marker, and live-tracking toggle. */
export default function MapPage() {
  const { locationEnabled, setUserLocation, isLiveTracking } = useAppStore();
  const { lat, lon } = useGeolocation(locationEnabled);

  useLiveTracking({ enabled: isLiveTracking, lat, lon });

  useEffect(() => {
    if (lat !== null && lon !== null) {
      setUserLocation(lat, lon);
    }
  }, [lat, lon, setUserLocation]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ flex: 1, position: 'relative' }}>
        <DpsMap />
      </div>
      <div style={bottomBar}>
        <LiveTrackingToggle />
      </div>
    </div>
  );
}

const bottomBar: React.CSSProperties = {
  padding: '12px 16px',
  background: 'var(--tg-theme-secondary-bg-color, #2c2c2e)',
  borderTop: '1px solid rgba(255,255,255,0.1)',
};
