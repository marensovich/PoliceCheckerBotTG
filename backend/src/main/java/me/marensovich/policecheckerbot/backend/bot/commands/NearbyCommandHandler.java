package me.marensovich.policecheckerbot.backend.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.dto.PostResponse;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.PostService;
import me.marensovich.policecheckerbot.backend.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

/**
 * Handler for the {@code /nearby} command.
 *
 * <p>If the user has a stored last-known location, displays DPS posts within
 * their configured notification radius immediately. Otherwise requests a fresh
 * location via the native Telegram location-share keyboard button.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NearbyCommandHandler {

    private final TelegramClient telegramClient;
    private final UserService userService;
    private final PostService postService;

    /**
     * Handle the {@code /nearby} command — show posts or request location.
     *
     * @param message incoming message
     */
    public void handle(Message message) {
        Long tgId = message.getFrom().getId();
        User user = userService.findByTgId(tgId);

        if (user != null && user.getLastLat() != null && user.getLastLon() != null) {
            showNearbyPosts(message.getChatId(), user.getLastLat(), user.getLastLon(),
                user.getNotifyRadiusKm().doubleValue());
        } else {
            requestLocation(message.getChatId());
        }
    }

    /**
     * Fetch and display DPS posts within the given radius, up to 10 entries.
     *
     * @param chatId   destination chat ID
     * @param lat      search centre latitude
     * @param lon      search centre longitude
     * @param radiusKm search radius in kilometres
     */
    public void showNearbyPosts(Long chatId, Double lat, Double lon, Double radiusKm) {
        try {
            List<PostResponse> posts = postService.findNearby(lat, lon, radiusKm);

            String text;
            if (posts.isEmpty()) {
                text = "✅ Постов ДПС в радиусе *" + radiusKm.intValue() + " км* не найдено.";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("🚨 *Посты ДПС в радиусе ").append(radiusKm.intValue()).append(" км:*\n\n");
                int i = 1;
                for (PostResponse post : posts) {
                    sb.append(i++).append(". 📍 ").append(post.getLat()).append(", ").append(post.getLon());
                    if (post.getDescription() != null) {
                        sb.append("\n   _").append(post.getDescription()).append("_");
                    }
                    sb.append("\n   👍 Рейтинг: ").append(post.getConfidence()).append("\n\n");
                    if (i > 10) {
                        sb.append("... и ещё ").append(posts.size() - 10).append(" постов на карте.");
                        break;
                    }
                }
                text = sb.toString();
            }

            telegramClient.execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode("Markdown")
                .build());
        } catch (Exception e) {
            log.error("/nearby handler error: {}", e.getMessage());
        }
    }

    private void requestLocation(Long chatId) {
        KeyboardButton locationButton = KeyboardButton.builder()
            .text("📍 Отправить геолокацию")
            .requestLocation(true)
            .build();

        ReplyKeyboardMarkup markup = ReplyKeyboardMarkup.builder()
            .keyboard(List.of(new KeyboardRow(locationButton)))
            .resizeKeyboard(true)
            .oneTimeKeyboard(true)
            .build();

        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(chatId.toString())
                .text("🔍 Отправьте вашу геолокацию для поиска постов ДПС рядом:")
                .replyMarkup(markup)
                .build());
        } catch (Exception e) {
            log.error("Location request error: {}", e.getMessage());
        }
    }
}
