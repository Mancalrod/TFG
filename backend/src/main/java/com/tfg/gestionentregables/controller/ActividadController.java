package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.service.ActividadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de actividades.
 * Implementa endpoints para SYSOP-005 a SYSOP-009.
 */
@RestController
@RequestMapping("/api/actividades")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ActividadController {

    private final ActividadService actividadService;

    @GetMapping("/{id}")
    public ResponseEntity<ActividadDTO> obtenerActividad(@PathVariable Long id) {
        return ResponseEntity.ok(actividadService.obtenerActividadPorId(id));
    }

    /**
     * SYSOP-005: Crear una nueva actividad en un curso.
     */
    @PostMapping("/curso/{cursoId}")
    public ResponseEntity<ActividadDTO> crearActividad(
            @PathVariable Long cursoId,
            @Valid @RequestBody CrearActividadDTO dto) {
        ActividadDTO actividad = actividadService.crearActividad(dto, cursoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(actividad);
    }

    /**
     * SYSOP-006: Listar actividades de un curso (para profesor).
     */
    @GetMapping("/curso/{cursoId}")
    public ResponseEntity<List<ActividadDTO>> listarActividadesCurso(@PathVariable Long cursoId) {
        return ResponseEntity.ok(actividadService.listarActividadesCurso(cursoId));
    }

    /**
     * SYSOP-007: Listar actividades visibles para un grupo.
     */
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<ActividadDTO>> listarActividadesGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(actividadService.listarActividadesVisiblesGrupo(grupoId));
    }

    /**
     * SYSOP-008: Obtener actividad con entregables.
     */
    @GetMapping("/{id}/detalle")
    public ResponseEntity<ActividadDTO> obtenerActividadConEntregables(@PathVariable Long id) {
        return ResponseEntity.ok(actividadService.obtenerActividadConEntregables(id));
    }

    /**
     * SYSOP-009: Publicar/ocultar una actividad.
     */
    @PatchMapping("/{id}/visibilidad")
    public ResponseEntity<ActividadDTO> cambiarVisibilidad(
            @PathVariable Long id,
            @RequestParam Visibilidad visibilidad) {
        return ResponseEntity.ok(actividadService.cambiarVisibilidad(id, visibilidad));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActividadDTO> actualizarActividad(
            @PathVariable Long id,
            @Valid @RequestBody CrearActividadDTO dto) {
        return ResponseEntity.ok(actividadService.actualizarActividad(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarActividad(@PathVariable Long id) {
        actividadService.eliminarActividad(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/curso/{cursoId}/en-plazo")
    public ResponseEntity<List<ActividadDTO>> listarActividadesEnPlazo(@PathVariable Long cursoId) {
        return ResponseEntity.ok(actividadService.listarActividadesEnPlazo(cursoId));
    }

    @GetMapping("/curso/{cursoId}/proximas")
    public ResponseEntity<List<ActividadDTO>> listarActividadesProximas(
            @PathVariable Long cursoId,
            @RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(actividadService.listarActividadesProximasLimite(cursoId, dias));
    }
}
