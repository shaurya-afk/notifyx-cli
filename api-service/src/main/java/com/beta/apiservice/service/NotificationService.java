package com.beta.apiservice.service;

import com.beta.apiservice.dto.NotificationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.beta.apiservice.kafka.Producer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Producer kafkaProducer;
    private final MessageStorageService messageStorageService;

    private static final String NOTIFICATION_KEY_PREFIX = "user:notifications:";
    private static final String STATUS_KEY_PREFIX = "notification:status:";
    private static final int MAX_NOTIFICATIONS_PER_USER = 100;
    private static final int NOTIFICATION_TTL_DAYS = 30;

    public String sendNotification(NotificationRequest request, String projectId) throws JsonProcessingException {
        String notificationId = UUID.randomUUID().toString();

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("id", notificationId);
        notificationData.put("projectId", projectId);
        notificationData.put("recipients", request.recipients());
        notificationData.put("message", request.message());
        notificationData.put("title", request.title());
        notificationData.put("channel", request.channel());
        notificationData.put("template", request.template());
        notificationData.put("variables", request.variables());
        notificationData.put("channelConfig", request.channelConfig());
        notificationData.put("metadata", request.metadata());
        notificationData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        notificationData.put("status", "PENDING");

        String payload = objectMapper.writeValueAsString(notificationData);
        // Send to Kafka (fire-and-forget)
        kafkaProducer.sendMessage(notificationId, payload);

        // Store message for each recipient in Redis for user retrieval
        for (String recipient : request.recipients()) {
            try {
                // Store in message storage for user access
                messageStorageService.storeMessage(
                    projectId,
                    recipient,
                    request.message(),
                    request.title(),
                    request.channel(),
                    request.metadata()
                );
                
                // Also store notification status for delivery tracking
                storeNotificationStatus(projectId, recipient, notificationId, notificationData);
                
                log.info("Message stored for recipient: {} in project: {}", recipient, projectId);
            } catch (Exception e) {
                log.error("Failed to store message for recipient: {} in project: {}", recipient, projectId, e);
            }
        }

        log.info("Notification queued to Kafka for project {}: {}", projectId, notificationId);
        return notificationId;
    }

    private void storeNotificationStatus(String projectId, String recipient, String notificationId, Map<String, Object> data) {
        try {
            // Store individual notification status
            String statusKey = STATUS_KEY_PREFIX + notificationId;
            redisTemplate.opsForValue().set(statusKey, data, NOTIFICATION_TTL_DAYS, TimeUnit.DAYS);

            // Add to recipient's notification list (recent notifications)
            String recipientKey = NOTIFICATION_KEY_PREFIX + projectId + ":" + recipient;
            redisTemplate.opsForList().leftPush(recipientKey, notificationId);

            // Trim list to keep only recent notifications
            redisTemplate.opsForList().trim(recipientKey, 0, MAX_NOTIFICATIONS_PER_USER - 1);

            // Set expiration for recipient list
            redisTemplate.expire(recipientKey, NOTIFICATION_TTL_DAYS, TimeUnit.DAYS);

        } catch (Exception e) {
            log.warn("Failed to store notification status in Redis: {}", notificationId, e);
        }
    }

    public List<Object> getUserNotifications(String projectId, String recipient, int limit) {
        try {
            String recipientKey = NOTIFICATION_KEY_PREFIX + projectId + ":" + recipient;
            List<Object> ids = redisTemplate.opsForList().range(recipientKey, 0, limit - 1);

            if (ids == null) {
                return List.of();
            }

            return ids.stream()
                    .map(id -> redisTemplate.opsForValue().get(STATUS_KEY_PREFIX + id))
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("Failed to retrieve notifications for recipient: {} in project: {}", recipient, projectId, e);
            return List.of();
        }
    }

    public Object getNotificationStatus(String notificationId) {
        try {
            String statusKey = STATUS_KEY_PREFIX + notificationId;
            return redisTemplate.opsForValue().get(statusKey);
        } catch (Exception e) {
            log.error("Failed to get notification status: {}", notificationId, e);
            return null;
        }
    }

    public long getUserNotificationCount(String projectId, String recipient) {
        try {
            String recipientKey = NOTIFICATION_KEY_PREFIX + projectId + ":" + recipient;
            Long count = redisTemplate.opsForList().size(recipientKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Failed to get notification count for recipient: {} in project: {}", recipient, projectId, e);
            return 0;
        }
    }

    public boolean isRedisHealthy() {
        try {
            redisTemplate.opsForValue().set("health:check", "ok", 10, TimeUnit.SECONDS);
            String value = (String) redisTemplate.opsForValue().get("health:check");
            return "ok".equals(value);
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return false;
        }
    }
}
