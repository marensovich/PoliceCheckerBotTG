package me.marensovich.policecheckerbot.backend.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.config.BotConfig;
import me.marensovich.policecheckerbot.backend.repository.AppSettingsRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

/**
 * Handler for the {@code /subscribe} command.
 *
 * <p>Sends a formatted Premium info message followed by a Telegram Stars invoice
 * (currency {@code XTR}). Price and subscription duration are read from live
 * {@code AppSettings}, falling back to {@link BotConfig} defaults.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscribeCommandHandler {

    private final TelegramClient telegramClient;
    private final BotConfig botConfig;
    private final AppSettingsRepository appSettingsRepository;

    /**
     * Handle the {@code /subscribe} command.
     *
     * @param message incoming message
     */
    public void handle(Message message) {
        int priceStars = getIntSetting("subscription.price.stars", botConfig.getSubscriptionPriceStars());
        int durationDays = getIntSetting("subscription.duration.days", botConfig.getSubscriptionDurationDays());

        String infoText = String.format(
            "⭐ *DPS Tracker Premium*\n\n" +
            "🆓 *Бесплатно:*\n" +
            "• Просмотр карты постов ДПС\n" +
            "• Добавление меток\n" +
            "• Комментарии и голосование\n\n" +
            "⭐ *Premium — %d Stars/%d дней:*\n" +
            "• Live-трекинг маршрута\n" +
            "• Мгновенные Telegram-уведомления\n" +
            "• Расширенная история постов\n\n" +
            "Нажмите кнопку ниже для оплаты:",
            priceStars, durationDays
        );

        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text(infoText)
                .parseMode("Markdown")
                .build());

            telegramClient.execute(SendInvoice.builder()
                .chatId(message.getChatId().toString())
                .title("DPS Tracker Premium")
                .description("Оплатить подписку на " + durationDays + " дней.")
                .payload("premium_" + durationDays + "d_" + message.getFrom().getId())
                .providerToken("")
                .currency("XTR")
                .prices(List.of(new LabeledPrice("Premium подписка", priceStars)))
                .build());
        } catch (Exception e) {
            log.error("/subscribe handler error: {}", e.getMessage());
        }
    }

    private int getIntSetting(String key, int fallback) {
        return appSettingsRepository.findById(key)
            .map(s -> { try { return Integer.parseInt(s.getValue()); } catch (NumberFormatException e) { return fallback; } })
            .orElse(fallback);
    }
}
