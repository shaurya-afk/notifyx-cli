package com.beta.notifierservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
@Slf4j
public class NotificationReceiverController {

    private static final String STATUS_KEY_PREFIX = "notification:status:";
    private static final int NOTIFICATION_TTL_DAYS = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "Upstash-Signature", required = false) String signature,
            @RequestBody Map<String, Object> body
    ) {
        // NOTE: Signature verification is recommended. Implement when Java helper is available.
        if (signature == null || signature.isBlank()) {
            log.warn("Missing Upstash-Signature header; processing anyway (development mode)");
        }

        log.info("Received QStash message: {}", body);

        Object idObj = body.get("id");
        if (idObj instanceof String id) {
            Map<String, Object> updated = new HashMap<>(body);
            updated.put("status", "DELIVERED");
            updated.put("deliveredAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String statusKey = STATUS_KEY_PREFIX + id;
            redisTemplate.opsForValue().set(statusKey, updated, NOTIFICATION_TTL_DAYS, TimeUnit.DAYS);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        return ResponseEntity.ok(resp);
    }
}


