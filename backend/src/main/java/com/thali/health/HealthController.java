package com.thali.health;

import java.time.Instant;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public liveness + database connectivity check used by the frontend and by Render. */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final DataSource dataSource;
    private final String appName;

    public HealthController(DataSource dataSource, @Value("${spring.application.name}") String appName) {
        this.dataSource = dataSource;
        this.appName = appName;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp = isDatabaseUp();
        Map<String, Object> body = Map.of(
                "status", dbUp ? "UP" : "DEGRADED",
                "service", appName,
                "database", dbUp ? "UP" : "DOWN",
                "timestamp", Instant.now().toString());
        return dbUp ? ResponseEntity.ok(body) : ResponseEntity.status(503).body(body);
    }

    private boolean isDatabaseUp() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
