package com.beta.apiservice.dto;

import java.util.List;
import java.util.Map;

public record NotificationRequest(
    String projectId,                    // Which project is sending
    List<String> recipients,             // Multiple recipients
    String message,
    String title,                        // Optional title
    String channel,                      // email, sms, webhook, custom
    String template,                     // Optional template name
    Map<String, Object> variables,       // Template variables
    Map<String, Object> channelConfig,   // Channel-specific configuration
    Map<String, Object> metadata         // Project-specific metadata
){}
