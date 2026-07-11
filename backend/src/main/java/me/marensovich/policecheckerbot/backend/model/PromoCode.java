package me.marensovich.policecheckerbot.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a promotional code.
 *
 * <p>Promo codes grant one of two benefit types (see {@link PromoType}):
 * {@code VIP_DAYS} adds {@code value} days of Premium access;
 * {@code DISCOUNT_PERCENT} applies a {@code value}% discount on the next purchase.
 * Optional {@code maxUses} and {@code expiresAt} fields enforce usage limits.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** Benefit type: {@code VIP_DAYS} grants Premium days; {@code DISCOUNT_PERCENT} grants a purchase discount. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PromoType type;

    /** Benefit amount: number of days or discount percentage, depending on {@code type}. */
    @Column(name = "value", nullable = false)
    private Integer value;

    /** Maximum number of redemptions allowed. {@code null} = unlimited. */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    /** Expiry timestamp after which the code cannot be applied. {@code null} = never expires. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
