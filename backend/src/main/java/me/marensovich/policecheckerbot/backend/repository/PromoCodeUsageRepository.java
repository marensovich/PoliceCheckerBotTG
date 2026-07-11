package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.PromoCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for tracking promo code redemption history.
 *
 * <p>Each record links a user to a promo code they have applied.
 * The existence check enforces the one-use-per-user constraint at the repository level.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {

    /**
     * Check whether a specific user has already redeemed a specific promo code.
     *
     * @param promoCodeId the promo code's internal ID
     * @param userId      the user's internal ID
     * @return {@code true} if the user has already used this code
     */
    boolean existsByPromoCodeIdAndUserId(Long promoCodeId, Long userId);
}
