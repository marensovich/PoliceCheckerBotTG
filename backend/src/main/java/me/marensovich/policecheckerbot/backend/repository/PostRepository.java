package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.DpsPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for DPS post persistence and geo-spatial retrieval.
 *
 * <p>Includes native PostGIS queries using {@code GEOGRAPHY(POINT, 4326)} for distance
 * calculations and {@code ST_DWithin} for radius-based spatial filtering.
 * The {@code location} column carries a spatial index for efficient lookups.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface PostRepository extends JpaRepository<DpsPost, Long> {

    /**
     * Find all active posts within a geographic radius, ordered by distance from the query point.
     *
     * <p>Uses PostGIS {@code ST_DWithin} for radius filtering and {@code ST_Distance} for sorting.
     * Expired posts (with a non-null {@code expires_at} in the past) are excluded.
     *
     * @param lat          latitude of the search center
     * @param lon          longitude of the search center
     * @param radiusMeters search radius in metres
     * @return active posts within the radius, nearest first
     */
    @Query(value = """
        SELECT p.* FROM dps_posts p
        WHERE ST_DWithin(
            ST_MakePoint(p.lon, p.lat)::geography,
            ST_MakePoint(:lon, :lat)::geography,
            :radiusMeters
        )
        AND p.is_active = TRUE
        AND (p.expires_at IS NULL OR p.expires_at > NOW())
        ORDER BY ST_Distance(ST_MakePoint(p.lon, p.lat)::geography, ST_MakePoint(:lon, :lat)::geography)
        """, nativeQuery = true)
    List<DpsPost> findNearby(@Param("lat") Double lat,
                              @Param("lon") Double lon,
                              @Param("radiusMeters") Double radiusMeters);

    /**
     * Find all posts created by a specific user, sorted by creation date descending.
     *
     * @param userId internal user ID
     * @return list of posts (both active and inactive) created by that user
     */
    @Query("SELECT p FROM DpsPost p WHERE p.addedBy.id = :userId ORDER BY p.createdAt DESC")
    List<DpsPost> findByUserId(@Param("userId") Long userId);

    /**
     * Deactivate all active posts whose confidence score has fallen below the given threshold.
     * Used by the scheduled moderation sweep.
     *
     * @param threshold minimum acceptable confidence score (e.g. {@code -5})
     * @return number of posts deactivated
     */
    @Modifying
    @Query("UPDATE DpsPost p SET p.isActive = false WHERE p.confidence < :threshold AND p.isActive = true")
    int deactivateByLowConfidence(@Param("threshold") int threshold);

    /**
     * Deactivate all active posts whose {@code expires_at} timestamp is in the past.
     *
     * @param now current server time
     * @return number of posts deactivated
     */
    @Modifying
    @Query("UPDATE DpsPost p SET p.isActive = false WHERE p.isActive = true AND p.expiresAt IS NOT NULL AND p.expiresAt <= :now")
    int deactivateExpired(@Param("now") java.time.LocalDateTime now);

    /**
     * Deactivate old posts that were never confirmed (confidence ≤ 0) and are older than
     * the given date. Used to auto-expire stale unverified reports.
     *
     * @param olderThan cutoff date — posts created before this date are candidates
     * @return number of posts deactivated
     */
    @Modifying
    @Query("UPDATE DpsPost p SET p.isActive = false " +
           "WHERE p.createdAt < :olderThan AND p.confidence <= 0 AND p.isActive = true")
    int deactivateStaleUnconfirmed(@Param("olderThan") LocalDateTime olderThan);

    /** Returns the total number of currently active posts. */
    long countByIsActiveTrue();

    /**
     * Check whether an active CAMERA post already exists within {@code radius} metres
     * of the given coordinates. Used for deduplication during bulk camera imports.
     */
    @Query(value = """
        SELECT COUNT(*) > 0 FROM dps_posts
        WHERE post_type = 'CAMERA'
          AND is_active = TRUE
          AND ST_DWithin(
              ST_MakePoint(lon, lat)::geography,
              ST_MakePoint(:lon, :lat)::geography,
              :radius
          )
        """, nativeQuery = true)
    boolean existsCameraNearby(@Param("lat") double lat,
                                @Param("lon") double lon,
                                @Param("radius") double radius);

    /**
     * Return a paginated list of active posts whose confidence is at or below the threshold,
     * ordered by confidence ascending (lowest-trust posts first).
     * Used to populate the admin moderation queue.
     *
     * @param threshold confidence ceiling for inclusion in the queue
     * @param pageable  pagination parameters
     * @return page of low-confidence posts
     */
    @Query("SELECT p FROM DpsPost p WHERE p.isActive = true AND p.confidence <= :threshold ORDER BY p.confidence ASC")
    org.springframework.data.domain.Page<DpsPost> findModerationQueue(
        @Param("threshold") int threshold,
        org.springframework.data.domain.Pageable pageable);

    /**
     * Count new posts grouped by calendar day for the last {@code days} days.
     * Returns rows of {@code (day VARCHAR, cnt BIGINT)}.
     *
     * @param days number of days to look back
     * @return list of {@code Object[]} with index 0 = date string, index 1 = count
     */
    @Query(value = """
        SELECT TO_CHAR(created_at, 'YYYY-MM-DD') as day, COUNT(*) as cnt
        FROM dps_posts
        WHERE created_at >= NOW() - CAST(:days || ' days' AS INTERVAL)
        GROUP BY day ORDER BY day
        """, nativeQuery = true)
    java.util.List<Object[]> countPostsPerDay(@Param("days") int days);

    /**
     * Count new user registrations grouped by calendar day for the last {@code days} days.
     * Returns rows of {@code (day VARCHAR, cnt BIGINT)}.
     *
     * @param days number of days to look back
     * @return list of {@code Object[]} with index 0 = date string, index 1 = count
     */
    @Query(value = """
        SELECT TO_CHAR(created_at, 'YYYY-MM-DD') as day, COUNT(*) as cnt
        FROM users
        WHERE created_at >= NOW() - CAST(:days || ' days' AS INTERVAL)
        GROUP BY day ORDER BY day
        """, nativeQuery = true)
    java.util.List<Object[]> countUsersPerDay(@Param("days") int days);

    /**
     * Count distinct active users (by {@code last_seen}) grouped by calendar day
     * for the last {@code days} days. Returns rows of {@code (day VARCHAR, cnt BIGINT)}.
     *
     * @param days number of days to look back
     * @return list of {@code Object[]} with index 0 = date string, index 1 = count
     */
    @Query(value = """
        SELECT TO_CHAR(last_seen, 'YYYY-MM-DD') as day, COUNT(DISTINCT id) as cnt
        FROM users
        WHERE last_seen IS NOT NULL
          AND last_seen >= NOW() - CAST(:days || ' days' AS INTERVAL)
        GROUP BY day ORDER BY day
        """, nativeQuery = true)
    java.util.List<Object[]> countActiveUsersPerDay(@Param("days") int days);

    /**
     * Count posts created by a user within a sliding time window.
     * Used to enforce the per-hour rate limit on post creation.
     *
     * @param userId internal user ID
     * @param since  start of the time window (typically {@code now - 1 hour})
     * @return number of posts created since {@code since}
     */
    @Query("SELECT COUNT(p) FROM DpsPost p WHERE p.addedBy.id = :userId AND p.createdAt >= :since")
    long countRecentByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
