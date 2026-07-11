package me.marensovich.policecheckerbot.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.dto.LocationMessage;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.LiveTrackingService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

/**
 * STOMP WebSocket controller for receiving live location updates.
 *
 * <p>While live tracking is active, the frontend publishes the user's current
 * coordinates to {@code /app/location} every 10–15 seconds via STOMP over SockJS.
 * The server stores the position, then notifies nearby users of DPS posts
 * through Telegram Bot alerts.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final LiveTrackingService liveTrackingService;

    /**
     * Process an incoming location update from a live-tracking client.
     *
     * <p>STOMP destination: {@code SEND /app/location}
     *
     * <p>Unauthenticated frames are silently dropped. The location is persisted
     * via {@link LiveTrackingService#processLocationUpdate(User, Double, Double)} which
     * also triggers proximity-based DPS post notifications.
     *
     * @param message STOMP message carrying lat/lon coordinates
     * @param user    authenticated user resolved by Spring Security
     */
    @MessageMapping("/location")
    public void handleLocationUpdate(LocationMessage message,
                                     @AuthenticationPrincipal User user) {
        if (user == null) {
            log.warn("Received location update from unauthenticated WebSocket client");
            return;
        }
        log.debug("Location update: userId={}, lat={}, lon={}",
            user.getId(), message.getLat(), message.getLon());
        liveTrackingService.processLocationUpdate(user, message.getLat(), message.getLon());
    }
}
