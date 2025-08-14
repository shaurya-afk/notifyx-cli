package com.beta.apiservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageStorageService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${notification.storage.ttl-days:30}")
    private int notificationTtlDays;
    
    @Value("${notification.storage.max-per-user:100}")
    private int maxNotificationsPerUser;
    
    private static final String MESSAGE_KEY_PREFIX = "message:";
    private static final String USER_MESSAGES_KEY_PREFIX = "user:messages:";
    private static final String PROJECT_MESSAGES_KEY_PREFIX = "project:messages:";
    
    /**
     * Store a message in Redis with proper indexing
     */
    public String storeMessage(String projectId, String recipient, String message, String title, 
                             String channel, Map<String, Object> metadata) {
        try {
            String messageId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", messageId);
            messageData.put("projectId", projectId);
            messageData.put("recipient", recipient);
            messageData.put("message", message);
            messageData.put("title", title);
            messageData.put("channel", channel);
            messageData.put("metadata", metadata != null ? metadata : new HashMap<>());
            messageData.put("createdAt", now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            messageData.put("status", "STORED");
            messageData.put("read", false);
            
            // Store the message with TTL
            String messageKey = MESSAGE_KEY_PREFIX + messageId;
            redisTemplate.opsForValue().set(messageKey, messageData, notificationTtlDays, TimeUnit.DAYS);
            
            // Add to user's message list
            String userMessagesKey = USER_MESSAGES_KEY_PREFIX + projectId + ":" + recipient;
            redisTemplate.opsForList().leftPush(userMessagesKey, messageId);
            
            // Trim to keep only recent messages
            redisTemplate.opsForList().trim(userMessagesKey, 0, maxNotificationsPerUser - 1);
            redisTemplate.expire(userMessagesKey, notificationTtlDays, TimeUnit.DAYS);
            
            // Add to project's message list
            String projectMessagesKey = PROJECT_MESSAGES_KEY_PREFIX + projectId;
            redisTemplate.opsForList().leftPush(projectMessagesKey, messageId);
            redisTemplate.expire(projectMessagesKey, notificationTtlDays, TimeUnit.DAYS);
            
            log.info("Message stored successfully: {} for recipient: {} in project: {}", 
                    messageId, recipient, projectId);
            
            return messageId;
            
        } catch (Exception e) {
            log.error("Failed to store message for recipient: {} in project: {}", recipient, projectId, e);
            throw new RuntimeException("Failed to store message", e);
        }
    }
    
    /**
     * Get messages for a specific user
     */
    public List<Map<String, Object>> getUserMessages(String projectId, String recipient, int limit) {
        try {
            String userMessagesKey = USER_MESSAGES_KEY_PREFIX + projectId + ":" + recipient;
            List<Object> messageIds = redisTemplate.opsForList().range(userMessagesKey, 0, limit - 1);
            
            if (messageIds == null || messageIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Map<String, Object>> messages = new ArrayList<>();
            for (Object messageId : messageIds) {
                String messageKey = MESSAGE_KEY_PREFIX + messageId;
                Object messageData = redisTemplate.opsForValue().get(messageKey);
                if (messageData instanceof Map) {
                    messages.add((Map<String, Object>) messageData);
                }
            }
            
            return messages;
            
        } catch (Exception e) {
            log.error("Failed to retrieve messages for recipient: {} in project: {}", recipient, projectId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get a specific message by ID
     */
    public Map<String, Object> getMessage(String messageId) {
        try {
            String messageKey = MESSAGE_KEY_PREFIX + messageId;
            Object messageData = redisTemplate.opsForValue().get(messageKey);
            
            if (messageData instanceof Map) {
                return (Map<String, Object>) messageData;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Failed to retrieve message: {}", messageId, e);
            return null;
        }
    }
    
    /**
     * Mark a message as read
     */
    public boolean markMessageAsRead(String messageId) {
        try {
            String messageKey = MESSAGE_KEY_PREFIX + messageId;
            Object messageData = redisTemplate.opsForValue().get(messageKey);
            
            if (messageData instanceof Map) {
                Map<String, Object> message = (Map<String, Object>) messageData;
                message.put("read", true);
                message.put("readAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                
                redisTemplate.opsForValue().set(messageKey, message, notificationTtlDays, TimeUnit.DAYS);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Failed to mark message as read: {}", messageId, e);
            return false;
        }
    }
    
    /**
     * Get unread message count for a user
     */
    public long getUnreadMessageCount(String projectId, String recipient) {
        try {
            List<Map<String, Object>> messages = getUserMessages(projectId, recipient, maxNotificationsPerUser);
            return messages.stream()
                    .filter(message -> !Boolean.TRUE.equals(message.get("read")))
                    .count();
                    
        } catch (Exception e) {
            log.error("Failed to get unread count for recipient: {} in project: {}", recipient, projectId, e);
            return 0;
        }
    }
    
    /**
     * Delete a message
     */
    public boolean deleteMessage(String messageId, String projectId, String recipient) {
        try {
            String messageKey = MESSAGE_KEY_PREFIX + messageId;
            String userMessagesKey = USER_MESSAGES_KEY_PREFIX + projectId + ":" + recipient;
            String projectMessagesKey = PROJECT_MESSAGES_KEY_PREFIX + projectId;
            
            // Remove from Redis
            redisTemplate.delete(messageKey);
            redisTemplate.opsForList().remove(userMessagesKey, 1, messageId);
            redisTemplate.opsForList().remove(projectMessagesKey, 1, messageId);
            
            log.info("Message deleted successfully: {}", messageId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to delete message: {}", messageId, e);
            return false;
        }
    }
    
    /**
     * Get message statistics for a project
     */
    public Map<String, Object> getProjectMessageStats(String projectId) {
        try {
            String projectMessagesKey = PROJECT_MESSAGES_KEY_PREFIX + projectId;
            Long totalMessages = redisTemplate.opsForList().size(projectMessagesKey);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("projectId", projectId);
            stats.put("totalMessages", totalMessages != null ? totalMessages : 0);
            stats.put("ttlDays", notificationTtlDays);
            stats.put("maxPerUser", maxNotificationsPerUser);
            
            return stats;
            
        } catch (Exception e) {
            log.error("Failed to get project stats for project: {}", projectId, e);
            return new HashMap<>();
        }
    }
    
    /**
     * Check if Redis is healthy
     */
    public boolean isRedisHealthy() {
        try {
            String testKey = "health:check:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "ok", 10, TimeUnit.SECONDS);
            String value = (String) redisTemplate.opsForValue().get(testKey);
            return "ok".equals(value);
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return false;
        }
    }
}
