package me.marensovich.policecheckerbot.backend.bot.commands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Handler for the {@code /help} command.
 *
 * <p>Sends the complete list of available bot commands with brief descriptions.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HelpCommandHandler {

    private final TelegramClient telegramClient;

    /**
     * Handle the {@code /help} command.
     *
     * @param message incoming message
     */
    public void handle(Message message) {
        String text =
            "📖 *Справка по командам DPS Tracker:*\n\n" +
            "🗺 `/start` — открыть карту (MiniApp)\n" +
            "❓ `/help` — список команд\n" +
            "⭐ `/subscribe` — информация о Premium-подписке\n" +
            "📊 `/status` — текущий статус и настройки\n" +
            "🟢 `/live on` — включить live-трекинг _(только Premium)_\n" +
            "🔴 `/live off` — выключить live-трекинг\n" +
            "📏 `/radius [км]` — установить радиус уведомлений _(например: /radius 3)_\n" +
            "📍 `/addpost` — добавить пост ДПС через бота\n" +
            "🔍 `/nearby` — посты ДПС рядом с вами\n\n" +
            "💡 _Для добавления поста через карту — долгое нажатие на нужную точку._";

        try {
            telegramClient.execute(SendMessage.builder()
                .chatId(message.getChatId().toString())
                .text(text)
                .parseMode("Markdown")
                .build());
        } catch (Exception e) {
            log.error("/help handler error: {}", e.getMessage());
        }
    }
}
