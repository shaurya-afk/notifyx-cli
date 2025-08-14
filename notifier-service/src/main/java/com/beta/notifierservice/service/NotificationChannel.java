package com.beta.notifierservice.service;

import java.util.Map;

public interface NotificationChannel {
    String getChannelType();
    boolean send(String recipient, String message, String title, Map<String, Object> config);
    boolean supports(String channelType);
}
