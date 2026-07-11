package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket (STOMP) message carrying the user's current geographic position.
 *
 * <p>The frontend publishes this message to {@code /app/location} every 10–15 seconds
 * while live tracking is active.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationMessage {

    /** Latitude of the user's current position. */
    private Double lat;

    /** Longitude of the user's current position. */
    private Double lon;
}
