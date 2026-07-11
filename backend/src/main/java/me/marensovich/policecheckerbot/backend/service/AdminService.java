package me.marensovich.policecheckerbot.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.dto.*;
import me.marensovich.policecheckerbot.backend.model.*;
import me.marensovich.policecheckerbot.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service for admin-panel operations in DPS Tracker.
 *
 * <p>Handles statistics, user management, post moderation, broadcast messaging,
 * application settings, and admin action audit logging. All mutating operations
 * write an entry to the {@code AdminLog} table via {@link #writeLog}.
 *
 * <p>On startup ({@link #seedDefaultSettings}) missing settings are initialised
 * to default values, and stale defaults are migrated to updated values.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final AppSettingsRepository settingsRepository;
    private final AdminLogRepository logRepository;
    private final NotificationService notificationService;

    // ── Settings bootstrap ────────────────────────────────────────────────────────

    /**
     * Seed default application settings on startup if they do not yet exist,
     * and migrate any stale default values to their updated counterparts.
     */
    @PostConstruct
    public void seedDefaultSettings() {
        seed("nearby.radius.free", "5", "Free-tier search radius (km)");
        seed("nearby.radius.premium", "50", "Premium search radius (km)");
        migrateOldDefault("nearby.radius.free", "2", "5");
        migrateOldDefault("nearby.radius.premium", "10", "50");
        seed("subscription.price.stars", "100", "Subscription price (Stars)");
        seed("subscription.duration.days", "30", "Subscription duration (days)");
        seed("posts.rate.limit", "5", "Max posts per hour per user");
        seed("moderation.confidence.threshold", "-3", "Confidence threshold for moderation queue");
        seed("auto.confirm.radius.meters", "200", "Auto-confirm radius for live tracking (m)");
    }

    private void seed(String key, String value, String description) {
        if (!settingsRepository.existsById(key)) {
            settingsRepository.save(AppSettings.builder()
                .key(key).value(value).description(description).build());
        }
    }

    /** Update a setting only if its current value matches the old default — avoids overwriting manual changes. */
    private void migrateOldDefault(String key, String oldValue, String newValue) {
        settingsRepository.findById(key).ifPresent(s -> {
            if (oldValue.equals(s.getValue())) {
                s.setValue(newValue);
                settingsRepository.save(s);
            }
        });
    }

    /** Return all application settings. */
    @Transactional(readOnly = true)
    public List<AppSettings> getAllSettings() {
        return settingsRepository.findAll();
    }

    /**
     * Update the value of a single setting and write an audit log entry.
     *
     * @param key   setting key
     * @param value new value string
     * @param admin performing administrator
     * @return updated setting
     */
    @Transactional
    public AppSettings updateSetting(String key, String value, User admin) {
        AppSettings s = settingsRepository.findById(key)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Setting not found"));
        String old = s.getValue();
        s.setValue(value);
        settingsRepository.save(s);
        writeLog(admin, "setting_update", null, null, key + ": " + old + " → " + value);
        return s;
    }

    // ── Statistics ────────────────────────────────────────────────────────────────

    /** Return aggregate system counts for the admin dashboard. */
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        LocalDateTime now = LocalDateTime.now();
        return AdminStatsResponse.builder()
            .totalUsers(userRepository.count())
            .premiumUsers(userRepository.countByIsSubscribedTrue())
            .bannedUsers(userRepository.countByIsBannedTrue())
            .moderatorCount(userRepository.countByRole(Role.MODERATOR))
            .newUsersToday(userRepository.countByCreatedAtAfter(now.truncatedTo(ChronoUnit.DAYS)))
            .newUsersThisWeek(userRepository.countByCreatedAtAfter(now.minusDays(7)))
            .newUsersThisMonth(userRepository.countByCreatedAtAfter(now.minusDays(30)))
            .totalPosts(postRepository.count())
            .activePosts(postRepository.countByIsActiveTrue())
            .build();
    }

    /**
     * Return per-day post and user counts for the activity bar chart.
     *
     * @param days number of days to include (max 90)
     * @return list of {@link ActivityDataPoint} sorted by date ascending
     */
    @Transactional(readOnly = true)
    public List<ActivityDataPoint> getActivity(int days) {
        List<Object[]> posts       = postRepository.countPostsPerDay(days);
        List<Object[]> newUsers    = postRepository.countUsersPerDay(days);
        List<Object[]> activeUsers = postRepository.countActiveUsersPerDay(days);

        // index: 0=posts, 1=newUsers, 2=activeUsers
        Map<String, long[]> map = new TreeMap<>();
        for (Object[] row : posts) {
            map.computeIfAbsent(row[0].toString(), k -> new long[3])[0] = ((Number) row[1]).longValue();
        }
        for (Object[] row : newUsers) {
            map.computeIfAbsent(row[0].toString(), k -> new long[3])[1] = ((Number) row[1]).longValue();
        }
        for (Object[] row : activeUsers) {
            map.computeIfAbsent(row[0].toString(), k -> new long[3])[2] = ((Number) row[1]).longValue();
        }

        List<ActivityDataPoint> result = new ArrayList<>();
        map.forEach((date, c) -> result.add(new ActivityDataPoint(date, c[0], c[1], c[2])));
        return result;
    }

    // ── User management ───────────────────────────────────────────────────────────

    /** Return a paginated list of all users, newest first. */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toAdminUserResponse);
    }

    /**
     * Search users by username or Telegram ID (case-insensitive substring match).
     *
     * @param query    search string
     * @param pageable pagination parameters
     * @return page of matching users
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable).map(this::toAdminUserResponse);
    }

    /**
     * Filter users by role, ban status, and subscription status.
     * Any {@code null} parameter is treated as "no filter" for that dimension.
     *
     * @param role       restrict to this role, or {@code null} for all roles
     * @param banned     restrict to banned/non-banned users, or {@code null} for both
     * @param subscribed restrict to subscribed/free users, or {@code null} for both
     * @param pageable   pagination parameters
     * @return filtered page of users
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> filterUsers(Role role, Boolean banned, Boolean subscribed, Pageable pageable) {
        return userRepository.findFiltered(role, banned, subscribed, pageable).map(this::toAdminUserResponse);
    }

    /**
     * Import speed cameras from OpenStreetMap via the Overpass API for the given bounding box.
     *
     * <p>Queries {@code highway=speed_camera} nodes within the bbox, then bulk-creates
     * {@link PostType#CAMERA} posts. Cameras within 30 m of an existing active camera
     * are skipped to prevent duplicates on repeated imports.
     *
     * @param south south latitude of the bounding box
     * @param west  west longitude of the bounding box
     * @param north north latitude of the bounding box
     * @param east  east longitude of the bounding box
     * @param admin administrator performing the import
     * @return import summary (imported / skipped / total)
     */
    public CameraImportResponse importCamerasFromOSM(
            double south, double west, double north, double east, User admin) throws Exception {

        String query = String.format(
            "[out:json][bbox:%.6f,%.6f,%.6f,%.6f][timeout:90];" +
            "(node[\"highway\"=\"speed_camera\"];);" +
            "out body;",
            south, west, north, east
        );

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://overpass-api.de/api/interpreter"))
            .POST(HttpRequest.BodyPublishers.ofString(
                "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(120))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        JsonNode elements = root.path("elements");

        int imported = 0, skipped = 0;
        List<DpsPost> toSave = new ArrayList<>();

        for (JsonNode el : elements) {
            if (!"node".equals(el.path("type").asText())) continue;
            double lat = el.path("lat").asDouble();
            double lon = el.path("lon").asDouble();

            if (postRepository.existsCameraNearby(lat, lon, 30.0)) {
                skipped++;
                continue;
            }

            JsonNode tags = el.path("tags");
            String desc = null;
            if (tags.has("maxspeed")) {
                desc = "Камера (до " + tags.get("maxspeed").asText() + " км/ч)";
            }

            toSave.add(DpsPost.builder()
                .postType(PostType.CAMERA)
                .lat(lat)
                .lon(lon)
                .description(desc)
                .isActive(true)
                .confidence(5)
                .confirmedCount(0)
                .build());
            imported++;
        }

        postRepository.saveAll(toSave);
        int total = imported + skipped;
        log.info("Camera import: bbox=[{},{},{},{}] imported={} skipped={} total={}",
            south, west, north, east, imported, skipped, total);
        writeLog(admin, "camera_import", null, null,
            "bbox=[" + south + "," + west + "," + north + "," + east + "]" +
            " imported=" + imported + " skipped=" + skipped);

        return new CameraImportResponse(imported, skipped, total);
    }

    /**
     * Toggle the ban status of a user. Cannot ban another administrator.
     * Banning also clears the session token and disables live tracking.
     */
    @Transactional
    public AdminUserResponse toggleBan(Long userId, User admin) {
        User user = getUserOrThrow(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrators cannot be banned");
        }
        boolean newBanned = !Boolean.TRUE.equals(user.getIsBanned());
        user.setIsBanned(newBanned);
        if (newBanned) {
            user.setLiveTracking(false);
            user.setSessionToken(null);
        }
        userRepository.save(user);
        writeLog(admin, newBanned ? "ban" : "unban", "USER", userId, "@" + user.getUsername());
        if (newBanned) {
            notificationService.notifyAdminAction(user.getTgId(),
                "🚫 *Ваш аккаунт был заблокирован* администратором.\n\n" +
                "Если вы считаете это ошибкой — обратитесь в поддержку.");
        } else {
            notificationService.notifyAdminAction(user.getTgId(),
                "✅ *Ваш аккаунт разблокирован.* Вы снова можете пользоваться приложением.");
        }
        return toAdminUserResponse(user);
    }

    /** Grant a permanent 30-day Premium subscription to a user. */
    @Transactional
    public AdminUserResponse forceSubscribe(Long userId, User admin) {
        User user = getUserOrThrow(userId);
        user.setIsSubscribed(true);
        user.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(30));
        userRepository.save(user);
        writeLog(admin, "force_subscribe", "USER", userId, "@" + user.getUsername());
        notificationService.notifyAdminAction(user.getTgId(),
            "⭐ *Вам выдан Premium на 30 дней!*\n\n" +
            "Наслаждайтесь расширенным радиусом уведомлений и всеми возможностями приложения.");
        return toAdminUserResponse(user);
    }

    /** Revoke a user's Premium subscription and disable live tracking. */
    @Transactional
    public AdminUserResponse revokeSubscription(Long userId, User admin) {
        User user = getUserOrThrow(userId);
        user.setIsSubscribed(false);
        user.setSubscriptionExpiresAt(null);
        user.setLiveTracking(false);
        userRepository.save(user);
        writeLog(admin, "revoke_subscription", "USER", userId, "@" + user.getUsername());
        notificationService.notifyAdminAction(user.getTgId(),
            "⚠️ *Ваша Premium-подписка была отозвана* администратором.\n\n" +
            "Live-трекинг остановлен. Радиус уведомлений возвращён к бесплатному лимиту.");
        return toAdminUserResponse(user);
    }

    /** Grant a trial Premium period of the specified number of days (stacks on top of existing subscription). */
    @Transactional
    public AdminUserResponse grantTrial(Long userId, int days, User admin) {
        User user = getUserOrThrow(userId);
        LocalDateTime base = (user.getIsSubscribed() && user.getSubscriptionExpiresAt() != null
            && user.getSubscriptionExpiresAt().isAfter(LocalDateTime.now()))
            ? user.getSubscriptionExpiresAt()
            : LocalDateTime.now();
        user.setIsSubscribed(true);
        user.setSubscriptionExpiresAt(base.plusDays(days));
        userRepository.save(user);
        writeLog(admin, "grant_trial", "USER", userId, "@" + user.getUsername() + " +" + days + "d");
        notificationService.notifyAdminAction(user.getTgId(),
            "🎁 *Вам выдан пробный период Premium на " + days + " дн!*\n\n" +
            "Попробуйте все возможности приложения — расширенный радиус, live-трекинг и многое другое.");
        return toAdminUserResponse(user);
    }

    /**
     * Change a user's role. Cannot change an admin's own role. Use
     * {@link #assignModerator} to set {@code MODERATOR} (requires a region).
     */
    @Transactional
    public AdminUserResponse changeRole(Long userId, Role role, User admin) {
        if (userId.equals(admin.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own role");
        }
        if (role == Role.MODERATOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Use POST /users/{id}/moderator to assign the moderator role");
        }
        User user = getUserOrThrow(userId);
        Role old = user.getRole();
        user.setRole(role);
        if (old == Role.MODERATOR) {
            user.setModeratorRegion(null);
        }
        userRepository.save(user);
        writeLog(admin, "role_change", "USER", userId,
            "@" + user.getUsername() + ": " + old + " → " + role);
        if (role == Role.ADMIN) {
            notificationService.notifyAdminAction(user.getTgId(),
                "🛡 *Вам выдана роль Администратора* в DPS Tracker.\n\n" +
                "Откройте приложение и перейдите в панель администрирования.");
        }
        return toAdminUserResponse(user);
    }

    /** Assign the moderator role to a user with a specific geographic region. */
    @Transactional
    public AdminUserResponse assignModerator(Long userId, String region, User admin) {
        User user = getUserOrThrow(userId);
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators cannot be made moderators");
        }
        user.setRole(Role.MODERATOR);
        user.setModeratorRegion(region.trim());
        userRepository.save(user);
        writeLog(admin, "assign_moderator", "USER", userId,
            "@" + user.getUsername() + " → region: " + region);
        notificationService.notifyAdminAction(user.getTgId(),
            "👮 *Вам выдана роль Модератора* в DPS Tracker.\n\n" +
            "Ваш регион: *" + region.trim() + "*\n" +
            "Откройте приложение для работы с очередью модерации.");
        return toAdminUserResponse(user);
    }

    /** Revoke the moderator role, resetting the user to {@code USER} and clearing the region. */
    @Transactional
    public AdminUserResponse revokeModerator(Long userId, User admin) {
        User user = getUserOrThrow(userId);
        if (user.getRole() != Role.MODERATOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a moderator");
        }
        String region = user.getModeratorRegion();
        user.setRole(Role.USER);
        user.setModeratorRegion(null);
        userRepository.save(user);
        writeLog(admin, "revoke_moderator", "USER", userId,
            "@" + user.getUsername() + " (region: " + region + ")");
        notificationService.notifyAdminAction(user.getTgId(),
            "⚠️ *Ваши права Модератора были отозваны* администратором.\n\n" +
            "Вы переведены в статус обычного пользователя.");
        return toAdminUserResponse(user);
    }

    // ── Post management ───────────────────────────────────────────────────────────

    /** Return a paginated list of all posts (active and inactive), newest first. */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findAll(pageable).map(this::toPostResponse);
    }

    /** Return a paginated moderation queue of low-confidence active posts. */
    @Transactional(readOnly = true)
    public Page<PostResponse> getModerationQueue(Pageable pageable) {
        int threshold = Integer.parseInt(
            settingsRepository.findById("moderation.confidence.threshold")
                .map(AppSettings::getValue).orElse("-3"));
        return postRepository.findModerationQueue(threshold, pageable).map(this::toPostResponse);
    }

    /** Force-deactivate a post and record the action in the audit log. */
    @Transactional
    public void forceDeactivatePost(Long postId, User admin) {
        DpsPost post = postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.setIsActive(false);
        postRepository.save(post);
        writeLog(admin, "delete_post", "POST", postId,
            post.getPostType().name() + " @ " + post.getLat() + "," + post.getLon());
    }

    // ── Broadcast ─────────────────────────────────────────────────────────────────

    /**
     * Broadcast a message to the specified user group and log the action.
     *
     * @param request message and target group ({@code ALL}, {@code PREMIUM}, {@code FREE})
     * @param admin   performing administrator
     * @return number of messages dispatched
     */
    @Transactional
    public int broadcast(BroadcastRequest request, User admin) {
        List<User> targets = switch (request.getTarget()) {
            case "PREMIUM" -> userRepository.findAllPremium();
            case "FREE" -> userRepository.findAllActive().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getIsSubscribed())).toList();
            default -> userRepository.findAllActive();
        };
        String fullMessage = "📢 *Сообщение от Администратора:*\n\n" + request.getMessage();
        notificationService.broadcast(targets, fullMessage);
        writeLog(admin, "broadcast", null, null,
            "target=" + request.getTarget() + " count=" + targets.size());
        log.info("Broadcast: {} → {} users", request.getTarget(), targets.size());
        return targets.size();
    }

    // ── Audit log ─────────────────────────────────────────────────────────────────

    /** Return the paginated admin action audit log, newest entries first. */
    @Transactional(readOnly = true)
    public Page<AdminLogResponse> getLogs(Pageable pageable) {
        return logRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(this::toLogResponse);
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private void writeLog(User admin, String action, String targetType, Long targetId, String details) {
        logRepository.save(AdminLog.builder()
            .admin(admin).action(action)
            .targetType(targetType).targetId(targetId)
            .details(details).build());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AdminUserResponse toAdminUserResponse(User u) {
        return AdminUserResponse.builder()
            .id(u.getId()).tgId(u.getTgId())
            .username(u.getUsername()).firstName(u.getFirstName())
            .role(u.getRole()).moderatorRegion(u.getModeratorRegion())
            .reputationScore(u.getReputationScore())
            .isSubscribed(u.getIsSubscribed())
            .subscriptionExpiresAt(u.getSubscriptionExpiresAt())
            .isBanned(u.getIsBanned()).liveTracking(u.getLiveTracking())
            .lastSeen(u.getLastSeen()).createdAt(u.getCreatedAt())
            .build();
    }

    private PostResponse toPostResponse(DpsPost p) {
        return PostResponse.builder()
            .id(p.getId()).postType(p.getPostType())
            .lat(p.getLat()).lon(p.getLon())
            .description(p.getDescription())
            .addedByUsername(p.getAddedBy() != null ? p.getAddedBy().getUsername() : null)
            .isActive(p.getIsActive()).confidence(p.getConfidence())
            .confirmedCount(p.getConfirmedCount())
            .patrolSpeedKmh(p.getPatrolSpeedKmh())
            .expiresAt(p.getExpiresAt())
            .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
            .build();
    }

    private AdminLogResponse toLogResponse(AdminLog l) {
        return AdminLogResponse.builder()
            .id(l.getId())
            .adminUsername(l.getAdmin() != null ? l.getAdmin().getUsername() : "system")
            .action(l.getAction()).targetType(l.getTargetType())
            .targetId(l.getTargetId()).details(l.getDetails())
            .createdAt(l.getCreatedAt())
            .build();
    }
}
