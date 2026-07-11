package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.marensovich.policecheckerbot.backend.model.PromoType;

import java.time.LocalDateTime;

/**
 * Response DTO containing promo code details.
 *
 * <p>Returned when listing, creating, or deactivating promo codes via the admin API.
 * {@code usedCount} reflects how many users have already redeemed this code.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoCodeResponse {
    private Long id;
    private String code;
    private PromoType type;
    private Integer value;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
