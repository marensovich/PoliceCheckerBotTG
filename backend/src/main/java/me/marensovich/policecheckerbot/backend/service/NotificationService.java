package me.marensovich.policecheckerbot.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.config.BotConfig;
import me.marensovich.policecheckerbot.backend.model.DpsPost;
import me.marensovich.policecheckerbot.backend.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Service for sending notifications to users via the Telegram Bot API.
 *
 * <p>Used for:
 * <ul>
 *   <li>Alerting nearby users when a new DPS post is created (proximity notification)</li>
 *   <li>Notifying users about live-tracking state changes</li>
 *   <li>Notifying users about subscription expiry</li>
 *   <li>Admin-initiated broadcasts and user notifications</li>
 * </ul>
 *
 * <p>All sends are fire-and-forget — exceptions are caught and logged rather than propagated,
 * so a failed notification never blocks the calling request.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TelegramClient telegramClient;
    private final BotConfig botConfig;

    /**
     * Notify a list of users about a new nearby DPS post.
     *
     * <p>Respects each user's {@code notifyPostTypes} preference — users who have
     * opted out of notifications for this post type are silently skipped.
     *
     * @param users         users to notify (pre-filtered to those within notification radius)
     * @param post          the new DPS post
     * @param postTypeLabel unused display label (kept for API compatibility)
     */
    public void notifyUsersAboutNewPost(List<User> users, DpsPost post, String postTypeLabel) {
        for (User user : users) {
            try {
                String prefs = user.getNotifyPostTypes();
                if (prefs != null && !prefs.isBlank()) {
                    boolean wantsThisType = java.util.Arrays.stream(prefs.split(","))
                        .map(String::trim)
                        .anyMatch(t -> t.equals(post.getPostType().name()));
                    if (!wantsThisType) continue;
                }
                double distKm = haversineKm(
                    user.getLastLat(), user.getLastLon(),
                    post.getLat(), post.getLon());
                notifyPostNearby(user.getTgId(), post, distKm);
            } catch (Exception e) {
                log.warn("Notification error for tgId={}: {}", user.getTgId(), e.getMessage());
            }
        }
    }

    /**
     * Broadcast a plain-text message to a list of users.
     *
     * @param users   recipients
     * @param message message text (supports Telegram Markdown)
     */
    public void broadcast(List<User> users, String message) {
        for (User user : users) {
            sendMessage(user.getTgId(), message, null);
        }
    }

    /**
     * Send an admin-action notification to a user with an "Open app" button.
     *
     * @param tgId    recipient's Telegram user ID
     * @param message notification text
     */
    public void notifyAdminAction(Long tgId, String message) {
        InlineKeyboardButton openButton = InlineKeyboardButton.builder()
            .text("📱 Открыть приложение")
            .url(botConfig.getMiniappUrl())
            .build();
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
            .keyboard(List.of(new InlineKeyboardRow(openButton)))
            .build();
        sendMessage(tgId, message, markup);
    }

    /**
     * Send a proximity alert about a nearby DPS post with a deep-link "Open map" button.
     *
     * @param tgId       recipient's Telegram user ID
     * @param post       the nearby DPS post
     * @param distanceKm distance to the post in kilometres
     */
    public void notifyPostNearby(Long tgId, DpsPost post, double distanceKm) {
        String text = String.format(
            "🚨 *Внимание! Пост ДПС* в %.1f км от вас.\n\n" +
            "📍 Координаты: %.6f, %.6f\n" +
            "%s",
            distanceKm,
            post.getLat(),
            post.getLon(),
            post.getDescription() != null ? "📝 " + post.getDescription() : ""
        );

        InlineKeyboardButton mapButton = InlineKeyboardButton.builder()
            .text("🗺 Открыть карту")
            .url(botConfig.getMiniappUrl() + "?startapp=post_" + post.getId())
            .build();

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
            .keyboard(List.of(new InlineKeyboardRow(mapButton)))
            .build();

        sendMessage(tgId, text, markup);
    }

    /**
     * Notify a user that live tracking has been enabled.
     *
     * @param tgId     recipient's Telegram user ID
     * @param radiusKm the user's configured notification radius
     */
    public void notifyLiveEnabled(Long tgId, int radiusKm) {
        sendMessage(tgId,
            "🟢 *Live-трекинг включён.*\nРадиус уведомлений: *" + radiusKm + " км*",
            null);
    }

    /**
     * Notify a user that live tracking has been disabled.
     *
     * @param tgId recipient's Telegram user ID
     */
    public void notifyLiveDisabled(Long tgId) {
        sendMessage(tgId, "🔴 *Live-трекинг выключен.*", null);
    }

    /**
     * Notify a user that their Premium subscription has expired and live tracking has stopped.
     *
     * @param tgId recipient's Telegram user ID
     */
    public void notifySubscriptionExpired(Long tgId) {
        InlineKeyboardButton subscribeButton = InlineKeyboardButton.builder()
            .text("⭐ Продлить подписку")
            .callbackData("subscribe")
            .build();

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
            .keyboard(List.of(new InlineKeyboardRow(subscribeButton)))
            .build();

        sendMessage(tgId,
            "⚠️ *Ваша подписка истекла.* Live-трекинг остановлен.\n\n" +
            "Продлите подписку за " + botConfig.getSubscriptionPriceStars() + " ⭐ Telegram Stars.",
            markup);
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup markup) {
        try {
            SendMessage.SendMessageBuilder builder = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown");

            if (markup != null) {
                builder.replyMarkup(markup);
            }

            telegramClient.execute(builder.build());
        } catch (Exception e) {
            log.error("Failed to send notification to tgId={}: {}", chatId, e.getMessage());
        }
    }
}
