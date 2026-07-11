package me.marensovich.policecheckerbot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.dto.ApplyPromoRequest;
import me.marensovich.policecheckerbot.backend.dto.ApplyPromoResponse;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.PromoCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user-facing promo code redemption.
 *
 * <p>Admin promo code management (create, deactivate, delete, list) is handled
 * by {@link AdminController}. This controller exposes only the single apply endpoint
 * available to any authenticated user.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/promo")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    /**
     * Apply a promo code to the authenticated user's account.
     *
     * <p>The code lookup is case-insensitive. Each code can be used at most once per user.
     * On success, the response describes the benefit granted (Premium days or discount %).
     *
     * @param request promo code string
     * @param user    authenticated user
     * @return description of the applied benefit
     */
    @PostMapping("/apply")
    public ResponseEntity<ApplyPromoResponse> apply(
        @Valid @RequestBody ApplyPromoRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(promoCodeService.applyPromo(request.getCode(), user));
    }
}
