/**
 * Hook for managing WebSocket live-tracking via STOMP.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from '../api/client';

const WS_URL = import.meta.env.VITE_WS_URL ?? '/ws';
const LOCATION_INTERVAL_MS = 15_000;

interface UseLiveTrackingOptions {
  enabled: boolean;
  lat: number | null;
  lon: number | null;
}

/**
 * Connects to the WebSocket broker and publishes the user's location
 * every 15 seconds while live-tracking is enabled.
 *
 * @param options tracking configuration
 */
export function useLiveTracking({ enabled, lat, lon }: UseLiveTrackingOptions) {
  const clientRef = useRef<Client | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const connect = useCallback(() => {
    const token = getToken();
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('[LiveTracking] WebSocket connected');
      },
      onDisconnect: () => {
        console.log('[LiveTracking] WebSocket disconnected');
      },
    });

    client.activate();
    clientRef.current = client;
  }, []);

  const sendLocation = useCallback((currentLat: number, currentLon: number) => {
    if (clientRef.current?.connected) {
      clientRef.current.publish({
        destination: '/app/location',
        body: JSON.stringify({ lat: currentLat, lon: currentLon }),
      });
    }
  }, []);

  useEffect(() => {
    if (!enabled) {
      // Disconnect when tracking is disabled
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      return;
    }

    connect();

    intervalRef.current = setInterval(() => {
      if (lat !== null && lon !== null) {
        sendLocation(lat, lon);
      }
    }, LOCATION_INTERVAL_MS);

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
      if (clientRef.current) clientRef.current.deactivate();
    };
  }, [enabled, lat, lon, connect, sendLocation]);
}
