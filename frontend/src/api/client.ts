/**
 * Axios HTTP client for the DPS Tracker API.
 *
 * <p>Automatically attaches the Bearer session token from {@code localStorage}
 * to every request. On a 401 response against {@code /users/me} the token is
 * cleared; {@link App} then handles re-authentication without a page reload.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL ?? '/api';
const TOKEN_KEY = 'dps_session_token';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Attach the session token to every outgoing request
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Clear the token on 401/403 for /users/me — no page reload to avoid an
// auth-loop when multiple concurrent requests fire (React StrictMode).
// App.tsx handles re-authentication independently.
apiClient.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error.response?.status;
    const url: string = error.config?.url ?? '';
    if ((status === 401 || status === 403) && url.includes('/users/me')) {
      localStorage.removeItem(TOKEN_KEY);
    }
    return Promise.reject(error);
  }
);

/** Persist the session token to localStorage. */
export function saveToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

/** Retrieve the current session token from localStorage. */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

/** Remove the session token from localStorage. */
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

// ── API functions ─────────────────────────────────────────────────────────────

/** Authenticate with the backend using Telegram initData. */
export async function authTelegram(initData: string) {
  const res = await apiClient.post('/auth/telegram', { initData });
  return res.data;
}

/** Fetch DPS posts within a given radius (unauthenticated). */
export async function fetchNearbyPosts(lat: number, lon: number, radiusKm = 5) {
  const res = await apiClient.get('/posts', { params: { lat, lon, radiusKm } });
  return res.data;
}

/** Create a new DPS post on the map. */
export async function createPost(
  lat: number,
  lon: number,
  description?: string,
  postType: string = 'DPS_POST',
  patrolSpeedKmh?: number,
) {
  const res = await apiClient.post('/posts', { lat, lon, description, postType, patrolSpeedKmh });
  return res.data;
}

/** Delete a post by ID (owner only). */
export async function deletePost(postId: number) {
  await apiClient.delete(`/posts/${postId}`);
}

/** Vote on a DPS post (1 = confirm, -1 = fake). */
export async function votePost(postId: number, vote: 1 | -1) {
  const res = await apiClient.post(`/posts/${postId}/vote`, { vote });
  return res.data;
}

/** Fetch paginated comments for a post. */
export async function fetchComments(postId: number, page = 0) {
  const res = await apiClient.get(`/posts/${postId}/comments`, { params: { page } });
  return res.data;
}

/** Add a comment to a post. */
export async function addComment(postId: number, text: string) {
  const res = await apiClient.post(`/posts/${postId}/comments`, { text });
  return res.data;
}

/** Fetch the authenticated user's profile. */
export async function fetchProfile() {
  const res = await apiClient.get('/users/me');
  return res.data;
}

/** Update notification and display settings. */
export async function updateSettings(settings: { notifyRadiusKm?: number }) {
  const res = await apiClient.put('/users/me/settings', settings);
  return res.data;
}

/** Enable or disable WebSocket live-tracking. */
export async function toggleLiveTracking(enabled: boolean) {
  await apiClient.post('/users/me/live', { enabled });
}

/** Fetch posts created by the current user. */
export async function fetchMyPosts() {
  const res = await apiClient.get('/users/me/posts');
  return res.data;
}

/** Fetch posts near the user, respecting the subscription radius cap. */
export async function fetchNearbyAuthenticated(lat: number, lon: number) {
  const res = await apiClient.get('/posts/nearby', { params: { lat, lon } });
  return res.data;
}

/** Fetch a single post by ID (used for deep-link resolution). */
export async function fetchPostById(postId: number) {
  const res = await apiClient.get(`/posts/${postId}`);
  return res.data;
}

// ── Admin API ─────────────────────────────────────────────────────────────────

/** Fetch aggregate platform statistics (total users, posts, etc.). */
export async function adminFetchStats() {
  const res = await apiClient.get('/admin/stats');
  return res.data;
}

/** Fetch a paginated user list with optional search and role/status filters. */
export async function adminFetchUsers(
  page = 0,
  size = 20,
  search = '',
  role?: string,
  banned?: boolean,
  subscribed?: boolean,
) {
  const params: Record<string, unknown> = { page, size };
  if (search) params.search = search;
  if (role) params.role = role;
  if (banned !== undefined) params.banned = banned;
  if (subscribed !== undefined) params.subscribed = subscribed;
  const res = await apiClient.get('/admin/users', { params });
  return res.data;
}

/** Toggle the ban status of a user. */
export async function adminToggleBan(userId: number) {
  const res = await apiClient.put(`/admin/users/${userId}/ban`);
  return res.data;
}

/** Force-activate a Premium subscription for a user. */
export async function adminForceSubscribe(userId: number) {
  const res = await apiClient.put(`/admin/users/${userId}/subscribe`);
  return res.data;
}

/** Fetch a paginated list of all DPS posts. */
export async function adminFetchPosts(page = 0, size = 20) {
  const res = await apiClient.get('/admin/posts', { params: { page, size } });
  return res.data;
}

