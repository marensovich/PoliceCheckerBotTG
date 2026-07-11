package me.marensovich.policecheckerbot.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity for the administrator action audit log.
 *
 * <p>Written whenever an admin performs a privileged action: banning a user,
 * changing roles, force-subscribing, deactivating posts, broadcasting, etc.
 * Readable via the admin panel under the Logs section.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "admin_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    /** Action type string (e.g. {@code "ban"}, {@code "unban"}, {@code "subscribe"}, {@code "delete_post"}). */
    @Column(name = "action", length = 50, nullable = false)
    private String action;

    /** Entity type that was affected: {@code "USER"} or {@code "POST"}. */
    @Column(name = "target_type", length = 20)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "details", length = 500)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
