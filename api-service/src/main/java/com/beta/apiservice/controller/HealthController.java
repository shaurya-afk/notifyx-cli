package com.beta.apiservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health(){
        Map<String, Object> map = new HashMap<>();
        map.put("status","UP");
        map.put("service","NotifyX Backend");
        map.put("timestamp",System.currentTimeMillis());

        return ResponseEntity.ok(map);
    }

    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, Object>> actuatorHealth(){
        Map<String, Object> map = new HashMap<>();
        map.put("status","UP");
        map.put("service","NotifyX Backend");
        map.put("timestamp",System.currentTimeMillis());

        return ResponseEntity.ok(map);
    }

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("NotifyX Backend is running! Visit /health for status.");
    }
}
