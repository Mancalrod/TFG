package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para gestión de feedback.
 * Implementa endpoints para SYSOP-019 a SYSOP-021.
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackDTO> obtenerFeedback(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.obtenerFeedback(id));
    }

    /**
     * SYSOP-019: Añadir feedback a una entrega.
     */
    @PostMapping("/entrega/{entregaId}/profesor/{profesorId}")
    public ResponseEntity<FeedbackDTO> crearFeedback(
            @PathVariable Long entregaId,
            @PathVariable Long profesorId,
            @Valid @RequestBody CrearFeedbackDTO dto) {
        FeedbackDTO feedback = feedbackService.crearFeedback(entregaId, profesorId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }

    /**
     * SYSOP-020: Listar feedbacks de una entrega.
     */
    @GetMapping("/entrega/{entregaId}")
    public ResponseEntity<List<FeedbackDTO>> listarFeedbacksEntrega(@PathVariable Long entregaId) {
        return ResponseEntity.ok(feedbackService.listarFeedbacksEntrega(entregaId));
    }

    /**
     * SYSOP-021: Modificar un feedback.
     */
    @PutMapping("/{id}/profesor/{profesorId}")
    public ResponseEntity<FeedbackDTO> actualizarFeedback(
            @PathVariable Long id,
            @PathVariable Long profesorId,
            @Valid @RequestBody CrearFeedbackDTO dto) {
        return ResponseEntity.ok(feedbackService.actualizarFeedback(id, profesorId, dto));
    }

    @DeleteMapping("/{id}/profesor/{profesorId}")
    public ResponseEntity<Void> eliminarFeedback(
            @PathVariable Long id,
            @PathVariable Long profesorId) {
        feedbackService.eliminarFeedback(id, profesorId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profesor/{profesorId}")
    public ResponseEntity<List<FeedbackDTO>> listarFeedbacksProfesor(@PathVariable Long profesorId) {
        return ResponseEntity.ok(feedbackService.listarFeedbacksProfesor(profesorId));
    }

    @GetMapping("/estudiante/{estudianteId}/recientes")
    public ResponseEntity<Long> contarFeedbacksRecientes(
            @PathVariable Long estudianteId,
            @RequestParam(defaultValue = "7") int dias) {
        LocalDateTime desde = LocalDateTime.now().minusDays(dias);
        return ResponseEntity.ok(feedbackService.contarFeedbacksRecientes(estudianteId, desde));
    }
}
