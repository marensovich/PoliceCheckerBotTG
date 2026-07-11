/**
 * Bottom-sheet popup showing full DPS post details: description, author, rating, and comments.
 *
 * <p>Displays a live expiry countdown timer, a share button that deep-links via
 * {@code window.Telegram.WebApp.openTelegramLink}, voting controls, and a paginated
 * comment list.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useState, useRef } from 'react';
import type { DpsPost, Comment } from '../../types';
import { POST_TYPE_LABELS } from '../../types';
import { fetchComments } from '../../api/client';
import { tgWebApp } from '../../telegram/webapp';
import VoteButtons from './VoteButtons';
import CommentList from './CommentList';

interface PostPopupProps {
  post: DpsPost;
  onClose: () => void;
}

/** Full-screen bottom sheet with DPS post details. */
export default function PostPopup({ post, onClose }: PostPopupProps) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [showAllComments, setShowAllComments] = useState(false);
  const [loadingComments, setLoadingComments] = useState(true);
  const [currentPost, setCurrentPost] = useState(post);
  const [countdown, setCountdown] = useState('');
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (!currentPost.expiresAt) return;
    const update = () => {
      const diff = new Date(currentPost.expiresAt!).getTime() - Date.now();
      if (diff <= 0) { setCountdown('истёк'); return; }
      const h = Math.floor(diff / 3_600_000);
      const m = Math.floor((diff % 3_600_000) / 60_000);
      const s = Math.floor((diff % 60_000) / 1000);
      setCountdown(h > 0 ? `${h}ч ${m}м` : `${m}м ${s}с`);
    };
    update();
    timerRef.current = setInterval(update, 1000);
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [currentPost.expiresAt]);

  const handleShare = () => {
    const botUsername = import.meta.env.VITE_BOT_USERNAME as string | undefined;
    const deepLink = botUsername
      ? `https://t.me/${botUsername}?startapp=post_${currentPost.id}`
      : null;

    const typeLine = POST_TYPE_LABELS[currentPost.postType] ?? 'Пост ДПС';
    const coordsLine = `📍 ${currentPost.lat.toFixed(5)}, ${currentPost.lon.toFixed(5)}`;
    const descLine = currentPost.description ? `\n💬 ${currentPost.description}` : '';
    const text = `${typeLine}\n${coordsLine}${descLine}`;

    if (deepLink && tgWebApp?.openTelegramLink) {
      const shareUrl = `https://t.me/share/url?url=${encodeURIComponent(deepLink)}&text=${encodeURIComponent(text)}`;
      tgWebApp.openTelegramLink(shareUrl);
    } else if (deepLink) {
      navigator.clipboard?.writeText(`${text}\n${deepLink}`);
    } else {
      navigator.clipboard?.writeText(text);
    }
  };

  useEffect(() => {
    fetchComments(post.id)
      .then(setComments)
      .catch(() => setComments([]))
      .finally(() => setLoadingComments(false));
  }, [post.id]);

  const displayedComments = showAllComments ? comments : comments.slice(0, 3);

  return (
    <div style={overlay} onClick={onClose}>
      <div style={sheet} onClick={(e) => e.stopPropagation()}>
        {/* Drag handle */}
        <div style={handle} />

        <div style={{ overflowY: 'auto', maxHeight: '70vh', paddingBottom: 16 }}>
          <h3 style={{ margin: '0 0 4px', fontSize: 16 }}>
            {POST_TYPE_LABELS[currentPost.postType] ?? '📍 Объект'}
          </h3>
          <p style={{ margin: 0, fontSize: 12, opacity: 0.6 }}>
            {post.lat.toFixed(6)}, {post.lon.toFixed(6)}
          </p>

          {post.description && (
            <p style={{ marginTop: 8, fontSize: 14 }}>{post.description}</p>
          )}

          <p style={{ fontSize: 12, opacity: 0.6, marginTop: 4 }}>
            Добавил: {post.addedByUsername ?? 'аноним'} · {' '}
            {new Date(post.createdAt).toLocaleDateString('ru')}
          </p>

          <div style={{ display: 'flex', gap: 12, marginTop: 6, fontSize: 12, opacity: 0.75, flexWrap: 'wrap' }}>
            <span>✅ {currentPost.confirmedCount ?? 0} подтверждений</span>
            <span>⚡ рейтинг: {currentPost.confidence}</span>
            {countdown && <span style={{ color: countdown === 'истёк' ? '#ef9a9a' : 'inherit' }}>⏳ {countdown}</span>}
          </div>

          <button onClick={handleShare} style={shareBtn}>
            📤 Поделиться
          </button>

          <VoteButtons
            post={currentPost}
            onVoted={(updated) => setCurrentPost(updated)}
          />

          <div style={{ marginTop: 16 }}>
            <h4 style={{ margin: '0 0 8px', fontSize: 14 }}>
              💬 Комментарии ({comments.length})
            </h4>
            {loadingComments ? (
              <p style={{ opacity: 0.5 }}>Загрузка...</p>
            ) : (
              <CommentList
                postId={post.id}
                comments={displayedComments}
                onCommentAdded={(c) => setComments([c, ...comments])}
              />
            )}
            {comments.length > 3 && !showAllComments && (
              <button
                onClick={() => setShowAllComments(true)}
                style={showMoreBtn}
              >
                Показать все ({comments.length})
              </button>
            )}
          </div>
        </div>

        <button onClick={onClose} style={closeBtn}>✕</button>
      </div>
    </div>
  );
}

const overlay: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
  display: 'flex', alignItems: 'flex-end', zIndex: 3000,
};

const sheet: React.CSSProperties = {
  background: 'var(--tg-theme-bg-color, #1c1c1e)',
  color: 'var(--tg-theme-text-color, #fff)',
  borderRadius: '16px 16px 0 0',
  padding: '8px 20px 20px',
  width: '100%', maxWidth: 600, margin: '0 auto',
  position: 'relative',
  animation: 'slideUpBounce 0.42s cubic-bezier(0.34,1.56,0.64,1) both',
};

const handle: React.CSSProperties = {
  width: 40, height: 4, borderRadius: 2,
  background: 'rgba(255,255,255,0.3)',
  margin: '0 auto 12px',
};

const closeBtn: React.CSSProperties = {
  position: 'absolute', top: 12, right: 16,
  background: 'none', border: 'none', color: 'inherit',
  fontSize: 20, cursor: 'pointer', padding: 4,
};

const shareBtn: React.CSSProperties = {
  marginTop: 8, padding: '6px 14px',
  background: 'rgba(255,255,255,0.08)',
  border: '1px solid rgba(255,255,255,0.15)',
  borderRadius: 8, color: 'inherit',
  fontSize: 13, cursor: 'pointer',
};

const showMoreBtn: React.CSSProperties = {
  background: 'none', border: '1px solid rgba(255,255,255,0.2)',
  color: 'inherit', borderRadius: 8, padding: '6px 12px',
  cursor: 'pointer', fontSize: 13, marginTop: 8, width: '100%',
};
