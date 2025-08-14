package com.beta.apiservice.controller;

import com.beta.apiservice.dto.NotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import com.beta.apiservice.service.NotificationService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendNotification_validRequest_returnsOk() throws Exception {
        NotificationRequest req = new NotificationRequest(
            "project1", 
            List.of("user1"), 
            "Hello", 
            "Test Title", 
            "email", 
            null, 
            Map.of(), 
            Map.of(), 
            Map.of()
        );
        // Mock service to avoid serialization issues
        Mockito.when(notificationService.sendNotification(Mockito.any(), Mockito.anyString())).thenReturn("notif-1");
        mockMvc.perform(post("/api/notification/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification Sent!"));
    }

    @Test
    void sendNotification_invalidRequest_returnsServerError() throws Exception {
        // Invalid JSON (missing fields)
        String invalidJson = "{";
        mockMvc.perform(post("/api/notification/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkSendNotification_mixedRequests_returnsAccepted() throws Exception {
        List<NotificationRequest> requests = List.of(
                new NotificationRequest("project1", List.of("user1"), "Hello", "Title1", "email", null, Map.of(), Map.of(), Map.of()),
                new NotificationRequest("project1", List.of("user2"), null, "Title2", "email", null, Map.of(), Map.of(), Map.of()) // null message
        );
        mockMvc.perform(post("/api/notification/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void getVersion_returnsVersionInfo() throws Exception {
        mockMvc.perform(get("/api/notification/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }
} 