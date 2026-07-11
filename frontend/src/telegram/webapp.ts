/**
 * Typed wrapper around the Telegram Mini App WebApp API.
 *
 * <p>Provides safe, typed access to {@code window.Telegram.WebApp} and its
 * methods. All exported functions are no-ops when the app runs outside Telegram.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

declare global {
  interface Window {
    Telegram?: {
      WebApp: TelegramWebApp;
    };
  }
}

export interface TelegramWebApp {
  initData: string;
  initDataUnsafe: {
    user?: {
      id: number;
      first_name: string;
      last_name?: string;
      username?: string;
      language_code?: string;
    };
    start_param?: string;
    auth_date: number;
    hash: string;
  };
  version: string;
  platform: string;
  colorScheme: 'light' | 'dark';
  themeParams: {
    bg_color?: string;
    text_color?: string;
    hint_color?: string;
    link_color?: string;
    button_color?: string;
    button_text_color?: string;
    secondary_bg_color?: string;
  };
  isExpanded: boolean;
  viewportHeight: number;
  viewportStableHeight: number;
  ready(): void;
  expand(): void;
  close(): void;
  showAlert(message: string, callback?: () => void): void;
  showConfirm(message: string, callback: (confirmed: boolean) => void): void;
  openLink(url: string): void;
  openTelegramLink(url: string): void;
  addToHomeScreen(): void;
  hapticFeedback: {
    impactOccurred(style: 'light' | 'medium' | 'heavy' | 'rigid' | 'soft'): void;
    notificationOccurred(type: 'error' | 'success' | 'warning'): void;
    selectionChanged(): void;
  };
}

/** Live {@link TelegramWebApp} instance, or {@code null} when running outside Telegram. */
export const tgWebApp: TelegramWebApp | null =
  window.Telegram?.WebApp ?? null;

/** Initialise the Mini App: call {@code ready()} and {@code expand()} to go full-screen. */
export function initWebApp(): void {
  if (tgWebApp) {
    tgWebApp.ready();
    tgWebApp.expand();
  }
}

/** Return the raw {@code initData} string from Telegram WebApp, or empty string outside Telegram. */
export function getInitData(): string {
  return tgWebApp?.initData ?? '';
}

/** Return the active colour scheme reported by Telegram, defaulting to {@code 'dark'}. */
export function getColorScheme(): 'light' | 'dark' {
  return tgWebApp?.colorScheme ?? 'dark';
}

/** Return the Telegram theme parameters (colours) for the current user. */
export function getThemeParams() {
  return tgWebApp?.themeParams ?? {};
}

/** Return the Telegram user object from {@code initDataUnsafe}, or {@code null} outside Telegram. */
export function getTelegramUser() {
  return tgWebApp?.initDataUnsafe?.user ?? null;
}

/** Return the {@code start_param} from a deep-link (e.g. "post_123"), or {@code null} if absent. */
export function getStartParam(): string | null {
  return tgWebApp?.initDataUnsafe?.start_param ?? null;
}

/** Trigger a haptic impact feedback with the given style. */
export function hapticImpact(style: 'light' | 'medium' | 'heavy' = 'medium'): void {
  tgWebApp?.hapticFeedback?.impactOccurred(style);
}

/** Trigger a haptic success notification. */
export function hapticSuccess(): void {
  tgWebApp?.hapticFeedback?.notificationOccurred('success');
}

/** Trigger a haptic error notification. */
export function hapticError(): void {
  tgWebApp?.hapticFeedback?.notificationOccurred('error');
}

/** Trigger a haptic selection-changed feedback. */
export function hapticSelect(): void {
  tgWebApp?.hapticFeedback?.selectionChanged();
}

/** Prompt the user to add the Mini App to their device's home screen. */
export function addToHomeScreen(): void {
  tgWebApp?.addToHomeScreen();
}
