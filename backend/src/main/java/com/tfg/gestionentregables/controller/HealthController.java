package com.tfg.gestionentregables.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador para verificar el estado de la API.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "application", "Sistema de Gestión de Entregables",
            "version", "1.0.0-SNAPSHOT"
        ));
    }

    @GetMapping("/public/info")
    public ResponseEntity<Map<String, String>> publicInfo() {
        return ResponseEntity.ok(Map.of(
            "message", "API del Sistema de Gestión de Entregables - TFG",
            "documentation", "/swagger-ui.html"
        ));
    }
}
