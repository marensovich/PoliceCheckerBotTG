/**
 * Nearby Posts page — a sorted list of active DPS marks within the user's radius.
 *
 * <p>Free tier: up to 2 km. Premium: up to 10 km. The radius cap is enforced
 * server-side; the frontend only renders the data returned by the API.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useState, useCallback } from 'react';
import { fetchNearbyAuthenticated } from '../api/client';
import { useAppStore } from '../store';
import { POST_TYPE_LABELS } from '../types';
import type { DpsPost, NearbyResponse } from '../types';

function formatDistance(meters: number): string {
  if (meters < 1000) return `${Math.round(meters)} м`;
  return `${(meters / 1000).toFixed(1)} км`;
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}

export default function NearbyPage() {
  const { userLat, userLon, setSelectedPost, setCurrentPage } = useAppStore();
  const [data, setData] = useState<NearbyResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!userLat || !userLon) return;
    setLoading(true);
    setError(null);
    try {
      const result = await fetchNearbyAuthenticated(userLat, userLon);
      setData(result);
    } catch {
      setError('Не удалось загрузить посты. Проверьте соединение.');
    } finally {
      setLoading(false);
    }
  }, [userLat, userLon]);

  useEffect(() => { load(); }, [load]);

  if (!userLat || !userLon) {
    return (
      <div style={centerBox}>
        <span style={{ fontSize: 36 }}>📡</span>
        <p style={{ margin: '8px 0 0', opacity: 0.6 }}>Определяем ваше местоположение…</p>
      </div>
    );
  }

  return (
    <div style={page}>
      <div style={header}>
        <h2 style={title}>Посты рядом</h2>
        <button onClick={load} style={refreshBtn} disabled={loading}>
          {loading ? '⏳' : '🔄'}
        </button>
      </div>

      {/* Radius info + upgrade prompt for free users */}
      {data && (
        <div style={radiusBadge(data.premium)}>
          {data.premium ? (
            <span>⭐ Радиус: <b>{data.maxRadiusKm} км</b> (Premium)</span>
          ) : (
            <>
              <span>📡 Радиус: <b>{data.maxRadiusKm} км</b> (бесплатно)</span>
              <button
                style={upgradeBtn}
                onClick={() => setCurrentPage('profile')}
              >
                Оформить Premium →
              </button>
            </>
          )}
        </div>
      )}

      {error && <p style={errorText}>{error}</p>}

      {loading && !data && (
        <div style={{ padding: '8px 12px' }}>
          {[1, 2, 3].map((i) => (
            <div key={i} style={{ background: 'var(--tg-theme-secondary-bg-color,#2c2c2e)', borderRadius: 12, padding: '12px 14px', marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ flex: 1 }}>
                <div className="skeleton" style={{ height: 14, width: '60%', borderRadius: 6, marginBottom: 8 }} />
                <div className="skeleton" style={{ height: 11, width: '80%', borderRadius: 4, marginBottom: 6 }} />
                <div className="skeleton" style={{ height: 10, width: '40%', borderRadius: 4 }} />
              </div>
              <div className="skeleton" style={{ height: 28, width: 52, borderRadius: 8, marginLeft: 12 }} />
            </div>
          ))}
        </div>
      )}

      {data && data.posts.length === 0 && (
        <div style={centerBox}>
          <span style={{ fontSize: 40 }}>🎉</span>
          <p style={{ margin: '8px 0 0', opacity: 0.6 }}>
            Постов ДПС в радиусе {data.maxRadiusKm} км нет
          </p>
        </div>
      )}

      <div style={list}>
        {data?.posts.map((post) => (
          <PostCard
            key={post.id}
            post={post}
            onClick={() => {
              setSelectedPost(post);
              setCurrentPage('map');
            }}
          />
        ))}
      </div>
    </div>
  );
}

function PostCard({ post, onClick }: { post: DpsPost; onClick: () => void }) {
  const label = POST_TYPE_LABELS[post.postType] ?? post.postType;
  const distStr = post.distanceMeters != null ? formatDistance(post.distanceMeters) : null;

  return (
    <button style={card} onClick={onClick}>
      <div style={cardLeft}>
        <span style={typeLabel}>{label}</span>
        {post.description && <span style={desc}>{post.description}</span>}
        <span style={meta}>
          🕐 {formatTime(post.createdAt)}
          {post.expiresAt && <> · до {formatTime(post.expiresAt)}</>}
          {' · '}⚡ {post.confidence}
          {post.confirmedCount > 0 && <> · ✅ {post.confirmedCount}</>}
        </span>
      </div>
      {distStr && <span style={distBadge}>{distStr}</span>}
    </button>
  );
}

// ── Styles ───────────────────────────────────────────────────────────────────

const page: React.CSSProperties = {
  display: 'flex', flexDirection: 'column',
  height: '100%', overflow: 'hidden',
  background: 'var(--tg-theme-bg-color, #1c1c1e)',
  color: 'var(--tg-theme-text-color, #fff)',
};

const header: React.CSSProperties = {
  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
  padding: '16px 16px 8px',
};

const title: React.CSSProperties = { margin: 0, fontSize: 20, fontWeight: 700 };

const refreshBtn: React.CSSProperties = {
  background: 'none', border: 'none', fontSize: 20, cursor: 'pointer',
  color: 'var(--tg-theme-text-color, #fff)', padding: 4,
};

const radiusBadge = (premium: boolean): React.CSSProperties => ({
  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
  flexWrap: 'wrap', gap: 8,
  margin: '0 16px 8px',
  padding: '8px 12px',
  borderRadius: 10,
  background: premium
    ? 'linear-gradient(135deg, #1a237e, #283593)'
    : 'rgba(255,255,255,0.07)',
  fontSize: 13,
});

const upgradeBtn: React.CSSProperties = {
  background: '#ffd600', color: '#000',
  border: 'none', borderRadius: 6,
  padding: '4px 10px', cursor: 'pointer',
  fontWeight: 700, fontSize: 12,
};

const errorText: React.CSSProperties = {
  color: '#ef9a9a', margin: '0 16px 8px', fontSize: 13,
};

const list: React.CSSProperties = {
  flex: 1, overflowY: 'auto',
  padding: '0 12px 16px',
  display: 'flex', flexDirection: 'column', gap: 8,
};

const centerBox: React.CSSProperties = {
  flex: 1, display: 'flex', flexDirection: 'column',
  alignItems: 'center', justifyContent: 'center',
  padding: 32, textAlign: 'center',
};

const card: React.CSSProperties = {
  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
  gap: 12, width: '100%',
  background: 'var(--tg-theme-secondary-bg-color, #2c2c2e)',
  border: 'none', borderRadius: 12,
  padding: '12px 14px',
  cursor: 'pointer', textAlign: 'left',
  color: 'var(--tg-theme-text-color, #fff)',
};

const cardLeft: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', gap: 3, flex: 1, minWidth: 0,
};

const typeLabel: React.CSSProperties = { fontWeight: 600, fontSize: 14 };

const desc: React.CSSProperties = {
  fontSize: 12, opacity: 0.75,
  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
};

const meta: React.CSSProperties = { fontSize: 11, opacity: 0.55 };

const distBadge: React.CSSProperties = {
  background: 'rgba(33,150,243,0.2)',
  color: '#64b5f6',
  borderRadius: 8,
  padding: '4px 8px',
  fontSize: 12, fontWeight: 700,
  whiteSpace: 'nowrap', flexShrink: 0,
};
