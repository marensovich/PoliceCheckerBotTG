package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for persisting and querying administrator action log entries.
 *
 * <p>Provides paginated access to admin log records sorted by creation date descending,
 * enabling operators to audit all administrative actions performed in the system.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    Page<AdminLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
