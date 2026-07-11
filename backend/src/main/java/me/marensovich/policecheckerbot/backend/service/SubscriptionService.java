package me.marensovich.policecheckerbot.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.config.BotConfig;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.repository.AppSettingsRepository;
import me.marensovich.policecheckerbot.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing Premium subscriptions paid via Telegram Stars.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Activating Premium after a successful Telegram Stars payment</li>
 *   <li>Sending a Telegram Stars invoice on demand (user taps "Buy")</li>
 *   <li>Scheduled hourly sweep to deactivate expired subscriptions and
 *       stop live tracking for affected users</li>
 * </ul>
 *
 * <p>Subscription price and duration are read from {@code AppSettings} at runtime
 * and fall back to the values in {@link BotConfig} if no setting exists.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;
    private final BotConfig botConfig;
    private final NotificationService notificationService;
    private final TelegramClient telegramClient;
    private final AppSettingsRepository appSettingsRepository;

    /**
     * Activate a Premium subscription for the user who completed a Telegram Stars payment.
     *
     * <p>Called by the payment handler after the bot receives a {@code successful_payment} event.
     *
     * @param tgId Telegram user ID of the payer
     */
    @Transactional
    public void activateSubscription(Long tgId) {
        int durationDays = getIntSetting("subscription.duration.days", botConfig.getSubscriptionDurationDays());
        userRepository.findByTgId(tgId).ifPresent(user -> {
            user.setIsSubscribed(true);
            user.setSubscriptionExpiresAt(
                LocalDateTime.now().plusDays(durationDays)
            );
            userRepository.save(user);
            log.info("Subscription activated: tgId={}, expires={}", tgId, user.getSubscriptionExpiresAt());
        });
    }

    /**
     * Send a Telegram Stars invoice directly to the user's chat with the bot.
     *
     * <p>Called when the user taps "Buy" in the Mini App. The bot sends the invoice
     * while the Mini App remains open. Payment amount and subscription duration are
     * read from live settings.
     *
     * @param user authenticated user requesting the invoice
     * @throws ResponseStatusException 500 if the Telegram API is unavailable
     */
    public void sendInvoice(User user) {
        int priceStars = getIntSetting("subscription.price.stars", botConfig.getSubscriptionPriceStars());
        int durationDays = getIntSetting("subscription.duration.days", botConfig.getSubscriptionDurationDays());
        try {
            telegramClient.execute(SendInvoice.builder()
                .chatId(user.getTgId().toString())
                .title("DPS Tracker Premium")
                .description("Подписка на " + durationDays + " дней. Отслеживание постов ДПС в реальном времени.")
                .payload("premium_" + durationDays + "d_" + user.getTgId())
                .providerToken("")
                .currency("XTR")
                .prices(List.of(new LabeledPrice("Premium подписка", priceStars)))
                .build());
            log.info("Invoice sent: tgId={}", user.getTgId());
        } catch (Exception e) {
            log.error("Failed to send invoice for tgId={}: {}", user.getTgId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not send invoice. Try via the bot: /subscribe");
        }
    }

    /**
     * Scheduled sweep: deactivate subscriptions that have passed their expiry date.
     *
     * <p>Runs every hour. For each expired user:
     * <ol>
     *   <li>Clears the {@code isSubscribed} flag and expiry timestamp</li>
     *   <li>Disables live tracking</li>
     *   <li>Sends a Telegram expiry notification</li>
     * </ol>
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public void deactivateExpiredSubscriptions() {
        List<User> expired = userRepository.findExpiredSubscriptions(LocalDateTime.now());
        for (User user : expired) {
            user.setIsSubscribed(false);
            user.setLiveTracking(false);
            user.setSubscriptionExpiresAt(null);
            userRepository.save(user);
            log.info("Subscription expired: tgId={}", user.getTgId());
            notificationService.notifySubscriptionExpired(user.getTgId());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private int getIntSetting(String key, int fallback) {
        return appSettingsRepository.findById(key)
            .map(s -> { try { return Integer.parseInt(s.getValue()); } catch (NumberFormatException e) { return fallback; } })
            .orElse(fallback);
    }
}
