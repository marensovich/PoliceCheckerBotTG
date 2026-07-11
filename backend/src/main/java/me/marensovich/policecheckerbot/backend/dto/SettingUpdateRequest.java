package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for updating a single application-wide setting value.
 *
 * <p>Used by {@code PUT /api/admin/settings/{key}}. The string value is stored as-is
 * and interpreted by the consuming service (e.g. parsed to an integer for radius settings).
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class SettingUpdateRequest {

    @NotBlank
    private String value;
}
