package me.marensovich.policecheckerbot.backend.service;

import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.dto.CommentRequest;
import me.marensovich.policecheckerbot.backend.dto.CommentResponse;
import me.marensovich.policecheckerbot.backend.model.DpsPost;
import me.marensovich.policecheckerbot.backend.model.PostComment;
import me.marensovich.policecheckerbot.backend.model.PostHistory;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.repository.CommentRepository;
import me.marensovich.policecheckerbot.backend.repository.PostHistoryRepository;
import me.marensovich.policecheckerbot.backend.repository.PostRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Service for managing comments on DPS map posts.
 *
 * <p>Provides paginated comment retrieval and comment creation.
 * Every new comment is also recorded in the post's history log.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final PostHistoryRepository postHistoryRepository;

    /**
     * Return a paginated list of comments for a post, newest first.
     *
     * @param postId post ID
     * @param page   zero-based page number
     * @return list of comments on the requested page
     * @throws ResponseStatusException 404 if the post does not exist
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId, int page) {
        getPostOrThrow(postId);
        return commentRepository.findByPostId(postId, PageRequest.of(page, DEFAULT_PAGE_SIZE))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Add a comment to a post and write the event to the post history log.
     *
     * @param postId  post ID
     * @param request comment text (max 300 chars)
     * @param user    authenticated comment author
     * @return the newly created comment
     * @throws ResponseStatusException 404 if the post does not exist
     */
    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request, User user) {
        DpsPost post = getPostOrThrow(postId);

        PostComment comment = PostComment.builder()
            .post(post)
            .user(user)
            .text(request.getText())
            .build();
        comment = commentRepository.save(comment);

        postHistoryRepository.save(PostHistory.builder()
            .post(post)
            .action("commented")
            .performedBy(user)
            .meta(Map.of("commentId", comment.getId(), "text", request.getText()))
            .build());

        return toResponse(comment);
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private DpsPost getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private CommentResponse toResponse(PostComment comment) {
        return CommentResponse.builder()
            .id(comment.getId())
            .postId(comment.getPost().getId())
            .username(comment.getUser() != null ? comment.getUser().getUsername() : "anonymous")
            .text(comment.getText())
            .createdAt(comment.getCreatedAt())
            .build();
    }
}
