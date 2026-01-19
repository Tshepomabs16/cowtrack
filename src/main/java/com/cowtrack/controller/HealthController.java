package com.cowtrack.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "CowTrack API");
        response.put("version", "1.0.0");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/api/ping")
    public String ping() {
        return "pong";
    }
}