package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for assigning the moderator role to a user.
 *
 * <p>The {@code region} field names the geographic area the moderator will oversee
 * (e.g. "Moscow"). Maximum 100 characters.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class ModeratorRequest {

    @NotBlank(message = "Region must not be blank")
    @Size(max = 100, message = "Region name must not exceed 100 characters")
    private String region;
}
