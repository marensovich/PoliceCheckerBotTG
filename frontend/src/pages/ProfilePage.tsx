/**
 * User profile page with settings and subscription status.
 *
 * @author marensovich
 * @version 2.0.0
 * @since 1.0.0
 */

import { useEffect, useState, useCallback } from 'react';
import { fetchProfile, updateSettings, updateNotifyPrefs, applyPromoCode, createTicket, fetchMyTickets, closeMyTicket } from '../api/client';
import type { PostType, Ticket } from '../types';
import { POST_TYPE_LABELS } from '../types';
import { useAppStore } from '../store';
import type { UserProfile } from '../types';
import { addToHomeScreen as tgAddToHomeScreen } from '../telegram/webapp';

/** User profile and settings page. */
export default function ProfilePage() {
  const { setProfile, profile, locationEnabled, setLocationEnabled, patrolSpeedKmh, setPatrolSpeedKmh } = useAppStore();
  const [loading, setLoading] = useState(!profile);
  const [radius, setRadius] = useState(profile?.notifyRadiusKm ?? 5);
  const [localSpeed, setLocalSpeed] = useState(patrolSpeedKmh);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [promoCode, setPromoCode] = useState('');
  const [promoLoading, setPromoLoading] = useState(false);
  const [promoResult, setPromoResult] = useState<{ ok: boolean; msg: string } | null>(null);

  const allTypes = Object.keys(POST_TYPE_LABELS) as PostType[];
  const [notifyTypes, setNotifyTypes] = useState<string[]>([]);

  useEffect(() => {
    if (profile?.notifyPostTypes !== undefined) {
      setNotifyTypes(
        profile.notifyPostTypes
          ? profile.notifyPostTypes.split(',').map((s) => s.trim()).filter(Boolean)
          : []
      );
    }
  }, [profile?.notifyPostTypes]);

  const toggleNotifyType = useCallback(async (type: string) => {
    setNotifyTypes((prev) => {
      const next = prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type];
      updateNotifyPrefs(next.join(',')).then(setProfile).catch(() => {});
      return next;
    });
  }, [setProfile]);

  useEffect(() => {
    if (!profile) {
      fetchProfile()
        .then((p: UserProfile) => { setProfile(p); setRadius(p.notifyRadiusKm); })
        .finally(() => setLoading(false));
    }
  }, [profile, setProfile]);

  const isPremium = profile?.isSubscribed === true;
  const maxRadius = isPremium ? 50 : 5;

  const handleApplyPromo = async () => {
    if (!promoCode.trim()) return;
    setPromoLoading(true);
    setPromoResult(null);
    try {
      const res = await applyPromoCode(promoCode.trim());
      setPromoResult({ ok: true, msg: res.message });
      setPromoCode('');
      const updated = await fetchProfile();
      setProfile(updated);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Промокод недействителен';
      setPromoResult({ ok: false, msg });
    } finally {
      setPromoLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    setPatrolSpeedKmh(localSpeed);
    try {
      const updated = await updateSettings({ notifyRadiusKm: radius });
      setProfile(updated);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div style={center}>Загрузка профиля...</div>;

  if (profile?.isBanned) {
    return (
      <div style={page}>
        <div style={bannedCard}>
          <div style={{ fontWeight: 700, fontSize: 15, color: '#ef9a9a', marginBottom: 6 }}>🚫 Аккаунт заблокирован</div>
          <div style={{ fontSize: 13, lineHeight: 1.6, opacity: 0.8 }}>
            Доступ ограничен. Если вы считаете это ошибкой — обратитесь в поддержку.
          </div>
        </div>
        <SupportSection />
      </div>
    );
  }

  return (
    <div style={page}>

      {/* ── Profile header ── */}
      {profile && (
        <div style={profileHeader}>
          <div style={avatarCircle}>
            {(profile.firstName?.[0] ?? profile.username?.[0] ?? '?').toUpperCase()}
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 17, fontWeight: 700 }}>{profile.firstName ?? profile.username ?? '—'}</div>
            {profile.username && (
              <div style={{ fontSize: 12, opacity: 0.5, marginTop: 2 }}>@{profile.username}</div>
            )}
            <div style={{ display: 'flex', gap: 6, marginTop: 6, flexWrap: 'wrap' }}>
              {profile.role === 'ADMIN'     && <span style={roleBadge('#7c4dff')}>Администратор</span>}
              {profile.role === 'MODERATOR' && <span style={roleBadge('#ff9800')}>Модератор</span>}
              {profile.isSubscribed         && <span style={roleBadge('#ffd600', '#000')}>⭐ Premium</span>}
              <span style={repBadge((profile.reputationScore ?? 0) >= 0)}>
                {(profile.reputationScore ?? 0) >= 0 ? '+' : ''}{profile.reputationScore ?? 0} рейтинг
              </span>
            </div>
          </div>
        </div>
      )}

      {/* ── Subscription ── */}
      <div style={{ marginBottom: 16 }}><SubscriptionCard profile={profile} /></div>

      {/* ── Settings group ── */}
      <div style={group}>
        <div style={groupTitle}>Настройки</div>

        {/* Geolocation */}
        <SettingRow
          icon="📍"
          label="Геолокация"
          sub={locationEnabled ? 'Показывать моё местоположение' : 'Маркер скрыт'}
          right={
            <button onClick={() => setLocationEnabled(!locationEnabled)} style={toggle(locationEnabled)} aria-label="Геолокация">
              <div style={toggleThumb(locationEnabled)} />
            </button>
          }
        />

        <Divider />

        {/* Patrol speed */}
        <div style={settingBlock}>
          <SettingRow
            icon="🚗"
            label="Скорость патруля"
            sub="Для радиуса неопределённости патрульной машины"
            right={<span style={{ fontSize: 14, fontWeight: 700, opacity: 0.8 }}>{localSpeed} км/ч</span>}
          />
          <input type="range" min={10} max={200} step={5} value={localSpeed}
            onChange={(e) => setLocalSpeed(Number(e.target.value))}
            style={{ width: '100%', margin: '2px 0 4px' }} />
        </div>

        <Divider />

        {/* Notify radius */}
        <div style={settingBlock}>
          <SettingRow
            icon="🔔"
            label={`Радиус уведомлений: ${radius} км`}
            sub={isPremium ? 'Premium: до 50 км' : 'Free: до 5 км · Premium → 50 км'}
            right={null}
          />
          <input type="range" min={1} max={maxRadius} value={Math.min(radius, maxRadius)}
            onChange={(e) => setRadius(Number(e.target.value))}
            style={{ width: '100%', margin: '2px 0 4px' }} />
        </div>

        <Divider />

        {/* Notify types */}
        <div style={settingBlock}>
          <div style={rowInner}>
            <span style={rowIcon}>🏷</span>
            <div style={{ flex: 1 }}>
              <div style={rowLabel}>Уведомления о типах</div>
              <div style={rowSub}>Пусто = все типы</div>
            </div>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, paddingLeft: 36, marginTop: 8 }}>
            {allTypes.map((type) => {
              const selected = notifyTypes.includes(type);
              const active = notifyTypes.length === 0 || selected;
              return (
                <button key={type} onClick={() => toggleNotifyType(type)} style={chip(selected, active)}>
                  {POST_TYPE_LABELS[type]}
                </button>
              );
            })}
            {notifyTypes.length > 0 && (
              <button onClick={() => { setNotifyTypes([]); updateNotifyPrefs('').then(setProfile).catch(() => {}); }}
                style={chip(false, false, true)}>
                Сбросить
              </button>
            )}
          </div>
        </div>
      </div>

      {/* ── Save & add to home ── */}
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 16 }}>
        <button onClick={handleSave} disabled={saving} style={primaryBtn}>
          {saved ? '✅ Сохранено!' : saving ? 'Сохранение...' : 'Сохранить настройки'}
        </button>
        <button onClick={tgAddToHomeScreen} style={ghostBtn}>
          📲 Добавить на главный экран
        </button>
      </div>

      {/* ── Promo code ── */}
      <div style={group}>
        <div style={groupTitle}>Промокод</div>
        {profile?.promoDiscountPercent != null && (
          <div style={{ padding: '0 16px 10px', fontSize: 13, color: '#81c784' }}>
            ✅ Скидка {profile.promoDiscountPercent}% на следующую покупку
          </div>
        )}
        <div style={{ padding: '0 16px 16px', display: 'flex', gap: 8 }}>
          <input placeholder="Введите промокод" value={promoCode}
            onChange={(e) => setPromoCode(e.target.value.toUpperCase())}
            onKeyDown={(e) => e.key === 'Enter' && handleApplyPromo()}
            style={inputStyle} />
          <button onClick={handleApplyPromo} disabled={promoLoading || !promoCode.trim()} style={applyBtn}>
            {promoLoading ? '…' : 'Применить'}
          </button>
        </div>
        {promoResult && (
          <p style={{ margin: '-8px 16px 12px', fontSize: 13, color: promoResult.ok ? '#81c784' : '#ef9a9a' }}>
            {promoResult.msg}
          </p>
        )}
      </div>

      {/* ── Support ── */}
      <SupportSection />
    </div>
  );
}

