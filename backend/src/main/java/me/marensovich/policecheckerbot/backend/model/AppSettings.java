package me.marensovich.policecheckerbot.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity for application-wide configuration settings managed via the admin panel.
 *
 * <p>Settings are stored as string key-value pairs (e.g. {@code free_radius_km=2},
 * {@code premium_radius_km=10}). The consuming service parses values to the
 * appropriate type at runtime. All changes are audited through the admin log.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettings {

    @Id
    @Column(name = "key", length = 100)
    private String key;

    @Column(name = "value", length = 500, nullable = false)
    private String value;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    private void touch() { updatedAt = LocalDateTime.now(); }
}
