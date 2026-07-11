/**
 * Root component of the DPS Tracker Mini App.
 *
 * <p>Handles Telegram WebApp authentication, loads the user profile,
 * renders the bottom navigation bar, and mounts the current page.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect, useRef, useState } from 'react';
import { initWebApp, getInitData, getThemeParams, getStartParam, hapticSelect } from './telegram/webapp';
import { authTelegram, saveToken, getToken, clearToken, fetchProfile, fetchPostById } from './api/client';
import { useAppStore } from './store';
import { ToastContainer } from './components/Toast';
import MapPage from './pages/MapPage';
import HistoryPage from './pages/HistoryPage';
import NearbyPage from './pages/NearbyPage';
import ProfilePage from './pages/ProfilePage';
import AdminPage from './pages/AdminPage';
import RadarPage from './pages/RadarPage';
import type { Page } from './types';

function extractErrorMessage(err: unknown, fallback: string): string {
  if (err && typeof err === 'object') {
    const axiosErr = err as { response?: { data?: { message?: string } }; message?: string };
    if (axiosErr.response?.data?.message) return axiosErr.response.data.message;
    if (axiosErr.message) return axiosErr.message;
  }
  return fallback;
}

export default function App() {
  const [authLoading, setAuthLoading] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);
  const { setToken, setProfile, currentPage, setCurrentPage, profile, setSelectedPost } = useAppStore();
  // Guard against React StrictMode double-invoking useEffect, which would send two concurrent
  // authTelegram() requests — each generating a different session token that overwrites the other.
  const authStarted = useRef(false);

  useEffect(() => {
    initWebApp();
    const theme = getThemeParams();
    const root = document.documentElement;
    if (theme.bg_color) root.style.setProperty('--tg-theme-bg-color', theme.bg_color);
    if (theme.text_color) root.style.setProperty('--tg-theme-text-color', theme.text_color);
    if (theme.button_color) root.style.setProperty('--tg-theme-button-color', theme.button_color);
    if (theme.button_text_color) root.style.setProperty('--tg-theme-button-text-color', theme.button_text_color);
    if (theme.secondary_bg_color) root.style.setProperty('--tg-theme-secondary-bg-color', theme.secondary_bg_color);
  }, []);

  useEffect(() => {
    if (authStarted.current) return;
    authStarted.current = true;

    const authenticate = async () => {
      try {
        const existingToken = getToken();
        if (existingToken) {
          try {
            const profile = await fetchProfile();
            setProfile(profile);
            setToken(existingToken);
            setAuthLoading(false);
            return;
          } catch {
            // Token is stale — clear it before re-authenticating
            clearToken();
          }
        }

        const initData = getInitData();
        if (!initData) {
          setAuthLoading(false);
          return;
        }

        const response = await authTelegram(initData);
        saveToken(response.token);
        setToken(response.token);

        // Retry once — the upsert transaction may not have committed yet
        let profile;
        try {
          profile = await fetchProfile();
        } catch {
          await new Promise((r) => setTimeout(r, 400));
          profile = await fetchProfile();
        }
        setProfile(profile);
      } catch (err: unknown) {
        const msg = extractErrorMessage(err, 'Ошибка авторизации');
        setAuthError(msg);
      } finally {
        setAuthLoading(false);
      }
    };

    authenticate();
  }, [setToken, setProfile]);

  // Resolve post deep-link (t.me/bot?startapp=post_123) after successful authentication
  useEffect(() => {
    if (!profile) return;
    const startParam = getStartParam();
    const postMatch = startParam?.match(/^post_(\d+)$/);
    if (!postMatch) return;
    fetchPostById(parseInt(postMatch[1]))
      .then(setSelectedPost)
      .catch(() => {});
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profile?.id]);

  // Banned users may only access the profile page (support section).
  // Must be before any early returns to satisfy Rules of Hooks.
  const isBanned = profile?.isBanned === true;
  useEffect(() => {
    if (isBanned && currentPage !== 'profile') {
      setCurrentPage('profile');
    }
  }, [isBanned, currentPage, setCurrentPage]);

  if (authLoading) {
    return (
      <div style={fullScreen}>
        {/* Radar rings */}
        <div style={radarWrap}>
          {[0, 0.4, 0.8].map((delay) => (
            <div key={delay} style={{ ...radarRing, animationDelay: `${delay}s` }} />
          ))}
          <div style={radarIcon}>🗺</div>
        </div>
        <div style={loaderTitle}>DPS Tracker</div>
        {/* Animated dots */}
        <div style={{ display: 'flex', gap: 5, marginTop: 10 }}>
          {[0, 0.18, 0.36].map((d) => (
            <div key={d} style={{ ...dot, animationDelay: `${d}s` }} />
          ))}
        </div>
      </div>
    );
  }

  if (authError) {
    return (
      <div style={fullScreen}>
        <p style={{ color: '#f44336' }}>⚠️ {authError}</p>
        <button onClick={() => window.location.reload()} style={retryBtn}>
          Попробовать снова
        </button>
      </div>
    );
  }

  const isAdmin = profile?.role === 'ADMIN';

  const pages: Record<Page, React.ReactNode> = {
    map: <MapPage />,
    history: <HistoryPage />,
    nearby: <NearbyPage />,
    profile: <ProfilePage />,
    admin: <AdminPage />,
    radar: <RadarPage />,
  };

  const navItems: { page: Page; icon: string; label: string }[] = [
    { page: 'map', icon: '🗺', label: 'Карта' },
    { page: 'nearby', icon: '📡', label: 'Рядом' },
    { page: 'radar', icon: '🧭', label: 'Радар' },
    { page: 'history', icon: '📋', label: 'История' },
    { page: 'profile', icon: '👤', label: 'Профиль' },
    ...(isAdmin ? [{ page: 'admin' as Page, icon: '🛡', label: 'Адм' }] : []),
  ];

  return (
    <div style={appContainer}>
      <div style={content}>
        <div key={currentPage} className="page-enter" style={{ height: '100%', overflow: 'hidden' }}>
          {pages[currentPage]}
        </div>
      </div>
      <nav style={navBar}>
        {navItems.map(({ page, icon, label }) => {
          const disabled = isBanned && page !== 'profile';
          return (
            <button
              key={page}
              onClick={() => { if (!disabled) { hapticSelect(); setCurrentPage(page); } }}
              style={navBtn(currentPage === page, disabled)}
              aria-disabled={disabled}
            >
              <span style={{ fontSize: 22 }}>{icon}</span>
              <span style={{ fontSize: 10 }}>{label}</span>
            </button>
          );
        })}
      </nav>
      <ToastContainer />
    </div>
  );
}

