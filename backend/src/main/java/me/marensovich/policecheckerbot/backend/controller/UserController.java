package me.marensovich.policecheckerbot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.dto.LiveTrackingRequest;
import me.marensovich.policecheckerbot.backend.dto.NotifyPrefsRequest;
import me.marensovich.policecheckerbot.backend.dto.PostResponse;
import me.marensovich.policecheckerbot.backend.dto.UserResponse;
import me.marensovich.policecheckerbot.backend.dto.UserSettingsRequest;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.LiveTrackingService;
import me.marensovich.policecheckerbot.backend.service.PostService;
import me.marensovich.policecheckerbot.backend.service.SubscriptionService;
import me.marensovich.policecheckerbot.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the current user's profile, settings, and live-tracking toggle.
 *
 * <p>All endpoints are scoped to the authenticated caller ({@code /api/users/me/**}).
 * Banned users may only access this controller — other endpoints reject them via
 * {@link me.marensovich.policecheckerbot.backend.security.SessionFilter}.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /api/users/me}              — fetch profile</li>
 *   <li>{@code PUT  /api/users/me/settings}      — update notification radius</li>
 *   <li>{@code PUT  /api/users/me/notify-prefs}  — update post-type notification filter</li>
 *   <li>{@code POST /api/users/me/live}          — toggle live tracking (Premium only)</li>
 *   <li>{@code GET  /api/users/me/posts}         — list own posts</li>
 *   <li>{@code POST /api/users/me/subscribe}     — request a Telegram Stars invoice</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final LiveTrackingService liveTrackingService;
    private final PostService postService;
    private final SubscriptionService subscriptionService;

    /**
     * Return the authenticated user's full profile.
     *
     * @param user authenticated user
     * @return user profile including subscription state and settings
     */
    @GetMapping
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }

    /**
     * Update the authenticated user's notification radius.
     *
     * @param request new settings (notification radius 1–50 km)
     * @param user    authenticated user
     * @return updated profile
     */
    @PutMapping("/settings")
    public ResponseEntity<UserResponse> updateSettings(
        @Valid @RequestBody UserSettingsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(userService.updateSettings(user, request));
    }

    /**
     * Update the authenticated user's post-type notification filter.
     *
     * @param request comma-separated list of post types, or empty string for all types
     * @param user    authenticated user
     * @return updated profile
     */
    @PutMapping("/notify-prefs")
    public ResponseEntity<UserResponse> updateNotifyPrefs(
        @RequestBody NotifyPrefsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(userService.updateNotifyPrefs(user, request));
    }

    /**
     * Enable or disable live location tracking for the authenticated user.
     * Requires an active Premium subscription.
     *
     * @param request {@code enabled} flag
     * @param user    authenticated user
     */
    @PostMapping("/live")
    public ResponseEntity<Void> toggleLiveTracking(
        @Valid @RequestBody LiveTrackingRequest request,
        @AuthenticationPrincipal User user
    ) {
        liveTrackingService.setLiveTracking(user, request.getEnabled());
        return ResponseEntity.ok().build();
    }

    /**
     * Return all posts created by the authenticated user, newest first.
     *
     * @param user authenticated user
     * @return list of the user's posts
     */
    @GetMapping("/posts")
    public ResponseEntity<List<PostResponse>> getMyPosts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(postService.getUserPosts(user));
    }

    /**
     * Request a Telegram Stars payment invoice for a Premium subscription.
     * The bot will send the invoice directly to the user's Telegram chat.
     *
     * @param user authenticated user
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Void> requestSubscriptionInvoice(@AuthenticationPrincipal User user) {
        subscriptionService.sendInvoice(user);
        return ResponseEntity.ok().build();
    }
}
