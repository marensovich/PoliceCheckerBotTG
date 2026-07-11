/**
 * Comment list and inline submission form for a DPS post.
 *
 * <p>Shows existing comments in chronological order and exposes an inline
 * textarea form hidden by default. On successful submit, the new comment is
 * propagated to the parent via the {@code onCommentAdded} callback.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useState } from 'react';
import { addComment } from '../../api/client';
import type { Comment } from '../../types';

interface CommentListProps {
  postId: number;
  comments: Comment[];
  onCommentAdded: (comment: Comment) => void;
}

/** Comment list for a post with an inline form for adding new comments. */
export default function CommentList({ postId, comments, onCommentAdded }: CommentListProps) {
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);

  const handleSubmit = async () => {
    if (!text.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const comment = await addComment(postId, text.trim());
      onCommentAdded(comment);
      setText('');
      setShowForm(false);
    } catch {
      setError('Failed to send comment');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      {comments.length === 0 && (
        <p style={{ opacity: 0.5, fontSize: 13 }}>Нет комментариев</p>
      )}
      {comments.map((c) => (
        <div key={c.id} style={commentCard}>
          <p style={{ margin: 0, fontWeight: 600, fontSize: 13 }}>{c.username}</p>
          <p style={{ margin: '4px 0 0', fontSize: 14 }}>{c.text}</p>
          <p style={{ margin: '4px 0 0', fontSize: 11, opacity: 0.5 }}>
            {new Date(c.createdAt).toLocaleString('ru')}
          </p>
        </div>
      ))}

      {!showForm ? (
        <button onClick={() => setShowForm(true)} style={addBtn}>
          + Добавить комментарий
        </button>
      ) : (
        <div style={{ marginTop: 8 }}>
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="Ваш комментарий..."
            maxLength={300}
            rows={2}
            style={textarea}
          />
          {error && <p style={{ color: '#f44336', fontSize: 12, margin: '4px 0' }}>{error}</p>}
          <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
            <button onClick={() => setShowForm(false)} style={cancelBtn}>Отмена</button>
            <button onClick={handleSubmit} disabled={loading} style={submitBtn}>
              {loading ? '...' : 'Отправить'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

const commentCard: React.CSSProperties = {
  background: 'rgba(255,255,255,0.07)',
  borderRadius: 8, padding: '8px 12px', marginBottom: 6,
};

const addBtn: React.CSSProperties = {
  background: 'none', border: '1px dashed rgba(255,255,255,0.3)',
  color: 'inherit', borderRadius: 8, padding: '6px 12px',
  cursor: 'pointer', fontSize: 13, marginTop: 4, width: '100%',
};

const textarea: React.CSSProperties = {
  width: '100%', background: 'rgba(255,255,255,0.1)',
  color: 'inherit', border: 'none', borderRadius: 8,
  padding: 8, fontSize: 14, resize: 'none', boxSizing: 'border-box',
};

const cancelBtn: React.CSSProperties = {
  flex: 1, padding: '6px', borderRadius: 8,
  border: '1px solid rgba(255,255,255,0.2)',
  background: 'none', color: 'inherit', cursor: 'pointer', fontSize: 13,
};

const submitBtn: React.CSSProperties = {
  flex: 2, padding: '6px', borderRadius: 8,
  background: 'var(--tg-theme-button-color, #2196F3)',
  color: '#fff', border: 'none', cursor: 'pointer', fontSize: 13,
};
