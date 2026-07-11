package me.marensovich.policecheckerbot.backend.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.LiveTrackingService;
import me.marensovich.policecheckerbot.backend.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handler for the {@code /live on} and {@code /live off} commands.
 *
 * <p>Toggles live-location tracking for Premium subscribers. Free users receive
 * a prompt to subscribe. The command text is matched case-insensitively.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveCommandHandler {

    private final TelegramClient telegramClient;
    private final UserService userService;
    private final LiveTrackingService liveTrackingService;

    /**
     * Handle the {@code /live} command.
     *
     * @param message incoming message ({@code /live on} or {@code /live off})
     */
    public void handle(Message message) {
        String text = message.getText().trim().toLowerCase();
        Long tgId = message.getFrom().getId();
        User user = userService.findByTgId(tgId);

        if (user == null) {
            sendText(message.getChatId(), "❌ Сначала откройте MiniApp через /start.");
            return;
        }

        try {
            if (text.contains("on")) {
                liveTrackingService.setLiveTracking(user, true);
            } else if (text.contains("off")) {
                liveTrackingService.setLiveTracking(user, false);
            } else {
                sendText(message.getChatId(),
                    "Используйте:\n`/live on` — включить\n`/live off` — выключить");
            }
        } catch (org.springframework.web.server.ResponseStatusException e) {
            sendText(message.getChatId(),
                "⭐ Live-трекинг доступен только для Premium-подписчиков.\n" +
                "Оформите подписку через /subscribe");
        } catch (Exception e) {
            log.error("/live handler error: {}", e.getMessage());
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
