package me.marensovich.policecheckerbot.backend.model;

/**
 * Type of benefit granted by a promo code.
 *
 * <ul>
 *   <li>{@link #VIP_DAYS} — adds the code's {@code value} as days of Premium access</li>
 *   <li>{@link #DISCOUNT_PERCENT} — stores a percentage discount applied to the next purchase</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
public enum PromoType {
    VIP_DAYS,
    DISCOUNT_PERCENT
}
