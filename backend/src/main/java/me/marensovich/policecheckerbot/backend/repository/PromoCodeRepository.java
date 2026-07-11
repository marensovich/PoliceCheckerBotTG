package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for promo code persistence and case-insensitive lookup.
 *
 * <p>Promo codes are unique identifiers that grant either Premium days ({@code VIP_DAYS})
 * or a purchase discount ({@code DISCOUNT_PERCENT}) when applied by a user.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    /**
     * Find a promo code by its code string, ignoring case differences.
     *
     * @param code the promo code string (e.g. {@code "SUMMER25"})
     * @return an {@link Optional} containing the matching code, or empty if not found
     */
    Optional<PromoCode> findByCodeIgnoreCase(String code);
}
