package com.beta.apiservice.dto;

import java.time.LocalDateTime;

public record ProjectResponse(
    String projectId,
    String projectName,
    String apiKey,
    String webhookSecret,
    String dashboardUrl,
    LocalDateTime createdAt,
    String status
){}
