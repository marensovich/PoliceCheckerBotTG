/**
 * History page — DPS posts created by the current user.
 *
 * <p>Also displays the last 10 recently viewed posts (maintained by
 * {@link useRecentlyViewed}) above the user's own post list.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useState } from 'react';
import { fetchMyPosts } from '../api/client';
import type { DpsPost } from '../types';
import { POST_TYPE_LABELS } from '../types';
import { useAppStore } from '../store';
import { useRecentlyViewed } from '../hooks/useRecentlyViewed';

/** Page showing the user's post history and recently viewed posts. */
export default function HistoryPage() {
  const [posts, setPosts] = useState<DpsPost[]>([]);
  const [loading, setLoading] = useState(true);
  const { setSelectedPost, setCurrentPage } = useAppStore();
  const { getAll: getRecent, clear: clearRecent } = useRecentlyViewed();
  const recentlyViewed = getRecent();

  useEffect(() => {
    fetchMyPosts()
      .then(setPosts)
      .catch(() => setPosts([]))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div style={{ padding: 16 }}>
        {[1, 2, 3, 4].map((i) => (
          <div key={i} style={{ background: 'rgba(255,255,255,0.06)', borderRadius: 12, padding: '12px 14px', marginBottom: 10 }}>
            <div className="skeleton" style={{ height: 14, width: '55%', borderRadius: 6, marginBottom: 8 }} />
            <div className="skeleton" style={{ height: 11, width: '35%', borderRadius: 4, marginBottom: 6 }} />
            <div className="skeleton" style={{ height: 10, width: '45%', borderRadius: 4 }} />
          </div>
        ))}
      </div>
    );
  }

  if (posts.length === 0) {
    return (
      <div style={center}>
        <p>Вы ещё не добавили ни одного поста.</p>
        <button
          onClick={() => setCurrentPage('map')}
          style={addBtn}
        >
          🗺 Открыть карту
        </button>
      </div>
    );
  }

  return (
    <div style={{ padding: 16, overflowY: 'auto' }}>
      {recentlyViewed.length > 0 && (
        <div style={{ marginBottom: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
            <h3 style={{ margin: 0, fontSize: 15 }}>🕐 Недавно просмотренные</h3>
            <button onClick={clearRecent} style={clearBtn}>Очистить</button>
          </div>
          {recentlyViewed.map((post) => (
            <div
              key={post.id}
              style={card(post.isActive)}
              onClick={() => { setSelectedPost(post); setCurrentPage('map'); }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ fontWeight: 600, fontSize: 13 }}>
                  {POST_TYPE_LABELS[post.postType] ?? '📍 ДПС'}
                </span>
                <span style={{ fontSize: 11, opacity: 0.5 }}>
                  {post.lat.toFixed(4)}, {post.lon.toFixed(4)}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      <h2 style={{ margin: '0 0 16px', fontSize: 18 }}>📋 Мои посты</h2>
      {posts.map((post) => (
        <div
          key={post.id}
          style={card(post.isActive)}
          onClick={() => {
            setSelectedPost(post);
            setCurrentPage('map');
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontWeight: 600, fontSize: 14 }}>
              {POST_TYPE_LABELS[post.postType] ?? '📍 ДПС'}
            </span>
            <span style={badge(post.isActive)}>
              {post.isActive ? '✅ Активен' : '❌ Деактивирован'}
            </span>
          </div>
          <p style={{ margin: '4px 0 0', fontSize: 12, opacity: 0.6 }}>
            {post.lat.toFixed(6)}, {post.lon.toFixed(6)}
          </p>
          {post.description && (
            <p style={{ margin: '4px 0 0', fontSize: 13 }}>{post.description}</p>
          )}
          <div style={{ display: 'flex', gap: 16, marginTop: 8, fontSize: 12, opacity: 0.7 }}>
            <span>👍 {post.confidence}</span>
            <span>{new Date(post.createdAt).toLocaleDateString('ru')}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

const center: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', alignItems: 'center',
  justifyContent: 'center', height: '100%', gap: 16, padding: 24,
};

const card = (active: boolean): React.CSSProperties => ({
  background: active ? 'rgba(255,255,255,0.07)' : 'rgba(255,255,255,0.03)',
  borderRadius: 12, padding: '12px 14px', marginBottom: 10,
  cursor: 'pointer', transition: 'background 0.2s',
  opacity: active ? 1 : 0.6,
});

const badge = (active: boolean): React.CSSProperties => ({
  fontSize: 11, padding: '2px 8px', borderRadius: 20,
  background: active ? 'rgba(76,175,80,0.2)' : 'rgba(244,67,54,0.2)',
  color: active ? '#81c784' : '#ef9a9a',
});

const clearBtn: React.CSSProperties = {
  background: 'none', border: 'none', color: 'rgba(255,255,255,0.4)',
  fontSize: 12, cursor: 'pointer', padding: 0,
};

const addBtn: React.CSSProperties = {
  background: 'var(--tg-theme-button-color, #2196F3)',
  color: '#fff', border: 'none', borderRadius: 10,
  padding: '10px 20px', cursor: 'pointer', fontSize: 14,
};
