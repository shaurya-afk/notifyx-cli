package com.beta.notifierservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "notifyx.notifications", groupId = "notifyx")
    public void listen(String messageJson) {
        log.info("✅ Kafka message received: {}", messageJson);

        try {
            JsonNode node = objectMapper.readTree(messageJson);
            String userId = node.get("userId").asText();

            String redisKey = "user:" + userId + ":notifications";

            redisTemplate.opsForList().leftPush(redisKey, messageJson);

            log.info("📦 Saved to Redis list: notifications");
        } catch (Exception e) {
            log.error("❌ Failed to push to Redis", e);
        }
    }
}
