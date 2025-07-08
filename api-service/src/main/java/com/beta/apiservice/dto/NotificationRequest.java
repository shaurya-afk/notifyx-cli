package com.beta.apiservice.dto;

public record NotificationRequest (
    String userId,
    String message,
    String type
){}
