package com.beta.notifierservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NotificationStatusService {
    private static final String STATUS_KEY_PREFIX = "notification:status:";
    private static final int NOTIFICATION_TTL_DAYS = 30;

    private final RedisTemplate<String, String> redisTemplate;

    public void save(String notificationId, Map<String, Object> data) {
        redisTemplate.opsForValue().set(STATUS_KEY_PREFIX + notificationId, data.toString(), NOTIFICATION_TTL_DAYS, TimeUnit.DAYS);
    }
}


