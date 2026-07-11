/**
 * Main map component for DPS Tracker, built on Leaflet + OpenStreetMap.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet.markercluster';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import 'leaflet.markercluster/dist/MarkerCluster.Default.css';
import { useAppStore } from '../../store';
import { usePosts } from '../../hooks/usePosts';
import { createPost } from '../../api/client';
import { hapticSuccess } from '../../telegram/webapp';
import { useSound } from '../../hooks/useSound';
import PostPopup from '../PostCard/PostPopup';
import AddPostPopup from './AddPostPopup';
import UserMarker from './UserMarker';
import PostMarker from './PostMarker';
import type { DpsPost } from '../../types';
import { useRecentlyViewed } from '../../hooks/useRecentlyViewed';

// Fix Leaflet default icon paths when bundled by Vite
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
delete (L.Icon.Default.prototype as Record<string, unknown>)._getIconUrl;
L.Icon.Default.mergeOptions({ iconUrl: markerIcon, shadowUrl: markerShadow });

const DEFAULT_CENTER: [number, number] = [55.7558, 37.6173]; // Moscow
const DEFAULT_ZOOM = 13;

const fabStyle = (bottom: number): React.CSSProperties => ({
  position: 'absolute',
  bottom,
  right: 16,
  zIndex: 1000,
  width: 56,
  height: 56,
  borderRadius: '50%',
  background: 'var(--tg-theme-button-color, #2196F3)',
  color: 'white',
  border: 'none',
  fontSize: 24,
  cursor: 'pointer',
  boxShadow: '0 2px 8px rgba(0,0,0,0.4)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
});

const confirmBtnStyle: React.CSSProperties = {
  padding: '12px 20px',
  borderRadius: 12,
  border: 'none',
  background: 'var(--tg-theme-button-color, #2196F3)',
  color: 'var(--tg-theme-button-text-color, #fff)',
  fontSize: 15,
  fontWeight: 600,
  cursor: 'pointer',
  boxShadow: '0 2px 12px rgba(0,0,0,0.35)',
  whiteSpace: 'nowrap',
};

const filterBar: React.CSSProperties = {
  position: 'absolute',
  top: 10,
  left: '50%',
  transform: 'translateX(-50%)',
  zIndex: 1000,
  display: 'flex',
  gap: 4,
  background: 'rgba(28,28,30,0.85)',
  borderRadius: 20,
  padding: '4px 6px',
  backdropFilter: 'blur(8px)',
};

const filterChip = (active: boolean): React.CSSProperties => ({
  background: active ? 'var(--tg-theme-button-color, #2196F3)' : 'rgba(255,255,255,0.12)',
  color: '#fff',
  border: 'none',
  borderRadius: 14,
  padding: '4px 10px',
  fontSize: 13,
  cursor: 'pointer',
  fontWeight: active ? 700 : 400,
  transition: 'background 0.15s',
});

const cancelBtnStyle: React.CSSProperties = {
  padding: '12px 20px',
  borderRadius: 12,
  border: 'none',
  background: 'rgba(0,0,0,0.55)',
  color: '#fff',
  fontSize: 15,
  cursor: 'pointer',
  boxShadow: '0 2px 12px rgba(0,0,0,0.35)',
  whiteSpace: 'nowrap',
};

/**
 * Full-screen Leaflet map: renders DPS post markers, the user's position marker,
 * and supports a long-press gesture to add a new post.
 */
