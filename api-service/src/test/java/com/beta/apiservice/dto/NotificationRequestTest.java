package com.beta.apiservice.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRequestTest {
    @Test
    void validNotificationRequest() {
        NotificationRequest req = new NotificationRequest("user1", "Hello", "info");
        assertEquals("user1", req.userId());
        assertEquals("Hello", req.message());
        assertEquals("info", req.type());
    }

    @Test
    void nullFieldsAllowed() {
        NotificationRequest req = new NotificationRequest(null, null, null);
        assertNull(req.userId());
        assertNull(req.message());
        assertNull(req.type());
    }
} 