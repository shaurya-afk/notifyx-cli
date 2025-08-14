package com.beta.apiservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    
    private String id;
    private String name;
    private String apiKey;
    private String webhookUrl;
    private String webhookSecret;
    private String contactEmail;
    private Integer rateLimit;
    private ProjectStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> channelConfigs;
    
    public enum ProjectStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}
