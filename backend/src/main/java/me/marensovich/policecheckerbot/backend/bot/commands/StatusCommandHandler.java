package me.marensovich.policecheckerbot.backend.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.format.DateTimeFormatter;

/**
 * Handler for the {@code /status} command.
 *
 * <p>Displays the user's current subscription tier, Premium expiry date (if applicable),
 * notification radius, and live-tracking toggle state.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusCommandHandler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TelegramClient telegramClient;
    private final UserService userService;

    /**
     * Handle the {@code /status} command.
     *
     * @param message incoming message
     */
    public void handle(Message message) {
        Long tgId = message.getFrom().getId();
        User user = userService.findByTgId(tgId);

        String text;
        if (user == null) {
            text = "❌ Вы не зарегистрированы. Откройте MiniApp через /start.";
        } else {
            String subscriptionStatus = user.getIsSubscribed()
                ? "✅ Premium (до " + (user.getSubscriptionExpiresAt() != null
                    ? user.getSubscriptionExpiresAt().format(DATE_FMT) : "—") + ")"
                : "🆓 Бесплатный";

            text = String.format(
                "📊 *Ваш статус:*\n\n" +
                "🔑 Подписка: %s\n" +
                "📏 Радиус уведомлений: *%d км*\n" +
                "🟢 Live-трекинг: *%s*",
                subscriptionStatus,
                user.getNotifyRadiusKm(),
                user.getLiveTracking() ? "включён" : "выключен"
            );
        }

        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text(text)
                .parseMode("Markdown")
                .build());
        } catch (Exception e) {
            log.error("/status handler error: {}", e.getMessage());
        }
    }
}
