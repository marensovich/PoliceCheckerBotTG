package me.marensovich.policecheckerbot.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity recording that a user has redeemed a specific promo code.
 *
 * <p>The unique constraint on {@code (promo_code_id, user_id)} prevents a user
 * from applying the same code more than once at the database level.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "promo_code_usages", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"promo_code_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promo_code_id", nullable = false)
    private PromoCode promoCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "used_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime usedAt = LocalDateTime.now();
}
