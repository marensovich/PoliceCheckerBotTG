package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for casting a vote on a DPS post.
 *
 * <p>A user may cast exactly one vote per post; subsequent calls replace the existing vote.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class VoteRequest {

    /**
     * Vote value: {@code 1} to confirm the post as genuine, {@code -1} to flag it as fake.
     */
    @NotNull(message = "Vote value is required")
    private Short vote;
}
