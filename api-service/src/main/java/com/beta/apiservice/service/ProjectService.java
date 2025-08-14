package com.beta.apiservice.service;

import com.beta.apiservice.dto.ProjectRegistrationRequest;
import com.beta.apiservice.dto.ProjectResponse;
import com.beta.apiservice.model.Project;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String PROJECT_KEY_PREFIX = "project:";
    private static final String API_KEY_PREFIX = "api_key:";
    private static final int PROJECT_TTL_DAYS = 365;
    
    public ProjectResponse registerProject(ProjectRegistrationRequest request) throws JsonProcessingException {
        String projectId = "proj_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String apiKey = "sk_live_" + UUID.randomUUID().toString().replace("-", "");
        String webhookSecret = "whsec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        Project project = new Project();
        project.setId(projectId);
        project.setName(request.projectName());
        project.setApiKey(apiKey);
        project.setWebhookUrl(request.webhookUrl());
        project.setWebhookSecret(webhookSecret);
        project.setContactEmail(request.contactEmail());
        project.setRateLimit(request.rateLimit() != null ? request.rateLimit() : 1000);
        project.setStatus(Project.ProjectStatus.ACTIVE);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        project.setChannelConfigs(request.channelConfigs());
        
        // Store project in Redis
        String projectKey = PROJECT_KEY_PREFIX + projectId;
        String projectJson = objectMapper.writeValueAsString(project);
        redisTemplate.opsForValue().set(projectKey, projectJson, PROJECT_TTL_DAYS, TimeUnit.DAYS);
        
        // Store API key mapping
        String apiKeyKey = API_KEY_PREFIX + apiKey;
        redisTemplate.opsForValue().set(apiKeyKey, projectId, PROJECT_TTL_DAYS, TimeUnit.DAYS);
        
        log.info("Project registered: {}", projectId);
        
        return new ProjectResponse(
            projectId,
            project.getName(),
            apiKey,
            webhookSecret,
            "https://notifyx.com/dashboard/" + projectId,
            project.getCreatedAt(),
            project.getStatus().name()
        );
    }
    
    public Project getProjectByApiKey(String apiKey) {
        try {
            String apiKeyKey = API_KEY_PREFIX + apiKey;
            String projectId = (String) redisTemplate.opsForValue().get(apiKeyKey);
            
            if (projectId == null) {
                return null;
            }
            
            String projectKey = PROJECT_KEY_PREFIX + projectId;
            String projectJson = (String) redisTemplate.opsForValue().get(projectKey);
            
            if (projectJson == null) {
                return null;
            }
            
            return objectMapper.readValue(projectJson, Project.class);
        } catch (Exception e) {
            log.error("Failed to get project by API key", e);
            return null;
        }
    }
    
    public Project getProjectById(String projectId) {
        try {
            String projectKey = PROJECT_KEY_PREFIX + projectId;
            String projectJson = (String) redisTemplate.opsForValue().get(projectKey);
            
            if (projectJson == null) {
                return null;
            }
            
            return objectMapper.readValue(projectJson, Project.class);
        } catch (Exception e) {
            log.error("Failed to get project by ID: {}", projectId, e);
            return null;
        }
    }
    
    public boolean isProjectActive(String projectId) {
        Project project = getProjectById(projectId);
        return project != null && project.getStatus() == Project.ProjectStatus.ACTIVE;
    }
}
