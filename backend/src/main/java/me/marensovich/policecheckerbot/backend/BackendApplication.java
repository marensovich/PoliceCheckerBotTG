package me.marensovich.policecheckerbot.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the DPS Tracker backend application.
 *
 * <p>DPS Tracker is a Telegram Mini App for crowd-sourced tracking of traffic police
 * (DPS) posts. It provides a real-time collaborative map, WebSocket-based live
 * location sharing, push notifications via Telegram, and a Premium subscription
 * purchased with Telegram Stars.
 *
 * <p>{@link EnableScheduling} activates scheduled tasks: post expiry sweeps,
 * subscription expiry deactivations, and live-tracking cleanup.
 *
 * @author marensovich
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
