package com.beta.apiservice.controller;

import com.beta.apiservice.service.MessageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    
    private final MessageStorageService messageStorageService;
    
    /**
     * Store a new message
     */
    @PostMapping("/store")
    public ResponseEntity<?> storeMessage(
            @RequestBody Map<String, Object> request,
            @RequestAttribute("projectId") String projectId) {
        
        try {
            String recipient = (String) request.get("recipient");
            String message = (String) request.get("message");
            String title = (String) request.get("title");
            String channel = (String) request.get("channel");
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) request.get("metadata");
            
            if (recipient == null || message == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "recipient and message are required"));
            }
            
            String messageId = messageStorageService.storeMessage(
                    projectId, recipient, message, title, channel, metadata);
            
            Map<String, Object> response = new HashMap<>();
            response.put("messageId", messageId);
            response.put("projectId", projectId);
            response.put("recipient", recipient);
            response.put("status", "STORED");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Failed to store message for project: {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to store message: " + e.getMessage()));
        }
    }
    
    /**
     * Get messages for a specific user
     */
    @GetMapping("/user/{recipient}")
    public ResponseEntity<?> getUserMessages(
            @PathVariable String recipient,
            @RequestParam(defaultValue = "10") int limit,
            @RequestAttribute("projectId") String projectId) {
        
        try {
            List<Map<String, Object>> messages = messageStorageService.getUserMessages(projectId, recipient, limit);
            long unreadCount = messageStorageService.getUnreadMessageCount(projectId, recipient);
            
            Map<String, Object> response = new HashMap<>();
            response.put("projectId", projectId);
            response.put("recipient", recipient);
            response.put("messages", messages);
            response.put("totalMessages", messages.size());
            response.put("unreadCount", unreadCount);
            response.put("limit", limit);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve messages for recipient: {} in project: {}", recipient, projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve messages: " + e.getMessage()));
        }
    }
    
    /**
     * Get a specific message by ID
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable String messageId) {
        try {
            Map<String, Object> message = messageStorageService.getMessage(messageId);
            
            if (message == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            log.error("Failed to retrieve message: {}", messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve message: " + e.getMessage()));
        }
    }
    
    /**
     * Mark a message as read
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<?> markMessageAsRead(@PathVariable String messageId) {
        try {
            boolean success = messageStorageService.markMessageAsRead(messageId);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "messageId", messageId,
                        "status", "READ",
                        "message", "Message marked as read successfully"
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Failed to mark message as read: {}", messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark message as read: " + e.getMessage()));
        }
    }
    
    /**
     * Get unread message count for a user
     */
    @GetMapping("/user/{recipient}/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @PathVariable String recipient,
            @RequestAttribute("projectId") String projectId) {
        
        try {
            long unreadCount = messageStorageService.getUnreadMessageCount(projectId, recipient);
            
            Map<String, Object> response = new HashMap<>();
            response.put("projectId", projectId);
            response.put("recipient", recipient);
            response.put("unreadCount", unreadCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get unread count for recipient: {} in project: {}", recipient, projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get unread count: " + e.getMessage()));
        }
    }
    
    /**
     * Delete a message
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String messageId,
            @RequestParam String recipient,
            @RequestAttribute("projectId") String projectId) {
        
        try {
            boolean success = messageStorageService.deleteMessage(messageId, projectId, recipient);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "messageId", messageId,
                        "status", "DELETED",
                        "message", "Message deleted successfully"
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Failed to delete message: {}", messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete message: " + e.getMessage()));
        }
    }
    
    /**
     * Get project message statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getProjectStats(@RequestAttribute("projectId") String projectId) {
        try {
            Map<String, Object> stats = messageStorageService.getProjectMessageStats(projectId);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to get project stats for project: {}", projectId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get project stats: " + e.getMessage()));
        }
    }
    
    /**
     * Health check for message storage
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            boolean redisHealthy = messageStorageService.isRedisHealthy();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", redisHealthy ? "UP" : "DOWN");
            response.put("service", "Message Storage");
            response.put("redis", redisHealthy ? "UP" : "DOWN");
            response.put("timestamp", System.currentTimeMillis());
            
            HttpStatus status = redisHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            log.error("Message storage health check failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Health check failed: " + e.getMessage()));
        }
    }
}
