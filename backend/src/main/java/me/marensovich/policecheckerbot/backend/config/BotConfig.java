package me.marensovich.policecheckerbot.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Telegram Bot configuration parameters.
 *
 * <p>Values are loaded from environment variables (or {@code application.properties}):
 * <ul>
 *   <li>{@code telegram.bot.token} — bot token issued by @BotFather</li>
 *   <li>{@code telegram.bot.username} — bot username without the {@code @} prefix</li>
 *   <li>{@code telegram.bot.miniapp-url} — HTTPS URL of the deployed Mini App</li>
 *   <li>{@code app.subscription.price-stars} — Premium price in Telegram Stars (default 100)</li>
 *   <li>{@code app.subscription.duration-days} — Premium duration in days (default 30)</li>
 *   <li>{@code app.live-tracking.notification-cooldown-minutes} — minimum interval between
 *       repeat alerts for the same post per user (default 10 min)</li>
 *   <li>{@code app.rate-limit.posts-per-hour} — maximum posts a user may create per hour (default 5)</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@Configuration
public class BotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.miniapp-url}")
    private String miniappUrl;

    @Value("${app.subscription.price-stars:100}")
    private int subscriptionPriceStars;

    @Value("${app.subscription.duration-days:30}")
    private int subscriptionDurationDays;

    @Value("${app.live-tracking.notification-cooldown-minutes:10}")
    private int notificationCooldownMinutes;

    @Value("${app.rate-limit.posts-per-hour:5}")
    private int postsPerHour;
}
