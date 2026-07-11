/**
 * Shared TypeScript type definitions for DPS Tracker Frontend.
 *
 * <p>Contains domain models (posts, users, tickets, promo codes), API response
 * shapes, and utility types used across the entire Mini App.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

export type PostType = 'DPS_POST' | 'PATROL_CAR' | 'CAMERA' | 'AMBUSH' | 'HIDDEN_POST';

export const POST_TYPE_LABELS: Record<PostType, string> = {
  DPS_POST: '🚔 Пост ДПС',
  PATROL_CAR: '🚗 Патрульная машина',
  CAMERA: '📷 Камера',
  AMBUSH: '👁 Засада',
  HIDDEN_POST: '🕵️ Скрытый пост',
};

export interface DpsPost {
  id: number;
  postType: PostType;
  lat: number;
  lon: number;
  description?: string;
  addedByUsername?: string;
  isActive: boolean;
  confidence: number;
  confirmedCount: number;
  patrolSpeedKmh?: number;
  expiresAt?: string;
  distanceMeters?: number;
  createdAt: string;
  updatedAt: string;
  commentsCount?: number;
}

export interface Comment {
  id: number;
  postId: number;
  username: string;
  text: string;
  createdAt: string;
}

export type UserRole = 'USER' | 'MODERATOR' | 'ADMIN';

export interface UserProfile {
  id: number;
  tgId: number;
  username?: string;
  firstName?: string;
  role: UserRole;
  reputationScore: number;
  notifyPostTypes?: string;
  isSubscribed: boolean;
  subscriptionExpiresAt?: string;
  isBanned: boolean;
  notifyRadiusKm: number;
  liveTracking: boolean;
  promoDiscountPercent?: number;
  createdAt: string;
}

export interface AppSetting {
  key: string;
  value: string;
  description?: string;
  updatedAt: string;
}

export interface AdminLogEntry {
  id: number;
  adminUsername: string;
  action: string;
  targetType?: string;
  targetId?: number;
  details?: string;
  createdAt: string;
}

export interface ActivityDataPoint {
  date: string;
  posts: number;
  users: number;
  activeUsers: number;
}

export interface AuthResponse {
  token: string;
  tgId: number;
  username?: string;
  firstName?: string;
  isSubscribed: boolean;
}

export interface NearbyResponse {
  maxRadiusKm: number;
  premium: boolean;
  posts: DpsPost[];
}

export interface AdminStats {
  totalUsers: number;
  premiumUsers: number;
  bannedUsers: number;
  moderatorCount: number;
  newUsersToday: number;
  newUsersThisWeek: number;
  newUsersThisMonth: number;
  totalPosts: number;
  activePosts: number;
}

export interface CameraImportResult {
  imported: number;
  skipped: number;
  total: number;
}

export interface AdminUser {
  id: number;
  tgId: number;
  username?: string;
  firstName?: string;
  role: UserRole;
  moderatorRegion?: string;
  reputationScore: number;
  isSubscribed: boolean;
  subscriptionExpiresAt?: string;
  isBanned: boolean;
  liveTracking: boolean;
  lastSeen?: string;
  createdAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export type Page = 'map' | 'history' | 'nearby' | 'profile' | 'admin' | 'radar';

export type PromoType = 'VIP_DAYS' | 'DISCOUNT_PERCENT';

export interface PromoCode {
  id: number;
  code: string;
  type: PromoType;
  value: number;
  maxUses?: number;
  usedCount: number;
  expiresAt?: string;
  isActive: boolean;
  createdAt: string;
}

export interface ApplyPromoResponse {
  message: string;
  type: PromoType;
  value: number;
}

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'CLOSED';

export interface Ticket {
  id: number;
  userId: number;
  username?: string;
  firstName?: string;
  subject: string;
  message: string;
  status: TicketStatus;
  adminReply?: string;
  repliedByUsername?: string;
  createdAt: string;
  updatedAt: string;
}
