package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.Material;
import com.tfg.gestionentregables.service.EntregaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Controlador REST para gestión de entregas.
 * Implementa endpoints para SYSOP-013 a SYSOP-018.
 */
@RestController
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class EntregaController {

    private final EntregaService entregaService;

    /**
     * SYSOP-014: Obtener detalle de una entrega.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntregaDTO> obtenerEntrega(@PathVariable Long id) {
        return ResponseEntity.ok(entregaService.obtenerEntrega(id));
    }

    /**
     * SYSOP-013: Realizar una entrega.
     */
    @PostMapping("/entregable/{entregableId}/estudiante/{estudianteId}")
    public ResponseEntity<EntregaDTO> realizarEntrega(
            @PathVariable Long entregableId,
            @PathVariable Long estudianteId,
            @RequestParam String nombre,
            @RequestParam(required = false) List<MultipartFile> archivos) {
        EntregaDTO entrega = entregaService.realizarEntrega(entregableId, estudianteId, nombre, archivos);
        return ResponseEntity.status(HttpStatus.CREATED).body(entrega);
    }

    /**
     * SYSOP-015: Listar entregas de un entregable para evaluación.
     */
    @GetMapping("/entregable/{entregableId}")
    public ResponseEntity<List<EntregaResumenDTO>> listarEntregasParaEvaluar(
            @PathVariable Long entregableId) {
        return ResponseEntity.ok(entregaService.listarEntregasParaEvaluar(entregableId));
    }

    /**
     * SYSOP-016: Listar entregas de un estudiante (historial de versiones).
     */
    @GetMapping("/entregable/{entregableId}/estudiante/{estudianteId}")
    public ResponseEntity<List<EntregaDTO>> listarEntregasEstudiante(
            @PathVariable Long entregableId,
            @PathVariable Long estudianteId) {
        return ResponseEntity.ok(entregaService.listarEntregasEstudiante(entregableId, estudianteId));
    }

    /**
     * SYSOP-017: Calificar una entrega.
     */
    @PostMapping("/{id}/calificar")
    public ResponseEntity<EntregaDTO> calificarEntrega(
            @PathVariable Long id,
            @Valid @RequestBody CalificacionDTO calificacion) {
        return ResponseEntity.ok(entregaService.calificarEntrega(id, calificacion));
    }

    /**
     * SYSOP-018: Descargar archivo de una entrega.
     * Soporta descarga desde OneDrive y desde sistema de archivos local.
     */
    @GetMapping("/archivo/{materialId}")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable Long materialId) {
        Material material = entregaService.obtenerArchivo(materialId);

        // Si el material está en OneDrive, descargar desde OneDrive
        if (material.getOneDriveItemId() != null && material.getOneDriveOwnerUserId() != null) {
            try {
                InputStream stream = entregaService.descargarContenidoArchivo(materialId);
                InputStreamResource resource = new InputStreamResource(stream);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + material.getNombre() + "\"")
                        .body(resource);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().build();
            }
        }

        // Fallback: descargar desde sistema de archivos local
        try {
            Path filePath = Paths.get(material.getRuta());
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + material.getNombre() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<EntregaDTO>> listarTodasEntregasEstudiante(
            @PathVariable Long estudianteId) {
        return ResponseEntity.ok(entregaService.listarTodasEntregasEstudiante(estudianteId));
    }

    @GetMapping("/profesor/{profesorId}/pendientes")
    public ResponseEntity<List<EntregaResumenDTO>> listarEntregasPendientesCalificar(
            @PathVariable Long profesorId) {
        return ResponseEntity.ok(entregaService.listarEntregasPendientesCalificar(profesorId));
    }

    @GetMapping("/entregable/{entregableId}/estadisticas")
    public ResponseEntity<EntregaEstadisticasDTO> obtenerEstadisticas(
            @PathVariable Long entregableId) {
        return ResponseEntity.ok(entregaService.obtenerEstadisticas(entregableId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEntrega(@PathVariable Long id) {
        entregaService.eliminarEntrega(id);
        return ResponseEntity.noContent().build();
    }
}
