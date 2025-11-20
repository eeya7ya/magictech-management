package com.magictech.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration for real-time notifications
 * Enables push notifications without polling
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Register STOMP endpoints for WebSocket connections
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint for desktop JavaFX clients and web clients
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*") // Allow all origins (restrict in production)
                .withSockJS(); // Fallback for browsers that don't support WebSocket
    }

    /**
     * Configure message broker for pub/sub messaging
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for sending messages to clients
        // Messages sent to /topic or /queue will be routed to subscribers
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages from clients to server
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        registry.setUserDestinationPrefix("/user");
    }
}
