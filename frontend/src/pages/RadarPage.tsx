/**
 * Radar page — a full-screen SVG compass showing nearest DPS posts
 * and allowing navigation to the map on tap.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useRef, useState, useCallback } from 'react';
import { useAppStore } from '../store';
import { POST_TYPE_LABELS } from '../types';
import type { DpsPost } from '../types';

// ── Post type colours ─────────────────────────────────────────────────────────

const POST_COLORS: Record<string, string> = {
  DPS_POST:    '#ef5350',
  PATROL_CAR:  '#ff7043',
  CAMERA:      '#ab47bc',
  AMBUSH:      '#ef9a9a',
  HIDDEN_POST: '#ff8f00',
};

// ── Geometry helpers ──────────────────────────────────────────────────────────

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

function fmtDist(m: number): string {
  return m < 1000 ? `${Math.round(m)} м` : `${(m / 1000).toFixed(1)} км`;
}

function fmtBearing(deg: number): string {
  const dirs = ['С', 'СВ', 'В', 'ЮВ', 'Ю', 'ЮЗ', 'З', 'СЗ'];
  return dirs[Math.round(deg / 45) % 8];
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function RadarPage() {
  const { posts, userLat, userLon, setSelectedPost, setCurrentPage, profile } = useAppStore();
  const maxRadiusKm = profile?.isSubscribed ? 50 : 5;

  const [heading, setHeading] = useState(0);
  const [granted, setGranted]  = useState(false);
  const [hasGyro, setHasGyro]  = useState(true);
  const [highlightId, setHighlightId] = useState<number | null>(null);
  const listRef = useRef<HTMLDivElement>(null);

  // Request permission and subscribe to the gyroscope
  const requestPermission = useCallback(async () => {
    const DOE = DeviceOrientationEvent as unknown as { requestPermission?: () => Promise<string> };
    if (typeof DOE.requestPermission === 'function') {
      try {
        const res = await DOE.requestPermission();
        return res === 'granted';
      } catch {
        return false;
      }
    }
    return true;
  }, []);

  useEffect(() => {
    let ok = false;
    const init = async () => {
      ok = granted || await requestPermission();
      setGranted(ok);
      if (!ok) setHasGyro(false);
    };
    init();
  }, []);  // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!granted) return;
    let gotEvent = false;
    const handler = (e: DeviceOrientationEvent) => {
      gotEvent = true;
      const ios = (e as unknown as { webkitCompassHeading?: number }).webkitCompassHeading;
      const h = ios != null ? ios : (e.alpha != null ? 360 - e.alpha : 0);
      setHeading(h);
    };
    window.addEventListener('deviceorientation', handler, true);
    // If no event arrives within 2 s, assume no gyroscope
    const t = setTimeout(() => { if (!gotEvent) setHasGyro(false); }, 2000);
    return () => {
      window.removeEventListener('deviceorientation', handler, true);
      clearTimeout(t);
    };
  }, [granted]);

  // Nearest active posts within the subscription radius
  const nearby = (userLat && userLon)
    ? posts
        .filter((p) => p.isActive)
        .map((p) => ({
          post: p,
          bearing:  getBearing(userLat, userLon, p.lat, p.lon),
          distance: getDistance(userLat, userLon, p.lat, p.lon),
        }))
        .filter((p) => p.distance <= maxRadiusKm * 1000)
        .sort((a, b) => a.distance - b.distance)
        .slice(0, 20)
    : [];

  const handlePostSelect = (post: DpsPost) => {
    setSelectedPost(post);
    setCurrentPage('map');
  };

  const scrollToPost = (id: number) => {
    setHighlightId(id);
    const el = document.getElementById(`radar-post-${id}`);
    el?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    setTimeout(() => setHighlightId(null), 1500);
  };

  // ── SVG compass ──────────────────────────────────────────────────────────────

  const SIZE   = 280;
  const C      = SIZE / 2;
  const RING   = C - 20;   // post ring
  const INNER  = RING - 28; // inner circle

  return (
    <div style={page}>
      {/* Header */}
      <div style={header}>
        <h2 style={title}>🧭 Радар</h2>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 2 }}>
          <span style={{ fontSize: 11, color: profile?.isSubscribed ? '#ffd54f' : 'rgba(255,255,255,0.45)' }}>
            {profile?.isSubscribed ? `⭐ до ${maxRadiusKm} км` : `🆓 до ${maxRadiusKm} км`}
          </span>
          {!hasGyro && (
            <span style={{ fontSize: 11, color: '#ffd600', opacity: 0.85 }}>
              Гироскоп недоступен
            </span>
          )}
        </div>
      </div>

      {/* No geolocation */}
      {(!userLat || !userLon) && (
        <div style={noGeo}>
          <span style={{ fontSize: 40 }}>📍</span>
          <p style={{ opacity: 0.6, margin: '8px 0 0' }}>Включите геолокацию для работы радара</p>
        </div>
      )}

      {/* Compass */}
      {userLat && userLon && (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 0 4px' }}>
          <svg width={SIZE} height={SIZE} style={{ overflow: 'visible' }}>
            {/* Background */}
            <defs>
              <radialGradient id="bgGrad" cx="50%" cy="50%" r="50%">
                <stop offset="0%"   stopColor="rgba(33,150,243,0.05)" />
                <stop offset="100%" stopColor="rgba(14,14,20,0)" />
              </radialGradient>
            </defs>
            <circle cx={C} cy={C} r={RING + 16} fill="rgba(14,14,22,0.92)" />
            <circle cx={C} cy={C} r={RING + 16} fill="url(#bgGrad)" />

            {/* Concentric rings */}
            {[0.33, 0.66, 1.0].map((frac) => (
              <circle key={frac} cx={C} cy={C} r={INNER * frac}
                fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth={1} />
            ))}

            {/* Post ring */}
            <circle cx={C} cy={C} r={RING}
              fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth={1.5}
              strokeDasharray="3 4" />

            {/* Rotating rose */}
            <g transform={`rotate(${-heading}, ${C}, ${C})`}>
              {/* Tick marks every 10° */}
              {Array.from({ length: 36 }, (_, i) => i * 10).map((a) => {
                const r  = (a * Math.PI) / 180;
                const isMaj = a % 90 === 0;
                const isMed = a % 45 === 0 && !isMaj;
                const len = isMaj ? 12 : isMed ? 8 : 4;
                return (
                  <line key={a}
                    x1={C + (RING - len) * Math.sin(r)} y1={C - (RING - len) * Math.cos(r)}
                    x2={C + RING * Math.sin(r)}          y2={C - RING * Math.cos(r)}
                    stroke={isMaj ? 'rgba(255,255,255,0.6)' : isMed ? 'rgba(255,255,255,0.25)' : 'rgba(255,255,255,0.1)'}
                    strokeWidth={isMaj ? 2 : 1}
                  />
                );
              })}

              {/* Cardinal directions */}
              {[
                { a: 0,   l: 'С', c: '#ef5350', fw: '900' },
                { a: 90,  l: 'В', c: 'rgba(255,255,255,0.55)', fw: '600' },
                { a: 180, l: 'Ю', c: 'rgba(255,255,255,0.55)', fw: '600' },
                { a: 270, l: 'З', c: 'rgba(255,255,255,0.55)', fw: '600' },
              ].map(({ a, l, c, fw }) => {
                const r = (a * Math.PI) / 180;
                const d = RING - 18;
                return (
                  <text key={a}
                    x={C + d * Math.sin(r)} y={C - d * Math.cos(r) + 4}
                    textAnchor="middle" fill={c} fontSize="13" fontWeight={fw}
                  >{l}</text>
                );
              })}

              {/* North needle */}
              <polygon points={`${C},${C - INNER + 6} ${C - 6},${C + 10} ${C + 6},${C + 10}`}
                fill="#ef5350" opacity={0.9} />
              <polygon points={`${C},${C + INNER - 6} ${C - 6},${C - 10} ${C + 6},${C - 10}`}
                fill="rgba(255,255,255,0.2)" />
              {/* Centre hub */}
              <circle cx={C} cy={C} r={7} fill="#1a1a2e" stroke="rgba(255,255,255,0.3)" strokeWidth={1.5} />
            </g>

            {/* Posts — fixed in screen space */}
            {nearby.map(({ post, bearing, distance }) => {
              const angle  = ((bearing - heading) + 360) % 360;
              const rad    = (angle * Math.PI) / 180;
              const x      = C + RING * Math.sin(rad);
              const y      = C - RING * Math.cos(rad);
              const color  = POST_COLORS[post.postType] ?? '#90caf9';
              const isNear = distance < 1000;
              const isHigh = highlightId === post.id;

              return (
                <g key={post.id} style={{ cursor: 'pointer' }}
                  onClick={() => { scrollToPost(post.id); }}>
                  {/* Pulse animation for nearby posts */}
                  {isNear && (
                    <circle cx={x} cy={y} r={12} fill={color} opacity={0.15}>
                      <animate attributeName="r" values="8;16;8" dur="2s" repeatCount="indefinite" />
                      <animate attributeName="opacity" values="0.2;0;0.2" dur="2s" repeatCount="indefinite" />
                    </circle>
                  )}
                  {/* Main dot */}
                  <circle cx={x} cy={y} r={isHigh ? 11 : isNear ? 9 : 6}
                    fill={color} opacity={0.95}
                    stroke={isHigh ? '#fff' : 'rgba(0,0,0,0.5)'}
                    strokeWidth={isHigh ? 2.5 : 1.5}
                    style={{ transition: 'r 0.2s' }}
                  />
                  {/* Distance label next to the dot */}
                  {distance < 5000 && (
                    <text
                      x={x + (x > C ? 14 : -14)} y={y + 4}
                      textAnchor={x > C ? 'start' : 'end'}
                      fill={color} fontSize="9" fontWeight="700"
                      stroke="rgba(0,0,0,0.6)" strokeWidth={2} paintOrder="stroke"
                    >
                      {fmtDist(distance)}
                    </text>
                  )}
                </g>
              );
            })}

            {/* User marker */}
            <circle cx={C} cy={C} r={10} fill="rgba(33,150,243,0.2)" />
            <circle cx={C} cy={C} r={5}  fill="rgba(33,150,243,0.9)" />
            <circle cx={C} cy={C} r={2.5} fill="#fff" />
          </svg>

          {/* Heading + post count */}
          <div style={{ display: 'flex', gap: 20, marginTop: 8, fontSize: 12, opacity: 0.55 }}>
            <span>Курс: <b style={{ color: '#fff', opacity: 1 }}>{Math.round(heading)}° {fmtBearing(heading)}</b></span>
            <span>Постов: <b style={{ color: '#fff', opacity: 1 }}>{nearby.length}</b></span>
          </div>
        </div>
      )}

      {/* Post list */}
      {userLat && userLon && (
        <div ref={listRef} style={postList}>
          {nearby.length === 0 && (
            <div style={{ textAlign: 'center', padding: 24, opacity: 0.5 }}>
              Постов рядом нет 🎉
            </div>
          )}
          {nearby.map(({ post, bearing, distance }) => {
            const color  = POST_COLORS[post.postType] ?? '#90caf9';
            const isHigh = highlightId === post.id;
            return (
              <button
                id={`radar-post-${post.id}`}
                key={post.id}
                onClick={() => handlePostSelect(post)}
                style={postCard(isHigh, color)}
              >
                {/* Colour stripe on the left */}
                <div style={{ width: 4, borderRadius: 2, alignSelf: 'stretch', background: color, flexShrink: 0 }} />

                <div style={{ flex: 1, minWidth: 0, textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 2 }}>
                    {POST_TYPE_LABELS[post.postType] ?? post.postType}
                  </div>
                  {post.description && (
                    <div style={{ fontSize: 11, opacity: 0.6, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {post.description}
                    </div>
                  )}
                  <div style={{ fontSize: 11, opacity: 0.45, marginTop: 3 }}>
                    ⚡ {post.confidence} · {new Date(post.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
                  </div>
                </div>

                <div style={{ textAlign: 'right', flexShrink: 0, display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
                  <span style={{ fontSize: 14, fontWeight: 700, color }}>{fmtDist(distance)}</span>
                  <span style={{ fontSize: 11, opacity: 0.45 }}>{fmtBearing(bearing)}</span>
                  <span style={{ fontSize: 11, opacity: 0.35 }}>→ На карту</span>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────

const page: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden',
  background: 'var(--tg-theme-bg-color, #1c1c1e)', color: 'var(--tg-theme-text-color, #fff)',
};

const header: React.CSSProperties = {
  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
  padding: '14px 16px 8px', flexShrink: 0,
};

const title: React.CSSProperties = { margin: 0, fontSize: 18, fontWeight: 700 };

const noGeo: React.CSSProperties = {
  flex: 1, display: 'flex', flexDirection: 'column',
  alignItems: 'center', justifyContent: 'center',
};

const postList: React.CSSProperties = {
  flex: 1, overflowY: 'auto', padding: '0 12px 16px',
  display: 'flex', flexDirection: 'column', gap: 6,
};

const postCard = (highlighted: boolean, accentColor: string): React.CSSProperties => ({
  display: 'flex', alignItems: 'center', gap: 10, width: '100%',
  background: highlighted ? `${accentColor}22` : 'rgba(255,255,255,0.05)',
  border: highlighted ? `1px solid ${accentColor}55` : '1px solid rgba(255,255,255,0.07)',
  borderRadius: 12, padding: '10px 12px',
  cursor: 'pointer', color: 'var(--tg-theme-text-color, #fff)',
  transition: 'background 0.3s, border-color 0.3s',
});