/** Deactivate (soft-delete) a DPS post by ID. */
export async function adminDeactivatePost(postId: number) {
  await apiClient.delete(`/admin/posts/${postId}`);
}

/** Fetch the moderation queue of posts awaiting review. */
export async function adminGetModerationQueue(page = 0, size = 20) {
  const res = await apiClient.get('/admin/posts/moderation', { params: { page, size } });
  return res.data;
}

/** Send a broadcast message to all users (or a targeted subset). */
export async function adminBroadcast(message: string, target: string) {
  const res = await apiClient.post('/admin/broadcast', { message, target });
  return res.data;
}

/** Fetch all application-level settings. */
export async function adminGetSettings() {
  const res = await apiClient.get('/admin/settings');
  return res.data;
}

/** Update a single application-level setting by key. */
export async function adminUpdateSetting(key: string, value: string) {
  const res = await apiClient.put(`/admin/settings/${key}`, { value });
  return res.data;
}

/** Fetch the paginated admin action log. */
export async function adminGetLogs(page = 0, size = 30) {
  const res = await apiClient.get('/admin/logs', { params: { page, size } });
  return res.data;
}

/** Change a user's role (USER / MODERATOR / ADMIN). */
export async function adminChangeRole(userId: number, role: string) {
  const res = await apiClient.put(`/admin/users/${userId}/role`, { role });
  return res.data;
}

/** Fetch daily post and user activity data for the last {@code days} days. */
export async function adminGetActivity(days = 14) {
  const res = await apiClient.get('/admin/stats/activity', { params: { days } });
  return res.data;
}

/** Update the current user's notification post-type filter. */
export async function updateNotifyPrefs(notifyPostTypes: string) {
  const res = await apiClient.put('/users/me/notify-prefs', { notifyPostTypes });
  return res.data;
}

/** Apply a promo code for the current user. */
export async function applyPromoCode(code: string) {
  const res = await apiClient.post('/promo/apply', { code });
  return res.data;
}

/** Grant a trial Premium subscription of the given number of days. */
export async function adminGrantTrial(userId: number, days: number) {
  const res = await apiClient.post(`/admin/users/${userId}/trial`, { days });
  return res.data;
}

/** Revoke a user's active Premium subscription. */
export async function adminRevokeSubscription(userId: number) {
  const res = await apiClient.put(`/admin/users/${userId}/revoke-subscription`);
  return res.data;
}

/** Assign the moderator role to a user for the given region. */
export async function adminAssignModerator(userId: number, region: string) {
  const res = await apiClient.post(`/admin/users/${userId}/moderator`, { region });
  return res.data;
}

/** Revoke the moderator role from a user. */
export async function adminRevokeModerator(userId: number) {
  const res = await apiClient.delete(`/admin/users/${userId}/moderator`);
  return res.data;
}

/** Fetch all promo codes. */
export async function adminGetPromos() {
  const res = await apiClient.get('/admin/promo');
  return res.data;
}

/** Create a new promo code. */
export async function adminCreatePromo(data: {
  code: string;
  type: string;
  value: number;
  maxUses?: number;
  expiresAt?: string;
}) {
  const res = await apiClient.post('/admin/promo', data);
  return res.data;
}

/** Deactivate a promo code without deleting it. */
export async function adminDeactivatePromo(id: number) {
  const res = await apiClient.put(`/admin/promo/${id}/deactivate`);
  return res.data;
}

/** Permanently delete a promo code. */
export async function adminDeletePromo(id: number) {
  await apiClient.delete(`/admin/promo/${id}`);
}

/** Import speed cameras from OpenStreetMap Overpass API for the given bounding box. */
export async function adminImportCameras(south: number, west: number, north: number, east: number) {
  const res = await apiClient.post('/admin/cameras/import', { south, west, north, east });
  return res.data;
}

// ── Support tickets ───────────────────────────────────────────────────────────

/** Create a new support ticket for the current user. */
export async function createTicket(subject: string, message: string) {
  const res = await apiClient.post('/tickets', { subject, message });
  return res.data;
}

/** Fetch all support tickets submitted by the current user. */
export async function fetchMyTickets() {
  const res = await apiClient.get('/tickets/my');
  return res.data;
}

/** Close a support ticket (user action). */
export async function closeMyTicket(ticketId: number) {
  const res = await apiClient.put(`/tickets/${ticketId}/close`);
  return res.data;
}

/** Fetch all support tickets (admin view), optionally filtered by status. */
export async function adminGetTickets(page = 0, size = 20, status?: string) {
  const params: Record<string, unknown> = { page, size };
  if (status) params.status = status;
  const res = await apiClient.get('/admin/tickets', { params });
  return res.data;
}

/** Post an admin reply to a support ticket, optionally changing its status. */
export async function adminReplyTicket(ticketId: number, reply: string, status?: string) {
  const res = await apiClient.post(`/admin/tickets/${ticketId}/reply`, { reply, status });
  return res.data;
}

/** Close a support ticket (admin action). */
export async function adminCloseTicket(ticketId: number) {
  const res = await apiClient.put(`/admin/tickets/${ticketId}/close`);
  return res.data;
}
