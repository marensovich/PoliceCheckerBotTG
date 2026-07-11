package me.marensovich.policecheckerbot.backend.repository;

import me.marensovich.policecheckerbot.backend.model.Ticket;
import me.marensovich.policecheckerbot.backend.model.TicketStatus;
import me.marensovich.policecheckerbot.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for support ticket persistence and retrieval.
 *
 * <p>Provides user-scoped ticket listing and paginated admin access with optional
 * status filtering. All queries sort by creation date descending so the most recent
 * tickets appear first.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Return all tickets submitted by a user, newest first.
     *
     * @param user the ticket owner
     * @return list of the user's tickets
     */
    List<Ticket> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Return all tickets across all users in descending creation order.
     *
     * @param pageable pagination parameters
     * @return page of tickets
     */
    Page<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Return tickets filtered by status in descending creation order.
     *
     * @param status   ticket status filter ({@code OPEN}, {@code IN_PROGRESS}, or {@code CLOSED})
     * @param pageable pagination parameters
     * @return page of tickets with the given status
     */
    Page<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);
}
