package me.marensovich.policecheckerbot.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a bulk camera import from an external source (e.g. OpenStreetMap Overpass API).
 *
 * <p>Returned by {@code POST /api/admin/cameras/import}.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraImportResponse {

    /** Number of camera posts successfully created. */
    private int imported;

    /** Number of nodes skipped because a camera already exists within the deduplication radius. */
    private int skipped;

    /** Total nodes returned by the data source before deduplication. */
    private int total;
}
