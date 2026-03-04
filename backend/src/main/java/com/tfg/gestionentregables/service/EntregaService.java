package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Servicio para gestión de entregas.
 * Implementa operaciones SYSOP-013 a SYSOP-018.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EntregaService {

    private static final String ENTREGA_NOT_FOUND = "Entrega no encontrada con ID: ";

    private final EntregaRepository entregaRepository;
    private final EntregableRepository entregableRepository;
    private final EstudianteRepository estudianteRepository;
    private final MaterialRepository materialRepository;
    private final EntityMapper mapper;
    private final OneDriveService oneDriveService;

    // Directorio base para almacenar archivos (fallback local)
    private static final String UPLOAD_DIR = "uploads/entregas";

    /**
     * SYSOP-013: Realiza una entrega para un entregable.
     * Si OneDrive está habilitado:
     *  - Sube los archivos al OneDrive de cada profesor del curso
     *  - Si el alumno tiene OneDrive conectado, también sube una copia a su OneDrive
     * Si no, almacena localmente como fallback.
     */
    public EntregaDTO realizarEntrega(Long entregableId, Long estudianteId, String nombre, List<MultipartFile> archivos) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new EntityNotFoundException("Entregable no encontrado con ID: " + entregableId));
        
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado con ID: " + estudianteId));

        // Obtener entregas anteriores
        List<Entrega> entregasAnteriores = entregaRepository.findByEntregableIdAndEstudianteId(entregableId, estudianteId);
        int ultimaVersion = entregasAnteriores.stream()
                .mapToInt(Entrega::getVersion)
                .max()
                .orElse(0);

        // Verificar si el entregable permite reenvío
        if (ultimaVersion > 0 && !entregable.getPermiteReenvio()) {
            throw new IllegalStateException("Este entregable no permite reenvío de entregas");
        }

        // Desactivar versiones anteriores
        entregasAnteriores.forEach(e -> e.setEsVersionActiva(false));
        entregaRepository.saveAll(entregasAnteriores);

        // Crear nueva entrega
        LocalDateTime ahora = LocalDateTime.now();
        
        Entrega entrega = Entrega.builder()
                .nombre(nombre)
                .version(ultimaVersion + 1)
                .fechaEntrega(ahora)
                .estado(EstadoEntrega.ENTREGADO)
                .esVersionActiva(true)
                .entregable(entregable)
                .estudiante(estudiante)
                .build();

        entrega = entregaRepository.save(entrega);

        // Procesar archivos adjuntos
        if (archivos != null && !archivos.isEmpty()) {
            for (MultipartFile archivo : archivos) {
                Material material = guardarArchivoConOneDrive(archivo, entrega, entregable, estudiante);
                materialRepository.save(material);
            }
        }

        // Refrescar la entrega desde la BD para incluir los materiales guardados
        entrega = entregaRepository.findById(entrega.getId()).orElseThrow();
        return mapper.toDTO(entrega);
    }

    /**
     * SYSOP-014: Obtiene detalle de una entrega.
     */
    @Transactional(readOnly = true)
    public EntregaDTO obtenerEntrega(Long entregaId) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new EntityNotFoundException(ENTREGA_NOT_FOUND + entregaId));
        return mapper.toDTO(entrega);
    }

    /**
     * SYSOP-015: Lista entregas de un entregable para evaluación (vista profesor).
     */
    @Transactional(readOnly = true)
    public List<EntregaResumenDTO> listarEntregasParaEvaluar(Long entregableId) {
        if (!entregableRepository.existsById(entregableId)) {
            throw new EntityNotFoundException("Entregable no encontrado con ID: " + entregableId);
        }
        
        return entregaRepository.findByEntregableIdAndEsVersionActiva(entregableId, true).stream()
                .map(mapper::toResumenDTO)
                .toList();
    }

    /**
     * SYSOP-016: Lista entregas de un estudiante en un entregable (historial de versiones).
     */
    @Transactional(readOnly = true)
    public List<EntregaDTO> listarEntregasEstudiante(Long entregableId, Long estudianteId) {
        return entregaRepository.findByEntregableIdAndEstudianteId(entregableId, estudianteId)
                .stream()
                .sorted((a, b) -> Integer.compare(b.getVersion(), a.getVersion()))
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * SYSOP-017: Califica una entrega.
     */
    public EntregaDTO calificarEntrega(Long entregaId, CalificacionDTO calificacion) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new EntityNotFoundException("Entrega no encontrada con ID: " + entregaId));

        // Validar nota máxima
        Double notaMaxima = entrega.getEntregable().getNotaMaxima();
        if (notaMaxima != null && calificacion.getNota() > notaMaxima) {
            throw new IllegalArgumentException("La calificación no puede ser mayor que la nota máxima (" + notaMaxima + ")");
        }

        entrega.setCalificacion(calificacion.getNota());
        entrega.setEstado(EstadoEntrega.CALIFICADO);
        entrega.setFechaCalificacion(LocalDateTime.now());

        entrega = entregaRepository.save(entrega);
        return mapper.toDTO(entrega);
    }

    /**
     * SYSOP-018: Descarga archivo de una entrega.
     */
    @Transactional(readOnly = true)
    public Material obtenerArchivo(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado con ID: " + materialId));
    }

    /**
     * Lista todas las entregas de un estudiante.
     */
    @Transactional(readOnly = true)
    public List<EntregaDTO> listarTodasEntregasEstudiante(Long estudianteId) {
        if (!estudianteRepository.existsById(estudianteId)) {
            throw new EntityNotFoundException("Estudiante no encontrado con ID: " + estudianteId);
        }
        
        return entregaRepository.findByEstudianteId(estudianteId).stream()
                .sorted((a, b) -> b.getFechaEntrega().compareTo(a.getFechaEntrega()))
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Lista entregas pendientes de calificar de un profesor.
     */
    @Transactional(readOnly = true)
    public List<EntregaResumenDTO> listarEntregasPendientesCalificar(Long profesorId) {
        return entregaRepository.findByEstadoAndEsVersionActiva(EstadoEntrega.ENTREGADO, true).stream()
                .map(mapper::toResumenDTO)
                .toList();
    }

    /**
     * Obtiene estadísticas de entregas de un entregable.
     */
    @Transactional(readOnly = true)
    public EntregaEstadisticasDTO obtenerEstadisticas(Long entregableId) {
        if (!entregableRepository.existsById(entregableId)) {
            throw new EntityNotFoundException("Entregable no encontrado con ID: " + entregableId);
        }

        List<Entrega> entregasActivas = entregaRepository.findByEntregableIdAndEsVersionActiva(entregableId, true);
        
        long totalEntregas = entregasActivas.size();
        long entregadasATiempo = entregasActivas.stream()
                .filter(Entrega::fueATiempo)
                .count();
        long calificadas = entregasActivas.stream()
                .filter(e -> e.getEstado() == EstadoEntrega.CALIFICADO)
                .count();
        Double promedioCalificacion = entregasActivas.stream()
                .filter(e -> e.getCalificacion() != null)
                .mapToDouble(Entrega::getCalificacion)
                .average()
                .orElse(0.0);

        return EntregaEstadisticasDTO.builder()
                .entregableId(entregableId)
                .totalEntregas(totalEntregas)
                .entregasATiempo(entregadasATiempo)
                .entregasTardias(totalEntregas - entregadasATiempo)
                .entregasCalificadas(calificadas)
                .entregasPendientes(totalEntregas - calificadas)
                .promedioCalificacion(promedioCalificacion > 0 ? promedioCalificacion : null)
                .build();
    }

    /**
     * Elimina una entrega (solo si no está calificada).
     */
    public void eliminarEntrega(Long entregaId) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new EntityNotFoundException("Entrega no encontrada con ID: " + entregaId));

        if (entrega.getEstado() == EstadoEntrega.CALIFICADO) {
            throw new IllegalStateException("No se puede eliminar una entrega ya calificada");
        }

        entregaRepository.delete(entrega);
    }

    // ========================================
    // ALMACENAMIENTO DE ARCHIVOS
    // ========================================

    /**
     * Guarda un archivo con soporte para OneDrive.
     * Si OneDrive está habilitado, sube al OneDrive de los profesores del curso.
     * Si el alumno tiene OneDrive conectado, también sube a su OneDrive.
     * Si OneDrive no está habilitado, usa almacenamiento local como fallback.
     */
    private Material guardarArchivoConOneDrive(MultipartFile archivo, Entrega entrega,
                                                Entregable entregable, Estudiante estudiante) {
        String nombreOriginal = archivo.getOriginalFilename();
        String extension = nombreOriginal != null && nombreOriginal.contains(".")
                ? nombreOriginal.substring(nombreOriginal.lastIndexOf("."))
                : "";
        String nombreUnico = UUID.randomUUID().toString() + extension;

        // Datos para organizar las carpetas en OneDrive
        Actividad actividad = entregable.getActividad();
        Curso curso = actividad.getCurso();
        String estudianteNombre = estudiante.getUsuario().getNombre();

        // Intentar subir a OneDrive si está habilitado
        if (oneDriveService.isEnabled()) {
            return guardarEnOneDrive(archivo, entrega, curso, actividad, entregable,
                    estudiante, nombreOriginal, nombreUnico);
        }

        // Fallback: almacenamiento local
        return guardarArchivoLocal(archivo, entrega, nombreOriginal, nombreUnico);
    }

    /**
     * Sube el archivo a OneDrive de los profesores del curso y opcionalmente al del alumno.
     * Almacena la referencia del primer profesor como material principal.
     */
    private Material guardarEnOneDrive(MultipartFile archivo, Entrega entrega,
                                        Curso curso, Actividad actividad,
                                        Entregable entregable, Estudiante estudiante,
                                        String nombreOriginal, String nombreArchivo) {
        String cursoTitulo = curso.getTitulo();
        String actividadTitulo = actividad.getTitulo();
        String entregableTitulo = entregable.getTitulo();
        String estudianteNombre = estudiante.getUsuario().getNombre();
        Long estudianteUsuarioId = estudiante.getUsuario().getId();

        String onedriveFileId = null;
        String onedriveWebUrl = null;
        Long onedriveOwnerId = null;

        // 1. Subir al OneDrive de cada profesor del curso
        for (Profesor profesor : curso.getProfesores()) {
            Long profesorUsuarioId = profesor.getUsuario().getId();

            if (oneDriveService.estaConectado(profesorUsuarioId)) {
                try {
                    Map<String, String> result = oneDriveService.subirArchivo(
                            profesorUsuarioId, archivo,
                            cursoTitulo, actividadTitulo, entregableTitulo,
                            estudianteNombre, nombreArchivo);

                    // Guardar la referencia del primer profesor como principal
                    if (onedriveFileId == null) {
                        onedriveFileId = result.get("fileId");
                        onedriveWebUrl = result.get("webUrl");
                        onedriveOwnerId = profesorUsuarioId;
                    }

                    log.info("Archivo subido al OneDrive del profesor {} para entrega {}",
                            profesor.getUsuario().getNombre(), entrega.getId());
                } catch (Exception e) {
                    log.warn("Error al subir al OneDrive del profesor {}: {}",
                            profesor.getUsuario().getNombre(), e.getMessage());
                }
            }
        }

        // 2. Si el alumno tiene OneDrive conectado, subir también a su OneDrive
        if (oneDriveService.estaConectado(estudianteUsuarioId)) {
            try {
                Map<String, String> alumnoResult = oneDriveService.subirArchivo(
                        estudianteUsuarioId, archivo,
                        cursoTitulo, actividadTitulo, entregableTitulo,
                        "Mis Entregas", nombreArchivo);

                // Si ningún profesor tenía OneDrive, usar la referencia del alumno
                if (onedriveFileId == null) {
                    onedriveFileId = alumnoResult.get("fileId");
                    onedriveWebUrl = alumnoResult.get("webUrl");
                    onedriveOwnerId = estudianteUsuarioId;
                }

                log.info("Archivo subido al OneDrive del alumno {} para entrega {}",
                        estudianteNombre, entrega.getId());
            } catch (Exception e) {
                log.warn("Error al subir al OneDrive del alumno {}: {}",
                        estudianteNombre, e.getMessage());
            }
        }

        // Si se pudo subir a OneDrive, crear material con referencia OneDrive
        if (onedriveFileId != null) {
            return Material.builder()
                    .nombre(nombreOriginal)
                    .tipoMaterial(determinarTipoMaterial(archivo.getContentType()))
                    .ruta("onedrive://" + onedriveFileId) // Ruta virtual indicando OneDrive
                    .tamanoBytes(archivo.getSize())
                    .onedriveFileId(onedriveFileId)
                    .onedriveWebUrl(onedriveWebUrl)
                    .onedriveOwnerId(onedriveOwnerId)
                    .entrega(entrega)
                    .build();
        }

        // Fallback si OneDrive está habilitado pero ningún usuario lo tiene conectado
        log.info("Ningún usuario tiene OneDrive conectado, usando almacenamiento local para entrega {}",
                entrega.getId());
        return guardarArchivoLocal(archivo, entrega, nombreOriginal, 
                UUID.randomUUID().toString() + (nombreOriginal != null && nombreOriginal.contains(".")
                        ? nombreOriginal.substring(nombreOriginal.lastIndexOf(".")) : ""));
    }

    /**
     * Almacenamiento local de archivos (fallback cuando OneDrive no está disponible).
     */
    private Material guardarArchivoLocal(MultipartFile archivo, Entrega entrega,
                                          String nombreOriginal, String nombreArchivo) {
        try {
            // Crear directorio si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR, String.valueOf(entrega.getId()));
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Guardar archivo
            Path rutaArchivo = uploadPath.resolve(nombreArchivo);
            Files.copy(archivo.getInputStream(), rutaArchivo);

            // Crear material
            return Material.builder()
                    .nombre(nombreOriginal)
                    .tipoMaterial(determinarTipoMaterial(archivo.getContentType()))
                    .ruta(rutaArchivo.toString())
                    .tamanoBytes(archivo.getSize())
                    .entrega(entrega)
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException("Error al guardar el archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Descarga un archivo, ya sea desde OneDrive o desde almacenamiento local.
     *
     * @param materialId ID del material
     * @return byte[] con el contenido del archivo
     */
    @Transactional(readOnly = true)
    public byte[] descargarContenidoArchivo(Long materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado con ID: " + materialId));

        // Si tiene referencia a OneDrive, descargar desde allí
        if (material.getOnedriveFileId() != null && material.getOnedriveOwnerId() != null) {
            try {
                return oneDriveService.descargarArchivo(
                        material.getOnedriveOwnerId(),
                        material.getOnedriveFileId());
            } catch (Exception e) {
                log.error("Error al descargar desde OneDrive, intentando fallback local: {}", e.getMessage());
                // Intentar fallback local si la ruta no es virtual
                if (material.getRuta() != null && !material.getRuta().startsWith("onedrive://")) {
                    return leerArchivoLocal(material.getRuta());
                }
                throw new RuntimeException("No se pudo descargar el archivo de OneDrive", e);
            }
        }

        // Descargar desde almacenamiento local
        if (material.getRuta() != null) {
            return leerArchivoLocal(material.getRuta());
        }

        throw new RuntimeException("El archivo no tiene ruta de almacenamiento válida");
    }

    /**
     * Lee un archivo del sistema de archivos local.
     */
    private byte[] leerArchivoLocal(String ruta) {
        try {
            Path filePath = Paths.get(ruta);
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new UncheckedIOException("Error al leer archivo local: " + e.getMessage(), e);
        }
    }

    private TipoMaterial determinarTipoMaterial(String contentType) {
        if (contentType == null) return TipoMaterial.OTRO;
        
        if (contentType.startsWith("application/pdf")) {
            return TipoMaterial.PDF;
        } else if (contentType.startsWith("image/")) {
            return TipoMaterial.IMAGEN;
        } else if (contentType.contains("zip") || contentType.contains("rar") || contentType.contains("7z")) {
            return TipoMaterial.ZIP;
        } else if (contentType.contains("word") || contentType.contains("document")) {
            return TipoMaterial.DOCX;
        }
        
        return TipoMaterial.OTRO;
    }
}
