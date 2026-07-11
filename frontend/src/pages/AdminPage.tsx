/**
 * DPS Tracker administration panel.
 * Top-level menu → nested sections with their own back navigation.
 *
 * @author marensovich
 * @version 2.0.0
 * @since 1.0.0
 */

import { useEffect, useState, useCallback } from 'react';
import {
  adminFetchStats, adminFetchUsers, adminToggleBan, adminForceSubscribe,
  adminFetchPosts, adminDeactivatePost, adminGetModerationQueue,
  adminBroadcast, adminGetSettings, adminUpdateSetting,
  adminGetLogs, adminChangeRole, adminGetActivity,
  adminGrantTrial, adminRevokeSubscription, adminAssignModerator, adminRevokeModerator,
  adminGetPromos, adminCreatePromo, adminDeactivatePromo, adminDeletePromo,
  adminGetTickets, adminReplyTicket, adminCloseTicket,
} from '../api/client';
import type {
  AdminStats, AdminUser, DpsPost, PagedResponse,
  AppSetting, AdminLogEntry, ActivityDataPoint, PromoCode, Ticket,
} from '../types';
import { POST_TYPE_LABELS } from '../types';

// ── Navigation types ──────────────────────────────────────────────────────────

type Section =
  | 'home'
  | 'stats'
  | 'users'
  | 'content'
  | 'content_posts'
  | 'content_moderation'
  | 'broadcast'
  | 'promo'
  | 'tickets'
  | 'system'
  | 'system_settings'
  | 'system_logs';

// ── Root component ────────────────────────────────────────────────────────────

export default function AdminPage() {
  const [section, setSection] = useState<Section>('home');

  const nav = (s: Section) => setSection(s);
  const back = (s: Section) => setSection(s);

  return (
    <div style={page}>
      {section === 'home'              && <HomeScreen nav={nav} />}
      {section === 'stats'             && <StatsSection back={() => back('home')} />}
      {section === 'users'             && <UsersSection back={() => back('home')} />}
      {section === 'content'             && <ContentMenu nav={nav} back={() => back('home')} />}
      {section === 'content_posts'       && <PostsSection back={() => back('content')} />}
      {section === 'content_moderation'  && <ModerationSection back={() => back('content')} />}
      {section === 'broadcast'         && <BroadcastSection back={() => back('home')} />}
      {section === 'promo'             && <PromoSection back={() => back('home')} />}
      {section === 'system'            && <SystemMenu nav={nav} back={() => back('home')} />}
      {section === 'system_settings'   && <SettingsSection back={() => back('system')} />}
      {section === 'system_logs'       && <LogsSection back={() => back('system')} />}
      {section === 'tickets'            && <TicketsSection back={() => back('home')} />}
    </div>
  );
}

// ── Home screen — tiles ───────────────────────────────────────────────────────

