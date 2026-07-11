package me.marensovich.policecheckerbot.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a user's vote on a DPS post (confidence rating).
 *
 * <p>Each user may cast exactly one vote per post. The database enforces this with a
 * {@code UNIQUE(post_id, user_id)} constraint. Subsequent votes from the same user
 * replace the existing record rather than creating a new one.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(
    name = "post_votes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private DpsPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Vote value: {@code +1} to confirm the post as genuine, {@code -1} to flag it as fake.
     * Allowed values are enforced at the database level via a {@code CHECK} constraint.
     */
    @Column(name = "vote", nullable = false)
    private Short vote;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
