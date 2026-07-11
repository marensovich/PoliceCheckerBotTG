/**
 * Hook providing Web Audio API sound effects for in-app feedback.
 *
 * <p>All sounds are generated programmatically via the {@code AudioContext} API.
 * Errors are silently swallowed to handle browsers that block audio autoplay.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useCallback, useRef } from 'react';

export function useSound() {
  const ctxRef = useRef<AudioContext | null>(null);

  const ctx = () => {
    if (!ctxRef.current) ctxRef.current = new AudioContext();
    return ctxRef.current;
  };

  /** Play a melodic chord — used when a new DPS post appears nearby. */
  const playNotification = useCallback(() => {
    try {
      const ac = ctx();
      [880, 1108, 1320].forEach((freq, i) => {
        const osc  = ac.createOscillator();
        const gain = ac.createGain();
        osc.connect(gain);
        gain.connect(ac.destination);
        osc.type = 'sine';
        osc.frequency.value = freq;
        const t = ac.currentTime + i * 0.09;
        gain.gain.setValueAtTime(0, t);
        gain.gain.linearRampToValueAtTime(0.12, t + 0.05);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.55);
        osc.start(t);
        osc.stop(t + 0.55);
      });
    } catch { /* AudioContext blocked by browser */ }
  }, []);

  /** Play a short alert tone — used for errors or warnings. */
  const playAlert = useCallback(() => {
    try {
      const ac  = ctx();
      const osc  = ac.createOscillator();
      const gain = ac.createGain();
      osc.connect(gain);
      gain.connect(ac.destination);
      osc.type = 'sine';
      osc.frequency.setValueAtTime(660, ac.currentTime);
      osc.frequency.setValueAtTime(440, ac.currentTime + 0.15);
      gain.gain.setValueAtTime(0.18, ac.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ac.currentTime + 0.4);
      osc.start(ac.currentTime);
      osc.stop(ac.currentTime + 0.4);
    } catch { /* ignore */ }
  }, []);

  /** Play a soft confirmation tone — used after a successful action. */
  const playConfirm = useCallback(() => {
    try {
      const ac  = ctx();
      const osc  = ac.createOscillator();
      const gain = ac.createGain();
      osc.connect(gain);
      gain.connect(ac.destination);
      osc.type = 'sine';
      osc.frequency.setValueAtTime(440, ac.currentTime);
      osc.frequency.linearRampToValueAtTime(660, ac.currentTime + 0.12);
      gain.gain.setValueAtTime(0.1, ac.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ac.currentTime + 0.3);
      osc.start(ac.currentTime);
      osc.stop(ac.currentTime + 0.3);
    } catch { /* ignore */ }
  }, []);

  return { playNotification, playAlert, playConfirm };
}
