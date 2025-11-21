package com.magictech.core.messaging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.magictech.core.messaging.constants.NotificationConstants;
import com.magictech.core.messaging.dto.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Service for listening to Redis pub/sub notifications.
 * Manages subscriptions and dispatches notifications to JavaFX UI.
 */
@Service
public class NotificationListenerService implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationListenerService.class);

    @Autowired
    private RedisMessageListenerContainer messageListenerContainer;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    private final List<Consumer<NotificationMessage>> listeners = new CopyOnWriteArrayList<>();
    private final List<ChannelTopic> subscribedChannels = new ArrayList<>();

    private boolean isInitialized = false;

    /**
     * Subscribe to Redis channels for a specific module.
     */
    public void subscribeToModule(String moduleName) {
        if (!isInitialized) {
            logger.warn("NotificationListenerService not initialized yet, deferring subscription");
            return;
        }

        try {
            String[] channels = NotificationConstants.getChannelsForModule(moduleName);

            for (String channel : channels) {
                ChannelTopic topic = new ChannelTopic(channel);

                // Check if already subscribed
                if (subscribedChannels.stream().noneMatch(t -> t.getTopic().equals(channel))) {
                    messageListenerContainer.addMessageListener(this, topic);
                    subscribedChannels.add(topic);
                    logger.info("Subscribed to channel: {}", channel);
                }
            }
        } catch (Exception e) {
            logger.error("Error subscribing to module {}: {}", moduleName, e.getMessage(), e);
        }
    }

    /**
     * Subscribe to all notification channels (for Storage module).
     */
    public void subscribeToAll() {
        if (!isInitialized) {
            logger.warn("NotificationListenerService not initialized yet, deferring subscription");
            return;
        }

        try {
            String[] allChannels = NotificationConstants.getAllModuleChannels();

            for (String channel : allChannels) {
                ChannelTopic topic = new ChannelTopic(channel);

                // Check if already subscribed
                if (subscribedChannels.stream().noneMatch(t -> t.getTopic().equals(channel))) {
                    messageListenerContainer.addMessageListener(this, topic);
                    subscribedChannels.add(topic);
                    logger.info("Subscribed to channel: {}", channel);
                }
            }
        } catch (Exception e) {
            logger.error("Error subscribing to all channels: {}", e.getMessage(), e);
        }
    }

    /**
     * Unsubscribe from a specific channel.
     */
    public void unsubscribe(String channel) {
        try {
            ChannelTopic topic = new ChannelTopic(channel);
            messageListenerContainer.removeMessageListener(this, topic);
            subscribedChannels.removeIf(t -> t.getTopic().equals(channel));
            logger.info("Unsubscribed from channel: {}", channel);
        } catch (Exception e) {
            logger.error("Error unsubscribing from channel {}: {}", channel, e.getMessage(), e);
        }
    }

    /**
     * Unsubscribe from all channels.
     */
    public void unsubscribeAll() {
        try {
            for (ChannelTopic topic : subscribedChannels) {
                messageListenerContainer.removeMessageListener(this, topic);
            }
            subscribedChannels.clear();
            logger.info("Unsubscribed from all channels");
        } catch (Exception e) {
            logger.error("Error unsubscribing from all channels: {}", e.getMessage(), e);
        }
    }

    /**
     * Register a listener to receive notifications.
     */
    public void addListener(Consumer<NotificationMessage> listener) {
        listeners.add(listener);
        logger.debug("Added notification listener, total listeners: {}", listeners.size());
    }

    /**
     * Remove a listener.
     */
    public void removeListener(Consumer<NotificationMessage> listener) {
        listeners.remove(listener);
        logger.debug("Removed notification listener, total listeners: {}", listeners.size());
    }

    /**
     * MessageListener implementation - called when a message is received.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            logger.debug("Received message on channel {}: {}", channel, body);

            // Deserialize message
            NotificationMessage notification = objectMapper.readValue(body, NotificationMessage.class);

            // Dispatch to all registered listeners
            for (Consumer<NotificationMessage> listener : listeners) {
                try {
                    listener.accept(notification);
                } catch (Exception e) {
                    logger.error("Error in notification listener: {}", e.getMessage(), e);
                }
            }

            logger.info("Dispatched notification to {} listeners: {}", listeners.size(), notification.getTitle());

        } catch (Exception e) {
            logger.error("Error processing notification message: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize the listener service.
     * Called after construction and dependency injection.
     */
    @PostConstruct
    public void initialize() {
        try {
            logger.info("Initializing NotificationListenerService");
            isInitialized = true;
            logger.info("NotificationListenerService initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing NotificationListenerService: {}", e.getMessage(), e);
        }
    }

    /**
     * Cleanup on shutdown.
     */
    @PreDestroy
    public void cleanup() {
        try {
            logger.info("Cleaning up NotificationListenerService");
            unsubscribeAll();
            listeners.clear();
        } catch (Exception e) {
            logger.error("Error cleaning up NotificationListenerService: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if listener is ready.
     */
    public boolean isReady() {
        return isInitialized && messageListenerContainer.isRunning();
    }

    /**
     * Get list of subscribed channels.
     */
    public List<String> getSubscribedChannels() {
        return subscribedChannels.stream()
            .map(ChannelTopic::getTopic)
            .toList();
    }
}
