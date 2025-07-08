package com.beta.apiservice.controller;

import com.beta.apiservice.dto.NotificationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequest request) throws JsonProcessingException {
        try {
            String payload = objectMapper.writeValueAsString(request);
            kafkaTemplate.send("notifyx.notifications",request.userId(), payload);
            return ResponseEntity.ok(Map.of("message", "Notification Sent!"));
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to serialize notification.");
        }
    }

//      TODO(make the redis template!)
//    @GetMapping("/status/{userId}")
//    public ResponseEntity<List<String>> getNotificationStatus(@PathVariable String userId) {
//        List<String> notifications = redisTemplate.opsForList()
//                .range("user:" + userId + ":notifications", 0, 10);
//        return ResponseEntity.ok(notifications);
//    }


    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkSendNotification(@RequestBody List<NotificationRequest> requests) {
        int success = 0;
        int failed = 0;

        for (NotificationRequest req : requests) {
            try {
                String payload = objectMapper.writeValueAsString(req);
                kafkaTemplate.send("notifyx.notifications", req.userId(), payload);
                success++;
            } catch (JsonProcessingException e) {
                log.warn("Skipping invalid payload for user: {}", req.userId());
                failed++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bulk notification request processed");
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
}
