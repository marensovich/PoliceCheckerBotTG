/**
 * Hook for obtaining the user's geolocation.
 *
 * <p>First attempts to use {@code Telegram.WebApp.LocationManager} (new API,
 * available inside Telegram). Falls back to the standard
 * {@code navigator.geolocation} API when LocationManager is unavailable.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useState, useEffect } from 'react';

interface GeolocationState {
  lat: number | null;
  lon: number | null;
  error: string | null;
  loading: boolean;
}

export function useGeolocation(enabled = true): GeolocationState {
  const [state, setState] = useState<GeolocationState>({
    lat: null,
    lon: null,
    error: null,
    loading: enabled,
  });

  useEffect(() => {
    if (!enabled) {
      setState({ lat: null, lon: null, error: null, loading: false });
      return;
    }
    const tg = (window as Record<string, unknown>).Telegram as {
      WebApp?: {
        LocationManager?: {
          isInited: boolean;
          isLocationAvailable: boolean;
          init: (cb: () => void) => void;
          getLocation: (cb: (loc: { latitude: number; longitude: number } | null) => void) => void;
        };
      };
    } | undefined;

    const locationManager = tg?.WebApp?.LocationManager;

    // Telegram LocationManager API (Bot API 8.0+)
    if (locationManager) {
      const startTgLocation = () => {
        if (!locationManager.isLocationAvailable) {
          setState((s) => ({ ...s, error: 'Geolocation unavailable in Telegram', loading: false }));
          return;
        }

        const poll = () => {
          locationManager.getLocation((loc) => {
            if (loc) {
              setState({ lat: loc.latitude, lon: loc.longitude, error: null, loading: false });
            }
          });
        };

        poll();
        // Poll every 10 seconds
        const interval = setInterval(poll, 10_000);
        return () => clearInterval(interval);
      };

      if (!locationManager.isInited) {
        locationManager.init(startTgLocation);
      } else {
        return startTgLocation() ?? undefined;
      }
      return;
    }

    // Fallback: standard browser Geolocation API
    if (!navigator.geolocation) {
      setState((s) => ({ ...s, error: 'Geolocation is not supported by this browser', loading: false }));
      return;
    }

    const watchId = navigator.geolocation.watchPosition(
      (pos) => {
        setState({
          lat: pos.coords.latitude,
          lon: pos.coords.longitude,
          error: null,
          loading: false,
        });
      },
      (err) => {
        setState((s) => ({ ...s, error: err.message, loading: false }));
      },
      { enableHighAccuracy: true, maximumAge: 10_000, timeout: 15_000 },
    );

    return () => navigator.geolocation.clearWatch(watchId);
  }, []);

  return state;
}
