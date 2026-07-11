/**
 * AR-style SVG compass widget for the DPS Tracker map.
 *
 * <p>Uses the {@code DeviceOrientationEvent} API (with iOS 13+ permission request)
 * to read the device heading and rotate the compass rose accordingly. Nearby active
 * DPS posts are plotted as coloured dots on the compass ring at their true bearing.
 * The widget is toggled by a floating action button and can be dismissed by toggling again.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useState, useCallback } from 'react';
import type { DpsPost } from '../../types';

interface ARCompassProps {
  posts: DpsPost[];
  userLat: number | null;
  userLon: number | null;
}

const POST_COLORS: Record<string, string> = {
  DPS_POST:    '#ef5350',
  PATROL_CAR:  '#ff7043',
  CAMERA:      '#ab47bc',
  AMBUSH:      '#ef9a9a',
  HIDDEN_POST: '#ff8f00',
};

function getBearing(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const φ1 = lat1 * Math.PI / 180;
  const φ2 = lat2 * Math.PI / 180;
  const y = Math.sin(dLon) * Math.cos(φ2);
  const x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(dLon);
  return (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;
}

function getDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

const fmt = (m: number) => m < 1000 ? `${Math.round(m)}м` : `${(m / 1000).toFixed(1)}км`;

/**
 * AR compass component.
 * Renders a toggle FAB and, when active, an SVG compass rose with nearby DPS post indicators.
 */
