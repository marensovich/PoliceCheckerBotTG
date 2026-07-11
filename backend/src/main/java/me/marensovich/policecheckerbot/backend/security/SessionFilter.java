package me.marensovich.policecheckerbot.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.model.Role;
import me.marensovich.policecheckerbot.backend.model.User;
import me.marensovich.policecheckerbot.backend.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Per-request authentication filter that resolves users from their session token.
 *
 * <p>Reads the {@code Authorization: Bearer <token>} header on every request and
 * looks the token up in the database. If found, populates the
 * {@link SecurityContextHolder} with the authenticated user and their granted authorities:
 * <ul>
 *   <li>{@code ROLE_USER} — always granted</li>
 *   <li>{@code ROLE_PREMIUM} or {@code ROLE_FREE} — based on subscription status</li>
 *   <li>{@code ROLE_ADMIN} — granted only if {@link Role#ADMIN}</li>
 * </ul>
 *
 * <p>Banned users are blocked from all endpoints except:
 * <ul>
 *   <li>{@code GET /api/users/me} — to show their profile / ban reason</li>
 *   <li>{@code /api/tickets/**} — to allow submitting a ban-appeal support ticket</li>
 * </ul>
 *
 * <p>Public paths ({@code /api/auth/telegram}) pass through without authentication.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            Optional<User> userOpt = userRepository.findBySessionToken(token);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (Boolean.TRUE.equals(user.getIsBanned())) {
                    String path = request.getRequestURI();
                    boolean allowed = path.equals("/api/users/me")
                        || path.startsWith("/api/tickets");
                    if (!allowed) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"message\":\"Your account has been banned.\",\"status\":403}");
                        return;
                    }
                }

                var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                authorities.add(user.getIsSubscribed()
                    ? new SimpleGrantedAuthority("ROLE_PREMIUM")
                    : new SimpleGrantedAuthority("ROLE_FREE"));
                if (user.getRole() == Role.ADMIN) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
