package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


/**
 * Servicio para gestión de entregas.
 * Implementa operaciones SYSOP-013 a SYSOP-018.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EntregaService {

    private final EntregaRepository entregaRepository;
    private final EntregableRepository entregableRepository;
    private final EstudianteRepository estudianteRepository;
    private final MaterialRepository materialRepository;
    private final EntityMapper mapper;

    // Directorio base para almacenar archivos
    private static final String UPLOAD_DIR = "uploads/entregas";

    /**
     * SYSOP-013: Realiza una entrega para un entregable.
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
                Material material = guardarArchivo(archivo, entrega);
                materialRepository.save(material);
            }
        }

        return mapper.toDTO(entrega);
    }

    /**
     * SYSOP-014: Obtiene detalle de una entrega.
     */
    @Transactional(readOnly = true)
    public EntregaDTO obtenerEntrega(Long entregaId) {
        Entrega entrega = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new EntityNotFoundException("Entrega no encontrada con ID: " + entregaId));
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
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new EntityNotFoundException("Entregable no encontrado con ID: " + entregableId));

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

    // Método auxiliar para guardar archivos
    private Material guardarArchivo(MultipartFile archivo, Entrega entrega) {
        try {
            // Crear directorio si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR, String.valueOf(entrega.getId()));
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generar nombre único para el archivo
            String nombreOriginal = archivo.getOriginalFilename();
            String extension = nombreOriginal != null && nombreOriginal.contains(".")
                    ? nombreOriginal.substring(nombreOriginal.lastIndexOf("."))
                    : "";
            String nombreArchivo = UUID.randomUUID().toString() + extension;

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
            throw new RuntimeException("Error al guardar el archivo: " + e.getMessage(), e);
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
