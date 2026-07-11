package me.marensovich.policecheckerbot.backend.model;

/**
 * User roles in DPS Tracker.
 *
 * <ul>
 *   <li>{@link #USER} — standard registered user; can create posts, vote, and comment</li>
 *   <li>{@link #MODERATOR} — assigned to a geographic region; can deactivate posts in that region</li>
 *   <li>{@link #ADMIN} — full system access including the admin panel</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
public enum Role {
    USER,
    MODERATOR,
    ADMIN
}
