package com.beta.apiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageStorageServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ListOperations<String, Object> listOps;

    private MessageStorageService messageStorageService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        messageStorageService = new MessageStorageService(redisTemplate, objectMapper);
        
        // Set default values for configuration
        ReflectionTestUtils.setField(messageStorageService, "notificationTtlDays", 30);
        ReflectionTestUtils.setField(messageStorageService, "maxNotificationsPerUser", 100);
        
        // Setup mocks
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
    }

    @Test
    void storeMessage_shouldReturnMessageId() {
        // Given
        String projectId = "project1";
        String recipient = "user1@example.com";
        String message = "Test message";
        String title = "Test title";
        String channel = "webhook";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "test");

        // When
        String messageId = messageStorageService.storeMessage(projectId, recipient, message, title, channel, metadata);

        // Then
        assertNotNull(messageId);
        assertFalse(messageId.isEmpty());
        
        // Verify Redis operations were called
        verify(valueOps, times(1)).set(anyString(), any(Map.class), eq(30L), eq(java.util.concurrent.TimeUnit.DAYS));
        verify(listOps, times(2)).leftPush(anyString(), anyString());
        verify(listOps, times(2)).trim(anyString(), eq(0L), eq(99L));
        verify(redisTemplate, times(2)).expire(anyString(), eq(30L), eq(java.util.concurrent.TimeUnit.DAYS));
    }

    @Test
    void getUserMessages_shouldReturnMessages() {
        // Given
        String projectId = "project1";
        String recipient = "user1@example.com";
        String messageId = "msg-123";
        
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", messageId);
        messageData.put("message", "Test message");
        
        when(listOps.range(anyString(), eq(0L), eq(9L)))
            .thenReturn(List.of(messageId));
        when(valueOps.get(anyString()))
            .thenReturn(messageData);

        // When
        List<Map<String, Object>> messages = messageStorageService.getUserMessages(projectId, recipient, 10);

        // Then
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals(messageId, messages.get(0).get("id"));
    }

    @Test
    void getUserMessages_emptyList_shouldReturnEmptyList() {
        // Given
        String projectId = "project1";
        String recipient = "user1@example.com";
        
        when(listOps.range(anyString(), eq(0L), eq(9L)))
            .thenReturn(List.of());

        // When
        List<Map<String, Object>> messages = messageStorageService.getUserMessages(projectId, recipient, 10);

        // Then
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void markMessageAsRead_shouldReturnTrue() {
        // Given
        String messageId = "msg-123";
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", messageId);
        messageData.put("read", false);
        
        when(valueOps.get(anyString())).thenReturn(messageData);

        // When
        boolean result = messageStorageService.markMessageAsRead(messageId);

        // Then
        assertTrue(result);
        verify(valueOps, times(1)).set(anyString(), any(Map.class), eq(30L), eq(java.util.concurrent.TimeUnit.DAYS));
    }

    @Test
    void markMessageAsRead_messageNotFound_shouldReturnFalse() {
        // Given
        String messageId = "msg-123";
        when(valueOps.get(anyString())).thenReturn(null);

        // When
        boolean result = messageStorageService.markMessageAsRead(messageId);

        // Then
        assertFalse(result);
    }

    @Test
    void getUnreadMessageCount_shouldReturnCount() {
        // Given
        String projectId = "project1";
        String recipient = "user1@example.com";
        String messageId = "msg-123";
        
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", messageId);
        messageData.put("read", false);
        
        when(listOps.range(anyString(), eq(0L), eq(99L)))
            .thenReturn(List.of(messageId));
        when(valueOps.get(anyString()))
            .thenReturn(messageData);

        // When
        long count = messageStorageService.getUnreadMessageCount(projectId, recipient);

        // Then
        assertEquals(1, count);
    }

    @Test
    void isRedisHealthy_shouldReturnTrue() {
        // Given
        doNothing().when(valueOps).set(anyString(), eq("ok"), eq(10L), eq(java.util.concurrent.TimeUnit.SECONDS));
        when(valueOps.get(anyString())).thenReturn("ok");

        // When
        boolean result = messageStorageService.isRedisHealthy();

        // Then
        assertTrue(result);
    }
}
