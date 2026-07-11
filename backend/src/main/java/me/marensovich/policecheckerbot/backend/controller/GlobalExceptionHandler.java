package me.marensovich.policecheckerbot.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global REST exception handler that converts exceptions into a consistent JSON error format.
 *
 * <p>All error responses follow the shape {@code {"status": <httpCode>, "message": "<reason>"}}.
 * This ensures the frontend always receives a parseable error body regardless of exception type.
 *
 * <ul>
 *   <li>{@link ResponseStatusException} — uses {@link ResponseStatusException#getReason()} as
 *       the message so callers see the exact human-readable reason set at the throw site.</li>
 *   <li>{@link MethodArgumentNotValidException} — returns the first field-level validation
 *       message from the binding result with HTTP 400.</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle {@link ResponseStatusException} thrown anywhere in the controller layer.
     *
     * <p>The response body's {@code message} field is populated from
     * {@link ResponseStatusException#getReason()} so that callers receive the human-readable
     * reason rather than a generic HTTP status description.
     *
     * @param ex the exception
     * @return {@code {"status": N, "message": "..."}}, mirroring the exception's HTTP status
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            message = ex.getStatusCode().toString();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", ex.getStatusCode().value());
        body.put("message", message);
        log.debug("ResponseStatusException: {} — {}", ex.getStatusCode().value(), message);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /**
     * Handle {@link MethodArgumentNotValidException} from {@code @Valid} bean validation.
     *
     * <p>Returns HTTP 400 with the first field error's default message.
     *
     * @param ex the validation exception
     * @return {@code {"status": 400, "message": "<first field error message>"}}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse("Validation error");
        Map<String, Object> body = Map.of("status", 400, "message", message);
        return ResponseEntity.badRequest().body(body);
    }
}
