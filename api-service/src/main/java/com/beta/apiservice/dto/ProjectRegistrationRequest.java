package com.beta.apiservice.dto;

import java.util.List;
import java.util.Map;

public record ProjectRegistrationRequest(
    String projectName,
    String contactEmail,
    String webhookUrl,
    List<String> channels,
    Integer rateLimit,
    Map<String, Object> channelConfigs
){}
