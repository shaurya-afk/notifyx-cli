package com.beta.apiservice.config;

import com.beta.apiservice.model.Project;
import com.beta.apiservice.service.ProjectService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private final ProjectService projectService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip authentication for certain endpoints
        String requestURI = request.getRequestURI();
        if (shouldSkipAuthentication(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String apiKey = extractApiKey(request);
        
        if (apiKey == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"API key is required\"}");
            return;
        }
        
        Project project = projectService.getProjectByApiKey(apiKey);
        
        if (project == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid API key\"}");
            return;
        }
        
        if (project.getStatus() != Project.ProjectStatus.ACTIVE) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Project is not active\"}");
            return;
        }
        
        // Add project info to request attributes
        request.setAttribute("project", project);
        request.setAttribute("projectId", project.getId());
        
        filterChain.doFilter(request, response);
    }
    
    private boolean shouldSkipAuthentication(String requestURI) {
        return requestURI.startsWith("/api/notification/health") ||
               requestURI.startsWith("/api/notification/version") ||
               requestURI.startsWith("/api/projects/register") ||
               requestURI.startsWith("/api/messages/") && 
               (requestURI.contains("/user/") || requestURI.matches("/api/messages/[^/]+$")) ||
               requestURI.startsWith("/swagger-ui") ||
               requestURI.startsWith("/v3/api-docs");
    }
    
    private String extractApiKey(HttpServletRequest request) {
        // Check Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Check X-API-Key header
        String apiKeyHeader = request.getHeader("X-API-Key");
        if (apiKeyHeader != null) {
            return apiKeyHeader;
        }
        
        // Check query parameter
        return request.getParameter("api_key");
    }
}
