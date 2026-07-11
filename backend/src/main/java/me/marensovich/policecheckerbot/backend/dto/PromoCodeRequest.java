package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import me.marensovich.policecheckerbot.backend.model.PromoType;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a new promo code.
 *
 * <p>Supports two benefit types via {@link PromoType}:
 * {@code VIP_DAYS} grants Premium access for the specified number of days,
 * {@code DISCOUNT_PERCENT} grants a percentage discount on the next purchase.
 * Both {@code maxUses} and {@code expiresAt} are optional —
 * {@code null} means unlimited uses / no expiry.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class PromoCodeRequest {

    @NotBlank
    private String code;

    @NotNull
    private PromoType type;

    @NotNull
    @Min(1)
    private Integer value;

    /** Maximum number of uses allowed. {@code null} = unlimited. */
    private Integer maxUses;

    /** Expiry timestamp for this code. {@code null} = never expires. */
    private LocalDateTime expiresAt;
}
