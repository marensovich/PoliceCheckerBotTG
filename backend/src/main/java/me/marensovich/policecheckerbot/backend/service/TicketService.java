package me.marensovich.policecheckerbot.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.dto.TicketReplyRequest;
import me.marensovich.policecheckerbot.backend.dto.TicketRequest;
import me.marensovich.policecheckerbot.backend.dto.TicketResponse;
import me.marensovich.policecheckerbot.backend.model.Role;
import me.marensovich.policecheckerbot.backend.model.Ticket;
import me.marensovich.policecheckerbot.backend.model.TicketStatus;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.repository.TicketRepository;
import me.marensovich.policecheckerbot.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service for support ticket lifecycle management.
 *
 * <p>Users (including banned users) can submit tickets and close their own.
 * Admins can list all tickets with optional status filtering, post replies,
 * and force-close any ticket. Admin replies trigger Telegram notifications
 * to the ticket author, and new tickets trigger Telegram alerts to all admins.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Submit a new support ticket and notify all admins via Telegram.
     *
     * @param user    ticket submitter (may be a banned user)
     * @param request ticket subject and message
     * @return the created ticket
     */
    @Transactional
    public TicketResponse createTicket(User user, TicketRequest request) {
        Ticket ticket = Ticket.builder()
            .user(user)
            .subject(request.getSubject().trim())
            .message(request.getMessage().trim())
            .build();
        ticket = ticketRepository.save(ticket);

        String userName = user.getUsername() != null ? "@" + user.getUsername() : user.getFirstName();
        String preview = ticket.getMessage();
        if (preview.length() > 150) preview = preview.substring(0, 150) + "...";
        String notifyText = "📨 *Новый тикет поддержки #" + ticket.getId() + "*\n\n" +
            "От: " + userName + "\n" +
            "Тема: *" + ticket.getSubject() + "*\n\n" + preview;

        for (User admin : userRepository.findByRole(Role.ADMIN)) {
            notificationService.notifyAdminAction(admin.getTgId(), notifyText);
        }
        log.info("Ticket #{} created by tgId={}", ticket.getId(), user.getTgId());
        return toResponse(ticket);
    }

    /**
     * Return all tickets submitted by the given user, newest first.
     *
     * @param user authenticated user
     * @return list of the user's tickets
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(User user) {
        return ticketRepository.findByUserOrderByCreatedAtDesc(user)
            .stream().map(this::toResponse).toList();
    }

    /**
     * Return a paginated list of all tickets (admin view), optionally filtered by status.
     *
     * @param statusFilter status string ({@code OPEN}, {@code IN_PROGRESS}, {@code CLOSED}), or blank for all
     * @param pageable     pagination parameters
     * @return page of tickets
     */
    @Transactional(readOnly = true)
    public Page<TicketResponse> adminGetTickets(String statusFilter, Pageable pageable) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            TicketStatus status = TicketStatus.valueOf(statusFilter.toUpperCase());
            return ticketRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(this::toResponse);
        }
        return ticketRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    /**
     * Post an admin reply to a ticket, update its status, and notify the ticket author.
     *
     * @param ticketId ticket ID
     * @param request  reply text and optional new status
     * @param admin    authenticated admin
     * @return updated ticket
     * @throws ResponseStatusException 400 if the ticket is already closed, 404 if not found
     */
    @Transactional
    public TicketResponse adminReply(Long ticketId, TicketReplyRequest request, User admin) {
        Ticket ticket = getOrThrow(ticketId);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket is already closed");
        }
        ticket.setAdminReply(request.getReply().trim());
        ticket.setRepliedBy(admin);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            ticket.setStatus(TicketStatus.valueOf(request.getStatus().toUpperCase()));
        } else {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket = ticketRepository.save(ticket);

        notificationService.notifyAdminAction(ticket.getUser().getTgId(),
            "💬 *Ответ на ваш тикет #" + ticketId + "*\n\n" +
            "Тема: " + ticket.getSubject() + "\n\n" +
            "*Ответ поддержки:*\n" + request.getReply().trim());
        log.info("Ticket #{} replied by admin={}", ticketId, admin.getUsername());
        return toResponse(ticket);
    }

    /**
     * Close one of the caller's own tickets.
     *
     * @param ticketId ticket ID
     * @param user     authenticated user (must be the ticket owner)
     * @return updated ticket with {@code CLOSED} status
     * @throws ResponseStatusException 403 if the ticket belongs to another user, 400 if already closed
     */
    @Transactional
    public TicketResponse closeMyTicket(Long ticketId, User user) {
        Ticket ticket = getOrThrow(ticketId);
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This ticket does not belong to you");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket is already closed");
        }
        ticket.setStatus(TicketStatus.CLOSED);
        return toResponse(ticketRepository.save(ticket));
    }

    /**
     * Force-close any ticket (admin operation).
     *
     * @param ticketId ticket ID
     * @param admin    authenticated admin
     * @return updated ticket with {@code CLOSED} status
     */
    @Transactional
    public TicketResponse adminClose(Long ticketId, User admin) {
        Ticket ticket = getOrThrow(ticketId);
        ticket.setStatus(TicketStatus.CLOSED);
        ticket = ticketRepository.save(ticket);
        log.info("Ticket #{} closed by admin={}", ticketId, admin.getUsername());
        return toResponse(ticket);
    }

    private Ticket getOrThrow(Long id) {
        return ticketRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
            .id(t.getId())
            .userId(t.getUser().getId())
            .username(t.getUser().getUsername())
            .firstName(t.getUser().getFirstName())
            .subject(t.getSubject())
            .message(t.getMessage())
            .status(t.getStatus().name())
            .adminReply(t.getAdminReply())
            .repliedByUsername(t.getRepliedBy() != null ? t.getRepliedBy().getUsername() : null)
            .createdAt(t.getCreatedAt())
            .updatedAt(t.getUpdatedAt())
            .build();
    }
}
