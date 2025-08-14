package com.beta.notifierservice.kafka;

import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
public class Producer {

    private static final Logger logger = LoggerFactory.getLogger(Producer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topic:notifyx_test}")
    private String topic;

    public Producer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String key, String value) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, value);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("Produced event to topic {}: key = {} value = {}", topic, key, value);
            } else {
                logger.error("Failed to produce event to topic {}: key = {} value = {}", topic, key, value, ex);
            }
        });
    }
}