function HomeScreen({ nav }: { nav: (s: Section) => void }) {
  const tiles: { section: Section; icon: string; label: string; hint: string; color: string }[] = [
    { section: 'stats',     icon: '📊', label: 'Статистика',   hint: 'Пользователи, посты, активность', color: '#1565c0' },
    { section: 'users',     icon: '👥', label: 'Пользователи', hint: 'Бан, подписка, роль, пробный',    color: '#2e7d32' },
    { section: 'content',   icon: '📍', label: 'Контент',      hint: 'Посты, модерация, камеры',        color: '#6a1b9a' },
    { section: 'broadcast', icon: '📢', label: 'Рассылка',     hint: 'Сообщение всем пользователям',   color: '#e65100' },
    { section: 'promo',     icon: '🎟', label: 'Промокоды',    hint: 'Скидки и VIP-дни',               color: '#00695c' },
    { section: 'tickets',   icon: '🎧', label: 'Поддержка',    hint: 'Тикеты от пользователей',        color: '#4527a0' },
    { section: 'system',    icon: '⚙️', label: 'Система',      hint: 'Настройки и журнал действий',    color: '#37474f' },
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      <div style={sectionHeader}>
        <span style={{ fontSize: 20 }}>🛡</span>
        <h2 style={sectionTitle}>Панель</h2>
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: '8px 12px 16px' }}>
        <div style={tileGrid}>
          {tiles.map((t) => (
            <button key={t.section} title={t.hint} onClick={() => nav(t.section)} style={tile(t.color)}>
              <span style={{ fontSize: 30, lineHeight: 1 }}>{t.icon}</span>
              <span style={tileLabel}>{t.label}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── Statistics ────────────────────────────────────────────────────────────────

function StatsSection({ back }: { back: () => void }) {
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [activity, setActivity] = useState<ActivityDataPoint[]>([]);
  const [period, setPeriod] = useState(14);
  const [loadingActivity, setLoadingActivity] = useState(false);

  useEffect(() => { adminFetchStats().then(setStats); }, []);

  useEffect(() => {
    setLoadingActivity(true);
    adminGetActivity(period).then((data) => { setActivity(data); setLoadingActivity(false); });
  }, [period]);

  return (
    <SectionShell icon="📊" title="Статистика" back={back}>
      {!stats ? <Loader /> : (
        <>
          <div style={statGrid}>
            {[
              { label: 'Всего',    value: stats.totalUsers,    icon: '👥', color: '#42a5f5' },
              { label: 'Premium',  value: stats.premiumUsers,  icon: '⭐', color: '#ffd54f' },
              { label: 'Модер.',   value: stats.moderatorCount,icon: '👮', color: '#ce93d8' },
              { label: 'Забанено', value: stats.bannedUsers,   icon: '🚫', color: '#ef5350' },
              { label: 'Меток',    value: stats.totalPosts,    icon: '📍', color: '#66bb6a' },
              { label: 'Активных', value: stats.activePosts,   icon: '✅', color: '#26c6da' },
            ].map(({ label, value, icon, color }) => (
              <div key={label} style={statCard(color)}>
                <span style={{ fontSize: 22 }}>{icon}</span>
                <span style={{ fontSize: 20, fontWeight: 800, color }}>{value.toLocaleString('ru-RU')}</span>
                <span style={{ fontSize: 10, opacity: 0.6, textAlign: 'center' }}>{label}</span>
              </div>
            ))}
          </div>

          {/* User growth */}
          <div style={{ ...chartCard, marginBottom: 10 }}>
            <p style={cardLabel}>📈 Новые пользователи</p>
            <div style={{ display: 'flex', gap: 8 }}>
              {[
                { label: 'Сегодня',  value: stats.newUsersToday,     color: '#42a5f5' },
                { label: 'Неделя',   value: stats.newUsersThisWeek,  color: '#66bb6a' },
                { label: 'Месяц',    value: stats.newUsersThisMonth, color: '#ffd54f' },
              ].map(({ label, value, color }) => (
                <div key={label} style={{ flex: 1, textAlign: 'center', padding: '10px 8px', background: color + '15', borderRadius: 10, border: `1px solid ${color}30` }}>
                  <div style={{ fontSize: 22, fontWeight: 800, color }}>{value}</div>
                  <div style={{ fontSize: 10, opacity: 0.55, marginTop: 2 }}>{label}</div>
                </div>
              ))}
            </div>
          </div>

          <div style={{ ...chartCard, marginBottom: 10 }}>
            <p style={cardLabel}>👥 Распределение пользователей</p>
            <UserDonut stats={stats} />
          </div>

          {/* User activity chart */}
          <div style={{ ...chartCard, marginBottom: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
              <p style={{ ...cardLabel, marginBottom: 0 }}>👥 Пользователи</p>
              <div style={{ display: 'flex', gap: 4 }}>
                {([7, 14, 30] as const).map((d) => (
                  <button key={d} onClick={() => setPeriod(d)} style={periodBtn(period === d)}>{d}д</button>
                ))}
              </div>
            </div>
            {loadingActivity ? <Loader /> : activity.length > 0 ? (
              <>
                <UserChart activity={activity} />
                <div style={{ display: 'flex', gap: 16, marginTop: 6, fontSize: 11, opacity: 0.45 }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}><span style={{ width: 8, height: 8, borderRadius: 2, background: '#66bb6a', display: 'inline-block' }} />Активные</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}><span style={{ width: 8, height: 8, borderRadius: 2, background: '#42a5f5', display: 'inline-block' }} />Новые</span>
                </div>
              </>
            ) : <p style={{ opacity: 0.4, fontSize: 13 }}>Нет данных</p>}
          </div>

          {/* Posts activity chart */}
          <div style={chartCard}>
            <p style={{ ...cardLabel, marginBottom: 10 }}>📍 Посты</p>
            {loadingActivity ? <Loader /> : activity.length > 0 ? (
              <>
                <ActivityChart activity={activity} />
                <div style={{ display: 'flex', gap: 16, marginTop: 6, fontSize: 11, opacity: 0.45 }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}><span style={{ width: 8, height: 8, borderRadius: 2, background: '#42a5f5', display: 'inline-block' }} />Новых меток</span>
                </div>
              </>
            ) : <p style={{ opacity: 0.4, fontSize: 13 }}>Нет данных</p>}
          </div>
        </>
      )}
    </SectionShell>
  );
}

function UserDonut({ stats }: { stats: AdminStats }) {
  const free = Math.max(0, stats.totalUsers - stats.premiumUsers - stats.bannedUsers);
  const total = stats.totalUsers || 1;
  const segments = [
    { label: 'Свободные', value: free,              color: '#42a5f5' },
    { label: 'Premium',   value: stats.premiumUsers, color: '#ffd54f' },
    { label: 'Забанено',  value: stats.bannedUsers,  color: '#ef5350' },
  ].filter((s) => s.value > 0);

  const R = 44, r = 27, cx = 58, cy = 58;
  let offsetAngle = -Math.PI / 2;
  const arcs = segments.map((s) => {
    const angle = (s.value / total) * 2 * Math.PI;
    const x1 = cx + R * Math.cos(offsetAngle);
    const y1 = cy + R * Math.sin(offsetAngle);
    const x2 = cx + R * Math.cos(offsetAngle + angle);
    const y2 = cy + R * Math.sin(offsetAngle + angle);
    const xi1 = cx + r * Math.cos(offsetAngle);
    const yi1 = cy + r * Math.sin(offsetAngle);
    const xi2 = cx + r * Math.cos(offsetAngle + angle);
    const yi2 = cy + r * Math.sin(offsetAngle + angle);
    const large = angle > Math.PI ? 1 : 0;
    const d = `M${x1},${y1} A${R},${R},0,${large},1,${x2},${y2} L${xi2},${yi2} A${r},${r},0,${large},0,${xi1},${yi1} Z`;
    offsetAngle += angle;
    return { ...s, d };
  });

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
      <svg viewBox="0 0 116 116" style={{ width: 100, height: 100, flexShrink: 0 }}>
        {arcs.map((arc) => <path key={arc.label} d={arc.d} fill={arc.color} opacity={0.88} />)}
        <text x={cx} y={cy - 5} fontSize={18} fontWeight={800} fill="#fff" textAnchor="middle">{stats.totalUsers.toLocaleString('ru-RU')}</text>
        <text x={cx} y={cy + 11} fontSize={8} fill="rgba(255,255,255,0.45)" textAnchor="middle">всего</text>
      </svg>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 7, flex: 1 }}>
        {segments.map((s) => (
          <div key={s.label} style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: 12 }}>
            <span style={{ width: 10, height: 10, borderRadius: '50%', background: s.color, flexShrink: 0, display: 'inline-block' }} />
            <span style={{ opacity: 0.65, flex: 1 }}>{s.label}</span>
            <span style={{ fontWeight: 700 }}>{s.value.toLocaleString('ru-RU')}</span>
            <span style={{ opacity: 0.4, fontSize: 11, minWidth: 34, textAlign: 'right' }}>{Math.round(s.value / total * 100)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}

/** Bar chart showing new DPS posts per day. */
function ActivityChart({ activity }: { activity: ActivityDataPoint[] }) {
  const W = 300, H = 100;
  const L = 28, R2 = 4, TOP = 6, BOT = 20;
  const chartW = W - L - R2;
  const chartH = H - TOP - BOT;

  const maxVal = Math.max(...activity.map((a) => a.posts), 1);
  const niceMax = niceCeil(maxVal);
  const yTicks = [0, 0.5, 1].map((f) => Math.round(f * niceMax));

  const n = activity.length;
  const groupW = chartW / n;
  const barW = Math.max(2, Math.min(12, groupW * 0.65));

  const xOf = (i: number) => L + i * groupW + groupW / 2;
  const yOf = (val: number) => TOP + chartH - (val / niceMax) * chartH;
  const labelStep = Math.ceil(n / 7);

  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 100 }}>
      {yTicks.map((v) => (
        <g key={v}>
          <line x1={L} x2={W - R2} y1={yOf(v)} y2={yOf(v)} stroke="rgba(255,255,255,0.07)" strokeWidth={1} />
          <text x={L - 3} y={yOf(v) + 4} fontSize={7} fill="rgba(255,255,255,0.3)" textAnchor="end">{v}</text>
        </g>
      ))}
      {activity.map((d, i) => {
        const bh = Math.max(2, yOf(0) - yOf(d.posts));
        return (
          <g key={d.date}>
            <rect x={xOf(i) - barW / 2} y={yOf(d.posts)} width={barW} height={bh} fill="#42a5f5" rx={2} opacity={0.9}>
              <title>{d.date}: {d.posts} постов</title>
            </rect>
            {i % labelStep === 0 && (
              <text x={xOf(i)} y={H - 3} fontSize={7} fill="rgba(255,255,255,0.3)" textAnchor="middle">{fmtShortDate(d.date)}</text>
            )}
          </g>
        );
      })}
    </svg>
  );
}

/** Line + bar chart showing active users (bars) and new registrations (line) per day. */
function UserChart({ activity }: { activity: ActivityDataPoint[] }) {
  const W = 300, H = 120;
  const L = 28, R2 = 4, TOP = 6, BOT = 20;
  const chartW = W - L - R2;
  const chartH = H - TOP - BOT;

  const maxVal = Math.max(...activity.map((a) => a.activeUsers), ...activity.map((a) => a.users), 1);
  const niceMax = niceCeil(maxVal);
  const yTicks = [0, 0.5, 1].map((f) => Math.round(f * niceMax));

  const n = activity.length;
  const groupW = chartW / n;
  const barW = Math.max(2, Math.min(10, groupW * 0.55));

  const xOf = (i: number) => L + i * groupW + groupW / 2;
  const yOf = (val: number) => TOP + chartH - (val / niceMax) * chartH;
  const labelStep = Math.ceil(n / 7);

  // Line path for new users
  const linePts = activity.map((d, i) => `${xOf(i)},${yOf(d.users)}`).join(' ');

  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 120 }}>
      {yTicks.map((v) => (
        <g key={v}>
          <line x1={L} x2={W - R2} y1={yOf(v)} y2={yOf(v)} stroke="rgba(255,255,255,0.07)" strokeWidth={1} />
          <text x={L - 3} y={yOf(v) + 4} fontSize={7} fill="rgba(255,255,255,0.3)" textAnchor="end">{v}</text>
        </g>
      ))}
      {/* Active users bars */}
      {activity.map((d, i) => {
        const bh = Math.max(2, yOf(0) - yOf(d.activeUsers));
        return (
          <rect key={d.date} x={xOf(i) - barW / 2} y={yOf(d.activeUsers)} width={barW} height={bh}
            fill="#66bb6a" rx={2} opacity={0.75}>
            <title>{d.date}: {d.activeUsers} активных</title>
          </rect>
        );
      })}
      {/* New users line */}
      {activity.length > 1 && (
        <polyline points={linePts} fill="none" stroke="#42a5f5" strokeWidth={2} strokeLinejoin="round" />
      )}
      {activity.map((d, i) => (
        <circle key={d.date + 'pt'} cx={xOf(i)} cy={yOf(d.users)} r={2.5} fill="#42a5f5">
          <title>{d.date}: {d.users} новых</title>
        </circle>
      ))}
      {/* X-axis labels */}
      {activity.map((d, i) => i % labelStep === 0 && (
        <text key={d.date + 'l'} x={xOf(i)} y={H - 3} fontSize={7} fill="rgba(255,255,255,0.3)" textAnchor="middle">
          {fmtShortDate(d.date)}
        </text>
      ))}
    </svg>
  );
}

