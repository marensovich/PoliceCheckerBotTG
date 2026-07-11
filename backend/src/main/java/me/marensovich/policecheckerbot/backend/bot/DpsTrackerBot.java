package me.marensovich.policecheckerbot.backend.bot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.bot.commands.*;
import me.marensovich.policecheckerbot.backend.bot.handlers.LocationHandler;
import me.marensovich.policecheckerbot.backend.bot.handlers.PaymentHandler;
import me.marensovich.policecheckerbot.backend.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

/**
 * Main Telegram bot for DPS Tracker.
 *
 * <p>Implements {@link SpringLongPollingBot} for automatic integration with
 * {@code telegrambots-springboot-longpolling-starter}. All incoming {@link Update}
 * objects are dispatched by type to dedicated handler components.
 *
 * <p>Supported commands:
 * <ul>
 *   <li>{@code /start} — welcome message with Mini App button</li>
 *   <li>{@code /help} — full command list</li>
 *   <li>{@code /subscribe} — Premium subscription info + Telegram Stars invoice</li>
 *   <li>{@code /status} — subscription and tracking status</li>
 *   <li>{@code /live on|off} — toggle live tracking (Premium only)</li>
 *   <li>{@code /radius [km]} — set notification radius</li>
 *   <li>{@code /addpost} — create a post via native location share</li>
 *   <li>{@code /nearby} — show DPS posts near the user's last known location</li>
 * </ul>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 * @see StartCommandHandler
 * @see PaymentHandler
 * @see LocationHandler
 */
@Slf4j
@Component
public class DpsTrackerBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private final BotConfig botConfig;
    private final StartCommandHandler startHandler;
    private final HelpCommandHandler helpHandler;
    private final SubscribeCommandHandler subscribeHandler;
    private final StatusCommandHandler statusHandler;
    private final LiveCommandHandler liveHandler;
    private final RadiusCommandHandler radiusHandler;
    private final AddPostCommandHandler addPostHandler;
    private final NearbyCommandHandler nearbyHandler;
    private final PaymentHandler paymentHandler;
    private final LocationHandler locationHandler;

    public DpsTrackerBot(BotConfig botConfig,
                         StartCommandHandler startHandler,
                         HelpCommandHandler helpHandler,
                         SubscribeCommandHandler subscribeHandler,
                         StatusCommandHandler statusHandler,
                         LiveCommandHandler liveHandler,
                         RadiusCommandHandler radiusHandler,
                         AddPostCommandHandler addPostHandler,
                         NearbyCommandHandler nearbyHandler,
                         PaymentHandler paymentHandler,
                         LocationHandler locationHandler) {
        this.botConfig = botConfig;
        this.startHandler = startHandler;
        this.helpHandler = helpHandler;
        this.subscribeHandler = subscribeHandler;
        this.statusHandler = statusHandler;
        this.liveHandler = liveHandler;
        this.radiusHandler = radiusHandler;
        this.addPostHandler = addPostHandler;
        this.nearbyHandler = nearbyHandler;
        this.paymentHandler = paymentHandler;
        this.locationHandler = locationHandler;
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    /**
     * Process a single incoming {@link Update} from Telegram.
     *
     * <p>Dispatch priority:
     * <ol>
     *   <li>Pre-checkout query (payment flow)</li>
     *   <li>Successful payment</li>
     *   <li>Location message</li>
     *   <li>Text command</li>
     * </ol>
     *
     * @param update incoming update from Telegram
     */
    public void consume(Update update) {
        try {
            if (update.hasPreCheckoutQuery()) {
                paymentHandler.handlePreCheckout(update.getPreCheckoutQuery());
                return;
            }

            if (!update.hasMessage()) return;
            Message message = update.getMessage();

            if (message.hasSuccessfulPayment()) {
                paymentHandler.handleSuccessfulPayment(message);
                return;
            }

            if (message.hasLocation()) {
                locationHandler.handle(message);
                return;
            }

            if (message.hasText()) {
                routeCommand(message);
            }

        } catch (Exception e) {
            log.error("Update processing error: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a batch of incoming updates sequentially.
     *
     * @param updates list of updates from the long-polling response
     */
    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::consume);
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private void routeCommand(Message message) {
        String text = message.getText().trim();

        if (text.startsWith("/start")) {
            startHandler.handle(message);
        } else if (text.startsWith("/help")) {
            helpHandler.handle(message);
        } else if (text.startsWith("/subscribe")) {
            subscribeHandler.handle(message);
        } else if (text.startsWith("/status")) {
            statusHandler.handle(message);
        } else if (text.startsWith("/live")) {
            liveHandler.handle(message);
        } else if (text.startsWith("/radius")) {
            radiusHandler.handle(message);
        } else if (text.startsWith("/addpost")) {
            addPostHandler.handle(message);
        } else if (text.startsWith("/nearby")) {
            nearbyHandler.handle(message);
        } else {
            log.debug("Unknown command from tgId={}: {}", message.getFrom().getId(), text);
        }
    }
}
