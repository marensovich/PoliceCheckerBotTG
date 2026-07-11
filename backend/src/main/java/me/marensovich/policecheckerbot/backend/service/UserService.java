package me.marensovich.policecheckerbot.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.config.BotConfig;
import me.marensovich.policecheckerbot.backend.dto.AuthRequest;
import me.marensovich.policecheckerbot.backend.dto.AuthResponse;
import me.marensovich.policecheckerbot.backend.dto.NotifyPrefsRequest;
import me.marensovich.policecheckerbot.backend.dto.UserResponse;
import me.marensovich.policecheckerbot.backend.dto.UserSettingsRequest;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.repository.UserRepository;
import me.marensovich.policecheckerbot.backend.security.TelegramAuthValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.UUID;

/**
 * Service for managing DPS Tracker user accounts.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Authenticating users via Telegram WebApp {@code initData} (HMAC-SHA256 validation)</li>
 *   <li>Creating and updating user profiles via atomic upsert</li>
 *   <li>Updating notification settings (radius, post-type filter)</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TelegramAuthValidator authValidator;
    private final TelegramClient telegramClient;
    private final BotConfig botConfig;

    /**
     * Authenticate a user via Telegram WebApp {@code initData}.
     *
     * <p>Validates the HMAC-SHA256 signature, then upserts the user record and
     * issues a new UUID session token. Verifies via the Bot API that the user has
     * opened a chat with the bot before granting access.
     *
     * @param request request body containing the signed {@code initData} string
     * @return {@link AuthResponse} with the session token and basic profile
     * @throws ResponseStatusException 401 if the signature is invalid, 403 if the user has not started the bot
     */
    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        String initData = request.getInitData();

        if (!authValidator.validate(initData)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "initData rejected: invalid signature or expired data");
        }

        Long tgId = authValidator.extractTgId(initData);
        String username  = authValidator.extractUsername(initData);
        String firstName = authValidator.extractFirstName(initData);

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "A Telegram @username is required to use this app — set one in Telegram: " +
                "Profile → Edit Profile → Username");
        }

        try {
            telegramClient.execute(GetChat.builder().chatId(tgId.toString()).build());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("chat not found") || msg.contains("user_deactivated")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Start a chat with the bot first: find @" + botConfig.getBotUsername() +
                    " in Telegram and send /start");
            }
            log.warn("Could not verify bot chat for tgId={}: {}", tgId, e.getMessage());
        }

        String sessionToken = UUID.randomUUID().toString();
        userRepository.upsert(tgId, username, firstName, sessionToken);
        User user = userRepository.findByTgId(tgId).orElseThrow();
        log.info("User authenticated: tgId={}, username=@{}", tgId, username);

        return AuthResponse.builder()
            .token(sessionToken)
            .tgId(user.getTgId())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .isSubscribed(user.getIsSubscribed())
            .build();
    }

    /**
     * Return the current user's profile DTO.
     *
     * @param user authenticated user from the security context
     * @return user profile DTO
     */
    public UserResponse getProfile(User user) {
        return toResponse(user);
    }

    /**
     * Update the user's notification radius.
     *
     * <p>Free users are capped at 5 km; Premium users may set up to 50 km.
     *
     * @param user    authenticated user
     * @param request new settings
     * @return updated profile
     */
    @Transactional
    public UserResponse updateSettings(User user, UserSettingsRequest request) {
        if (request.getNotifyRadiusKm() != null) {
            int maxAllowed = Boolean.TRUE.equals(user.getIsSubscribed()) ? 50 : 5;
            user.setNotifyRadiusKm(Math.min(request.getNotifyRadiusKm(), maxAllowed));
        }
        user = userRepository.save(user);
        return toResponse(user);
    }

    /**
     * Update the user's post-type notification filter.
     *
     * @param user    authenticated user
     * @param request comma-separated post types, or empty for all
     * @return updated profile
     */
    @Transactional
    public UserResponse updateNotifyPrefs(User user, NotifyPrefsRequest request) {
        user.setNotifyPostTypes(request.getNotifyPostTypes());
        user = userRepository.save(user);
        return toResponse(user);
    }

    /**
     * Look up a user by Telegram ID.
     *
     * @param tgId Telegram user ID
     * @return user entity or {@code null} if not found
     */
    public User findByTgId(Long tgId) {
        return userRepository.findByTgId(tgId).orElse(null);
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .tgId(user.getTgId())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .role(user.getRole())
            .reputationScore(user.getReputationScore())
            .notifyPostTypes(user.getNotifyPostTypes())
            .isSubscribed(user.getIsSubscribed())
            .subscriptionExpiresAt(user.getSubscriptionExpiresAt())
            .isBanned(user.getIsBanned())
            .notifyRadiusKm(user.getNotifyRadiusKm())
            .liveTracking(user.getLiveTracking())
            .promoDiscountPercent(user.getPromoDiscountPercent())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
