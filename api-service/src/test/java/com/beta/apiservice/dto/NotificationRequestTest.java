package com.beta.apiservice.dto;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRequestTest {
    @Test
    void validNotificationRequest() {
        NotificationRequest req = new NotificationRequest("project1", List.of("user1"), "Hello", "Test Title", "email", null, Map.of(), Map.of(), Map.of());
        assertEquals("project1", req.projectId());
        assertEquals(List.of("user1"), req.recipients());
        assertEquals("Hello", req.message());
        assertEquals("Test Title", req.title());
        assertEquals("email", req.channel());
    }

    @Test
    void nullFieldsAllowed() {
        NotificationRequest req = new NotificationRequest(null, null, null, null, null, null, null, null, null);
        assertNull(req.projectId());
        assertNull(req.recipients());
        assertNull(req.message());
        assertNull(req.title());
        assertNull(req.channel());
    }
} 