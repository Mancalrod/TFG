package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.service.EntregableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de entregables.
 * Implementa endpoints para SYSOP-010 a SYSOP-012.
 */
@RestController
@RequestMapping("/api/entregables")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class EntregableController {

    private final EntregableService entregableService;

    /**
     * SYSOP-012: Obtener detalle de un entregable.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntregableDTO> obtenerEntregable(@PathVariable Long id) {
        return ResponseEntity.ok(entregableService.obtenerEntregable(id));
    }

    /**
     * SYSOP-010: Crear un nuevo entregable en una actividad.
     */
    @PostMapping("/actividad/{actividadId}")
    public ResponseEntity<EntregableDTO> crearEntregable(
            @PathVariable Long actividadId,
            @Valid @RequestBody CrearEntregableDTO dto) {
        EntregableDTO entregable = entregableService.crearEntregable(dto, actividadId);
        return ResponseEntity.status(HttpStatus.CREATED).body(entregable);
    }

    /**
     * SYSOP-011: Listar entregables de una actividad.
     */
    @GetMapping("/actividad/{actividadId}")
    public ResponseEntity<List<EntregableDTO>> listarEntregablesActividad(
            @PathVariable Long actividadId) {
        return ResponseEntity.ok(entregableService.listarEntregablesActividad(actividadId));
    }

    @GetMapping("/actividad/{actividadId}/visibles")
    public ResponseEntity<List<EntregableDTO>> listarEntregablesVisibles(
            @PathVariable Long actividadId) {
        return ResponseEntity.ok(entregableService.listarEntregablesVisibles(actividadId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntregableDTO> actualizarEntregable(
            @PathVariable Long id,
            @Valid @RequestBody CrearEntregableDTO dto) {
        return ResponseEntity.ok(entregableService.actualizarEntregable(id, dto));
    }

    @PatchMapping("/{id}/visibilidad")
    public ResponseEntity<EntregableDTO> cambiarVisibilidad(
            @PathVariable Long id,
            @RequestParam Visibilidad visibilidad) {
        return ResponseEntity.ok(entregableService.cambiarVisibilidad(id, visibilidad));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEntregable(@PathVariable Long id) {
        entregableService.eliminarEntregable(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/actividad/{actividadId}/en-plazo")
    public ResponseEntity<List<EntregableDTO>> listarEntregablesEnPlazo(
            @PathVariable Long actividadId) {
        return ResponseEntity.ok(entregableService.listarEntregablesEnPlazo(actividadId));
    }

    @GetMapping("/actividad/{actividadId}/proximos")
    public ResponseEntity<List<EntregableDTO>> listarEntregablesProximos(
            @PathVariable Long actividadId,
            @RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(entregableService.listarEntregablesProximosVencer(actividadId, dias));
    }

    @GetMapping("/actividad/{actividadId}/pendientes/{estudianteId}")
    public ResponseEntity<List<EntregableDTO>> listarEntregablesPendientes(
            @PathVariable Long actividadId,
            @PathVariable Long estudianteId) {
        return ResponseEntity.ok(entregableService.listarEntregablesPendientesEstudiante(actividadId, estudianteId));
    }
}
