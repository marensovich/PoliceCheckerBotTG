package me.marensovich.policecheckerbot.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for {@code POST /api/admin/cameras/import}.
 *
 * <p>Specifies the geographic bounding box used for the Overpass API query.
 * All four coordinates are required and must form a valid bbox (south ≤ north, west ≤ east).
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class CameraImportRequest {

    @NotNull
    private Double south;

    @NotNull
    private Double west;

    @NotNull
    private Double north;

    @NotNull
    private Double east;
}
