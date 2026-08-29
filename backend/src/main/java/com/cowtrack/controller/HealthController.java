package com.cowtrack.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

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
                           <p><a href="/actuator/health">GET /actuator/health</a> - Formatted health, with probes</p>
                       </div>
                       <p><strong>Base URL:</strong> http://localhost:8081</p>
                   </body>
               </html>
               """;
    }

    /**
     * Liveness/readiness for load balancers and human debugging.
     *
     * <p>Unlike the typical vanity health route this reports the real availability
     * of the database: if {@code SELECT 1} fails, the response is HTTP 503 with
     * {@code status: DOWN} instead of a confident "UP" the app does not deserve.
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        HttpStatus status;
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (one != null && one == 1) {
                response.put("status", "UP");
                response.put("database", "UP");
                status = HttpStatus.OK;
            } else {
                response.put("status", "DOWN");
                response.put("database", "DOWN");
                status = HttpStatus.SERVICE_UNAVAILABLE;
            }
        } catch (Exception ex) {
            response.put("status", "DOWN");
            response.put("database", "DOWN");
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }
        response.put("service", "CowTrack API");
        response.put("version", "1.0.0");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return new ResponseEntity<>(response, status);
    }

    @GetMapping("/api/ping")
    public String ping() {
        return "pong";
    }
}