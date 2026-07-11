/**
 * Hook for loading DPS posts within a given radius of the user's location.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */

import { useCallback, useState } from 'react';
import { fetchNearbyPosts } from '../api/client';
import { useAppStore } from '../store';
import type { DpsPost } from '../types';

interface UsePostsResult {
  posts: DpsPost[];
  loading: boolean;
  error: string | null;
  refresh: (lat: number, lon: number, radiusKm?: number) => Promise<void>;
}

/**
 * Manages the DPS post list for the current map viewport.
 *
 * @returns object containing the post array, loading/error state, and a refresh function
 */
export function usePosts(): UsePostsResult {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { posts, setPosts } = useAppStore();

  const refresh = useCallback(
    async (lat: number, lon: number, radiusKm = 5) => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchNearbyPosts(lat, lon, radiusKm);
        setPosts(data);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : 'Failed to load posts';
        setError(msg);
      } finally {
        setLoading(false);
      }
    },
    [setPosts]
  );

  return { posts, loading, error, refresh };
}
