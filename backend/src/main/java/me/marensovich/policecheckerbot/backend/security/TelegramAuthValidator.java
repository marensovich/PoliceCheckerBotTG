package me.marensovich.policecheckerbot.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates Telegram WebApp {@code initData} signatures using the official algorithm.
 *
 * <p>Validation algorithm (per Telegram documentation):
 * <ol>
 *   <li>Parse {@code initData} as a URL-encoded key-value string.</li>
 *   <li>Extract and remove the {@code hash} field.</li>
 *   <li>Sort remaining fields lexicographically and join them with {@code \n}.</li>
 *   <li>Compute {@code HMAC-SHA256(data_check_string, secret_key)}, where
 *       {@code secret_key = HMAC-SHA256(bot_token, "WebAppData")}.</li>
 *   <li>Compare the result (hex) with the extracted {@code hash}.</li>
 * </ol>
 *
 * <p>Additionally validates {@code auth_date}: the data must not be older than
 * {@code telegram.auth.max-age-seconds} (default 86 400 s = 24 h).
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class TelegramAuthValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String botToken;
    private final long maxAuthAgeSeconds;

    public TelegramAuthValidator(
        @Value("${telegram.bot.token}") String botToken,
        @Value("${telegram.auth.max-age-seconds:86400}") long maxAuthAgeSeconds
    ) {
        this.botToken = botToken;
        this.maxAuthAgeSeconds = maxAuthAgeSeconds;
    }

    /**
     * Validate the {@code initData} string sent by the Telegram Mini App client.
     *
     * @param initData URL-encoded string from {@code window.Telegram.WebApp.initData}
     * @return {@code true} if the HMAC signature is correct and the data is not stale
     */
    public boolean validate(String initData) {
        try {
            Map<String, String> params = parseParams(initData);

            String hash = params.remove("hash");
            if (hash == null) {
                log.warn("initData is missing the hash field");
                return false;
            }

            String authDateStr = params.get("auth_date");
            if (authDateStr == null) {
                log.warn("initData is missing the auth_date field");
                return false;
            }

            long authDate = Long.parseLong(authDateStr);
            long now = System.currentTimeMillis() / 1000;
            long ageSeconds = now - authDate;
            if (ageSeconds > maxAuthAgeSeconds) {
                log.warn("initData is stale: auth_date={}, now={}, age={}s (limit={}s)",
                    authDate, now, ageSeconds, maxAuthAgeSeconds);
                return false;
            }

            String dataCheckString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

            byte[] secretKey = hmac(botToken.getBytes(StandardCharsets.UTF_8), "WebAppData");
            byte[] expectedHash = hmac(dataCheckString.getBytes(StandardCharsets.UTF_8), secretKey);
            String expectedHex = toHex(expectedHash);

            return expectedHex.equalsIgnoreCase(hash);

        } catch (Exception e) {
            log.error("initData validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract the Telegram user ID from {@code initData} without re-validating the signature.
     * Call only after {@link #validate} returns {@code true}.
     *
     * @param initData URL-encoded initData string
     * @return Telegram user ID
     * @throws IllegalArgumentException if the {@code user} field is missing
     */
    public Long extractTgId(String initData) {
        Map<String, String> params = parseParams(initData);
        String userJson = params.get("user");
        if (userJson == null) {
            throw new IllegalArgumentException("Field 'user' is missing from initData");
        }
        return extractJsonLongField(userJson, "id");
    }

    /**
     * Extract the Telegram username from {@code initData}.
     *
     * @param initData URL-encoded initData string
     * @return username or {@code null} if not present
     */
    public String extractUsername(String initData) {
        Map<String, String> params = parseParams(initData);
        String userJson = params.get("user");
        if (userJson == null) return null;
        return extractJsonStringField(userJson, "username");
    }

    /**
     * Extract the user's first name from {@code initData}.
     *
     * @param initData URL-encoded initData string
     * @return first name or {@code null} if not present
     */
    public String extractFirstName(String initData) {
        Map<String, String> params = parseParams(initData);
        String userJson = params.get("user");
        if (userJson == null) return null;
        return extractJsonStringField(userJson, "first_name");
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private Map<String, String> parseParams(String initData) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : initData.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    private byte[] hmac(byte[] data, String key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        return mac.doFinal(data);
    }

    private byte[] hmac(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(key, HMAC_SHA256));
        return mac.doFinal(data);
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Long extractJsonLongField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) return Long.parseLong(m.group(1));
        throw new IllegalArgumentException("Field '" + field + "' not found in user JSON");
    }

    private String extractJsonStringField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
