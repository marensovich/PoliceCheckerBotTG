package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.PostHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for the audit history of actions performed on DPS posts.
 *
 * <p>History entries are written whenever a post is voted on, deactivated by an admin,
 * or auto-expired. Useful for admin review and post lifecycle reconstruction.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface PostHistoryRepository extends JpaRepository<PostHistory, Long> {

    /**
     * Return the full action history for a post, ordered by event time descending.
     *
     * @param postId the post's internal ID
     * @return list of history records, newest first
     */
    @Query("SELECT h FROM PostHistory h WHERE h.post.id = :postId ORDER BY h.createdAt DESC")
    List<PostHistory> findByPostId(@Param("postId") Long postId);
}
