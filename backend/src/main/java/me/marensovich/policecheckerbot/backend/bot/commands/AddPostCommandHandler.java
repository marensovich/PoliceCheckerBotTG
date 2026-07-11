package me.marensovich.policecheckerbot.backend.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

/**
 * Handler for the {@code /addpost} command.
 *
 * <p>Presents the user with a native Telegram location-share button.
 * Once the user shares their location, {@link me.marensovich.policecheckerbot.backend.bot.handlers.LocationHandler}
 * picks it up and creates a DPS post at that coordinate.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddPostCommandHandler {

    private final TelegramClient telegramClient;

    /**
     * Handle the {@code /addpost} command by requesting the user's location.
     *
     * @param message incoming message
     */
    public void handle(Message message) {
        KeyboardButton locationButton = KeyboardButton.builder()
            .text("📍 Отправить мою геолокацию")
            .requestLocation(true)
            .build();

        ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
            .keyboard(List.of(new KeyboardRow(locationButton)))
            .resizeKeyboard(true)
            .oneTimeKeyboard(true)
            .build();

        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text("📍 *Добавление поста ДПС*\n\nНажмите кнопку ниже, чтобы отправить вашу геолокацию.\n\n" +
                      "Пост будет создан в вашем текущем местоположении.")
                .parseMode("Markdown")
                .replyMarkup(markup)
                .build());
        } catch (Exception e) {
            log.error("/addpost handler error: {}", e.getMessage());
        }
    }
}
