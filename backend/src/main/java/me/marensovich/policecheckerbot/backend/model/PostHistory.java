package me.marensovich.policecheckerbot.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * JPA entity for the audit history of actions performed on a DPS post.
 *
 * <p>Every significant event on a post creates one record. The {@code meta} JSONB
 * column carries event-specific payload (e.g. the vote value, deactivation reason).
 *
 * <p>Known action types:
 * <ul>
 *   <li>{@code created} — post was created</li>
 *   <li>{@code confirmed} — post was confirmed by a vote</li>
 *   <li>{@code voted_fake} — post was flagged as fake</li>
 *   <li>{@code commented} — a comment was added</li>
 *   <li>{@code deactivated} — post was deactivated (by admin, low confidence, or expiry)</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "post_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private DpsPost post;

    /** Action type string (e.g. {@code "created"}, {@code "confirmed"}, {@code "deactivated"}). */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    /** Optional JSONB metadata payload for the event (vote value, reason, etc.). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb")
    private Map<String, Object> meta;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
