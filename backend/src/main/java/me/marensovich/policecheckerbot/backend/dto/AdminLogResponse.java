package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for an administrator action log entry.
 *
 * <p>Returned when paginating the admin action log ({@code GET /api/admin/logs}).
 * Each entry records who performed the action, what it was, when it happened,
 * and which entity was affected.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLogResponse {
    private Long id;
    private String adminUsername;
    private String action;
    private String targetType;
    private Long targetId;
    private String details;
    private LocalDateTime createdAt;
}
