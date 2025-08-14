package com.beta.notifierservice.kafka;

import com.beta.notifierservice.service.NotificationChannel;
import com.beta.notifierservice.service.NotificationStatusService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class Consumer {

    private final ObjectMapper objectMapper;
    private final NotificationStatusService notificationStatusService;
    private final List<NotificationChannel> notificationChannels;

    @KafkaListener(id = "notifierServiceConsumer", topics = "${app.kafka.topic:notifyx_test}", groupId = "notifier-service-group")
    public void listen(String value,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("Consumed event from topic {}: key = {}", topic, key);
        
        try {
            JsonNode notificationData = objectMapper.readTree(value);
            processNotification(notificationData, key);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse notification data", e);
        } catch (Exception e) {
            log.error("Failed to process notification", e);
        }
    }
    
    private void processNotification(JsonNode notificationData, String notificationId) {
        try {
            String projectId = notificationData.get("projectId").asText();
            String channel = notificationData.get("channel").asText();
            String message = notificationData.get("message").asText();
            String title = notificationData.has("title") ? notificationData.get("title").asText() : null;
            
            JsonNode recipientsNode = notificationData.get("recipients");
            JsonNode channelConfigNode = notificationData.get("channelConfig");
            
            Map<String, Object> channelConfig = null;
            if (channelConfigNode != null && !channelConfigNode.isNull()) {
                channelConfig = objectMapper.convertValue(channelConfigNode, Map.class);
            }
            
            // Find the appropriate channel
            NotificationChannel targetChannel = notificationChannels.stream()
                .filter(ch -> ch.supports(channel))
                .findFirst()
                .orElse(null);
            
            if (targetChannel == null) {
                log.error("No channel found for type: {}", channel);
                updateNotificationStatus(notificationId, "FAILED", "Channel not supported: " + channel);
                return;
            }
            
            // Send to each recipient
            boolean allSuccess = true;
            for (JsonNode recipientNode : recipientsNode) {
                String recipient = recipientNode.asText();
                
                boolean success = targetChannel.send(recipient, message, title, channelConfig);
                if (!success) {
                    allSuccess = false;
                    log.warn("Failed to send notification to recipient: {}", recipient);
                }
            }
            
            // Update status
            String status = allSuccess ? "DELIVERED" : "PARTIALLY_DELIVERED";
            updateNotificationStatus(notificationId, status, null);
            
        } catch (Exception e) {
            log.error("Failed to process notification: {}", notificationId, e);
            updateNotificationStatus(notificationId, "FAILED", e.getMessage());
        }
    }
    
    private void updateNotificationStatus(String notificationId, String status, String errorMessage) {
        try {
            Map<String, Object> statusData = Map.of(
                "notificationId", notificationId,
                "status", status,
                "timestamp", System.currentTimeMillis(),
                "errorMessage", errorMessage
            );
            
            notificationStatusService.save(notificationId, statusData);
            log.info("Updated notification status: {} -> {}", notificationId, status);
            
        } catch (Exception e) {
            log.error("Failed to update notification status: {}", notificationId, e);
        }
    }
}


