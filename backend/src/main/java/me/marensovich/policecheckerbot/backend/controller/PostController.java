package me.marensovich.policecheckerbot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.dto.NearbyResponse;
import me.marensovich.policecheckerbot.backend.dto.PostRequest;
import me.marensovich.policecheckerbot.backend.dto.PostResponse;
import me.marensovich.policecheckerbot.backend.dto.VoteRequest;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.repository.PostHistoryRepository;
import me.marensovich.policecheckerbot.backend.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for DPS map post management.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET  /api/posts?lat=&lon=&radius=} — public: nearby posts (no auth required)</li>
 *   <li>{@code GET  /api/posts/nearby?lat=&lon=}  — authenticated: radius capped by subscription tier</li>
 *   <li>{@code POST /api/posts}                   — create a new post (auth required)</li>
 *   <li>{@code DELETE /api/posts/{id}}             — delete own post</li>
 *   <li>{@code POST /api/posts/{id}/vote}          — cast a vote (+1 confirm / -1 fake)</li>
 *   <li>{@code GET  /api/posts/{id}}               — get a single post by ID (deep-link support)</li>
 *   <li>{@code GET  /api/posts/{id}/history}       — get the audit history of a post</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostHistoryRepository postHistoryRepository;

    /**
     * Return active DPS posts within a geographic radius. No authentication required.
     *
     * @param lat      latitude of the search center
     * @param lon      longitude of the search center
     * @param radiusKm search radius in kilometres (default 5)
     * @return list of posts sorted by distance
     */
    @GetMapping
    public ResponseEntity<List<PostResponse>> getNearby(
        @RequestParam Double lat,
        @RequestParam Double lon,
        @RequestParam(defaultValue = "5") Double radiusKm
    ) {
        return ResponseEntity.ok(postService.findNearby(lat, lon, radiusKm));
    }

    /**
     * Return nearby posts with the caller's subscription-based radius limit applied.
     *
     * <p>Free users: 2 km max. Premium users: 10 km max.
     * Distance from the query point is included in each post response.
     *
     * @param lat  latitude of the caller
     * @param lon  longitude of the caller
     * @param user authenticated user
     * @return posts within the applicable radius plus metadata about the limit
     */
    @GetMapping("/nearby")
    public ResponseEntity<NearbyResponse> getNearbyAuthenticated(
        @RequestParam Double lat,
        @RequestParam Double lon,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(postService.findNearbyAuthenticated(lat, lon, user));
    }

    /**
     * Create a new DPS map post. Returns 201 Created.
     *
     * @param request post coordinates, type, and optional description
     * @param user    authenticated user
     * @return the created post
     */
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
        @Valid @RequestBody PostRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(postService.createPost(request, user));
    }

    /**
     * Delete (deactivate) the caller's own post. Returns 204 No Content.
     *
     * @param id   post ID
     * @param user authenticated user (must be the post author)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        postService.deletePost(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cast a vote on a post ({@code +1} to confirm, {@code -1} to flag as fake).
     *
     * @param id      post ID
     * @param request vote value
     * @param user    authenticated user
     * @return updated post with recalculated confidence score
     */
    @PostMapping("/{id}/vote")
    public ResponseEntity<PostResponse> vote(
        @PathVariable Long id,
        @Valid @RequestBody VoteRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(postService.vote(id, request, user));
    }

    /**
     * Get a single post by ID, used for deep-link navigation from Telegram messages.
     *
     * @param id post ID
     * @return post or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.findById(id));
    }

    /**
     * Get the full audit history of a post (votes, deactivations, etc.), newest first.
     *
     * @param id post ID
     * @return list of history records
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(postHistoryRepository.findByPostId(id));
    }
}
