package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single data point in the admin activity chart.
 *
 * <p>Represents new DPS posts, new registered users, and daily-active users
 * for a given calendar date. Used to render the activity chart on the admin stats page.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDataPoint {
    private String date;
    private long posts;
    /** New user registrations on this date. */
    private long users;
    /** Distinct users who were active (had a lastSeen update) on this date. */
    private long activeUsers;
}
