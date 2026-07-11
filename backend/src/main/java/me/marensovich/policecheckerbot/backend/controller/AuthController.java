package me.marensovich.policecheckerbot.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.marensovich.policecheckerbot.backend.dto.AuthRequest;
import me.marensovich.policecheckerbot.backend.dto.AuthResponse;
import me.marensovich.policecheckerbot.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Telegram WebApp authentication.
 *
 * <p>This is the only fully public API endpoint — all other endpoints require a Bearer token
 * issued by this controller. The {@code initData} string from {@code window.Telegram.WebApp}
 * is validated server-side via HMAC-SHA256 before a session token is issued.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Authenticate a Telegram Mini App user using Telegram WebApp {@code initData}.
     *
     * <p>{@code POST /api/auth/telegram}
     *
     * @param request request body containing the signed {@code initData} string
     * @return session token and basic user profile on success; 401 if initData is invalid
     */
    @PostMapping("/telegram")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(userService.authenticate(request));
    }
}