function niceCeil(n: number): number {
  if (n <= 4) return Math.max(4, n);
  const mag = Math.pow(10, Math.floor(Math.log10(n)));
  return Math.ceil(n / mag) * mag;
}

function fmtShortDate(iso: string): string {
  const d = new Date(iso);
  return `${d.getDate()}.${String(d.getMonth() + 1).padStart(2, '0')}`;
}

// ── Users ─────────────────────────────────────────────────────────────────────

type UserFilter = 'ALL' | 'ADMIN' | 'MODERATOR' | 'PREMIUM' | 'BANNED';

function UsersSection({ back }: { back: () => void }) {
  const [data, setData] = useState<PagedResponse<AdminUser> | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<UserFilter>('ALL');
  const [actionId, setActionId] = useState<number | null>(null);
  const [trialDays, setTrialDays] = useState<Record<number, string>>({});
  const [modRegion, setModRegion] = useState<Record<number, string>>({});
  const [expanded, setExpanded] = useState<number | null>(null);

  const load = useCallback((p: number, q: string, f: UserFilter) => {
    const role = f === 'ADMIN' ? 'ADMIN' : f === 'MODERATOR' ? 'MODERATOR' : undefined;
    const subscribed = f === 'PREMIUM' ? true : undefined;
    const banned = f === 'BANNED' ? true : undefined;
    adminFetchUsers(p, 10, q, role, banned, subscribed).then((res) => { setData(res); setPage(p); });
  }, []);

  useEffect(() => { load(0, query, filter); }, [query, filter, load]);

  const update = (u: AdminUser) =>
    setData((prev) => prev ? { ...prev, content: prev.content.map((x) => x.id === u.id ? u : x) } : prev);

  const act = async (id: number, fn: () => Promise<AdminUser>) => {
    setActionId(id);
    update(await fn());
    setActionId(null);
  };

  const handleTrial = async (u: AdminUser) => {
    const days = parseInt(trialDays[u.id] ?? '3', 10);
    if (!days || days < 1) return;
    setActionId(u.id);
    update(await adminGrantTrial(u.id, days));
    setTrialDays((prev) => { const n = { ...prev }; delete n[u.id]; return n; });
    setActionId(null);
  };

  const handleAssignModerator = async (u: AdminUser) => {
    const region = modRegion[u.id]?.trim();
    if (!region) return;
    setActionId(u.id);
    update(await adminAssignModerator(u.id, region));
    setModRegion((prev) => { const n = { ...prev }; delete n[u.id]; return n; });
    setActionId(null);
  };

  return (
    <SectionShell icon="👥" title="Пользователи" back={back}>
      {/* Search bar */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
        <input
          placeholder="Поиск по username / tgId"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && setQuery(search)}
          style={inputStyle}
        />
        <button onClick={() => setQuery(search)} style={iconBtn}>🔍</button>
        {query && <button onClick={() => { setQuery(''); setSearch(''); }} style={iconBtn}>✕</button>}
      </div>

      {/* Role/status filter chips */}
      <div style={{ display: 'flex', gap: 6, marginBottom: 12, overflowX: 'auto' }}>
        {([
          { v: 'ALL' as UserFilter,       label: 'Все' },
          { v: 'ADMIN' as UserFilter,     label: '🛡 ADM' },
          { v: 'MODERATOR' as UserFilter, label: '👮 МОД' },
          { v: 'PREMIUM' as UserFilter,   label: '⭐ Premium' },
          { v: 'BANNED' as UserFilter,    label: '🚫 Бан' },
        ]).map(({ v, label }) => (
          <button key={v} onClick={() => { setFilter(v); setPage(0); }}
            style={{ ...segBtn(filter === v), flexShrink: 0, padding: '6px 10px', fontSize: 11 }}>
            {label}
          </button>
        ))}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {data?.content.map((u) => (
          <div key={u.id} style={userCard(u.isBanned)}>
            {/* Card header — name and meta */}
            <button
              style={{ background: 'none', border: 'none', width: '100%', textAlign: 'left', cursor: 'pointer', padding: 0, color: 'inherit' }}
              onClick={() => setExpanded(expanded === u.id ? null : u.id)}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <span style={{ fontWeight: 700, fontSize: 14 }}>
                    {u.firstName ?? u.username ?? `#${u.id}`}
                    {u.role === 'ADMIN' && <span style={badge('#7c4dff')}>ADM</span>}
                    {u.role === 'MODERATOR' && <span style={badge('#ff9800')}>МОД</span>}
                    {u.isBanned && <span style={badge('#ef5350')}>БАН</span>}
                    {u.isSubscribed && <span style={badge('#ffd600', '#000')}>⭐</span>}
                  </span>
                  <div style={{ fontSize: 11, opacity: 0.5, marginTop: 2 }}>
                    {u.username ? `@${u.username}` : `tg:${u.tgId}`}
                    {' · '}rep: {u.reputationScore ?? 0}
                    {u.lastSeen ? ' · ' + fmtDate(u.lastSeen) : ''}
                  </div>
                </div>
                <span style={{ opacity: 0.4, fontSize: 12, paddingTop: 2 }}>{expanded === u.id ? '▲' : '▼'}</span>
              </div>
            </button>

            {/* Expanded actions */}
            {expanded === u.id && (
              <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid rgba(255,255,255,0.08)' }}>
                {u.isSubscribed && u.subscriptionExpiresAt && (
                  <p style={{ margin: '0 0 10px', fontSize: 12, color: '#81c784' }}>
                    Premium до {new Date(u.subscriptionExpiresAt).toLocaleDateString('ru-RU')}
                  </p>
                )}

                {/* Block: ban / role */}
                <div style={actionGroup}>
                  <p style={actionGroupLabel}>Управление</p>
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    <ActionBtn
                      color={u.isBanned ? '#66bb6a' : '#ef5350'}
                      label={u.isBanned ? '✅ Разблокировать' : '🚫 Заблокировать'}
                      disabled={actionId === u.id || u.role === 'ADMIN'}
                      onClick={() => act(u.id, () => adminToggleBan(u.id))}
                    />
                    <ActionBtn
                      color={u.role === 'ADMIN' ? '#ce93d8' : '#90caf9'}
                      label={u.role === 'ADMIN' ? '↓ Снять ADM' : '↑ Назначить ADM'}
                      disabled={actionId === u.id}
                      onClick={() => act(u.id, () => adminChangeRole(u.id, u.role === 'ADMIN' ? 'USER' : 'ADMIN'))}
                    />
                  </div>
                </div>

                {/* Block: moderator */}
                {u.role !== 'ADMIN' && (
                  <div style={actionGroup}>
                    <p style={actionGroupLabel}>Модератор</p>
                    {u.role === 'MODERATOR' ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                        <p style={{ margin: 0, fontSize: 12, color: '#ffb74d' }}>
                          📍 Регион: <strong>{u.moderatorRegion ?? '—'}</strong>
                        </p>
                        <ActionBtn
                          color="#ef5350"
                          label="✕ Снять с модерации"
                          disabled={actionId === u.id}
                          onClick={() => act(u.id, () => adminRevokeModerator(u.id))}
                        />
                      </div>
                    ) : (
                      <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                        <input
                          placeholder="Регион (напр. Москва)"
                          value={modRegion[u.id] ?? ''}
                          onChange={(e) => setModRegion((prev) => ({ ...prev, [u.id]: e.target.value }))}
                          list="mod-region-list"
                          style={{ ...inputStyle, flex: 1, padding: '7px 10px', fontSize: 12 }}
                        />
                        <button
                          disabled={actionId === u.id || !modRegion[u.id]?.trim()}
                          onClick={() => handleAssignModerator(u)}
                          style={{ ...primaryBtn, padding: '7px 12px', fontSize: 12, flexShrink: 0, opacity: (!modRegion[u.id]?.trim() || actionId === u.id) ? 0.4 : 1 }}
                        >
                          👮 Назначить
                        </button>
                      </div>
                    )}
                  </div>
                )}
                <datalist id="mod-region-list">
                  {['Москва', 'МО', 'Санкт-Петербург', 'Ленинградская обл.', 'ЦАО', 'ЗАО', 'ВАО', 'САО', 'ЮЗАО', 'ЮАО', 'СЗАО', 'Краснодарский край', 'Новосибирская обл.', 'Свердловская обл.'].map((r) => (
                    <option key={r} value={r} />
                  ))}
                </datalist>

                {/* Block: subscription */}
                <div style={actionGroup}>
                  <p style={actionGroupLabel}>Подписка</p>
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                    {!u.isSubscribed ? (
                      <ActionBtn
                        color="#ffd54f"
                        label="⭐ Выдать Premium (30 дн)"
                        disabled={actionId === u.id}
                        onClick={() => act(u.id, () => adminForceSubscribe(u.id))}
                      />
                    ) : (
                      <ActionBtn
                        color="#ef9a9a"
                        label="✕ Отозвать подписку"
                        disabled={actionId === u.id}
                        onClick={() => act(u.id, () => adminRevokeSubscription(u.id))}
                      />
                    )}
                    <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                      <input
                        type="number" min={1} max={365} placeholder="дней"
                        value={trialDays[u.id] ?? ''}
                        onChange={(e) => setTrialDays((prev) => ({ ...prev, [u.id]: e.target.value }))}
                        style={{ ...inputStyle, width: 60, padding: '6px 8px', textAlign: 'center' }}
                      />
                      <ActionBtn
                        color="#ffb74d"
                        label="🎁 Пробный период"
                        disabled={actionId === u.id || !trialDays[u.id]}
                        onClick={() => handleTrial(u)}
                      />
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>

      <Pager page={page} totalPages={data?.totalPages ?? 1} onChange={(p) => load(p, query, filter)} />
    </SectionShell>
  );
}

// ── Content — sub-menu ────────────────────────────────────────────────────────

function ContentMenu({ nav, back }: { nav: (s: Section) => void; back: () => void }) {
  const items: { section: Section; icon: string; label: string; desc: string }[] = [
    { section: 'content_posts',      icon: '📍', label: 'Все посты',        desc: 'Просмотр и деактивация' },
    { section: 'content_moderation', icon: '⚠️', label: 'Очередь модерации', desc: 'Посты с низким доверием' },
  ];
  return (
    <SectionShell icon="📍" title="Контент" back={back}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {items.map((it) => (
          <button key={it.section} onClick={() => nav(it.section)} style={menuRow}>
            <span style={{ fontSize: 28 }}>{it.icon}</span>
            <div style={{ textAlign: 'left' }}>
              <div style={{ fontWeight: 600, fontSize: 15 }}>{it.label}</div>
              <div style={{ fontSize: 12, opacity: 0.5, marginTop: 2 }}>{it.desc}</div>
            </div>
            <span style={{ marginLeft: 'auto', opacity: 0.35, fontSize: 18 }}>›</span>
          </button>
        ))}
      </div>
    </SectionShell>
  );
}

function PostsSection({ back }: { back: () => void }) {
  const [data, setData] = useState<PagedResponse<DpsPost> | null>(null);
  const [page, setPage] = useState(0);
  const [removing, setRemoving] = useState<number | null>(null);

  const load = (p: number) => adminFetchPosts(p, 12).then((r) => { setData(r); setPage(p); });
  useEffect(() => { load(0); }, []);

  const handleRemove = async (id: number) => {
    setRemoving(id);
    await adminDeactivatePost(id);
    setData((prev) => prev ? { ...prev, content: prev.content.map((p) => p.id === id ? { ...p, isActive: false } : p) } : prev);
    setRemoving(null);
  };

  return (
    <SectionShell icon="📍" title="Все посты" back={back}>
      <PostList posts={data?.content ?? []} onRemove={handleRemove} removingId={removing}
        pager={<Pager page={page} totalPages={data?.totalPages ?? 1} onChange={load} />} />
    </SectionShell>
  );
}

function ModerationSection({ back }: { back: () => void }) {
  const [data, setData] = useState<PagedResponse<DpsPost> | null>(null);
  const [page, setPage] = useState(0);
  const [removing, setRemoving] = useState<number | null>(null);

  const load = (p: number) => adminGetModerationQueue(p, 12).then((r) => { setData(r); setPage(p); });
  useEffect(() => { load(0); }, []);

  const handleRemove = async (id: number) => {
    setRemoving(id);
    await adminDeactivatePost(id);
    setData((prev) => prev ? { ...prev, content: prev.content.filter((p) => p.id !== id) } : prev);
    setRemoving(null);
  };

  return (
    <SectionShell icon="⚠️" title="Модерация" back={back}>
      {data?.content.length === 0
        ? <Empty text="Очередь пуста 🎉" />
        : <PostList posts={data?.content ?? []} onRemove={handleRemove} removingId={removing}
            pager={<Pager page={page} totalPages={data?.totalPages ?? 1} onChange={load} />} />
      }
    </SectionShell>
  );
}

// ── Broadcast ─────────────────────────────────────────────────────────────────

function BroadcastSection({ back }: { back: () => void }) {
  const [message, setMessage] = useState('');
  const [target, setTarget] = useState('ALL');
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);

  const targetLabel = target === 'ALL' ? 'всем пользователям' : target === 'PREMIUM' ? 'Premium-пользователям' : 'бесплатным пользователям';

  const handleSend = async () => {
    setSending(true);
    setConfirming(false);
    setResult(null);
    try {
      const res = await adminBroadcast(message, target);
      setResult(`✅ Отправлено ${res.sent} пользователям`);
      setMessage('');
    } catch {
      setResult('❌ Ошибка отправки');
    } finally {
      setSending(false);
    }
  };

  return (
    <SectionShell icon="📢" title="Рассылка" back={back}>
      <div style={formCard}>
        <p style={cardLabel}>Получатели</p>
        <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
          {[
            { v: 'ALL', label: '👥 Все' },
            { v: 'PREMIUM', label: '⭐ Premium' },
            { v: 'FREE', label: '🆓 Free' },
          ].map(({ v, label }) => (
            <button key={v} onClick={() => setTarget(v)} style={segBtn(target === v)}>{label}</button>
          ))}
        </div>

        <p style={cardLabel}>Сообщение (Markdown)</p>
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="*Важное* сообщение..."
          rows={6}
          style={textareaStyle}
        />
        <p style={{ fontSize: 11, opacity: 0.4, margin: '4px 0 0' }}>{message.length} / 2000</p>

        {/* Preview of what users will receive */}
        {message.trim() && (
          <div style={{ margin: '12px 0', padding: '10px 12px', borderRadius: 8, background: 'rgba(33,150,243,0.1)', border: '1px solid rgba(33,150,243,0.25)' }}>
            <p style={{ margin: '0 0 6px', fontSize: 10, opacity: 0.5, textTransform: 'uppercase', letterSpacing: 0.5 }}>Пользователь получит</p>
            <p style={{ margin: 0, fontSize: 12, opacity: 0.8, lineHeight: 1.5 }}>
              📢 <b>Сообщение от Администратора:</b><br /><br />
              {message}
            </p>
          </div>
        )}

        <div style={{ marginTop: 16 }}>
          {!confirming ? (
            <button
              onClick={() => setConfirming(true)}
              disabled={sending || !message.trim()}
              style={primaryBtn}
            >
              📢 Отправить {targetLabel}
            </button>
          ) : (
            <div style={{ background: 'rgba(239,83,80,0.12)', border: '1px solid rgba(239,83,80,0.3)', borderRadius: 10, padding: '14px' }}>
              <p style={{ margin: '0 0 12px', fontSize: 14, fontWeight: 600 }}>
                Подтвердите рассылку
              </p>
              <p style={{ margin: '0 0 14px', fontSize: 13, opacity: 0.7 }}>
                Сообщение будет отправлено <b>{targetLabel}</b>. Это действие нельзя отменить.
              </p>
              <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={handleSend} disabled={sending} style={{ ...primaryBtn, background: '#ef5350', flex: 1 }}>
                  {sending ? 'Отправка…' : '✓ Да, отправить'}
                </button>
                <button onClick={() => setConfirming(false)} style={{ ...primaryBtn, background: 'rgba(255,255,255,0.1)', flex: 1 }}>
                  Отмена
                </button>
              </div>
            </div>
          )}
        </div>

        {result && (
          <p style={{ fontSize: 14, marginTop: 12, opacity: 0.85, color: result.startsWith('✅') ? '#81c784' : '#ef9a9a' }}>
            {result}
          </p>
        )}
      </div>
    </SectionShell>
  );
}

// ── Promo codes ───────────────────────────────────────────────────────────────

function PromoSection({ back }: { back: () => void }) {
  const [promos, setPromos] = useState<PromoCode[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ code: '', type: 'VIP_DAYS', value: '', maxUses: '', expiresAt: '' });
  const [creating, setCreating] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    adminGetPromos().then((data: PromoCode[]) => { setPromos(data); setLoading(false); });
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async () => {
    if (!form.code.trim() || !form.value) return;
    setCreating(true);
    setErr(null);
    try {
      await adminCreatePromo({
        code: form.code.trim().toUpperCase(),
        type: form.type,
        value: parseInt(form.value, 10),
        maxUses: form.maxUses ? parseInt(form.maxUses, 10) : undefined,
        expiresAt: form.expiresAt || undefined,
      });
      setForm({ code: '', type: 'VIP_DAYS', value: '', maxUses: '', expiresAt: '' });
      setShowForm(false);
      load();
    } catch (e: unknown) {
      setErr((e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Ошибка');
    } finally {
      setCreating(false);
    }
  };

  const fmtExpiry = (iso?: string) => iso ? new Date(iso).toLocaleDateString('ru-RU') : '∞';

  return (
    <SectionShell icon="🎟" title="Промокоды" back={back}
      action={<button onClick={() => setShowForm((v) => !v)} style={headerActionBtn}>
        {showForm ? '✕ Отмена' : '+ Создать'}
      </button>}
    >
      {/* Create form */}
      {showForm && (
        <div style={{ ...formCard, marginBottom: 16 }}>
          <p style={cardLabel}>Новый промокод</p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <input placeholder="КОД (напр. SUMMER25)" value={form.code}
              onChange={(e) => setForm((f) => ({ ...f, code: e.target.value.toUpperCase() }))}
              style={inputStyle} />
            <div style={{ display: 'flex', gap: 8 }}>
              <select value={form.type} onChange={(e) => setForm((f) => ({ ...f, type: e.target.value }))}
                style={{ ...inputStyle, flex: 1 }}>
                <option value="VIP_DAYS">VIP дни</option>
                <option value="DISCOUNT_PERCENT">Скидка %</option>
              </select>
              <input type="number" placeholder={form.type === 'VIP_DAYS' ? 'Дней' : '%'} min={1}
                value={form.value} onChange={(e) => setForm((f) => ({ ...f, value: e.target.value }))}
                style={{ ...inputStyle, flex: '0 0 80px', textAlign: 'center' }} />
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <input type="number" placeholder="Макс. использований (∞)" value={form.maxUses}
                onChange={(e) => setForm((f) => ({ ...f, maxUses: e.target.value }))}
                style={{ ...inputStyle, flex: 1 }} />
              <input type="datetime-local" value={form.expiresAt}
                onChange={(e) => setForm((f) => ({ ...f, expiresAt: e.target.value }))}
                style={{ ...inputStyle, flex: 1, colorScheme: 'dark' }} />
            </div>
            {err && <p style={{ margin: 0, fontSize: 12, color: '#ef9a9a' }}>{err}</p>}
            <button onClick={handleCreate} disabled={creating || !form.code.trim() || !form.value}
              style={primaryBtn}>
              {creating ? 'Создание…' : 'Создать промокод'}
            </button>
          </div>
        </div>
      )}

      {loading && <Loader />}
      {!loading && promos.length === 0 && <Empty text="Промокодов нет. Создайте первый." />}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {promos.map((p) => (
          <div key={p.id} style={{ ...promoCard, opacity: p.isActive ? 1 : 0.45 }}>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                <span style={{ fontWeight: 800, fontSize: 15, letterSpacing: 0.5 }}>{p.code}</span>
                <span style={promoTypeBadge(p.type === 'VIP_DAYS')}>
                  {p.type === 'VIP_DAYS' ? `+${p.value} дн` : `-${p.value}%`}
                </span>
                {!p.isActive && <span style={{ fontSize: 10, opacity: 0.4 }}>деакт.</span>}
              </div>
              <div style={{ fontSize: 11, opacity: 0.5 }}>
                Использований: {p.usedCount}{p.maxUses ? `/${p.maxUses}` : ''} · Срок: {fmtExpiry(p.expiresAt)}
              </div>
            </div>
            <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
              {p.isActive && (
                <button style={dangerBtn('#ffcc80')} onClick={async () => {
                  await adminDeactivatePromo(p.id);
                  setPromos((prev) => prev.map((x) => x.id === p.id ? { ...x, isActive: false } : x));
                }}>Откл.</button>
              )}
              <button style={dangerBtn('#ef9a9a')} onClick={async () => {
                await adminDeletePromo(p.id);
                setPromos((prev) => prev.filter((x) => x.id !== p.id));
              }}>✕</button>
            </div>
          </div>
        ))}
      </div>
    </SectionShell>
  );
}

// ── System — sub-menu ─────────────────────────────────────────────────────────

function SystemMenu({ nav, back }: { nav: (s: Section) => void; back: () => void }) {
  const items: { section: Section; icon: string; label: string; desc: string }[] = [
    { section: 'system_settings', icon: '⚙️', label: 'Настройки', desc: 'Радиусы, лимиты, параметры' },
    { section: 'system_logs',     icon: '📜', label: 'Журнал',    desc: 'История действий администратора' },
  ];
  return (
    <SectionShell icon="⚙️" title="Система" back={back}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {items.map((it) => (
          <button key={it.section} onClick={() => nav(it.section)} style={menuRow}>
            <span style={{ fontSize: 28 }}>{it.icon}</span>
            <div style={{ textAlign: 'left' }}>
              <div style={{ fontWeight: 600, fontSize: 15 }}>{it.label}</div>
              <div style={{ fontSize: 12, opacity: 0.5, marginTop: 2 }}>{it.desc}</div>
            </div>
            <span style={{ marginLeft: 'auto', opacity: 0.35, fontSize: 18 }}>›</span>
          </button>
        ))}
      </div>
    </SectionShell>
  );
}

function SettingsSection({ back }: { back: () => void }) {
  const [settings, setSettings] = useState<AppSetting[]>([]);
  const [editing, setEditing] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState<string | null>(null);

  useEffect(() => { adminGetSettings().then(setSettings); }, []);

  const handleSave = async (key: string) => {
    setSaving(key);
    const updated = await adminUpdateSetting(key, editing[key] ?? '');
    setSettings((prev) => prev.map((s) => s.key === key ? updated : s));
    setEditing((prev) => { const n = { ...prev }; delete n[key]; return n; });
    setSaving(null);
  };

  return (
    <SectionShell icon="⚙️" title="Настройки" back={back}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {settings.map((s) => (
          <div key={s.key} style={settingCard}>
            <div style={{ marginBottom: 8 }}>
              <p style={{ margin: 0, fontSize: 13, fontWeight: 600 }}>{s.key}</p>
              {s.description && <p style={{ margin: '3px 0 0', fontSize: 11, opacity: 0.5 }}>{s.description}</p>}
            </div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <input
                style={{ ...inputStyle, flex: 1 }}
                value={editing[s.key] ?? s.value}
                onChange={(e) => setEditing((prev) => ({ ...prev, [s.key]: e.target.value }))}
                onKeyDown={(e) => e.key === 'Enter' && handleSave(s.key)}
              />
              {editing[s.key] !== undefined && editing[s.key] !== s.value && (
                <button style={primaryBtn} onClick={() => handleSave(s.key)} disabled={saving === s.key}>
                  {saving === s.key ? '…' : '✓ Сохранить'}
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </SectionShell>
  );
}

function LogsSection({ back }: { back: () => void }) {
  const [data, setData] = useState<PagedResponse<AdminLogEntry> | null>(null);
  const [page, setPage] = useState(0);

  const load = (p: number) => adminGetLogs(p, 20).then((r) => { setData(r); setPage(p); });
  useEffect(() => { load(0); }, []);

  return (
    <SectionShell icon="📜" title="Журнал" back={back}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {data?.content.map((l) => (
          <div key={l.id} style={logCard}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
              <span style={{ fontSize: 12, fontWeight: 600, color: '#90caf9' }}>{l.adminUsername}</span>
              <span style={{ fontSize: 10, opacity: 0.4 }}>{fmtDate(l.createdAt)}</span>
            </div>
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              <span style={logBadge(l.action)}>{l.action}</span>
              {l.details && <span style={{ fontSize: 11, opacity: 0.6, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{l.details}</span>}
            </div>
          </div>
        ))}
      </div>
      <Pager page={page} totalPages={data?.totalPages ?? 1} onChange={load} />
    </SectionShell>
  );
}

// ── Shared reusable components ─────────────────────────────────────────────────

function SectionShell({
  icon, title, back, action, children,
}: {
  icon: string; title: string; back: () => void; action?: React.ReactNode; children: React.ReactNode;
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
      <div style={sectionHeader}>
        <button onClick={back} style={backBtn}>‹</button>
        <span style={{ fontSize: 20 }}>{icon}</span>
        <h2 style={sectionTitle}>{title}</h2>
        {action && <div style={{ marginLeft: 'auto' }}>{action}</div>}
      </div>
      <div style={{ flex: 1, overflowY: 'auto', padding: '12px 12px 24px' }}>
        {children}
      </div>
    </div>
  );
}

function PostList({ posts, onRemove, removingId, pager }: {
  posts: DpsPost[]; onRemove: (id: number) => void; removingId: number | null; pager: React.ReactNode;
}) {
  return (
    <div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {posts.map((post) => (
          <div key={post.id} style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 12, padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 10, opacity: post.isActive ? 1 : 0.4 }}>
            <div style={{ flex: 1 }}>
              <span style={{ fontWeight: 600, fontSize: 13 }}>
                {POST_TYPE_LABELS[post.postType] ?? post.postType}
                {!post.isActive && <span style={{ marginLeft: 6, opacity: 0.5, fontSize: 10 }}>деакт.</span>}
              </span>
              <div style={{ fontSize: 11, opacity: 0.5, marginTop: 3 }}>
                {post.addedByUsername ? `@${post.addedByUsername} · ` : ''}
                ⚡{post.confidence} · {fmtDate(post.createdAt)}
              </div>
            </div>
            {post.isActive && (
              <button style={dangerBtn('#ef9a9a')} onClick={() => onRemove(post.id)} disabled={removingId === post.id}>
                Убрать
              </button>
            )}
          </div>
        ))}
      </div>
      {pager}
    </div>
  );
}

function ActionBtn({ color, label, disabled, onClick }: {
  color: string; label: string; disabled?: boolean; onClick: () => void;
}) {
  return (
    <button disabled={disabled} onClick={onClick}
      style={{ padding: '8px 14px', borderRadius: 8, border: `1px solid ${color}55`,
        background: color + '22', color, fontSize: 12, fontWeight: 600, cursor: 'pointer',
        opacity: disabled ? 0.4 : 1 }}>
      {label}
    </button>
  );
}

function Pager({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 16, padding: '14px 0' }}>
      <button onClick={() => onChange(page - 1)} disabled={page === 0} style={pagerBtn}>← Назад</button>
      <span style={{ fontSize: 13, opacity: 0.5 }}>{page + 1} / {totalPages}</span>
      <button onClick={() => onChange(page + 1)} disabled={page >= totalPages - 1} style={pagerBtn}>Вперёд →</button>
    </div>
  );
}

function Loader() { return <div style={{ textAlign: 'center', padding: 40, opacity: 0.45 }}>Загрузка…</div>; }
function Empty({ text }: { text: string }) { return <div style={{ textAlign: 'center', padding: 40, opacity: 0.45 }}>{text}</div>; }

function fmtDate(iso: string) {
  const d = new Date(iso);
  return d.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' }) + ' ' +
    d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}

// ── Styles ────────────────────────────────────────────────────────────────────

const page: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden',
  background: 'var(--tg-theme-bg-color, #1c1c1e)', color: 'var(--tg-theme-text-color, #fff)',
};

const sectionHeader: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 8,
  padding: '14px 12px 10px',
  borderBottom: '1px solid rgba(255,255,255,0.08)',
  flexShrink: 0,
};

const sectionTitle: React.CSSProperties = { margin: 0, fontSize: 17, fontWeight: 700 };

const backBtn: React.CSSProperties = {
  background: 'none', border: 'none', color: 'var(--tg-theme-button-color, #2196F3)',
  fontSize: 28, cursor: 'pointer', padding: '0 4px', lineHeight: 1, fontWeight: 300,
};

const headerActionBtn: React.CSSProperties = {
  background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.15)',
  color: '#fff', borderRadius: 8, padding: '6px 12px', fontSize: 13, cursor: 'pointer',
};

// Home screen tiles
const tileGrid: React.CSSProperties = {
  display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10,
};

const tile = (color: string): React.CSSProperties => ({
  display: 'flex', flexDirection: 'column', alignItems: 'flex-start',
  gap: 8, padding: '16px 14px',
  background: color + '22',
  border: `1px solid ${color}44`,
  borderRadius: 14, cursor: 'pointer', textAlign: 'left',
  color: 'var(--tg-theme-text-color, #fff)',
});

const tileLabel: React.CSSProperties = { fontSize: 14, fontWeight: 700, lineHeight: 1 };

// Cards
const statGrid: React.CSSProperties = { display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginBottom: 12 };

const statCard = (color: string): React.CSSProperties => ({
  display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4,
  background: color + '18', border: `1px solid ${color}33`,
  borderRadius: 12, padding: '14px 8px',
});

const chartCard: React.CSSProperties = {
  background: 'rgba(255,255,255,0.04)', borderRadius: 12, padding: '14px',
};

const formCard: React.CSSProperties = {
  background: 'rgba(255,255,255,0.04)', borderRadius: 12, padding: '14px',
};

const settingCard: React.CSSProperties = {
  background: 'rgba(255,255,255,0.05)', borderRadius: 12, padding: '12px 14px',
  display: 'flex', flexDirection: 'column',
};

const promoCard: React.CSSProperties = {
  background: 'rgba(255,255,255,0.05)', borderRadius: 12, padding: '12px 14px',
  display: 'flex', alignItems: 'center', gap: 10,
};

const logCard: React.CSSProperties = {
  background: 'rgba(255,255,255,0.04)', borderRadius: 10, padding: '10px 12px',
};

const userCard = (banned: boolean): React.CSSProperties => ({
  background: banned ? 'rgba(239,83,80,0.08)' : 'rgba(255,255,255,0.05)',
  border: `1px solid ${banned ? 'rgba(239,83,80,0.2)' : 'rgba(255,255,255,0.08)'}`,
  borderRadius: 12, padding: '12px 14px',
});

// Menu rows (for ContentMenu, SystemMenu)
const menuRow: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 14,
  background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.08)',
  borderRadius: 12, padding: '14px',
  cursor: 'pointer', color: 'var(--tg-theme-text-color, #fff)',
  width: '100%',
};

// Controls
const inputStyle: React.CSSProperties = {
  padding: '10px 12px', borderRadius: 8,
  border: '1px solid rgba(255,255,255,0.15)',
  background: 'rgba(255,255,255,0.07)', color: '#fff', fontSize: 13,
  width: '100%', boxSizing: 'border-box',
};

const textareaStyle: React.CSSProperties = {
  padding: '10px 12px', borderRadius: 8,
  border: '1px solid rgba(255,255,255,0.15)',
  background: 'rgba(255,255,255,0.07)', color: '#fff', fontSize: 13,
  width: '100%', boxSizing: 'border-box',
  resize: 'vertical', fontFamily: 'inherit', display: 'block',
};

const primaryBtn: React.CSSProperties = {
  padding: '10px 16px', borderRadius: 8, border: 'none',
  background: 'var(--tg-theme-button-color, #2196F3)', color: '#fff',
  fontSize: 13, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap',
};

// ── Support tickets ───────────────────────────────────────────────────────────

const TICKET_STATUS_LABEL: Record<string, { text: string; color: string }> = {
  OPEN:        { text: 'Открыт',   color: '#42a5f5' },
  IN_PROGRESS: { text: 'В работе', color: '#ffd54f' },
  CLOSED:      { text: 'Закрыт',   color: '#66bb6a' },
};

function TicketsSection({ back }: { back: () => void }) {
  const [tickets, setTickets] = useState<PagedResponse<Ticket> | null>(null);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [replyText, setReplyText] = useState<Record<number, string>>({});
  const [replyStatus, setReplyStatus] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);

  const load = useCallback((p: number, sf: string) => {
    setLoading(true);
    adminGetTickets(p, 15, sf || undefined)
      .then(setTickets)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(page, statusFilter); }, [page, statusFilter, load]);

  const handleReply = async (t: Ticket) => {
    const reply = replyText[t.id]?.trim();
    if (!reply) return;
    const updated: Ticket = await adminReplyTicket(t.id, reply, replyStatus[t.id] || undefined);
    setTickets((prev) => prev ? {
      ...prev,
      content: prev.content.map((x) => x.id === t.id ? updated : x),
    } : prev);
    setReplyText((r) => { const n = { ...r }; delete n[t.id]; return n; });
    setExpandedId(null);
  };

  const handleClose = async (t: Ticket) => {
    const updated: Ticket = await adminCloseTicket(t.id);
    setTickets((prev) => prev ? {
      ...prev,
      content: prev.content.map((x) => x.id === t.id ? updated : x),
    } : prev);
  };

  const filterBtns: Array<{ val: string; label: string }> = [
    { val: '', label: 'Все' },
    { val: 'OPEN', label: 'Открытые' },
    { val: 'IN_PROGRESS', label: 'В работе' },
    { val: 'CLOSED', label: 'Закрытые' },
  ];

  return (
    <SectionShell icon="🎧" title="Тикеты поддержки" back={back}>
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 12 }}>
        {filterBtns.map((b) => (
          <button key={b.val} onClick={() => { setStatusFilter(b.val); setPage(0); }}
            style={segBtn(statusFilter === b.val)}>
            {b.label}
          </button>
        ))}
      </div>

      {loading ? <Loader /> : !tickets || tickets.content.length === 0 ? (
        <p style={{ opacity: 0.4, fontSize: 13 }}>Нет тикетов</p>
      ) : (
        <>
          {tickets.content.map((t) => {
            const s = TICKET_STATUS_LABEL[t.status] ?? { text: t.status, color: '#fff' };
            const expanded = expandedId === t.id;
            return (
              <div key={t.id} style={{ background: 'rgba(255,255,255,0.05)', borderRadius: 10, marginBottom: 8, overflow: 'hidden' }}>
                <div
                  onClick={() => setExpandedId(expanded ? null : t.id)}
                  style={{ padding: '10px 12px', cursor: 'pointer', display: 'flex', gap: 8, alignItems: 'flex-start' }}
                >
                  <span style={{ fontSize: 11, color: s.color, background: s.color + '22',
                    padding: '2px 7px', borderRadius: 10, whiteSpace: 'nowrap', flexShrink: 0, marginTop: 1 }}>
                    {s.text}
                  </span>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 2 }}>
                      #{t.id} {t.subject}
                    </div>
                    <div style={{ fontSize: 11, opacity: 0.45 }}>
                      {t.username ? '@' + t.username : t.firstName ?? '?'} · {new Date(t.createdAt).toLocaleDateString('ru')}
                    </div>
                  </div>
                  <span style={{ fontSize: 11, opacity: 0.3 }}>{expanded ? '▲' : '▼'}</span>
                </div>

                {expanded && (
                  <div style={{ padding: '0 12px 12px', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
                    <p style={{ fontSize: 12, opacity: 0.45, margin: '10px 0 3px' }}>Сообщение:</p>
                    <p style={{ fontSize: 13, margin: '0 0 10px', lineHeight: 1.5 }}>{t.message}</p>

                    {t.adminReply && (
                      <div style={{ padding: '8px 10px', borderRadius: 8,
                        background: 'rgba(100,181,246,0.1)', borderLeft: '2px solid #42a5f5', marginBottom: 10 }}>
                        <p style={{ fontSize: 11, opacity: 0.5, margin: '0 0 3px' }}>
                          Ответ {t.repliedByUsername ? '@' + t.repliedByUsername : ''}:
                        </p>
                        <p style={{ fontSize: 13, margin: 0 }}>{t.adminReply}</p>
                      </div>
                    )}

                    {t.status !== 'CLOSED' && (
                      <>
                        <textarea
                          placeholder="Ответ пользователю..."
                          value={replyText[t.id] ?? ''}
                          onChange={(e) => setReplyText((r) => ({ ...r, [t.id]: e.target.value }))}
                          rows={3}
                          style={{ width: '100%', boxSizing: 'border-box', padding: '8px 10px',
                            borderRadius: 8, border: '1px solid rgba(255,255,255,0.12)',
                            background: 'rgba(255,255,255,0.07)', color: '#fff',
                            fontSize: 13, resize: 'vertical', fontFamily: 'inherit', marginBottom: 8 }}
                        />
                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                          <select
                            value={replyStatus[t.id] ?? ''}
                            onChange={(e) => setReplyStatus((r) => ({ ...r, [t.id]: e.target.value }))}
                            style={{ padding: '6px 10px', borderRadius: 7, border: '1px solid rgba(255,255,255,0.12)',
                              background: 'rgba(255,255,255,0.07)', color: '#fff', fontSize: 12 }}
                          >
                            <option value="">→ В работе</option>
                            <option value="OPEN">→ Открыт</option>
                            <option value="CLOSED">→ Закрыть</option>
                          </select>
                          <button
                            onClick={() => handleReply(t)}
                            disabled={!(replyText[t.id]?.trim())}
                            style={{ ...ActionBtnStyle(false), flex: 1 }}
                          >
                            Отправить ответ
                          </button>
                          <button
                            onClick={() => handleClose(t)}
                            style={{ ...ActionBtnStyle(true), whiteSpace: 'nowrap' }}
                          >
                            ✕ Закрыть тикет
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                )}
              </div>
            );
          })}

          {(tickets.totalPages > 1) && (
            <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 8 }}>
              <button disabled={page === 0} onClick={() => setPage(page - 1)} style={pagerBtn}>‹</button>
              <span style={{ fontSize: 12, opacity: 0.5, alignSelf: 'center' }}>
                {page + 1} / {tickets.totalPages}
              </span>
              <button disabled={page >= tickets.totalPages - 1} onClick={() => setPage(page + 1)} style={pagerBtn}>›</button>
            </div>
          )}
        </>
      )}
    </SectionShell>
  );
}

