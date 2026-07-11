package me.marensovich.policecheckerbot.backend.dto;

import lombok.Data;

/**
 * Request DTO for updating a user's post-type notification preferences.
 *
 * <p>{@code notifyPostTypes} is a comma-separated list of
 * {@link me.marensovich.policecheckerbot.backend.model.PostType} values
 * (e.g. {@code "DPS_POST,CAMERA"}). An empty string means "notify about all types".
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class NotifyPrefsRequest {
    /** Comma-separated post type filter. Empty string means all types. */
    private String notifyPostTypes;
}