const appContainer: React.CSSProperties = {
  display: 'flex', flexDirection: 'column',
  height: '100dvh',
  background: 'var(--tg-theme-bg-color, #1c1c1e)',
  color: 'var(--tg-theme-text-color, #fff)',
  overflow: 'hidden',
};

const content: React.CSSProperties = { flex: 1, overflow: 'hidden', position: 'relative' };

const navBar: React.CSSProperties = {
  display: 'flex',
  background: 'var(--tg-theme-secondary-bg-color, #2c2c2e)',
  borderTop: '1px solid rgba(255,255,255,0.1)',
  paddingBottom: 'env(safe-area-inset-bottom, 0)',
};

const navBtn = (active: boolean, disabled = false): React.CSSProperties => ({
  flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
  justifyContent: 'center', padding: '8px 0', background: 'none', border: 'none',
  color: disabled
    ? 'rgba(255,255,255,0.2)'
    : active
      ? 'var(--tg-theme-button-color, #2196F3)'
      : 'rgba(255,255,255,0.5)',
  cursor: disabled ? 'default' : 'pointer',
  gap: 2, transition: 'color 0.2s',
  opacity: disabled ? 0.35 : 1,
});

const fullScreen: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', alignItems: 'center',
  justifyContent: 'center', height: '100dvh',
  background: '#1c1c1e', color: '#fff', gap: 4,
};

const radarWrap: React.CSSProperties = {
  position: 'relative', width: 100, height: 100,
  display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16,
};

const radarRing: React.CSSProperties = {
  position: 'absolute', width: 80, height: 80, borderRadius: '50%',
  border: '2px solid rgba(33,150,243,0.6)',
  animation: 'radarPulse 2s ease-out infinite',
};

const radarIcon: React.CSSProperties = {
  fontSize: 36, position: 'relative', zIndex: 1,
};

const loaderTitle: React.CSSProperties = { fontSize: 22, fontWeight: 700, letterSpacing: 0.5 };

const dot: React.CSSProperties = {
  width: 7, height: 7, borderRadius: '50%',
  background: 'rgba(33,150,243,0.8)',
  animation: 'dotsWave 1.2s ease-in-out infinite',
};

const retryBtn: React.CSSProperties = {
  background: '#2196F3', color: '#fff', border: 'none',
  borderRadius: 10, padding: '10px 24px', cursor: 'pointer', fontSize: 14,
};
