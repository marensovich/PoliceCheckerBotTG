package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.PostVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for post vote (confidence) persistence and aggregation.
 *
 * <p>Each user may cast exactly one vote per post ({@code +1} to confirm, {@code -1} to mark as
 * fake). Aggregated vote sums drive the post's {@code confidence} field and automatic deactivation.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface VoteRepository extends JpaRepository<PostVote, Long> {

    /**
     * Find the vote cast by a specific user on a specific post.
     *
     * @param postId the post's internal ID
     * @param userId the voter's internal user ID
     * @return an {@link Optional} containing the existing vote, or empty if the user has not voted
     */
    Optional<PostVote> findByPostIdAndUserId(Long postId, Long userId);

    /**
     * Check whether a user has already voted on a post.
     *
     * @param postId the post's internal ID
     * @param userId the voter's internal user ID
     * @return {@code true} if a vote record exists
     */
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    /**
     * Return the sum of all vote values for a post (the net confidence score).
     * The result can be negative if downvotes exceed upvotes.
     *
     * @param postId the post's internal ID
     * @return sum of vote values, or {@code 0} if no votes exist
     */
    @Query("SELECT COALESCE(SUM(v.vote), 0) FROM PostVote v WHERE v.post.id = :postId")
    int sumVotesByPostId(@Param("postId") Long postId);

    /**
     * Count the number of positive votes ({@code vote > 0}) on a post.
     * Represents how many users have confirmed the post as genuine.
     *
     * @param postId the post's internal ID
     * @return number of confirmation votes
     */
    @Query("SELECT COUNT(v) FROM PostVote v WHERE v.post.id = :postId AND v.vote > 0")
    int countConfirmedByPostId(@Param("postId") Long postId);
}
