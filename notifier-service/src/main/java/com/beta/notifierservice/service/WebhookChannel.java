package com.beta.notifierservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookChannel implements NotificationChannel {
    
    private final RestTemplate restTemplate;
    
    @Override
    public String getChannelType() {
        return "webhook";
    }
    
    @Override
    public boolean send(String recipient, String message, String title, Map<String, Object> config) {
        try {
            String webhookUrl = (String) config.get("url");
            String secret = (String) config.get("secret");
            
            if (webhookUrl == null) {
                log.error("Webhook URL not provided in config");
                return false;
            }
            
            // Prepare webhook payload
            Map<String, Object> payload = Map.of(
                "recipient", recipient,
                "message", message,
                "title", title,
                "timestamp", System.currentTimeMillis(),
                "channel", "webhook"
            );
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            if (secret != null) {
                // Add signature header if secret is provided
                String signature = generateSignature(payload, secret);
                headers.set("X-Webhook-Signature", signature);
            }
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            // Send webhook
            ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("Webhook sent to {}: {}", webhookUrl, success ? "SUCCESS" : "FAILED");
            
            return success;
            
        } catch (Exception e) {
            log.error("Failed to send webhook notification to {}", recipient, e);
            return false;
        }
    }
    
    @Override
    public boolean supports(String channelType) {
        return "webhook".equals(channelType);
    }
    
    private String generateSignature(Map<String, Object> payload, String secret) {
        // Simple signature generation - in production, use proper HMAC
        return "sha256=" + payload.hashCode() + secret.hashCode();
    }
}