const ActionBtnStyle = (danger: boolean): React.CSSProperties => ({
  padding: '7px 14px', borderRadius: 7, border: 'none', cursor: 'pointer', fontSize: 12, fontWeight: 600,
  background: danger ? 'rgba(239,83,80,0.2)' : 'var(--tg-theme-button-color, #2196F3)',
  color: danger ? '#ef5350' : '#fff',
});

const dangerBtn = (color: string): React.CSSProperties => ({
  padding: '6px 12px', borderRadius: 6, border: `1px solid ${color}55`,
  background: color + '22', color, fontSize: 12, fontWeight: 600, cursor: 'pointer',
});

const iconBtn: React.CSSProperties = {
  padding: '10px 14px', borderRadius: 8, border: 'none',
  background: 'rgba(255,255,255,0.1)', color: '#fff', cursor: 'pointer', fontSize: 14, flexShrink: 0,
};

const segBtn = (active: boolean): React.CSSProperties => ({
  flex: 1, padding: '8px 6px', borderRadius: 8, border: 'none', cursor: 'pointer', fontSize: 12,
  background: active ? 'var(--tg-theme-button-color, #2196F3)' : 'rgba(255,255,255,0.08)',
  color: active ? '#fff' : 'rgba(255,255,255,0.6)', fontWeight: active ? 600 : 400,
});

