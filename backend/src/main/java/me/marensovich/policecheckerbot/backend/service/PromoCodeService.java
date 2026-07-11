package me.marensovich.policecheckerbot.backend.service;

import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.dto.ApplyPromoResponse;
import me.marensovich.policecheckerbot.backend.dto.PromoCodeRequest;
import me.marensovich.policecheckerbot.backend.dto.PromoCodeResponse;
import me.marensovich.policecheckerbot.backend.model.PromoCode;
import me.marensovich.policecheckerbot.backend.model.PromoCodeUsage;
import me.marensovich.policecheckerbot.backend.model.PromoType;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.repository.PromoCodeRepository;
import me.marensovich.policecheckerbot.backend.repository.PromoCodeUsageRepository;
import me.marensovich.policecheckerbot.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for promo code management and redemption.
 *
 * <p>Handles the complete lifecycle: admin creation, user application,
 * usage tracking, and deactivation. Each code may be applied at most once per user
 * (enforced at both the service and database levels).
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository usageRepository;
    private final UserRepository userRepository;

    /**
     * Apply a promo code to the authenticated user's account.
     *
     * <p>Validates the code (active, not expired, uses not exhausted, not already used by this user),
     * applies the benefit ({@code VIP_DAYS} or {@code DISCOUNT_PERCENT}), and records the usage.
     *
     * @param rawCode promo code string (case-insensitive)
     * @param user    authenticated user redeeming the code
     * @return description of the benefit granted
     * @throws ResponseStatusException 404 if not found, 410 if inactive/expired/exhausted, 409 if already used
     */
    @Transactional
    public ApplyPromoResponse applyPromo(String rawCode, User user) {
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(rawCode.trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found"));

        if (!Boolean.TRUE.equals(promo.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Promo code is no longer active");
        }
        if (promo.getExpiresAt() != null && promo.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Promo code has expired");
        }
        if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Promo code has reached its usage limit");
        }
        if (usageRepository.existsByPromoCodeIdAndUserId(promo.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already used this promo code");
        }

        String resultMessage;
        if (promo.getType() == PromoType.VIP_DAYS) {
            LocalDateTime base = (user.getIsSubscribed() && user.getSubscriptionExpiresAt() != null
                && user.getSubscriptionExpiresAt().isAfter(LocalDateTime.now()))
                ? user.getSubscriptionExpiresAt()
                : LocalDateTime.now();
            user.setIsSubscribed(true);
            user.setSubscriptionExpiresAt(base.plusDays(promo.getValue()));
            resultMessage = "✅ Added " + promo.getValue() + " days of Premium";
        } else {
            user.setPromoDiscountPercent(promo.getValue());
            resultMessage = "✅ " + promo.getValue() + "% discount activated for your next purchase";
        }

        userRepository.save(user);

        promo.setUsedCount(promo.getUsedCount() + 1);
        promoCodeRepository.save(promo);

        usageRepository.save(PromoCodeUsage.builder()
            .promoCode(promo).user(user).build());

        return new ApplyPromoResponse(resultMessage, promo.getType().name(), promo.getValue());
    }

    /**
     * Create a new promo code. The code string is normalised to uppercase.
     *
     * @param req creation request
     * @return the created promo code
     * @throws ResponseStatusException 409 if a code with the same name already exists
     */
    @Transactional
    public PromoCodeResponse createPromo(PromoCodeRequest req) {
        if (promoCodeRepository.findByCodeIgnoreCase(req.getCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A promo code with that name already exists");
        }
        PromoCode promo = PromoCode.builder()
            .code(req.getCode().trim().toUpperCase())
            .type(req.getType())
            .value(req.getValue())
            .maxUses(req.getMaxUses())
            .expiresAt(req.getExpiresAt())
            .build();
        return toResponse(promoCodeRepository.save(promo));
    }

    /**
     * Deactivate a promo code so it can no longer be applied.
     *
     * @param id promo code ID
     * @return updated promo code response
     */
    @Transactional
    public PromoCodeResponse deactivatePromo(Long id) {
        PromoCode promo = promoCodeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found"));
        promo.setIsActive(false);
        return toResponse(promoCodeRepository.save(promo));
    }

    /**
     * Permanently delete a promo code record.
     *
     * @param id promo code ID
     */
    @Transactional
    public void deletePromo(Long id) {
        if (!promoCodeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Promo code not found");
        }
        promoCodeRepository.deleteById(id);
    }

    /**
     * Return all promo codes (active and inactive).
     *
     * @return list of all promo codes
     */
    @Transactional(readOnly = true)
    public List<PromoCodeResponse> getAllPromos() {
        return promoCodeRepository.findAll().stream().map(this::toResponse).toList();
    }

    private PromoCodeResponse toResponse(PromoCode p) {
        return PromoCodeResponse.builder()
            .id(p.getId()).code(p.getCode()).type(p.getType()).value(p.getValue())
            .maxUses(p.getMaxUses()).usedCount(p.getUsedCount())
            .expiresAt(p.getExpiresAt()).isActive(p.getIsActive())
            .createdAt(p.getCreatedAt())
            .build();
    }
}
