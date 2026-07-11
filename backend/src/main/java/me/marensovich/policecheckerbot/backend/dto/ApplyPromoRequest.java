package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for applying a promo code to the current user's account.
 *
 * <p>The code lookup is case-insensitive. Each code may only be applied once per user.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ApplyPromoRequest {
    @NotBlank
    private String code;
}
