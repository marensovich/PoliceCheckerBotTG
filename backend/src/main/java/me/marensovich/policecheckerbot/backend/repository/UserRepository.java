package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DPS Tracker user persistence and retrieval.
 *
 * <p>Provides custom JPQL and native PostgreSQL/PostGIS queries for geo-proximity
 * notification lookups, subscription expiry scanning, and atomic upsert on login.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their Telegram user ID.
     *
     * @param tgId the unique Telegram user ID
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByTgId(Long tgId);

    /**
     * Find a user by their active session token.
     *
     * @param sessionToken the active session token (UUID)
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findBySessionToken(String sessionToken);

    /**
     * Find all users with live tracking enabled and a valid Premium subscription.
     *
     * @param now current server time used to compare against subscription expiry
     * @return list of users currently in live-tracking mode
     */
    @Query("SELECT u FROM User u WHERE u.liveTracking = true AND u.isSubscribed = true " +
           "AND u.subscriptionExpiresAt > :now")
    List<User> findAllActiveLiveTrackers(@Param("now") LocalDateTime now);

    /**
     * Find users whose Premium subscription has expired and should be deactivated.
     *
     * @param now current server time
     * @return list of users with expired subscriptions
     */
    @Query("SELECT u FROM User u WHERE u.isSubscribed = true AND u.subscriptionExpiresAt < :now")
    List<User> findExpiredSubscriptions(@Param("now") LocalDateTime now);

    /** Returns the total number of users with an active Premium subscription. */
    long countByIsSubscribedTrue();

    /** Returns the total number of banned users. */
    long countByIsBannedTrue();

    /**
     * Full-text search over username and Telegram ID (case-insensitive substring match).
     * Results are sorted by registration date descending.
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "CAST(u.tgId AS string) LIKE CONCAT('%', :q, '%') " +
           "ORDER BY u.createdAt DESC")
    org.springframework.data.domain.Page<User> searchUsers(
        @Param("q") String q,
        org.springframework.data.domain.Pageable pageable);

    /**
     * Find all users whose last known position falls within their configured notification
     * radius of the given coordinates, excluding the post author.
     * Uses PostGIS {@code ST_DWithin} for accurate geographic distance filtering.
     *
     * @param lat          latitude of the new post
     * @param lon          longitude of the new post
     * @param excludeTgId  Telegram ID of the user who created the post (excluded from results)
     * @return users who should receive a proximity notification
     */
    @Query(value = """
        SELECT u.* FROM users u
        WHERE u.last_lat IS NOT NULL
          AND u.tg_id != :excludeTgId
          AND u.is_banned = false
          AND ST_DWithin(
              ST_MakePoint(u.last_lon, u.last_lat)::geography,
              ST_MakePoint(:lon, :lat)::geography,
              u.notify_radius_km * 1000
          )
        """, nativeQuery = true)
    java.util.List<User> findUsersNearPost(
        @Param("lat") Double lat,
        @Param("lon") Double lon,
        @Param("excludeTgId") Long excludeTgId);

    /** Returns all active (non-banned) Premium subscribers. */
    @Query("SELECT u FROM User u WHERE u.isSubscribed = true AND u.isBanned = false")
    java.util.List<User> findAllPremium();

    /** Returns all non-banned users. */
    @Query("SELECT u FROM User u WHERE u.isBanned = false")
    java.util.List<User> findAllActive();

    /** Returns all users assigned the given role. */
    java.util.List<User> findByRole(me.marensovich.policecheckerbot.backend.model.Role role);

    /** Returns the number of users with the given role. */
    long countByRole(me.marensovich.policecheckerbot.backend.model.Role role);

    /** Returns the number of users registered after the given timestamp. */
    long countByCreatedAtAfter(LocalDateTime since);

    /**
     * Paginated user list with optional filters on role, ban status, and subscription status.
     * Pass {@code null} for any parameter to skip that filter.
     *
     * <p>The {@code :role IS NULL OR u.role = :role} pattern lets Hibernate short-circuit
     * the predicate when the caller passes {@code null}, effectively treating it as "any".
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:banned IS NULL OR u.isBanned = :banned) AND " +
           "(:subscribed IS NULL OR u.isSubscribed = :subscribed) " +
           "ORDER BY u.createdAt DESC")
    org.springframework.data.domain.Page<User> findFiltered(
        @Param("role") me.marensovich.policecheckerbot.backend.model.Role role,
        @Param("banned") Boolean banned,
        @Param("subscribed") Boolean subscribed,
        org.springframework.data.domain.Pageable pageable);

    /**
     * Atomic upsert: inserts a new user row, or updates {@code username}, {@code first_name},
     * and {@code session_token} on conflict with an existing {@code tg_id}.
     * Safe under concurrent logins — no lost-update race condition.
     *
     * @param tgId         Telegram user ID
     * @param username     Telegram username
     * @param firstName    user's first name from Telegram
     * @param sessionToken newly generated UUID session token
     */
    @Modifying
    @Query(value = """
        INSERT INTO users (tg_id, username, first_name, session_token,
                           is_subscribed, notify_radius_km, live_tracking,
                           is_banned, role, reputation_score, created_at)
        VALUES (:tgId, :username, :firstName, :sessionToken,
                false, 5, false, false, 'USER', 0, NOW())
        ON CONFLICT (tg_id) DO UPDATE
            SET username      = EXCLUDED.username,
                first_name    = EXCLUDED.first_name,
                session_token = EXCLUDED.session_token
        """, nativeQuery = true)
    void upsert(@Param("tgId") Long tgId,
                @Param("username") String username,
                @Param("firstName") String firstName,
                @Param("sessionToken") String sessionToken);

    /**
     * Update the last known geographic position and activity timestamp for a user.
     *
     * @param userId internal user ID
     * @param lat    latitude
     * @param lon    longitude
     * @param now    current server time
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLat = :lat, u.lastLon = :lon, u.lastSeen = :now WHERE u.id = :userId")
    void updateLocation(@Param("userId") Long userId,
                        @Param("lat") Double lat,
                        @Param("lon") Double lon,
                        @Param("now") LocalDateTime now);
}
