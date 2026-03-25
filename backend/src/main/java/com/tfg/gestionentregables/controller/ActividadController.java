package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.service.ActividadService;
import com.tfg.gestionentregables.service.SecurityContextUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final SecurityContextUserService securityContextUserService;

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
            @Valid @RequestBody CrearActividadDTO dto,
            Authentication authentication) {
        Long actorId = securityContextUserService.getCurrentUserId(authentication);
        boolean actorEsAdmin = securityContextUserService.hasRole(authentication, "ADMIN");
        ActividadDTO actividad = actividadService.crearActividad(dto, cursoId, actorId, actorEsAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(actividad);
    }

    /**
     * SYSOP-006: Listar actividades de un curso (para profesor).
     */
    @GetMapping("/curso/{cursoId}")
    public ResponseEntity<List<ActividadDTO>> listarActividadesCurso(@PathVariable Long cursoId,
                                                                     Authentication authentication) {
        Long actorId = securityContextUserService.getCurrentUserId(authentication);
        boolean actorEsAdmin = securityContextUserService.hasRole(authentication, "ADMIN");
        boolean actorEsProfesor = securityContextUserService.hasRole(authentication, "PROFESOR");
        boolean actorEsEstudiante = securityContextUserService.hasRole(authentication, "ESTUDIANTE");
        return ResponseEntity.ok(actividadService.listarActividadesCurso(
                cursoId,
                actorId,
                actorEsAdmin,
                actorEsProfesor,
                actorEsEstudiante));
    }

    /**
     * SYSOP-007: Listar actividades visibles para un grupo.
     */
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<ActividadDTO>> listarActividadesGrupo(@PathVariable Long grupoId,
                                                                     Authentication authentication) {
        Long actorId = securityContextUserService.getCurrentUserId(authentication);
        boolean actorEsAdmin = securityContextUserService.hasRole(authentication, "ADMIN");
        boolean actorEsProfesor = securityContextUserService.hasRole(authentication, "PROFESOR");
        boolean actorEsEstudiante = securityContextUserService.hasRole(authentication, "ESTUDIANTE");
        return ResponseEntity.ok(actividadService.listarActividadesVisiblesGrupo(
                grupoId,
                actorId,
                actorEsAdmin,
                actorEsProfesor,
                actorEsEstudiante));
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
            @RequestParam Visibilidad visibilidad,
            Authentication authentication) {
        Long actorId = securityContextUserService.getCurrentUserId(authentication);
        boolean actorEsAdmin = securityContextUserService.hasRole(authentication, "ADMIN");
        return ResponseEntity.ok(actividadService.cambiarVisibilidad(id, visibilidad, actorId, actorEsAdmin));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActividadDTO> actualizarActividad(
            @PathVariable Long id,
            @Valid @RequestBody CrearActividadDTO dto,
            Authentication authentication) {
        Long actorId = securityContextUserService.getCurrentUserId(authentication);
        boolean actorEsAdmin = securityContextUserService.hasRole(authentication, "ADMIN");
        return ResponseEntity.ok(actividadService.actualizarActividad(id, dto, actorId, actorEsAdmin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarActividad(@PathVariable Long id,
                                                  Authentication authentication) {
        Long actorId = securityContextUserService.getCurrentUserId(authentication);
        boolean actorEsAdmin = securityContextUserService.hasRole(authentication, "ADMIN");
        actividadService.eliminarActividad(id, actorId, actorEsAdmin);
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
