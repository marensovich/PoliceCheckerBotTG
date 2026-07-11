package me.marensovich.policecheckerbot.backend.model;

/**
 * Type of map marker in DPS Tracker.
 *
 * <p>Controls the marker's lifetime, visual appearance, and aging behaviour:
 * <ul>
 *   <li>{@link #DPS_POST} — stationary checkpoint: fades after 4 h, removed after 12 h</li>
 *   <li>{@link #PATROL_CAR} — patrol vehicle: valid for 30 min; uncertainty circle grows with reported speed</li>
 *   <li>{@link #CAMERA} — fixed speed/red-light camera: permanent marker</li>
 *   <li>{@link #AMBUSH} — hidden DPS surveillance: same lifetime as {@code DPS_POST}</li>
 *   <li>{@link #HIDDEN_POST} — concealed checkpoint (behind sign, in bushes, etc.): same as {@code DPS_POST}</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
public enum PostType {

    /** Stationary DPS checkpoint. Fades after 4 h, expires at 12 h. */
    DPS_POST,

    /** Patrol vehicle. Valid for 30 min; uncertainty circle grows at the reported speed. */
    PATROL_CAR,

    /** Fixed speed or red-light camera. Permanent marker with no expiry. */
    CAMERA,

    /** Hidden DPS surveillance / ambush. Same lifetime as {@code DPS_POST}. */
    AMBUSH,

    /** Concealed checkpoint (behind sign, in bushes, etc.). Same lifetime as {@code DPS_POST}. */
    HIDDEN_POST
}
