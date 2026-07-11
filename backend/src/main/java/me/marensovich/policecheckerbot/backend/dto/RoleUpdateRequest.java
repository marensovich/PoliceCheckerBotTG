package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import me.marensovich.policecheckerbot.backend.model.Role;

/**
 * Request DTO for changing a user's role.
 *
 * <p>Used by {@code PUT /api/admin/users/{id}/role}.
 * Valid roles are defined in {@link me.marensovich.policecheckerbot.backend.model.Role}.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class RoleUpdateRequest {

    @NotNull
    private Role role;
}
