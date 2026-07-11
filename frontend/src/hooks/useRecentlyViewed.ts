/**
 * Hook for managing the recently-viewed DPS posts list.
 *
 * <p>Persists up to {@code MAX} entries in {@code localStorage}.
 * Duplicate entries are deduplicated; the most recently viewed post
 * is always at the front of the list.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useCallback } from 'react';
import type { DpsPost } from '../types';

const KEY = 'dps_recently_viewed';
const MAX = 10;

/** Provides read, write, and clear access to the recently-viewed posts list. */
export function useRecentlyViewed() {
  const getAll = useCallback((): DpsPost[] => {
    try {
      return JSON.parse(localStorage.getItem(KEY) ?? '[]');
    } catch {
      return [];
    }
  }, []);

  const add = useCallback((post: DpsPost) => {
    const list = getAll().filter((p) => p.id !== post.id);
    list.unshift(post);
    localStorage.setItem(KEY, JSON.stringify(list.slice(0, MAX)));
  }, [getAll]);

  const clear = useCallback(() => localStorage.removeItem(KEY), []);

  return { getAll, add, clear };
}
