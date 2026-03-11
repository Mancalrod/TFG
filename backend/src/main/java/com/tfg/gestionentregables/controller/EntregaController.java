package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.Material;
import com.tfg.gestionentregables.service.EntregaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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
    private final com.tfg.gestionentregables.service.EntregableService entregableService;
    private final com.tfg.gestionentregables.service.ActividadService actividadService;

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
    @PostMapping("/entregable/{entregableId}/estudiante/{usuarioId}")
    public ResponseEntity<EntregaDTO> realizarEntrega(
            @PathVariable Long entregableId,
            @PathVariable Long usuarioId,
            @RequestParam String nombre,
            @RequestParam(required = false) List<MultipartFile> archivos) {
        EntregaDTO entrega = entregaService.realizarEntrega(entregableId, usuarioId, nombre, archivos);
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
    @GetMapping("/entregable/{entregableId}/estudiante/{usuarioId}")
    public ResponseEntity<List<EntregaDTO>> listarEntregasEstudiante(
            @PathVariable Long entregableId,
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(entregaService.listarEntregasEstudiante(entregableId, usuarioId));
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
     * Soporta tanto archivos de OneDrive como almacenamiento local.
     */
    @GetMapping("/archivo/{materialId}")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long materialId) {
        Material material = entregaService.obtenerArchivo(materialId);
        
        try {
            byte[] contenido = entregaService.descargarContenidoArchivo(materialId);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + material.getNombre() + "\"")
                    .body(contenido);
        } catch (Exception e) {
            // Fallback: intentar servir desde ruta local (compatibilidad)
            if (material.getRuta() != null && !material.getRuta().startsWith("onedrive://")) {
                try {
                    Path filePath = Paths.get(material.getRuta());
                    Resource resource = new UrlResource(filePath.toUri());
                    
                    if (resource.exists() && resource.isReadable()) {
                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .header(HttpHeaders.CONTENT_DISPOSITION, 
                                        "attachment; filename=\"" + material.getNombre() + "\"")
                                .body(resource.getContentAsByteArray());
                    }
                } catch (IOException ex) {
                    // ignorar
                }
            }
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Previsualizar archivo de una entrega (inline, sin forzar descarga).
     */
    @GetMapping("/archivo/{materialId}/preview")
    public ResponseEntity<byte[]> previsualizarArchivo(@PathVariable Long materialId) {
        Material material = entregaService.obtenerArchivo(materialId);

        try {
            byte[] contenido = entregaService.descargarContenidoArchivo(materialId);
            MediaType mediaType = resolverMediaType(material.getNombre());

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + material.getNombre() + "\"")
                    .body(contenido);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Descarga todos los archivos de las entregas activas de un entregable como ZIP.
     */
    @GetMapping("/entregable/{entregableId}/descargar-todo")
    public ResponseEntity<byte[]> descargarTodo(@PathVariable Long entregableId) {
        try {
            var entregable = entregableService.obtenerEntregable(entregableId);
            byte[] zipBytes = entregaService.descargarTodoComoZip(entregableId);
            String filename = sanitizarNombreArchivo(entregable.getTitulo()) + ".zip";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(zipBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Descarga todas las entregas de todos los entregables de una actividad como ZIP.
     */
    @GetMapping("/actividad/{actividadId}/descargar-todo")
    public ResponseEntity<byte[]> descargarTodoActividad(@PathVariable Long actividadId) {
        try {
            var actividad = actividadService.obtenerActividadPorId(actividadId);
            byte[] zipBytes = entregaService.descargarTodoActividadComoZip(actividadId);
            String filename = sanitizarNombreArchivo(actividad.getTitulo()) + ".zip";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(zipBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lista el contenido interno de un archivo ZIP para previsualización.
     */
    @GetMapping("/archivo/{materialId}/zip-contenido")
    public ResponseEntity<List<Map<String, Object>>> listarContenidoZip(@PathVariable Long materialId) {
        try {
            List<Map<String, Object>> contenido = entregaService.listarContenidoZip(materialId);
            return ResponseEntity.ok(contenido);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
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

    private String sanitizarNombreArchivo(String nombre) {
        if (nombre == null) return "entregas";
        return nombre.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ._-]", "_").trim();
    }

    private MediaType resolverMediaType(String nombre) {
        if (nombre == null) return MediaType.APPLICATION_OCTET_STREAM;
        String lower = nombre.toLowerCase();
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".txt")) return MediaType.TEXT_PLAIN;
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return MediaType.TEXT_HTML;
        if (lower.endsWith(".json")) return MediaType.APPLICATION_JSON;
        if (lower.endsWith(".xml")) return MediaType.APPLICATION_XML;
        if (lower.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
