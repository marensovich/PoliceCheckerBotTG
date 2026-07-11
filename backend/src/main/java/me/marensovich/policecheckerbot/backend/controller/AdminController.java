package me.marensovich.policecheckerbot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.dto.*;
import me.marensovich.policecheckerbot.backend.model.AppSettings;
import me.marensovich.policecheckerbot.backend.model.Role;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.AdminService;
import me.marensovich.policecheckerbot.backend.service.PromoCodeService;
import me.marensovich.policecheckerbot.backend.service.TicketService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the DPS Tracker admin panel.
 *
 * <p>All endpoints under {@code /api/admin/**} require the {@code ADMIN} role,
 * enforced by the {@link #requireAdmin(User)} guard. Sections:
 * <ul>
 *   <li><b>Stats</b>: aggregate counts and activity chart data</li>
 *   <li><b>Users</b>: search, ban/unban, subscription management, role changes, moderators</li>
 *   <li><b>Posts</b>: paginated list, moderation queue, force-deactivate</li>
 *   <li><b>Broadcast</b>: send Telegram messages to ALL / PREMIUM / FREE users</li>
 *   <li><b>Settings</b>: read and update application-wide key-value settings</li>
 *   <li><b>Logs</b>: paginated admin action audit log</li>
 *   <li><b>Promo codes</b>: create, list, deactivate, delete</li>
 *   <li><b>Support tickets</b>: list with status filter, reply, close</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PromoCodeService promoCodeService;
    private final TicketService ticketService;

    // ── Statistics ───────────────────────────────────────────────────────────────

    /** Return aggregate system statistics (user counts, post counts). */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats(@AuthenticationPrincipal User user) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.getStats());
    }

    /**
     * Return per-day post and user counts for the last {@code days} days (max 90).
     *
     * @param days number of days to include in the chart (default 14)
     */
    @GetMapping("/stats/activity")
    public ResponseEntity<List<ActivityDataPoint>> getActivity(
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "14") int days
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.getActivity(Math.min(days, 90)));
    }

    // ── Users ─────────────────────────────────────────────────────────────────────

    /**
     * Return a paginated list of all users, with optional search and role/status filters.
     *
     * <p>Filters can be combined freely. When {@code search} is set it takes precedence
     * over the role/banned/subscribed filters. All filter params are optional.
     *
     * @param search     case-insensitive substring match against username / Telegram ID
     * @param role       restrict to a single role: {@code USER}, {@code MODERATOR}, or {@code ADMIN}
     * @param banned     {@code true} = banned only, {@code false} = non-banned only, omit = both
     * @param subscribed {@code true} = Premium only, {@code false} = free only, omit = both
     */
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Role role,
        @RequestParam(required = false) Boolean banned,
        @RequestParam(required = false) Boolean subscribed,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        requireAdmin(user);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AdminUserResponse> result;
        if (search != null && !search.isBlank()) {
            result = adminService.searchUsers(search.trim(), pageable);
        } else if (role != null || banned != null || subscribed != null) {
            result = adminService.filterUsers(role, banned, subscribed, pageable);
        } else {
            result = adminService.getUsers(pageable);
        }
        return ResponseEntity.ok(result);
    }

    /** Toggle the ban status of a user. Returns the updated user profile. */
    @PutMapping("/users/{id}/ban")
    public ResponseEntity<AdminUserResponse> toggleBan(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.toggleBan(id, user));
    }

    /** Grant a permanent Premium subscription to a user. */
    @PutMapping("/users/{id}/subscribe")
    public ResponseEntity<AdminUserResponse> forceSubscribe(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.forceSubscribe(id, user));
    }

    /** Revoke a user's Premium subscription immediately. */
    @PutMapping("/users/{id}/revoke-subscription")
    public ResponseEntity<AdminUserResponse> revokeSubscription(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.revokeSubscription(id, user));
    }

    /** Grant a trial Premium period of the specified number of days. */
    @PostMapping("/users/{id}/trial")
    public ResponseEntity<AdminUserResponse> grantTrial(
        @PathVariable Long id,
        @Valid @RequestBody TrialRequest request,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.grantTrial(id, request.getDays(), user));
    }

    /** Change a user's role (USER / MODERATOR / ADMIN). */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<AdminUserResponse> changeRole(
        @PathVariable Long id,
        @Valid @RequestBody RoleUpdateRequest request,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.changeRole(id, request.getRole(), user));
    }

    /** Promote a user to MODERATOR and assign them to the specified region. */
    @PostMapping("/users/{id}/moderator")
    public ResponseEntity<AdminUserResponse> assignModerator(
        @PathVariable Long id,
        @Valid @RequestBody ModeratorRequest request,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.assignModerator(id, request.getRegion(), user));
    }

    /** Revoke the moderator role from a user and clear their assigned region. */
    @DeleteMapping("/users/{id}/moderator")
    public ResponseEntity<AdminUserResponse> revokeModerator(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.revokeModerator(id, user));
    }

    // ── Posts ─────────────────────────────────────────────────────────────────────

    /** Return a paginated list of all posts (active and inactive), newest first. */
    @GetMapping("/posts")
    public ResponseEntity<Page<PostResponse>> getPosts(
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        requireAdmin(user);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.getPosts(pageable));
    }

    /** Return a paginated moderation queue of low-confidence active posts. */
    @GetMapping("/posts/moderation")
    public ResponseEntity<Page<PostResponse>> getModerationQueue(
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        requireAdmin(user);
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getModerationQueue(pageable));
    }

    /** Force-deactivate a post regardless of its current status. */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deactivatePost(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        adminService.forceDeactivatePost(id, user);
        return ResponseEntity.noContent().build();
    }

    // ── Broadcast ─────────────────────────────────────────────────────────────────

    /**
     * Broadcast a Telegram message to a group of users.
     *
     * @return {@code {"sent": N}} where N is the number of messages dispatched
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcast(
        @Valid @RequestBody BroadcastRequest request,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        int count = adminService.broadcast(request, user);
        return ResponseEntity.ok(Map.of("sent", count));
    }

    // ── Settings ──────────────────────────────────────────────────────────────────

    /** Return all application-wide key-value settings. */
    @GetMapping("/settings")
    public ResponseEntity<List<AppSettings>> getSettings(@AuthenticationPrincipal User user) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.getAllSettings());
    }

    /** Update the value of a single application setting by key. */
    @PutMapping("/settings/{key}")
    public ResponseEntity<AppSettings> updateSetting(
        @PathVariable String key,
        @Valid @RequestBody SettingUpdateRequest request,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.updateSetting(key, request.getValue(), user));
    }

    // ── Audit log ─────────────────────────────────────────────────────────────────

    /** Return a paginated admin action audit log, newest entries first. */
    @GetMapping("/logs")
    public ResponseEntity<Page<AdminLogResponse>> getLogs(
        @AuthenticationPrincipal User user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(adminService.getLogs(PageRequest.of(page, size)));
    }

    // ── Promo codes ───────────────────────────────────────────────────────────────

    /** Return all promo codes (active and inactive). */
    @GetMapping("/promo")
    public ResponseEntity<List<PromoCodeResponse>> getPromos(@AuthenticationPrincipal User user) {
        requireAdmin(user);
        return ResponseEntity.ok(promoCodeService.getAllPromos());
    }

    /** Create a new promo code. Returns 201 Created on success. */
    @PostMapping("/promo")
    public ResponseEntity<PromoCodeResponse> createPromo(
        @Valid @RequestBody PromoCodeRequest request,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(promoCodeService.createPromo(request));
    }

    /** Deactivate a promo code so it can no longer be applied by users. */
    @PutMapping("/promo/{id}/deactivate")
    public ResponseEntity<PromoCodeResponse> deactivatePromo(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(promoCodeService.deactivatePromo(id));
    }

    /** Permanently delete a promo code record. */
    @DeleteMapping("/promo/{id}")
    public ResponseEntity<Void> deletePromo(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        promoCodeService.deletePromo(id);
        return ResponseEntity.noContent().build();
    }

    // ── Support tickets ───────────────────────────────────────────────────────────

    /**
     * Return a paginated list of support tickets, optionally filtered by {@code status}
     * ({@code OPEN}, {@code IN_PROGRESS}, {@code CLOSED}).
     */
    @GetMapping("/tickets")
    public ResponseEntity<Page<TicketResponse>> getTickets(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(ticketService.adminGetTickets(status, PageRequest.of(page, size)));
    }

    /** Post an admin reply to a support ticket and optionally update its status. */
    @PostMapping("/tickets/{id}/reply")
    public ResponseEntity<TicketResponse> replyTicket(
        @PathVariable Long id,
        @Valid @RequestBody TicketReplyRequest request,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(ticketService.adminReply(id, request, user));
    }

    /** Close a support ticket. */
    @PutMapping("/tickets/{id}/close")
    public ResponseEntity<TicketResponse> closeTicket(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        return ResponseEntity.ok(ticketService.adminClose(id, user));
    }

    // ── Camera import ─────────────────────────────────────────────────────────────

    /**
     * Bulk-import speed cameras from OpenStreetMap via the Overpass API.
     *
     * <p>The backend fetches {@code highway=speed_camera} nodes within the supplied
     * bounding box and creates {@code CAMERA} posts for any that are not already
     * present within a 30 m deduplication radius. Safe to call multiple times.
     *
     * @param request bounding box coordinates (south/west/north/east)
     * @return import summary: how many were created, skipped, and found in total
     */
    @PostMapping("/cameras/import")
    public ResponseEntity<?> importCameras(
        @Valid @RequestBody CameraImportRequest request,
        @AuthenticationPrincipal User user
    ) {
        requireAdmin(user);
        try {
            CameraImportResponse result = adminService.importCamerasFromOSM(
                request.getSouth(), request.getWest(), request.getNorth(), request.getEast(), user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Overpass API error: " + e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Throw 403 Forbidden if the authenticated user does not hold the ADMIN role. */
    private void requireAdmin(User user) {
        if (user == null || user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access restricted to administrators");
        }
    }
}
