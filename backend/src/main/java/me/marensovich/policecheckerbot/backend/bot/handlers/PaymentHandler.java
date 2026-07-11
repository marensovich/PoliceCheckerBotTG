package me.marensovich.policecheckerbot.backend.bot.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.marensovich.policecheckerbot.backend.service.SubscriptionService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handler for Telegram Stars payments (Telegram Payments API).
 *
 * <p>Handles two stages of the payment flow:
 * <ol>
 *   <li>{@link PreCheckoutQuery} — pre-authorization check sent by Telegram before
 *       charging the user. Must be answered within 10 seconds. Always approved here.</li>
 *   <li>{@code SuccessfulPayment} — confirmation that the charge completed.
 *       Triggers subscription activation via {@link SubscriptionService}.</li>
 * </ol>
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentHandler {

    private final TelegramClient telegramClient;
    private final SubscriptionService subscriptionService;

    /**
     * Answer a {@link PreCheckoutQuery} with OK.
     *
     * <p>Per Telegram's documentation, this must be answered within 10 seconds;
     * all queries are unconditionally approved.
     *
     * @param preCheckoutQuery the pre-checkout query from Telegram
     */
    public void handlePreCheckout(PreCheckoutQuery preCheckoutQuery) {
        try {
            telegramClient.execute(AnswerPreCheckoutQuery.builder()
                .preCheckoutQueryId(preCheckoutQuery.getId())
                .ok(true)
                .build());
            log.info("PreCheckout approved: userId={}, payload={}",
                preCheckoutQuery.getFrom().getId(), preCheckoutQuery.getInvoicePayload());
        } catch (Exception e) {
            log.error("PreCheckoutQuery handling error: {}", e.getMessage());
        }
    }

    /**
     * Process a {@code SuccessfulPayment} message and activate the user's Premium subscription.
     *
     * @param message message containing the successful payment data
     */
    public void handleSuccessfulPayment(Message message) {
        Long tgId = message.getFrom().getId();
        String payload = message.getSuccessfulPayment().getInvoicePayload();

        log.info("Successful payment: tgId={}, payload={}, stars={}",
            tgId, payload, message.getSuccessfulPayment().getTotalAmount());

        subscriptionService.activateSubscription(tgId);
    }
}