const periodBtn = (active: boolean): React.CSSProperties => ({
  padding: '4px 10px', borderRadius: 6, border: 'none', cursor: 'pointer', fontSize: 11,
  background: active ? 'var(--tg-theme-button-color, #2196F3)' : 'rgba(255,255,255,0.08)',
  color: active ? '#fff' : 'rgba(255,255,255,0.5)', fontWeight: active ? 600 : 400,
});

const pagerBtn: React.CSSProperties = {
  background: 'rgba(255,255,255,0.08)', color: '#fff', border: 'none',
  borderRadius: 8, padding: '8px 16px', cursor: 'pointer', fontSize: 13,
};

const cardLabel: React.CSSProperties = { margin: '0 0 10px', fontSize: 11, opacity: 0.5, textTransform: 'uppercase', letterSpacing: 0.5 };

const actionGroup: React.CSSProperties = { marginBottom: 10 };
const actionGroupLabel: React.CSSProperties = { margin: '0 0 6px', fontSize: 11, opacity: 0.45, textTransform: 'uppercase', letterSpacing: 0.3 };

const badge = (bg: string, color = '#fff'): React.CSSProperties => ({
  marginLeft: 6, borderRadius: 4, padding: '1px 5px', fontSize: 9, fontWeight: 700,
  background: bg, color, verticalAlign: 'middle',
});

const promoTypeBadge = (isVip: boolean): React.CSSProperties => ({
  borderRadius: 5, padding: '2px 7px', fontSize: 11, fontWeight: 700,
  background: isVip ? 'rgba(129,199,132,0.2)' : 'rgba(255,214,0,0.2)',
  color: isVip ? '#81c784' : '#ffd600',
  border: `1px solid ${isVip ? 'rgba(129,199,132,0.3)' : 'rgba(255,214,0,0.3)'}`,
});

const logBadge = (action: string): React.CSSProperties => ({
  borderRadius: 4, padding: '2px 6px', fontSize: 10, fontWeight: 600, whiteSpace: 'nowrap',
  background: action.includes('ban') ? 'rgba(239,83,80,0.2)' :
    action.includes('broadcast') ? 'rgba(255,213,0,0.2)' :
    action.includes('role') ? 'rgba(124,77,255,0.2)' : 'rgba(33,150,243,0.2)',
  color: '#fff',
});
