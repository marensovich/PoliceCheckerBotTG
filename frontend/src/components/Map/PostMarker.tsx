/**
 * Map marker component for a DPS-related object.
 *
 * <p>Renders different emoji icons depending on the post type.
 * DPS posts, ambushes, and hidden posts fade out over time (full brightness
 * for the first 4 hours, then linear decay to 30% at 12 hours).
 * Patrol cars additionally display an expanding uncertainty radius circle.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useRef } from 'react';
import L from 'leaflet';
import type { DpsPost, PostType } from '../../types';

const EMOJI_BY_TYPE: Record<PostType, string> = {
  DPS_POST: '🚔',
  PATROL_CAR: '🚗',
  CAMERA: '📷',
  AMBUSH: '👁',
  HIDDEN_POST: '🕵️',
};

const COLOR_BY_TYPE: Record<PostType, string> = {
  DPS_POST: '#e53935',
  PATROL_CAR: '#1e88e5',
  CAMERA: '#757575',
  AMBUSH: '#6a1b9a',
  HIDDEN_POST: '#37474f',
};

function makeIcon(post: DpsPost): L.DivIcon {
  const type: PostType = post.postType ?? 'DPS_POST';
  const emoji = EMOJI_BY_TYPE[type];
  const color = COLOR_BY_TYPE[type];

  // Fade: DPS/ambush/hidden — 100% for first 4 h, then linear down to 30% at 12 h
  let opacity = 1;
  if (post.expiresAt && (type === 'DPS_POST' || type === 'AMBUSH' || type === 'HIDDEN_POST')) {
    const createdMs = new Date(post.createdAt).getTime();
    const expiresMs = new Date(post.expiresAt).getTime();
    const totalMs = expiresMs - createdMs;           // 12 h in ms
    const activeMs = totalMs * (4 / 12);             // first 4 h — full brightness
    const elapsed = Date.now() - createdMs;
    if (elapsed > activeMs) {
      const fade = (elapsed - activeMs) / (totalMs - activeMs);
      opacity = Math.max(0.3, 1 - fade * 0.7);
    }
  }

  return L.divIcon({
    className: '',
    html: `<div style="opacity:${opacity.toFixed(2)}">
      <svg width="36" height="44" viewBox="0 0 36 44" xmlns="http://www.w3.org/2000/svg">
        <path d="M18 0C8.06 0 0 8.06 0 18c0 13.5 18 26 18 26S36 31.5 36 18C36 8.06 27.94 0 18 0z" fill="${color}"/>
        <text x="18" y="24" text-anchor="middle" fill="white" font-size="18" font-weight="bold">${emoji}</text>
      </svg>
    </div>`,
    iconSize: [36, 44],
    iconAnchor: [18, 44],
    popupAnchor: [0, -44],
  });
}

/** Patrol car uncertainty radius in metres: speed × elapsed time. */
function patrolRadius(post: DpsPost): number {
  const speedKmh = post.patrolSpeedKmh ?? 60;
  const elapsedMs = Date.now() - new Date(post.createdAt).getTime();
  const elapsedHours = elapsedMs / 3_600_000;
  return speedKmh * elapsedHours * 1000;
}

interface PostMarkerProps {
  post: DpsPost;
  map: L.Map;
  cluster: L.FeatureGroup;
  onClick: (post: DpsPost) => void;
}

/**
 * Adds the post marker to the cluster layer on mount and removes it on unmount.
 * For {@code PATROL_CAR} posts, also draws an uncertainty radius circle directly
 * on the map.
 */
export default function PostMarker({ post, map, cluster, onClick }: PostMarkerProps) {
  const markerRef = useRef<L.Marker | null>(null);
  const circleRef = useRef<L.Circle | null>(null);

  useEffect(() => {
    const icon = makeIcon(post);
    const marker = L.marker([post.lat, post.lon], { icon })
      .on('click', () => onClick(post));

    cluster.addLayer(marker);
    markerRef.current = marker;

    if (post.postType === 'PATROL_CAR') {
      const radius = patrolRadius(post);
      const circle = L.circle([post.lat, post.lon], {
        radius: Math.max(50, radius),
        color: COLOR_BY_TYPE.PATROL_CAR,
        fillColor: COLOR_BY_TYPE.PATROL_CAR,
        fillOpacity: 0.08,
        weight: 1.5,
        dashArray: '6 4',
      }).addTo(map);
      circleRef.current = circle;
    }

    return () => {
      cluster.removeLayer(marker);
      circleRef.current?.remove();
    };
  }, [map, cluster, post, onClick]);

  return null;
}
