package com.tfg.gestionentregables.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Controlador para verificar el estado de la API.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(@Autowired(required = false) DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return readinessCheck();
    }

    @GetMapping("/health/liveness")
    public ResponseEntity<Map<String, String>> livenessCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "type", "LIVENESS",
            "application", "AcademicFlow",
            "version", "1.0.0-SNAPSHOT"
        ));
    }

    @GetMapping("/health/readiness")
    public ResponseEntity<Map<String, String>> readinessCheck() {
        boolean dbUp = isDatabaseUp();
        HttpStatus status = dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(Map.of(
            "status", dbUp ? "UP" : "DOWN",
            "type", "READINESS",
            "database", dbUp ? "UP" : "DOWN",
            "application", "AcademicFlow",
            "version", "1.0.0-SNAPSHOT"
        ));
    }

    private boolean isDatabaseUp() {
        if (dataSource == null) {
            return true;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1")) {
            statement.execute();
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    @GetMapping("/public/info")
    public ResponseEntity<Map<String, String>> publicInfo() {
        return ResponseEntity.ok(Map.of(
            "message", "API de AcademicFlow - TFG",
            "documentation", "/swagger-ui.html"
        ));
    }
}
