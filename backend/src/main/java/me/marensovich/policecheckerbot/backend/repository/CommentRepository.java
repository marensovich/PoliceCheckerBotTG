package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.PostComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for post comment persistence and retrieval.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface CommentRepository extends JpaRepository<PostComment, Long> {

    /**
     * Return a paginated list of comments for a post, ordered by creation date descending
     * (newest first).
     *
     * @param postId   the post's internal ID
     * @param pageable pagination parameters
     * @return page of comments
     */
    @Query("SELECT c FROM PostComment c WHERE c.post.id = :postId ORDER BY c.createdAt DESC")
    List<PostComment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    /**
     * Count the total number of comments on a post.
     *
     * @param postId the post's internal ID
     * @return comment count
     */
    long countByPostId(Long postId);
}
