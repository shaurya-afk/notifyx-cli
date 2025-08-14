package com.beta.apiservice.controller;

import com.beta.apiservice.dto.NotificationRequest;
import com.beta.apiservice.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(
            @RequestBody NotificationRequest request,
            @RequestAttribute("projectId") String projectId){
        try{
            String notificationId = notificationService.sendNotification(request, projectId);
            return ResponseEntity.ok(Map.of(
                    "message", "Notification queued for delivery!",
                    "notificationId", notificationId,
                    "projectId", projectId
            ));
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to serialize notification"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send notification: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{recipient}")
    public ResponseEntity<Map<String, Object>> getNotificationStatus(
            @PathVariable String recipient,
            @RequestParam(defaultValue = "10") int limit,
            @RequestAttribute("projectId") String projectId) {

        List<Object> notifications = notificationService.getUserNotifications(projectId, recipient, limit);
        long totalCount = notificationService.getUserNotificationCount(projectId, recipient);

        Map<String, Object> response = new HashMap<>();
        response.put("projectId", projectId);
        response.put("recipient", recipient);
        response.put("notifications", notifications);
        response.put("totalCount", totalCount);
        response.put("limit", limit);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/notification/{notificationId}")
    public ResponseEntity<?> getSingleNotificationStatus(@PathVariable String notificationId) {
        Object status = notificationService.getNotificationStatus(notificationId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(status);
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkSendNotification(
            @RequestBody List<NotificationRequest> requests,
            @RequestAttribute("projectId") String projectId) {
        int success = 0;
        int failed = 0;

        for (NotificationRequest req : requests) {
            try {
                notificationService.sendNotification(req, projectId);
                success++;
            } catch (JsonProcessingException e) {
                log.warn("Skipping invalid payload for project: {}", projectId);
                failed++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bulk notification request processed");
        response.put("projectId", projectId);
        response.put("total", requests.size());
        response.put("sent", success);
        response.put("failed", failed);

        return ResponseEntity.accepted().body(response);
    }


    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        return ResponseEntity.ok(Map.of(
                "version", "1.0.0",
                "build", "stable",
                "name", "NotifyX API"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "NotifyX API");
        response.put("timestamp", System.currentTimeMillis());

        // Check Redis health
        boolean redisHealthy = notificationService.isRedisHealthy();
        response.put("redis", redisHealthy ? "UP" : "DOWN");

        HttpStatus status = redisHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(response);
    }
}
