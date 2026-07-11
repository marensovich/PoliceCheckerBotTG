package me.marensovich.policecheckerbot.backend.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.bot.commands.NearbyCommandHandler;
import me.marensovich.policecheckerbot.backend.dto.PostRequest;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.repository.AppSettingsRepository;
import me.marensovich.policecheckerbot.backend.service.PostService;
import me.marensovich.policecheckerbot.backend.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.location.Location;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handler for incoming location messages from the Telegram bot client.
 *
 * <p>Used in two flows:
 * <ul>
 *   <li>After {@code /addpost} — the user shares their location and a DPS post is created
 *       via {@link #createPostFromLocation}</li>
 *   <li>After {@code /nearby} — the location is used to search for nearby posts</li>
 * </ul>
 *
 * <p>In the current implementation, a received location is always treated as a
 * "show nearby posts" request (the most common use case). For post creation, the
 * Mini App UI or the {@link #createPostFromLocation} method should be called explicitly.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationHandler {

    private final TelegramClient telegramClient;
    private final UserService userService;
    private final PostService postService;
    private final NearbyCommandHandler nearbyCommandHandler;
    private final AppSettingsRepository appSettingsRepository;

    /**
     * Process an incoming location message.
     *
     * <p>Auto-confirms nearby posts within the configured radius, then displays
     * all active posts within the user's notification radius.
     *
     * @param message incoming message containing a {@link Location}
     */
    public void handle(Message message) {
        Location location = message.getLocation();
        Double lat = location.getLatitude().doubleValue();
        Double lon = location.getLongitude().doubleValue();
        Long tgId = message.getFrom().getId();

        User user = userService.findByTgId(tgId);

        if (user == null) {
            sendText(message.getChatId(), "❌ Сначала откройте MiniApp через /start.");
            return;
        }

        autoConfirmNearbyPosts(user, lat, lon);

        nearbyCommandHandler.showNearbyPosts(
            message.getChatId(), lat, lon,
            user.getNotifyRadiusKm().doubleValue()
        );
    }

    /**
     * Create a DPS post at the specified coordinates and confirm via Telegram message.
     *
     * @param user        post author
     * @param lat         latitude
     * @param lon         longitude
     * @param description optional post description (may be {@code null})
     * @param chatId      chat to reply to
     */
    public void createPostFromLocation(User user, Double lat, Double lon,
                                       String description, Long chatId) {
        try {
            PostRequest request = new PostRequest();
            request.setLat(lat);
            request.setLon(lon);
            request.setDescription(description);

            postService.createPost(request, user);
            sendText(chatId, "✅ Пост ДПС добавлен на карту!\n📍 Координаты: " + lat + ", " + lon);
        } catch (Exception e) {
            log.error("Post creation from location failed: {}", e.getMessage());
            sendText(chatId, "❌ Ошибка при добавлении поста: " + e.getMessage());
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    /** Auto-confirm (+1 vote) posts within the configured radius that the user hasn't voted on yet. */
    private void autoConfirmNearbyPosts(User user, double lat, double lon) {
        try {
            double radiusMeters = appSettingsRepository.findById("auto.confirm.radius.meters")
                .map(s -> { try { return Double.parseDouble(s.getValue()); } catch (NumberFormatException e) { return 200.0; } })
                .orElse(200.0);
            postService.autoConfirmNearby(user, lat, lon, radiusMeters);
        } catch (Exception e) {
            log.debug("Auto-confirm via bot location: {}", e.getMessage());
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