export default function ARCompass({ posts, userLat, userLon }: ARCompassProps) {
  const [heading, setHeading]   = useState(0);
  const [active, setActive]     = useState(false);
  const [granted, setGranted]   = useState(false);

  const requestPermission = useCallback(async () => {
    // iOS 13+ requires explicit permission for DeviceOrientationEvent
    const DOE = DeviceOrientationEvent as unknown as {
      requestPermission?: () => Promise<string>;
    };
    if (typeof DOE.requestPermission === 'function') {
      try {
        const res = await DOE.requestPermission();
        setGranted(res === 'granted');
        return res === 'granted';
      } catch {
        return false;
      }
    }
    setGranted(true);
    return true;
  }, []);

  const toggle = useCallback(async () => {
    if (!active) {
      const ok = granted || await requestPermission();
      if (ok) setActive(true);
    } else {
      setActive(false);
    }
  }, [active, granted, requestPermission]);

  useEffect(() => {
    if (!active) return;
    const handler = (e: DeviceOrientationEvent) => {
      const ios = (e as unknown as { webkitCompassHeading?: number }).webkitCompassHeading;
      const h = ios != null ? ios : (e.alpha != null ? 360 - e.alpha : 0);
      setHeading(h);
    };
    window.addEventListener('deviceorientation', handler, true);
    return () => window.removeEventListener('deviceorientation', handler, true);
  }, [active]);

  if (!userLat || !userLon) return null;

  const SIZE   = 140;
  const C      = SIZE / 2;
  const RADIUS = C - 14;

  const nearby = posts
    .filter((p) => p.isActive)
    .map((p) => ({
      post:     p,
      bearing:  getBearing(userLat, userLon, p.lat, p.lon),
      distance: getDistance(userLat, userLon, p.lat, p.lon),
    }))
    .sort((a, b) => a.distance - b.distance)
    .slice(0, 8);

  return (
    <>
      {/* Toggle button */}
      <button
        onClick={toggle}
        title="AR-компас"
        style={{
          position: 'absolute', bottom: 216, right: 16, zIndex: 1000,
          width: 56, height: 56, borderRadius: '50%',
          background: active ? 'var(--tg-theme-button-color, #2196F3)' : 'rgba(28,28,30,0.85)',
          color: '#fff', border: 'none', fontSize: 22, cursor: 'pointer',
          boxShadow: '0 2px 8px rgba(0,0,0,0.4)',
          backdropFilter: 'blur(8px)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          transition: 'background 0.2s',
        }}
      >
        🧭
      </button>

      {/* Compass widget */}
      {active && (
        <div style={{
          position: 'absolute', bottom: 90, left: 16, zIndex: 1000,
          width: SIZE, height: SIZE, borderRadius: '50%',
          background: 'rgba(14,14,20,0.88)',
          backdropFilter: 'blur(14px)',
          border: '1px solid rgba(255,255,255,0.14)',
          boxShadow: '0 6px 28px rgba(0,0,0,0.55)',
        }}>
          <svg width={SIZE} height={SIZE}>
            {/* Outer ring */}
            <circle cx={C} cy={C} r={RADIUS} fill="none"
              stroke="rgba(255,255,255,0.1)" strokeWidth={1.5} />

            {/* Rotating compass rose */}
            <g transform={`rotate(${-heading}, ${C}, ${C})`}>
              {/* Cardinal labels */}
              <text x={C}      y={C - RADIUS + 13} textAnchor="middle" fill="#ef5350" fontSize="10" fontWeight="800">N</text>
              <text x={C}      y={C + RADIUS - 3}  textAnchor="middle" fill="rgba(255,255,255,0.35)" fontSize="9">S</text>
              <text x={C + RADIUS - 5} y={C + 4}   textAnchor="middle" fill="rgba(255,255,255,0.35)" fontSize="9">E</text>
              <text x={C - RADIUS + 5} y={C + 4}   textAnchor="middle" fill="rgba(255,255,255,0.35)" fontSize="9">W</text>
              {/* Tick marks */}
              {[0, 45, 90, 135, 180, 225, 270, 315].map((a) => {
                const r  = (a * Math.PI) / 180;
                const r0 = RADIUS - (a % 90 === 0 ? 7 : 4);
                return (
                  <line key={a}
                    x1={C + r0 * Math.sin(r)}      y1={C - r0 * Math.cos(r)}
                    x2={C + RADIUS * Math.sin(r)}   y2={C - RADIUS * Math.cos(r)}
                    stroke={a % 90 === 0 ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.2)'}
                    strokeWidth={a % 90 === 0 ? 2 : 1}
                  />
                );
              })}
              {/* Needle */}
              <polygon
                points={`${C},${C - RADIUS + 16} ${C - 4},${C + 8} ${C + 4},${C + 8}`}
                fill="#ef5350" opacity={0.85}
              />
              <polygon
                points={`${C},${C + RADIUS - 16} ${C - 4},${C - 8} ${C + 4},${C - 8}`}
                fill="rgba(255,255,255,0.3)"
              />
            </g>

            {/* Post indicators — fixed in screen space */}
            {nearby.map(({ post, bearing, distance }) => {
              const angle = ((bearing - heading) + 360) % 360;
              const rad   = (angle * Math.PI) / 180;
              const x     = C + RADIUS * Math.sin(rad);
              const y     = C - RADIUS * Math.cos(rad);
              const color = POST_COLORS[post.postType] ?? '#90caf9';
              const isClose = distance < 2000;
              return (
                <g key={post.id}>
                  <circle cx={x} cy={y} r={isClose ? 7 : 5}
                    fill={color} opacity={0.92}
                    stroke="rgba(0,0,0,0.4)" strokeWidth={1}
                  />
                  {distance < 3000 && (
                    <text
                      x={x + (x > C ? 10 : -10)}
                      y={y + 4}
                      textAnchor={x > C ? 'start' : 'end'}
                      fill={color} fontSize="8" fontWeight="700"
                    >
                      {fmt(distance)}
                    </text>
                  )}
                </g>
              );
            })}

            {/* User dot */}
            <circle cx={C} cy={C} r={5} fill="rgba(33,150,243,0.9)" />
            <circle cx={C} cy={C} r={2.5} fill="#fff" />
          </svg>

          {/* Heading label */}
          <div style={{
            position: 'absolute', bottom: -22, left: '50%', transform: 'translateX(-50%)',
            fontSize: 11, color: 'rgba(255,255,255,0.6)', whiteSpace: 'nowrap',
            fontVariantNumeric: 'tabular-nums',
          }}>
            {Math.round(heading)}°
          </div>
        </div>
      )}
    </>
  );
}
