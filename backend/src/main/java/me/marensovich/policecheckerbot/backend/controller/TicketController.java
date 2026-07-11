package me.marensovich.policecheckerbot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.dto.TicketRequest;
import me.marensovich.policecheckerbot.backend.dto.TicketResponse;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user-facing support ticket operations.
 *
 * <p>Banned users are explicitly allowed to access these endpoints so they can
 * appeal their ban through the support system. Admin-side ticket operations
 * (reply, bulk listing) are handled by {@link AdminController}.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/tickets}         — submit a new ticket (201 Created)</li>
 *   <li>{@code GET  /api/tickets/my}      — list the caller's tickets</li>
 *   <li>{@code PUT  /api/tickets/{id}/close} — close own ticket</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * Submit a new support ticket. Returns 201 Created.
     *
     * @param request ticket subject and message body
     * @param user    authenticated user
     * @return the created ticket
     */
    @PostMapping
    public ResponseEntity<TicketResponse> create(
        @Valid @RequestBody TicketRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(user, request));
    }

    /**
     * Return all tickets submitted by the authenticated user, newest first.
     *
     * @param user authenticated user
     * @return list of the user's tickets
     */
    @GetMapping("/my")
    public ResponseEntity<List<TicketResponse>> myTickets(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ticketService.getMyTickets(user));
    }

    /**
     * Close one of the caller's own tickets.
     *
     * @param id   ticket ID (must belong to the caller)
     * @param user authenticated user
     * @return the updated ticket with {@code CLOSED} status
     */
    @PutMapping("/{id}/close")
    public ResponseEntity<TicketResponse> closeMyTicket(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ticketService.closeMyTicket(id, user));
    }
}
