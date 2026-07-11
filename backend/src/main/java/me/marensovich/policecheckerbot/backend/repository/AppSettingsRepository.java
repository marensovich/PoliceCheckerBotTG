package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for reading and writing application-wide configuration settings.
 *
 * <p>Settings are keyed by a {@code String} identifier (e.g. {@code "free_radius_km"})
 * and managed entirely through the standard JPA CRUD operations. All settings are
 * loaded at startup and can be updated live via the admin panel.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettings, String> {
}
