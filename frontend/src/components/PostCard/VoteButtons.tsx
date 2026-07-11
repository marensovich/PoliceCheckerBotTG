/**
 * Thumbs-up / thumbs-down voting controls for a DPS post.
 *
 * <p>One vote per session is enforced in the UI. After a successful vote,
 * haptic feedback is triggered and the updated post object is propagated
 * to the parent via {@code onVoted}.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useState } from 'react';
import { votePost } from '../../api/client';
import type { DpsPost } from '../../types';
import { hapticSuccess, hapticError } from '../../telegram/webapp';
import { useAppStore } from '../../store';

interface VoteButtonsProps {
  post: DpsPost;
  onVoted: (updated: DpsPost) => void;
}

/** 👍/👎 buttons for voting on the credibility of a DPS post. */
export default function VoteButtons({ post, onVoted }: VoteButtonsProps) {
  const [voted, setVoted] = useState<1 | -1 | null>(null);
  const [loading, setLoading] = useState(false);
  const addToast = useAppStore((s) => s.addToast);

  const handleVote = async (value: 1 | -1) => {
    if (voted !== null || loading) return;
    setLoading(true);
    try {
      const updated = await votePost(post.id, value);
      setVoted(value);
      onVoted(updated);
      hapticSuccess();
      addToast(value === 1 ? '👍 Подтверждено' : '👎 Отмечено как фейк', 'success');
    } catch (err: unknown) {
      hapticError();
      const msg = err instanceof Error ? err.message : 'Ошибка';
      addToast(msg.includes('409') ? 'Вы уже голосовали' : 'Ошибка голосования', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 12 }}>
      <button
        onClick={() => handleVote(1)}
        disabled={voted !== null || loading}
        style={voteBtn(voted === 1, '#4caf50')}
      >
        👍 Подтвердить
      </button>
      <span style={{ fontSize: 16, fontWeight: 700 }}>{post.confidence}</span>
      <button
        onClick={() => handleVote(-1)}
        disabled={voted !== null || loading}
        style={voteBtn(voted === -1, '#f44336')}
      >
        👎 Фейк
      </button>
    </div>
  );
}

const voteBtn = (active: boolean, color: string): React.CSSProperties => ({
  background: active ? color : 'rgba(255,255,255,0.1)',
  color: active ? '#fff' : 'inherit',
  border: 'none', borderRadius: 8, padding: '6px 12px',
  cursor: 'pointer', fontSize: 13, transition: 'all 0.2s',
});
