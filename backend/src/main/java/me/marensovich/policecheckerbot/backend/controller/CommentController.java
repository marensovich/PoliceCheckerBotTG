package me.marensovich.policecheckerbot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.dto.CommentRequest;
import me.marensovich.policecheckerbot.backend.dto.CommentResponse;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for comments on DPS posts.
 *
 * <p>Endpoints are nested under the parent post resource at
 * {@code /api/posts/{postId}/comments}. Listing comments is public;
 * adding a comment requires authentication.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * Return a paginated list of comments for a post, newest first.
     *
     * @param postId post ID
     * @param page   zero-based page number (default 0)
     * @return list of comments on the requested page
     */
    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
        @PathVariable Long postId,
        @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(commentService.getComments(postId, page));
    }

    /**
     * Add a comment to a post. Returns 201 Created.
     *
     * @param postId  post ID
     * @param request comment text (max 300 characters)
     * @param user    authenticated user
     * @return the newly created comment
     */
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
        @PathVariable Long postId,
        @Valid @RequestBody CommentRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(commentService.addComment(postId, request, user));
    }
}
