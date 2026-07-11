package me.marensovich.policecheckerbot.backend.model;

/**
 * Lifecycle status of a support ticket.
 *
 * <p>Tickets progress through the following states:
 * <ol>
 *   <li>{@link #OPEN} — newly submitted, awaiting admin review</li>
 *   <li>{@link #IN_PROGRESS} — an admin has acknowledged and is handling the ticket</li>
 *   <li>{@link #CLOSED} — resolved; no further action is expected</li>
 * </ol>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED
}
