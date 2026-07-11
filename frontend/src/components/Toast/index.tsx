/**
 * Toast notification system for the DPS Tracker Mini App.
 *
 * <p>{@link ToastContainer} renders all active toasts from the global store.
 * Each {@link ToastItem} auto-dismisses after 3 seconds and can also be
 * dismissed by tapping. Three visual variants are supported:
 * {@code success}, {@code error}, and {@code info}.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useEffect } from 'react';
import { useAppStore } from '../../store';
import type { Toast } from '../../store';

/** Fixed-position container that stacks active toast notifications. */
export function ToastContainer() {
  const { toasts } = useAppStore();
  return (
    <div style={{
      position: 'fixed', top: 16, left: '50%', transform: 'translateX(-50%)',
      zIndex: 9999, display: 'flex', flexDirection: 'column', gap: 8,
      width: 'calc(100% - 32px)', maxWidth: 360, pointerEvents: 'none',
    }}>
      {toasts.map((t) => <ToastItem key={t.id} toast={t} />)}
    </div>
  );
}

/** Single toast card that auto-dismisses after 3 seconds. */
function ToastItem({ toast }: { toast: Toast }) {
  const removeToast = useAppStore((s) => s.removeToast);

  useEffect(() => {
    const timer = setTimeout(() => removeToast(toast.id), 3000);
    return () => clearTimeout(timer);
  }, [toast.id, removeToast]);

  const icons  = { success: '✅', error: '⚠️', info: 'ℹ️' } as const;
  const colors = {
    success: 'rgba(46,125,50,0.96)',
    error:   'rgba(183,28,28,0.96)',
    info:    'rgba(13,71,161,0.96)',
  } as const;

  return (
    <div
      className="toast-enter"
      onClick={() => removeToast(toast.id)}
      style={{
        background: colors[toast.type],
        color: '#fff', borderRadius: 12,
        padding: '10px 14px',
        display: 'flex', alignItems: 'center', gap: 10,
        fontSize: 14, fontWeight: 500,
        boxShadow: '0 4px 24px rgba(0,0,0,0.5)',
        pointerEvents: 'auto', cursor: 'pointer',
        backdropFilter: 'blur(12px)',
        border: '1px solid rgba(255,255,255,0.12)',
      }}
    >
      <span style={{ fontSize: 16 }}>{icons[toast.type]}</span>
      <span style={{ flex: 1 }}>{toast.message}</span>
    </div>
  );
}
