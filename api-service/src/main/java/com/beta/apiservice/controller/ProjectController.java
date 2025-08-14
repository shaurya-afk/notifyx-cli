package com.beta.apiservice.controller;

import com.beta.apiservice.dto.ProjectRegistrationRequest;
import com.beta.apiservice.dto.ProjectResponse;
import com.beta.apiservice.service.ProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {
    
    private final ProjectService projectService;
    
    @PostMapping("/register")
    public ResponseEntity<?> registerProject(@RequestBody ProjectRegistrationRequest request) {
        try {
            ProjectResponse response = projectService.registerProject(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to register project", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to register project"));
        } catch (Exception e) {
            log.error("Failed to register project", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to register project: " + e.getMessage()));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentProject(@RequestAttribute("project") com.beta.apiservice.model.Project project) {
        return ResponseEntity.ok(Map.of(
            "projectId", project.getId(),
            "name", project.getName(),
            "contactEmail", project.getContactEmail(),
            "webhookUrl", project.getWebhookUrl(),
            "rateLimit", project.getRateLimit(),
            "status", project.getStatus(),
            "createdAt", project.getCreatedAt()
        ));
    }
}
