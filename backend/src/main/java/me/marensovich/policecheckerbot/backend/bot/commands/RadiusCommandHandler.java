package me.marensovich.policecheckerbot.backend.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.dto.UserSettingsRequest;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handler for the {@code /radius [km]} command.
 *
 * <p>Reads or sets the user's notification radius (1–50 km). Without an argument,
 * reports the current value. With a numeric argument, updates the setting via
 * {@link UserService#updateSettings}.
 *
 * <p>Example: {@code /radius 3} — set radius to 3 km.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RadiusCommandHandler {

    private final TelegramClient telegramClient;
    private final UserService userService;

    /**
     * Handle the {@code /radius} command.
     *
     * @param message incoming message
     */
    public void handle(Message message) {
        Long tgId = message.getFrom().getId();
        User user = userService.findByTgId(tgId);

        if (user == null) {
            sendText(message.getChatId(), "❌ Сначала откройте MiniApp через /start.");
            return;
        }

        String[] parts = message.getText().trim().split("\\s+");
        if (parts.length < 2) {
            sendText(message.getChatId(),
                "📏 Текущий радиус: *" + user.getNotifyRadiusKm() + " км*\n\n" +
                "Для изменения: `/radius [км]`\nПример: `/radius 3`");
            return;
        }

        try {
            int radius = Integer.parseInt(parts[1]);
            if (radius < 1 || radius > 50) {
                sendText(message.getChatId(), "❌ Радиус должен быть от 1 до 50 км.");
                return;
            }

            UserSettingsRequest settings = new UserSettingsRequest();
            settings.setNotifyRadiusKm(radius);
            userService.updateSettings(user, settings);

            sendText(message.getChatId(),
                "✅ Радиус уведомлений установлен: *" + radius + " км*");
        } catch (NumberFormatException e) {
            sendText(message.getChatId(), "❌ Укажите число: `/radius 5`");
        } catch (Exception e) {
            log.error("/radius handler error: {}", e.getMessage());
        }
    }

    private void sendText(Long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .build());
        } catch (Exception e) {
            log.error("Message send error: {}", e.getMessage());
        }
    }
}
