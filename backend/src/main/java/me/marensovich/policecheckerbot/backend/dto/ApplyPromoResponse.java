package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned when a promo code is successfully applied.
 *
 * <p>Describes the benefit that was granted:
 * {@code type} is the promo type string (e.g. {@code "VIP_DAYS"} or {@code "DISCOUNT_PERCENT"}),
 * and {@code value} is the numeric amount (days added or discount percentage).
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplyPromoResponse {
    private String message;
    private String type;
    private Integer value;
}
