package com.cowtrack.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    /**
     * Developer landing page listing the available endpoints.
     *
     * <p>Served from {@code /api/info} rather than {@code /} because the bundled
     * React application now owns the site root.
     */
    @GetMapping(value = "/api/info", produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        return """
               <html>
                   <body style="font-family: Arial, sans-serif; padding: 20px;">
                       <h1>🐄 CowTrack API</h1>
                       <p>Livestock Tracking and Management System</p>
                       <h3>📋 Available Endpoints:</h3>
                       <ul>
                           <li>/api/auth - Login, register, verify, logout</li>
                           <li>/api/users - User management</li>
                           <li>/api/cows - Cow management</li>
                           <li>/api/locations - Location tracking</li>
                           <li>/api/geofences - Geofence management</li>
                           <li>/api/alerts - Alert system</li>
                           <li>/api/health-records - Health records</li>
                           <li>/api/reminders - Reminders</li>
                           <li>/api/dashboard - Dashboard</li>
                       </ul>
                       <p>All endpoints except <code>/api/auth/**</code> and the health
                          checks below require an <code>Authorization: Bearer</code> token.</p>
                       <h3>🚀 Quick Tests:</h3>
                       <div style="background: #f5f5f5; padding: 10px; border-radius: 5px;">
                           <p><a href="/api/health">GET /api/health</a> - Health check</p>
                           <p><a href="/api/ping">GET /api/ping</a> - Simple ping</p>
                       </div>
                       <p><strong>Base URL:</strong> http://localhost:8081</p>
                   </body>
               </html>
               """;
    }

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