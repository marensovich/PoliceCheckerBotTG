package me.marensovich.policecheckerbot.backend.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

/**
 * Handler for the {@code /start} command.
 *
 * <p>Sends a welcome message and an inline keyboard with a Web App button
 * that opens the DPS Tracker Mini App map, plus a Help button.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartCommandHandler {

    private final TelegramClient telegramClient;
    private final BotConfig botConfig;

    /**
     * Handle the {@code /start} command.
     *
     * @param message incoming message containing the command
     */
    public void handle(Message message) {
        String firstName = message.getFrom().getFirstName();
        String text = String.format(
            "👋 Привет, *%s*!\n\n" +
            "🗺 *DPS Tracker* — краудсорсинговая карта постов ДПС.\n\n" +
            "📍 Добавляй посты ДПС, подтверждай существующие и получай уведомления в реальном времени.\n\n" +
            "⭐ *Premium* — live-трекинг и мгновенные уведомления за 100 Telegram Stars/мес.\n\n" +
            "Используй кнопку ниже, чтобы открыть карту:",
            firstName
        );

        InlineKeyboardButton mapButton = InlineKeyboardButton.builder()
            .text("🗺 Открыть карту")
            .webApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(botConfig.getMiniappUrl()))
            .build();

        InlineKeyboardButton helpButton = InlineKeyboardButton.builder()
            .text("❓ Помощь")
            .callbackData("help")
            .build();

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                new InlineKeyboardRow(mapButton),
                new InlineKeyboardRow(helpButton)
            ))
            .build();

        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text(text)
                .parseMode("Markdown")
                .replyMarkup(markup)
                .build());
        } catch (Exception e) {
            log.error("/start handler error: {}", e.getMessage());
        }
    }
}
