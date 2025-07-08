package com.beta.notifierservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.*;

class NotificationConsumerTest {
    private RedisTemplate<String, String> redisTemplate;
    private ListOperations<String, String> listOperations;
    private NotificationConsumer consumer;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        listOperations = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        consumer = new NotificationConsumer(redisTemplate);
    }

    @Test
    void listen_validMessage_savesToRedis() throws Exception {
        String json = "{\"userId\":\"user1\",\"message\":\"Hello\"}";
        consumer.listen(json);
        verify(listOperations, times(1)).leftPush(eq("user:user1:notifications"), eq(json));
    }

    @Test
    void listen_invalidMessage_logsError() {
        String invalidJson = "not a json";
        consumer.listen(invalidJson);
        // Should not throw, should log error, and not call Redis
        verify(listOperations, never()).leftPush(anyString(), anyString());
    }
} 