/**
 * Toggle button for enabling and disabling WebSocket live-tracking.
 *
 * <p>Visible only to Premium subscribers. Calls the backend to sync the
 * tracking state, then updates the global store. Shows an inline error
 * message if the API call fails.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useState } from 'react';
import { toggleLiveTracking } from '../../api/client';
import { useAppStore } from '../../store';

/** Enable/disable button for live-tracking (Premium subscribers only). */
export default function LiveTrackingToggle() {
  const { profile, isLiveTracking, setLiveTracking } = useAppStore();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!profile?.isSubscribed) {
    return (
      <div style={container}>
        <span style={{ fontSize: 13, opacity: 0.6 }}>
          🔒 Live-трекинг — только Premium
        </span>
      </div>
    );
  }

  const handleToggle = async () => {
    setLoading(true);
    setError(null);
    try {
      const newState = !isLiveTracking;
      await toggleLiveTracking(newState);
      setLiveTracking(newState);
    } catch {
      setError('Failed to toggle tracking');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={container}>
      <button onClick={handleToggle} disabled={loading} style={btn(isLiveTracking)}>
        {isLiveTracking ? '🔴 Остановить трекинг' : '🟢 Начать отслеживание'}
      </button>
      {error && <span style={{ fontSize: 12, color: '#f44336' }}>{error}</span>}
    </div>
  );
}

const container: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', gap: 4, alignItems: 'center',
};

const btn = (active: boolean): React.CSSProperties => ({
  background: active ? '#f44336' : '#4caf50',
  color: '#fff', border: 'none', borderRadius: 8,
  padding: '8px 16px', cursor: 'pointer', fontSize: 14, fontWeight: 600,
});