export default function DpsMap() {
  const mapRef = useRef<HTMLDivElement>(null);
  const leafletMap = useRef<L.Map | null>(null);
  const clusterRef = useRef<L.MarkerClusterGroup | null>(null);
  const longPressTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const { userLat, userLon, posts, addPost, setSelectedPost, selectedPost, addToast, profile } = useAppStore();
  const maxRadiusKm = profile?.isSubscribed ? 50 : 5;
  const { refresh } = usePosts();
  const { playNotification, playConfirm } = useSound();
  const prevPostCount = useRef(0);

  const { add: addRecentlyViewed } = useRecentlyViewed();
  const [mapReady, setMapReady] = useState(false);
  const [showAddPopup, setShowAddPopup] = useState(false);
  const [placingMode, setPlacingMode] = useState(false);
  const [addLat, setAddLat] = useState<number>(0);
  const [addLon, setAddLon] = useState<number>(0);
  const [activeFilter, setActiveFilter] = useState<string | null>(null);
  const centeredOnce = useRef(false);

  // Initialise the map
  useEffect(() => {
    if (!mapRef.current || leafletMap.current) return;

    const map = L.map(mapRef.current, {
      center: DEFAULT_CENTER,
      zoom: DEFAULT_ZOOM,
      zoomControl: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map);

    const cluster = L.markerClusterGroup({ chunkedLoading: true, maxClusterRadius: 60 });
    cluster.addTo(map);
    clusterRef.current = cluster;

    // Long-press — add a new post
    map.on('mousedown touchstart', (e: L.LeafletMouseEvent) => {
      longPressTimer.current = setTimeout(() => {
        setAddLat(e.latlng.lat);
        setAddLon(e.latlng.lng);
        setShowAddPopup(true);
      }, 600);
    });

    map.on('mouseup touchend', () => {
      if (longPressTimer.current) {
        clearTimeout(longPressTimer.current);
        longPressTimer.current = null;
      }
    });

    leafletMap.current = map;
    setMapReady(true);

    // Load posts on mount
    if (userLat && userLon) {
      refresh(userLat, userLon, maxRadiusKm);
    } else {
      refresh(DEFAULT_CENTER[0], DEFAULT_CENTER[1], maxRadiusKm);
    }

    return () => {
      map.remove();
      leafletMap.current = null;
      clusterRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Centre the map ONCE on the first geolocation fix
  useEffect(() => {
    if (leafletMap.current && userLat && userLon && !centeredOnce.current) {
      centeredOnce.current = true;
      leafletMap.current.setView([userLat, userLon], DEFAULT_ZOOM);
      refresh(userLat, userLon, maxRadiusKm);
    }
  }, [userLat, userLon, refresh, maxRadiusKm]);

  // Sound notification when new posts appear
  useEffect(() => {
    if (prevPostCount.current > 0 && posts.length > prevPostCount.current) {
      playNotification();
      addToast(`📍 +${posts.length - prevPostCount.current} новых постов рядом`, 'info');
    }
    prevPostCount.current = posts.length;
  }, [posts.length, playNotification, addToast]);

  // Auto-refresh posts every 5 minutes
  useEffect(() => {
    const interval = setInterval(() => {
      const lat = userLat ?? DEFAULT_CENTER[0];
      const lon = userLon ?? DEFAULT_CENTER[1];
      refresh(lat, lon, maxRadiusKm);
    }, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, [userLat, userLon, refresh, maxRadiusKm]);

  const handleAddPost = async (
    lat: number,
    lon: number,
    description?: string,
    postType?: string,
    patrolSpeedKmh?: number,
  ) => {
    try {
      const newPost = await createPost(lat, lon, description, postType, patrolSpeedKmh);
      addPost(newPost);
      hapticSuccess();
      playConfirm();
      addToast('✅ Пост добавлен на карту', 'success');
    } catch {
      // error is handled inside AddPostPopup
    }
    setShowAddPopup(false);
  };

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      <div ref={mapRef} style={{ width: '100%', height: '100%' }} />

      {/* Crosshair — visible only in placement mode */}
      {placingMode && (
        <>
          {/* Pin centred on the map */}
          <div style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -100%)',
            zIndex: 1001,
            pointerEvents: 'none',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
          }}>
            <div style={{
              fontSize: 40,
              lineHeight: 1,
              filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.5))',
            }}>📍</div>
          </div>

          {/* Horizontal crosshair line */}
          <div style={{
            position: 'absolute',
            top: '50%',
            left: '20%',
            width: '60%',
            height: 1,
            background: 'rgba(255,59,48,0.6)',
            zIndex: 1001,
            pointerEvents: 'none',
          }} />
          <div style={{
            position: 'absolute',
            top: '20%',
            left: '50%',
            width: 1,
            height: '60%',
            background: 'rgba(255,59,48,0.6)',
            zIndex: 1001,
            pointerEvents: 'none',
          }} />

          {/* Confirm/cancel buttons */}
          <div style={{
            position: 'absolute',
            bottom: 16,
            left: '50%',
            transform: 'translateX(-50%)',
            zIndex: 1002,
            display: 'flex',
            gap: 12,
          }}>
            <button
              onClick={() => setPlacingMode(false)}
              style={cancelBtnStyle}
            >
              Отмена
            </button>
            <button
              onClick={() => {
                const center = leafletMap.current?.getCenter();
                if (center) {
                  setAddLat(center.lat);
                  setAddLon(center.lng);
                }
                setPlacingMode(false);
                setShowAddPopup(true);
              }}
              style={confirmBtnStyle}
            >
              📍 Поставить здесь
            </button>
          </div>
        </>
      )}

      {/* Right-side FABs — hidden in placement mode */}
      {!placingMode && (
        <>
          {/* Add post FAB */}
          <button
            onClick={() => setPlacingMode(true)}
            style={fabStyle(80)}
          >
            +
          </button>

          {/* Return to my location FAB */}
          {userLat && userLon && (
            <button
              onClick={() => {
                leafletMap.current?.setView([userLat, userLon], DEFAULT_ZOOM, { animate: true });
              }}
              title="Моё местоположение"
              style={fabStyle(148)}
            >
              📍
            </button>
          )}
        </>
      )}

      {/* Post type filter bar */}
      {!placingMode && (
        <div style={filterBar}>
          {[
            { type: null, label: 'Все' },
            { type: 'DPS_POST', label: '🚔' },
            { type: 'PATROL_CAR', label: '🚗' },
            { type: 'CAMERA', label: '📷' },
            { type: 'AMBUSH', label: '👁' },
            { type: 'HIDDEN_POST', label: '🕵️' },
          ].map(({ type, label }) => (
            <button
              key={type ?? 'all'}
              onClick={() => setActiveFilter(type)}
              style={filterChip(activeFilter === type)}
            >
              {label}
            </button>
          ))}
        </div>
      )}

      {/* Post markers */}
      {mapReady && leafletMap.current && clusterRef.current && posts
        .filter((p) => !activeFilter || p.postType === activeFilter)
        .map((post) => (
          <PostMarker
            key={post.id}
            post={post}
            map={leafletMap.current!}
            cluster={clusterRef.current!}
            onClick={(p) => { setSelectedPost(p); addRecentlyViewed(p); }}
          />
        ))}

      {/* User location marker */}
      {mapReady && leafletMap.current && userLat && userLon && (
        <UserMarker map={leafletMap.current} lat={userLat} lon={userLon} />
      )}

      {/* Add-post popup */}
      {showAddPopup && (
        <AddPostPopup
          lat={addLat}
          lon={addLon}
          onConfirm={handleAddPost}
          onCancel={() => setShowAddPopup(false)}
        />
      )}

      {/* Post details popup */}
      {selectedPost && (
        <PostPopup post={selectedPost} onClose={() => setSelectedPost(null)} />
      )}
    </div>
  );
}