// ── Small helpers ─────────────────────────────────────────────────────────────

function SettingRow({ icon, label, sub, right }: {
  icon: string; label: string; sub?: string; right: React.ReactNode;
}) {
  return (
    <div style={rowInner}>
      <span style={rowIcon}>{icon}</span>
      <div style={{ flex: 1 }}>
        <div style={rowLabel}>{label}</div>
        {sub && <div style={rowSub}>{sub}</div>}
      </div>
      {right && <div style={{ flexShrink: 0 }}>{right}</div>}
    </div>
  );
}

function Divider() {
  return <div style={{ height: 1, background: 'rgba(255,255,255,0.07)', margin: '0 16px' }} />;
}

// ── Subscription ──────────────────────────────────────────────────────────────

function daysLeft(iso: string): number {
  return Math.max(0, Math.ceil((new Date(iso).getTime() - Date.now()) / 86_400_000));
}

function SubscriptionCard({ profile }: { profile: UserProfile | null }) {
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [err, setErr] = useState(false);

  if (!profile) return null;

  const isPremium = profile.isSubscribed === true;
  const expiry = profile.subscriptionExpiresAt ? new Date(profile.subscriptionExpiresAt) : null;
  const days = expiry ? daysLeft(profile.subscriptionExpiresAt!) : 0;

  const handleBuy = async () => {
    setLoading(true); setErr(false);
    try {
      const { apiClient } = await import('../api/client');
      await apiClient.post('/users/me/subscribe');
      setSent(true);
    } catch { setErr(true); }
    finally { setLoading(false); }
  };

  if (isPremium) {
    return (
      <div style={premiumCard}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
          <span style={{ fontSize: 26 }}>⭐</span>
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700, fontSize: 15 }}>DPS Tracker Premium</div>
            {expiry && (
              <div style={{ fontSize: 12, color: '#81c784', marginTop: 2 }}>
                До {expiry.toLocaleDateString('ru')} · {days} дн.
              </div>
            )}
          </div>
          {days <= 7 && days > 0 && (
            <span style={{ fontSize: 11, color: '#ffd54f', background: 'rgba(255,213,79,0.15)', borderRadius: 8, padding: '3px 8px' }}>
              ⚠ {days} дн.
            </span>
          )}
        </div>
        {[
          '🗺 Карта и посты рядом: 50 км',
          '🟢 Live-трекинг',
          '🔔 Уведомления без ограничений',
          '🔕 Нет рекламы',
        ].map((f) => (
          <div key={f} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, marginBottom: 6 }}>
            <span style={{ flex: 1 }}>{f}</span>
            <span style={{ color: '#81c784', fontWeight: 700 }}>✓</span>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div style={{ padding: '0 16px 4px' }}>
      <div style={freeCard}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <div>
            <div style={{ fontWeight: 700, fontSize: 15 }}>⭐ DPS Tracker Premium</div>
            <div style={{ fontSize: 12, opacity: 0.6, marginTop: 2 }}>
              {profile.promoDiscountPercent
                ? <span style={{ color: '#ffd54f' }}>Скидка {profile.promoDiscountPercent}% активна!</span>
                : 'Live-трекинг · 50 км · уведомления'}
            </div>
          </div>
          {sent
            ? <span style={{ fontSize: 12, color: '#81c784' }}>✅ Счёт отправлен</span>
            : <button onClick={handleBuy} disabled={loading} style={buyBtn}>{loading ? '...' : 'Купить'}</button>
          }
        </div>
        {err && <p style={{ margin: 0, fontSize: 11, color: '#ef9a9a' }}>Ошибка. Напишите боту /subscribe</p>}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
          {[
            { f: '🗺 Карта и посты рядом', free: '5 км', premium: '50 км' },
            { f: '🟢 Live-трекинг',       free: '✗',     premium: '✓' },
            { f: '🔔 Уведомления',        free: 'лимит', premium: 'без лимита' },
          ].map(({ f, free, premium }) => (
            <div key={f} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
              <span style={{ flex: 1, opacity: 0.75 }}>{f}</span>
              <span style={{ opacity: 0.45, minWidth: 40, textAlign: 'right' }}>{free}</span>
              <span style={{ color: '#81c784', fontWeight: 700, minWidth: 56, textAlign: 'right' }}>→ {premium}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── Support ───────────────────────────────────────────────────────────────────

const TICKET_STATUS_LABEL: Record<string, { text: string; color: string }> = {
  OPEN:        { text: 'Открыт',   color: '#42a5f5' },
  IN_PROGRESS: { text: 'В работе', color: '#ffd54f' },
  CLOSED:      { text: 'Закрыт',   color: '#66bb6a' },
};

function SupportSection() {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [closingId, setClosingId] = useState<number | null>(null);

  useEffect(() => { fetchMyTickets().then(setTickets).catch(() => {}); }, []);

  const handleSend = async () => {
    if (!subject.trim() || !message.trim()) return;
    setSending(true);
    setError(null);
    try {
      const t: Ticket = await createTicket(subject.trim(), message.trim());
      setTickets((prev) => [t, ...prev]);
      setSubject('');
      setMessage('');
      setFormOpen(false);
      setSent(true);
      setTimeout(() => setSent(false), 3000);
    } catch (e: unknown) {
      setError((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Ошибка отправки');
    } finally {
      setSending(false);
    }
  };

  const handleCloseTicket = async (id: number) => {
    setClosingId(id);
    try {
      const updated: Ticket = await closeMyTicket(id);
      setTickets((prev) => prev.map((t) => t.id === id ? updated : t));
    } catch { /* ignore */ }
    finally { setClosingId(null); }
  };

  return (
    <div style={group}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 16px', marginBottom: 12 }}>
        <div style={groupTitle} tabIndex={-1}>Поддержка</div>
        <button onClick={() => setFormOpen((v) => !v)} style={smallBtn}>
          {formOpen ? 'Отмена' : '+ Тикет'}
        </button>
      </div>

      {sent && <p style={{ margin: '-4px 16px 12px', fontSize: 13, color: '#81c784' }}>✅ Тикет отправлен</p>}

      {formOpen && (
        <div style={{ padding: '0 16px 16px' }}>
          <input placeholder="Тема" value={subject} onChange={(e) => setSubject(e.target.value)}
            maxLength={200} style={{ ...inputStyle, marginBottom: 8 }} />
          <textarea placeholder="Опишите проблему..." value={message}
            onChange={(e) => setMessage(e.target.value)} maxLength={2000} rows={4}
            style={{ ...inputStyle, resize: 'vertical', fontFamily: 'inherit', lineHeight: 1.5 }} />
          {error && <p style={{ fontSize: 12, color: '#ef9a9a', margin: '4px 0 0' }}>{error}</p>}
          <button onClick={handleSend} disabled={sending || !subject.trim() || message.trim().length < 10}
            style={{ ...primaryBtn, marginTop: 10 }}>
            {sending ? 'Отправка...' : 'Отправить'}
          </button>
        </div>
      )}

      <div style={{ padding: '0 16px 16px' }}>
        {tickets.length === 0
          ? (!formOpen && <p style={{ fontSize: 13, opacity: 0.4, margin: 0 }}>Нет обращений</p>)
          : tickets.map((t) => {
              const s = TICKET_STATUS_LABEL[t.status] ?? { text: t.status, color: '#fff' };
              return (
                <div key={t.id} style={ticketCard}>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start', marginBottom: 4 }}>
                    <span style={{ fontSize: 11, color: s.color, background: s.color + '22',
                      padding: '2px 7px', borderRadius: 10, whiteSpace: 'nowrap', flexShrink: 0 }}>
                      {s.text}
                    </span>
                    <span style={{ fontSize: 13, fontWeight: 600, flex: 1 }}>#{t.id} {t.subject}</span>
                  </div>
                  <p style={{ fontSize: 11, opacity: 0.4, margin: '0 0 6px' }}>
                    {new Date(t.createdAt).toLocaleDateString('ru')}
                  </p>
                  {t.adminReply && (
                    <div style={{ padding: '8px 10px', borderRadius: 8,
                      background: 'rgba(100,181,246,0.1)', borderLeft: '2px solid #42a5f5', marginBottom: 8 }}>
                      <p style={{ fontSize: 11, opacity: 0.5, margin: '0 0 3px' }}>Ответ поддержки:</p>
                      <p style={{ fontSize: 13, margin: 0 }}>{t.adminReply}</p>
                    </div>
                  )}
                  {t.status !== 'CLOSED' && (
                    <button onClick={() => handleCloseTicket(t.id)} disabled={closingId === t.id}
                      style={{ padding: '5px 12px', borderRadius: 7, border: 'none', cursor: 'pointer',
                        fontSize: 12, background: 'rgba(239,83,80,0.15)', color: '#ef9a9a', fontWeight: 600 }}>
                      {closingId === t.id ? '...' : '✕ Закрыть'}
                    </button>
                  )}
                </div>
              );
            })
        }
      </div>
    </div>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────

const page: React.CSSProperties = {
  overflowY: 'auto', height: '100%', boxSizing: 'border-box',
  background: 'var(--tg-theme-bg-color, #1c1c1e)',
  color: 'var(--tg-theme-text-color, #fff)',
  paddingBottom: 32,
};

const center: React.CSSProperties = {
  display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%',
};

// Profile header
const profileHeader: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 14,
  padding: '20px 16px 4px',
  marginBottom: 12,
};

const avatarCircle: React.CSSProperties = {
  width: 52, height: 52, borderRadius: '50%',
  background: 'var(--tg-theme-button-color, #2196F3)',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
  fontSize: 22, fontWeight: 700, flexShrink: 0,
  color: '#fff',
};

const roleBadge = (bg: string, color = '#fff'): React.CSSProperties => ({
  fontSize: 11, padding: '2px 8px', borderRadius: 8, fontWeight: 600,
  background: bg + '33', color: bg === '#ffd600' ? color : bg,
  border: `1px solid ${bg}55`,
});

const repBadge = (positive: boolean): React.CSSProperties => ({
  fontSize: 11, padding: '2px 8px', borderRadius: 8, fontWeight: 600,
  background: positive ? 'rgba(102,187,106,0.15)' : 'rgba(239,83,80,0.15)',
  color: positive ? '#81c784' : '#ef9a9a',
  border: `1px solid ${positive ? 'rgba(102,187,106,0.3)' : 'rgba(239,83,80,0.3)'}`,
});

const bannedCard: React.CSSProperties = {
  margin: '16px 16px 0',
  background: 'rgba(239,83,80,0.1)', border: '1px solid rgba(239,83,80,0.35)',
  borderRadius: 14, padding: '14px 16px',
};

// Grouped settings
const group: React.CSSProperties = {
  background: 'var(--tg-theme-secondary-bg-color, #2c2c2e)',
  borderRadius: 16, margin: '0 16px 16px',
  paddingTop: 14,
  overflow: 'hidden',
};

const groupTitle: React.CSSProperties = {
  fontSize: 13, fontWeight: 600, opacity: 0.5, textTransform: 'uppercase',
  letterSpacing: 0.5, marginBottom: 10, paddingLeft: 16,
};

const settingBlock: React.CSSProperties = { padding: '10px 16px' };
const rowInner: React.CSSProperties = { display: 'flex', alignItems: 'flex-start', gap: 12, padding: '4px 16px' };
const rowIcon: React.CSSProperties = { fontSize: 20, width: 24, textAlign: 'center', flexShrink: 0, marginTop: 1 };
const rowLabel: React.CSSProperties = { fontSize: 15, marginBottom: 2 };
const rowSub: React.CSSProperties = { fontSize: 12, opacity: 0.5 };

// Subscription cards
const premiumCard: React.CSSProperties = {
  margin: '0 16px',
  background: 'linear-gradient(135deg,#1a237e,#283593)',
  borderRadius: 16, padding: '16px',
};

const freeCard: React.CSSProperties = {
  background: 'linear-gradient(135deg,#1a237e,#283593)',
  borderRadius: 14, padding: '14px 16px',
};

const buyBtn: React.CSSProperties = {
  background: '#ffd600', color: '#000', border: 'none', borderRadius: 8,
  padding: '7px 16px', cursor: 'pointer', fontWeight: 700, fontSize: 13, whiteSpace: 'nowrap',
};

// Buttons
const primaryBtn: React.CSSProperties = {
  width: '100%', padding: '13px', borderRadius: 12,
  background: 'var(--tg-theme-button-color, #2196F3)',
  color: '#fff', border: 'none', cursor: 'pointer',
  fontSize: 15, fontWeight: 600,
};

const ghostBtn: React.CSSProperties = {
  width: '100%', padding: '12px', borderRadius: 12,
  background: 'rgba(255,255,255,0.07)',
  color: 'var(--tg-theme-text-color, #fff)',
  border: '1px solid rgba(255,255,255,0.1)',
  cursor: 'pointer', fontSize: 14,
};

const smallBtn: React.CSSProperties = {
  padding: '5px 12px', borderRadius: 8, border: 'none', cursor: 'pointer',
  background: 'var(--tg-theme-button-color, #2196F3)', color: '#fff', fontSize: 13,
};

const applyBtn: React.CSSProperties = {
  padding: '10px 16px', borderRadius: 10, border: 'none',
  background: 'var(--tg-theme-button-color, #2196F3)',
  color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer', whiteSpace: 'nowrap', flexShrink: 0,
};

// Toggle
const toggle = (on: boolean): React.CSSProperties => ({
  width: 51, height: 31, borderRadius: 16, border: 'none', cursor: 'pointer',
  background: on ? 'var(--tg-theme-button-color, #34C759)' : 'rgba(255,255,255,0.15)',
  position: 'relative', flexShrink: 0, transition: 'background 0.2s', padding: 0,
});

const toggleThumb = (on: boolean): React.CSSProperties => ({
  position: 'absolute', top: 3, left: on ? 23 : 3,
  width: 25, height: 25, borderRadius: '50%',
  background: '#fff', boxShadow: '0 1px 4px rgba(0,0,0,0.3)',
  transition: 'left 0.2s',
});

// Chips
const chip = (selected: boolean, active: boolean, reset = false): React.CSSProperties => ({
  padding: '5px 10px', borderRadius: 16, fontSize: 12,
  border: reset ? '1px solid rgba(255,255,255,0.2)' : 'none',
  cursor: 'pointer',
  background: selected ? 'var(--tg-theme-button-color, #2196F3)' : reset ? 'none' : 'rgba(255,255,255,0.1)',
  color: reset ? 'rgba(255,255,255,0.5)' : '#fff',
  opacity: active ? 1 : 0.45,
});

// Inputs
const inputStyle: React.CSSProperties = {
  width: '100%', boxSizing: 'border-box',
  padding: '10px 12px', borderRadius: 10,
  border: '1px solid rgba(255,255,255,0.1)',
  background: 'rgba(255,255,255,0.07)', color: '#fff', fontSize: 13,
};

// Tickets
const ticketCard: React.CSSProperties = {
  background: 'rgba(255,255,255,0.04)', borderRadius: 12, padding: '12px',
  marginBottom: 8,
};
