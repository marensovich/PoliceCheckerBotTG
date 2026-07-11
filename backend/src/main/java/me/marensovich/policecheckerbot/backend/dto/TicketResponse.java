package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO representing a support ticket, including any admin reply.
 *
 * <p>Status follows the lifecycle: {@code OPEN} → {@code IN_PROGRESS} → {@code CLOSED}.
 * {@code adminReply} and {@code repliedByUsername} are {@code null} until an admin responds.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private Long id;
    private Long userId;
    private String username;
    private String firstName;
    private String subject;
    private String message;
    private String status;
    private String adminReply;
    private String repliedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
