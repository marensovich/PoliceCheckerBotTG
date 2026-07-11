package me.marensovich.policecheckerbot.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.config.BotConfig;
import me.marensovich.policecheckerbot.backend.dto.NearbyResponse;
import me.marensovich.policecheckerbot.backend.dto.PostRequest;
import me.marensovich.policecheckerbot.backend.dto.PostResponse;
import me.marensovich.policecheckerbot.backend.dto.VoteRequest;
import me.marensovich.policecheckerbot.backend.model.*;
import me.marensovich.policecheckerbot.backend.repository.AppSettingsRepository;
import me.marensovich.policecheckerbot.backend.repository.PostHistoryRepository;
import me.marensovich.policecheckerbot.backend.repository.PostRepository;
import me.marensovich.policecheckerbot.backend.repository.UserRepository;
import me.marensovich.policecheckerbot.backend.repository.VoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for managing DPS Tracker map markers (posts).
 *
 * <p>Handles post creation with per-type expiry rules, confidence voting,
 * auto-confirmation from live tracking, and scheduled cleanup sweeps.
 *
 * <p>Post lifetime by type:
 * <ul>
 *   <li>{@code DPS_POST / AMBUSH / HIDDEN_POST}: fades after 4 h, expires after 12 h</li>
 *   <li>{@code PATROL_CAR}: expires after 30 min</li>
 *   <li>{@code CAMERA}: permanent (no expiry)</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final int LOW_CONFIDENCE_THRESHOLD = -5;

    private final PostRepository postRepository;
    private final VoteRepository voteRepository;
    private final PostHistoryRepository postHistoryRepository;
    private final BotConfig botConfig;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AppSettingsRepository appSettingsRepository;

    /**
     * Find an active post by ID.
     *
     * @param postId post ID
     * @return post response DTO
     * @throws ResponseStatusException 404 if the post does not exist or is inactive
     */
    @Transactional(readOnly = true)
    public PostResponse findById(Long postId) {
        return postRepository.findById(postId)
            .filter(DpsPost::getIsActive)
            .map(this::toResponse)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    /**
     * Find all active posts within the given radius (public endpoint).
     *
     * @param lat      search center latitude
     * @param lon      search center longitude
     * @param radiusKm search radius in kilometres (default 5)
     * @return list of nearby posts
     */
    @Transactional(readOnly = true)
    public List<PostResponse> findNearby(Double lat, Double lon, Double radiusKm) {
        double radiusMeters = (radiusKm != null ? radiusKm : 5.0) * 1000;
        return postRepository.findNearby(lat, lon, radiusMeters)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Find nearby posts with the caller's subscription-based radius limit applied.
     * Radii are read from {@code AppSettings} keys {@code nearby.radius.free}
     * and {@code nearby.radius.premium}.
     *
     * @param lat  caller's latitude
     * @param lon  caller's longitude
     * @param user authenticated user
     * @return posts within the applicable radius plus radius/tier metadata
     */
    @Transactional(readOnly = true)
    public NearbyResponse findNearbyAuthenticated(Double lat, Double lon, User user) {
        boolean premium = Boolean.TRUE.equals(user.getIsSubscribed());
        double maxRadiusKm = premium
            ? getDoubleSetting("nearby.radius.premium", 50.0)
            : getDoubleSetting("nearby.radius.free", 5.0);
        List<PostResponse> posts = postRepository.findNearby(lat, lon, maxRadiusKm * 1000)
            .stream()
            .map(post -> {
                PostResponse r = toResponse(post);
                r.setDistanceMeters(haversineMeters(lat, lon, post.getLat(), post.getLon()));
                return r;
            })
            .toList();
        return NearbyResponse.builder()
            .maxRadiusKm(maxRadiusKm)
            .premium(premium)
            .posts(posts)
            .build();
    }

    /**
     * Create a new map post, enforce the rate limit, and notify nearby users.
     *
     * @param request post coordinates, type, and optional description
     * @param user    authenticated post author
     * @return created post
     * @throws ResponseStatusException 429 if the per-hour rate limit is exceeded
     */
    @Transactional
    public PostResponse createPost(PostRequest request, User user) {
        checkRateLimit(user);

        PostType type = request.getPostType() != null ? request.getPostType() : PostType.DPS_POST;
        LocalDateTime expiresAt = computeExpiresAt(type, LocalDateTime.now());

        DpsPost post = DpsPost.builder()
            .postType(type)
            .lat(request.getLat())
            .lon(request.getLon())
            .description(request.getDescription())
            .addedBy(user)
            .expiresAt(expiresAt)
            .patrolSpeedKmh(type == PostType.PATROL_CAR ? request.getPatrolSpeedKmh() : null)
            .build();

        post = postRepository.save(post);
        logHistory(post, "created", user, Map.of("type", type.name(), "lat", request.getLat(), "lon", request.getLon()));
        log.info("Post created: type={} id={} userId={}", type, post.getId(), user.getId());

        notifyNearbyUsers(post);
        return toResponse(post);
    }

    /**
     * Deactivate (soft-delete) a post. Only the post's author may delete their own posts.
     *
     * @param postId post ID
     * @param user   authenticated user
     * @throws ResponseStatusException 403 if the user is not the post author, 404 if not found
     */
    @Transactional
    public void deletePost(Long postId, User user) {
        DpsPost post = getActivePostOrThrow(postId);
        if (!post.getAddedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this post");
        }
        post.setIsActive(false);
        postRepository.save(post);
        logHistory(post, "deactivated", user, Map.of("reason", "deleted_by_owner"));
    }

    /**
     * Cast a vote on a post and recalculate its confidence score.
     *
     * @param postId  post ID
     * @param request vote value ({@code +1} or {@code -1})
     * @param user    authenticated voter
     * @return updated post with recalculated confidence
     * @throws ResponseStatusException 409 if the user has already voted, 404 if post not found
     */
    @Transactional
    public PostResponse vote(Long postId, VoteRequest request, User user) {
        short voteValue = request.getVote();
        if (voteValue != 1 && voteValue != -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vote must be +1 or -1");
        }

        DpsPost post = getActivePostOrThrow(postId);

        if (voteRepository.existsByPostIdAndUserId(postId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already voted on this post");
        }

        voteRepository.save(PostVote.builder().post(post).user(user).vote(voteValue).build());

        post.setConfidence(voteRepository.sumVotesByPostId(postId));
        post.setConfirmedCount(voteRepository.countConfirmedByPostId(postId));
        postRepository.save(post);

        // Update reputation of the post author
        if (post.getAddedBy() != null) {
            User author = post.getAddedBy();
            author.setReputationScore(author.getReputationScore() + (voteValue > 0 ? 1 : -1));
            userRepository.save(author);
        }

        logHistory(post, voteValue > 0 ? "confirmed" : "voted_fake", user, Map.of("vote", voteValue));
        return toResponse(post);
    }

    /**
     * Auto-confirm all active posts within {@code radiusMeters} that the user has not yet voted on.
     * Called by the live tracking service when a location update is received.
     *
     * @param user         the live-tracking user
     * @param lat          current latitude
     * @param lon          current longitude
     * @param radiusMeters auto-confirm radius in metres
     */
    @Transactional
    public void autoConfirmNearby(User user, double lat, double lon, double radiusMeters) {
        postRepository.findNearby(lat, lon, radiusMeters).forEach(post -> {
            if (!voteRepository.existsByPostIdAndUserId(post.getId(), user.getId())) {
                voteRepository.save(PostVote.builder().post(post).user(user).vote((short) 1).build());
                post.setConfidence(voteRepository.sumVotesByPostId(post.getId()));
                post.setConfirmedCount(voteRepository.countConfirmedByPostId(post.getId()));
                postRepository.save(post);
                if (post.getAddedBy() != null) {
                    User author = post.getAddedBy();
                    author.setReputationScore(author.getReputationScore() + 1);
                    userRepository.save(author);
                }
                log.debug("Auto-confirmed: postId={} userId={}", post.getId(), user.getId());
            }
        });
    }

    /**
     * Return all posts created by the given user, newest first.
     *
     * @param user authenticated user
     * @return list of the user's posts
     */
    @Transactional(readOnly = true)
    public List<PostResponse> getUserPosts(User user) {
        return postRepository.findByUserId(user.getId()).stream().map(this::toResponse).toList();
    }

    /** Scheduled task: deactivate posts whose {@code expiresAt} is in the past. Runs every 5 minutes. */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void deactivateExpiredPosts() {
        int count = postRepository.deactivateExpired(LocalDateTime.now());
        if (count > 0) log.info("Deactivated {} expired posts", count);
    }

    /** Scheduled task: deactivate posts whose confidence has dropped below the threshold. Runs every 6 hours. */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000)
    @Transactional
    public void deactivateLowRatedPosts() {
        int count = postRepository.deactivateByLowConfidence(LOW_CONFIDENCE_THRESHOLD);
        if (count > 0) log.info("Deactivated {} low-confidence posts", count);
    }

    /** Scheduled task: deactivate unconfirmed posts older than 24 hours. Runs every 6 hours (offset). */
    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000 + 300_000)
    @Transactional
    public void deactivateStaleUnconfirmedPosts() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        int count = postRepository.deactivateStaleUnconfirmed(threshold);
        if (count > 0) log.info("Deactivated {} stale unconfirmed posts", count);
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private void notifyNearbyUsers(DpsPost post) {
        try {
            if (post.getAddedBy() == null) return;
            List<User> nearby = userRepository.findUsersNearPost(
                post.getLat(), post.getLon(), post.getAddedBy().getTgId());
            if (!nearby.isEmpty()) {
                notificationService.notifyUsersAboutNewPost(nearby, post, post.getPostType().name());
            }
        } catch (Exception e) {
            log.warn("Notification dispatch error: {}", e.getMessage());
        }
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371_000;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLam = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                 + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLam / 2) * Math.sin(dLam / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private LocalDateTime computeExpiresAt(PostType type, LocalDateTime now) {
        return switch (type) {
            case DPS_POST, AMBUSH, HIDDEN_POST -> now.plusHours(12);
            case PATROL_CAR -> now.plusMinutes(30);
            case CAMERA -> null;
        };
    }

    private void checkRateLimit(User user) {
        int limit = getIntSetting("posts.rate.limit", botConfig.getPostsPerHour());
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCount = postRepository.countRecentByUser(user.getId(), oneHourAgo);
        if (recentCount >= limit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded: no more than " + limit + " posts per hour");
        }
    }

    private int getIntSetting(String key, int fallback) {
        return appSettingsRepository.findById(key)
            .map(s -> { try { return Integer.parseInt(s.getValue()); } catch (NumberFormatException e) { return fallback; } })
            .orElse(fallback);
    }

    private double getDoubleSetting(String key, double fallback) {
        return appSettingsRepository.findById(key)
            .map(s -> { try { return Double.parseDouble(s.getValue()); } catch (NumberFormatException e) { return fallback; } })
            .orElse(fallback);
    }

    private DpsPost getActivePostOrThrow(Long postId) {
        return postRepository.findById(postId)
            .filter(DpsPost::getIsActive)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private void logHistory(DpsPost post, String action, User user, Map<String, Object> meta) {
        postHistoryRepository.save(PostHistory.builder()
            .post(post).action(action).performedBy(user).meta(meta).build());
    }

    private PostResponse toResponse(DpsPost post) {
        return PostResponse.builder()
            .id(post.getId())
            .postType(post.getPostType())
            .lat(post.getLat())
            .lon(post.getLon())
            .description(post.getDescription())
            .addedByUsername(post.getAddedBy() != null ? post.getAddedBy().getUsername() : null)
            .isActive(post.getIsActive())
            .confidence(post.getConfidence())
            .confirmedCount(post.getConfirmedCount())
            .patrolSpeedKmh(post.getPatrolSpeedKmh())
            .expiresAt(post.getExpiresAt())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
}
