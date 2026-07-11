/**
 * Form popup for adding a new DPS-related object to the map.
 *
 * <p>Supports post type selection (DPS post, patrol car, camera, ambush, hidden post).
 * When the type is {@code PATROL_CAR}, an additional speed slider (km/h) is shown.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useState } from 'react';
import type { PostType } from '../../types';
import { POST_TYPE_LABELS } from '../../types';
import { useAppStore } from '../../store';

interface AddPostPopupProps {
  lat: number;
  lon: number;
  onConfirm: (lat: number, lon: number, description?: string, postType?: PostType, patrolSpeedKmh?: number) => void;
  onCancel: () => void;
}

const ALL_TYPES: PostType[] = ['DPS_POST', 'PATROL_CAR', 'CAMERA', 'AMBUSH', 'HIDDEN_POST'];

/** Bottom-sheet popup with the add-object form. */
export default function AddPostPopup({ lat, lon, onConfirm, onCancel }: AddPostPopupProps) {
  const { patrolSpeedKmh: defaultSpeed } = useAppStore();
  const [postType, setPostType] = useState<PostType>('DPS_POST');
  const [description, setDescription] = useState('');
  const [speed, setSpeed] = useState(defaultSpeed);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    setLoading(true);
    setError(null);
    try {
      await onConfirm(
        lat,
        lon,
        description || undefined,
        postType,
        postType === 'PATROL_CAR' ? speed : undefined,
      );
    } catch {
      setError('Failed to add post. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={overlay}>
      <div style={popup}>
        <h3 style={{ margin: '0 0 12px', fontSize: 16 }}>📍 Добавить объект</h3>
        <p style={{ margin: '0 0 12px', fontSize: 12, opacity: 0.6 }}>
          {lat.toFixed(6)}, {lon.toFixed(6)}
        </p>

        {/* Object type selection */}
        <div style={{ marginBottom: 12 }}>
          <div style={sectionLabel}>Тип объекта</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {ALL_TYPES.map((t) => (
              <button
                key={t}
                onClick={() => setPostType(t)}
                style={typeBtn(t === postType)}
              >
                {POST_TYPE_LABELS[t]}
              </button>
            ))}
          </div>
        </div>

        {/* Patrol speed */}
        {postType === 'PATROL_CAR' && (
          <div style={{ marginBottom: 12 }}>
            <div style={sectionLabel}>Скорость: {speed} км/ч</div>
            <input
              type="range"
              min={10}
              max={200}
              value={speed}
              onChange={(e) => setSpeed(Number(e.target.value))}
              style={{ width: '100%' }}
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, opacity: 0.5 }}>
              <span>10</span><span>200 км/ч</span>
            </div>
          </div>
        )}

        {/* Optional description */}
        <textarea
          placeholder="Описание (опционально)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          maxLength={500}
          rows={2}
          style={textareaStyle}
        />

        {error && <p style={{ color: '#f44336', fontSize: 13, margin: '4px 0' }}>{error}</p>}

        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          <button onClick={onCancel} style={cancelBtn} disabled={loading}>
            Отмена
          </button>
          <button onClick={handleSubmit} style={confirmBtn} disabled={loading}>
            {loading ? '...' : '✅ Добавить'}
          </button>
        </div>
      </div>
    </div>
  );
}

const overlay: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.55)',
  display: 'flex', alignItems: 'flex-end', zIndex: 2000,
};

const popup: React.CSSProperties = {
  background: 'var(--tg-theme-bg-color, #1c1c1e)',
  color: 'var(--tg-theme-text-color, #fff)',
  borderRadius: '16px 16px 0 0',
  padding: 20,
  width: '100%',
  maxWidth: 600,
  margin: '0 auto',
  maxHeight: '85vh',
  overflowY: 'auto',
};

const sectionLabel: React.CSSProperties = {
  fontSize: 11, opacity: 0.5, textTransform: 'uppercase', marginBottom: 6,
};

const typeBtn = (active: boolean): React.CSSProperties => ({
  padding: '10px 14px',
  borderRadius: 10,
  border: active ? '2px solid var(--tg-theme-button-color, #2196F3)' : '1px solid rgba(255,255,255,0.12)',
  background: active ? 'rgba(33,150,243,0.18)' : 'transparent',
  color: 'inherit',
  cursor: 'pointer',
  fontSize: 14,
  textAlign: 'left',
  fontWeight: active ? 600 : 400,
  transition: 'all 0.15s',
});

const textareaStyle: React.CSSProperties = {
  width: '100%',
  background: 'var(--tg-theme-secondary-bg-color, #2c2c2e)',
  color: 'inherit',
  border: 'none',
  borderRadius: 8,
  padding: 10,
  fontSize: 14,
  resize: 'none',
  boxSizing: 'border-box',
};

const cancelBtn: React.CSSProperties = {
  flex: 1, padding: '10px', borderRadius: 8,
  border: '1px solid rgba(255,255,255,0.2)',
  background: 'transparent', color: 'inherit', cursor: 'pointer', fontSize: 14,
};

const confirmBtn: React.CSSProperties = {
  flex: 2, padding: '10px', borderRadius: 8,
  background: 'var(--tg-theme-button-color, #2196F3)',
  color: 'var(--tg-theme-button-text-color, #fff)',
  border: 'none', cursor: 'pointer', fontSize: 14, fontWeight: 600,
};
