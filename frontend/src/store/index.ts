/**
 * Global application state managed by Zustand.
 *
 * <p>Centralises authentication tokens, user profile, DPS posts, map state,
 * UI state (selected post, current page, live-tracking toggle), and the
 * toast notification queue. All fields that need to survive a page refresh
 * are synced to {@code localStorage}.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { create } from 'zustand';
import type { DpsPost, UserProfile, Page } from '../types';

export interface Toast {
  id: string;
  message: string;
  type: 'success' | 'error' | 'info';
}

interface AppState {
  // Auth
  token: string | null;
  setToken: (token: string | null) => void;

  // User
  profile: UserProfile | null;
  setProfile: (profile: UserProfile | null) => void;

  // Posts
  posts: DpsPost[];
  setPosts: (posts: DpsPost[]) => void;
  addPost: (post: DpsPost) => void;
  updatePost: (post: DpsPost) => void;

  // Map
  userLat: number | null;
  userLon: number | null;
  setUserLocation: (lat: number, lon: number) => void;

  // UI
  selectedPost: DpsPost | null;
  setSelectedPost: (post: DpsPost | null) => void;
  currentPage: Page;
  setCurrentPage: (page: Page) => void;
  isLiveTracking: boolean;
  setLiveTracking: (active: boolean) => void;

  locationEnabled: boolean;
  setLocationEnabled: (enabled: boolean) => void;

  /** Default patrol car speed (km/h). Persisted in localStorage. */
  patrolSpeedKmh: number;
  setPatrolSpeedKmh: (speed: number) => void;

  // Toasts
  toasts: Toast[];
  addToast: (message: string, type?: Toast['type']) => void;
  removeToast: (id: string) => void;
}

export const useAppStore = create<AppState>((set) => ({
  token: localStorage.getItem('dps_session_token'),
  setToken: (token) => {
    if (token) localStorage.setItem('dps_session_token', token);
    else localStorage.removeItem('dps_session_token');
    set({ token });
  },

  profile: null,
  setProfile: (profile) => set({ profile }),

  posts: [],
  setPosts: (posts) => set({ posts }),
  addPost: (post) => set((state) => ({ posts: [post, ...state.posts] })),
  updatePost: (updated) =>
    set((state) => ({
      posts: state.posts.map((p) => (p.id === updated.id ? updated : p)),
    })),

  userLat: null,
  userLon: null,
  setUserLocation: (lat, lon) => set({ userLat: lat, userLon: lon }),

  selectedPost: null,
  setSelectedPost: (post) => set({ selectedPost: post }),
  currentPage: 'map',
  setCurrentPage: (page) => set({ currentPage: page }),
  isLiveTracking: false,
  setLiveTracking: (active) => set({ isLiveTracking: active }),

  locationEnabled: localStorage.getItem('dps_location_enabled') !== 'false',
  setLocationEnabled: (enabled) => {
    localStorage.setItem('dps_location_enabled', String(enabled));
    set({ locationEnabled: enabled });
  },

  patrolSpeedKmh: Number(localStorage.getItem('dps_patrol_speed') ?? 60),
  setPatrolSpeedKmh: (speed) => {
    localStorage.setItem('dps_patrol_speed', String(speed));
    set({ patrolSpeedKmh: speed });
  },

  toasts: [],
  addToast: (message, type = 'info') => {
    const id = `${Date.now()}-${Math.random()}`;
    set((s) => ({ toasts: [...s.toasts, { id, message, type }] }));
  },
  removeToast: (id) => set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}));
