package me.marensovich.policecheckerbot.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.model.DpsPost;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.repository.AppSettingsRepository;
import me.marensovich.policecheckerbot.backend.repository.PostRepository;
import me.marensovich.policecheckerbot.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing user live-location tracking.
 *
 * <p>Processes WebSocket location updates from active live-tracking clients.
 * On each update the user's position is persisted, posts within the notification
 * radius are checked using PostGIS {@code ST_DWithin}, and proximity alerts are
 * dispatched. A per-post cooldown of {@value #notificationCooldownMs} ms prevents
 * repeated alerts for the same post within a 10-minute window.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveTrackingService {

    /** Minimum interval between repeat alerts for the same post (10 minutes). */
    private static final long notificationCooldownMs = 10L * 60 * 1000;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final PostService postService;
    private final AppSettingsRepository appSettingsRepository;

    /**
     * In-memory cooldown cache: {@code "userId:postId"} → timestamp of last notification.
     * Uses {@link ConcurrentHashMap} for thread safety under concurrent WebSocket messages.
     */
    private final Map<String, Long> notificationCooldownCache = new ConcurrentHashMap<>();

    /**
     * Enable or disable live tracking for a user.
     *
     * <p>Live tracking is a Premium-only feature; enabling it for a free user throws 403.
     * Sends a Telegram confirmation message when the state changes.
     *
     * @param user    authenticated user
     * @param enabled {@code true} to enable, {@code false} to disable
     * @throws ResponseStatusException 403 if the user does not have an active Premium subscription
     */
    @Transactional
    public void setLiveTracking(User user, boolean enabled) {
        if (enabled && !user.getIsSubscribed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Live tracking is available for Premium subscribers only");
        }

        user.setLiveTracking(enabled);
        userRepository.save(user);

        if (enabled) {
            notificationService.notifyLiveEnabled(user.getTgId(), user.getNotifyRadiusKm());
        } else {
            notificationService.notifyLiveDisabled(user.getTgId());
        }
        log.info("Live tracking {}: userId={}", enabled ? "enabled" : "disabled", user.getId());
    }

    /**
     * Process a location update from a live-tracking WebSocket client.
     *
     * <p>Steps:
     * <ol>
     *   <li>Persist the updated position to the database</li>
     *   <li>Auto-confirm nearby posts within the configured radius</li>
     *   <li>Check for DPS posts in the user's notification radius and send alerts
     *       (subject to the per-post cooldown)</li>
     * </ol>
     *
     * @param user authenticated user
     * @param lat  current latitude
     * @param lon  current longitude
     */
    @Transactional
    public void processLocationUpdate(User user, Double lat, Double lon) {
        if (!user.getLiveTracking() || !user.getIsSubscribed()) {
            return;
        }

        userRepository.updateLocation(user.getId(), lat, lon, LocalDateTime.now());

        double autoConfirmRadius = appSettingsRepository.findById("auto.confirm.radius.meters")
            .map(s -> { try { return Double.parseDouble(s.getValue()); } catch (NumberFormatException e) { return 200.0; } })
            .orElse(200.0);
        try {
            postService.autoConfirmNearby(user, lat, lon, autoConfirmRadius);
        } catch (Exception e) {
            log.debug("Auto-confirm (WebSocket): {}", e.getMessage());
        }

        double radiusMeters = user.getNotifyRadiusKm() * 1000.0;
        List<DpsPost> nearbyPosts = postRepository.findNearby(lat, lon, radiusMeters);

        for (DpsPost post : nearbyPosts) {
            String cooldownKey = user.getId() + ":" + post.getId();
            Long lastNotified = notificationCooldownCache.get(cooldownKey);
            long now = System.currentTimeMillis();

            if (lastNotified == null || (now - lastNotified) > notificationCooldownMs) {
                double distanceKm = calculateDistanceKm(lat, lon, post.getLat(), post.getLon());
                notificationService.notifyPostNearby(user.getTgId(), post, distanceKm);
                notificationCooldownCache.put(cooldownKey, now);
            }
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    /** Haversine distance in kilometres between two WGS-84 coordinates. */
    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
