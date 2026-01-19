package com.cowtrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling
@RestController
public class CowTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(CowTrackApplication.class, args);
    }

    @GetMapping("/")
    public String welcome() {
        return """
               <html>
                   <body style="font-family: Arial, sans-serif; padding: 20px;">
                       <h1>🐄 CowTrack API</h1>
                       <p>Livestock Tracking and Management System</p>
                       <h3>📋 Available Endpoints:</h3>
                       <ul>
                           <li><a href="/api/users">/api/users</a> - User management</li>
                           <li><a href="/api/cows">/api/cows</a> - Cow management</li>
                           <li><a href="/api/locations">/api/locations</a> - Location tracking</li>
                           <li><a href="/api/geofences">/api/geofences</a> - Geofence management</li>
                           <li><a href="/api/alerts">/api/alerts</a> - Alert system</li>
                           <li><a href="/api/health-records">/api/health-records</a> - Health records</li>
                           <li><a href="/api/reminders">/api/reminders</a> - Reminders</li>
                           <li><a href="/api/dashboard">/api/dashboard</a> - Dashboard</li>
                       </ul>
                       <h3>🚀 Quick Tests:</h3>
                       <div style="background: #f5f5f5; padding: 10px; border-radius: 5px;">
                           <p><a href="/api/health">GET /api/health</a> - Health check</p>
                           <p><a href="/api/ping">GET /api/ping</a> - Simple ping</p>
                       </div>
                       <p><strong>Base URL:</strong> http://localhost:8081</p>
                       <p><strong>Database:</strong> MySQL (cowtrack)</p>
                   </body>
               </html>
               """;
    }

    // REMOVE THESE METHODS - They're in HealthController
    // @GetMapping("/api/health")
    // public String health() { ... }

    // @GetMapping("/api/ping")
    // public String ping() { ... }
}