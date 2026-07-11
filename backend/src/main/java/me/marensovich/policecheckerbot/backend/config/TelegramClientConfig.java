package me.marensovich.policecheckerbot.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Factory configuration for the Telegram HTTP client.
 *
 * <p>Creates a singleton {@link TelegramClient} backed by OkHttp for sending
 * requests to the Telegram Bot API. Injected into all services and command
 * handlers that need to push messages.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class TelegramClientConfig {

    /**
     * Create an OkHttp-based Telegram client using the configured bot token.
     *
     * @param botConfig bot configuration containing the token
     * @return {@link TelegramClient} for executing Bot API requests
     */
    @Bean
    public TelegramClient telegramClient(BotConfig botConfig) {
        return new OkHttpTelegramClient(botConfig.getBotToken());
    }
}
