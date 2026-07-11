/**
 * Promotional banner for the DPS Tracker Premium subscription.
 *
 * <p>Clicking the buy button calls the backend API, which instructs the bot to send
 * a Telegram Stars invoice to the user's personal chat. The Mini App stays open
 * while payment is processed in the Telegram native UI.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useState } from 'react';
import { apiClient } from '../../api/client';

/** Premium subscription banner, shown only to free-tier users. */
export default function PremiumBanner() {
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState(false);

  const handleSubscribe = async () => {
    setLoading(true);
    setError(false);
    try {
      await apiClient.post('/users/me/subscribe');
      setSent(true);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={banner}>
      <span style={{ fontSize: 20 }}>⭐</span>
      <div style={{ flex: 1 }}>
        <p style={{ margin: 0, fontWeight: 700, fontSize: 14 }}>DPS Tracker Premium</p>
        {sent ? (
          <p style={{ margin: 0, fontSize: 12, color: '#81c784' }}>
            ✅ Счёт отправлен в бот — проверьте чат
          </p>
        ) : error ? (
          <p style={{ margin: 0, fontSize: 12, color: '#ef9a9a' }}>
            Ошибка. Напишите боту /subscribe
          </p>
        ) : (
          <p style={{ margin: 0, fontSize: 12, opacity: 0.7 }}>
            Live-трекинг и уведомления — 100 Stars/мес
          </p>
        )}
      </div>
      {!sent && (
        <button onClick={handleSubscribe} disabled={loading} style={btn}>
          {loading ? '...' : 'Купить'}
        </button>
      )}
    </div>
  );
}

const banner: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 12,
  background: 'linear-gradient(135deg, #1a237e, #283593)',
  borderRadius: 12, padding: '12px 16px', margin: '12px 0',
};

const btn: React.CSSProperties = {
  background: '#ffd600', color: '#000',
  border: 'none', borderRadius: 8,
  padding: '6px 14px', cursor: 'pointer',
  fontWeight: 700, fontSize: 13, whiteSpace: 'nowrap',
  opacity: 1,
};
