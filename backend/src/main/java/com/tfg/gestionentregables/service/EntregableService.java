package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Servicio para gestión de entregables.
 * Implementa operaciones SYSOP-010 a SYSOP-012.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EntregableService {

    private static final String ENTREGABLE_NOT_FOUND = "Entregable no encontrado con ID: ";

    private final EntregableRepository entregableRepository;
    private final ActividadRepository actividadRepository;
    private final EntityMapper mapper;

    /**
     * SYSOP-010: Crea un nuevo entregable en una actividad.
     */
    public EntregableDTO crearEntregable(CrearEntregableDTO dto, Long actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada con ID: " + actividadId));

        Entregable entregable = Entregable.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .fechaInicio(dto.getFechaInicio())
                .fechaLimite(dto.getFechaLimite())
                .notaMaxima(dto.getNotaMaxima())
                .tipoArchivoEsperado(dto.getTipoArchivoEsperado())
                .tamanoMaximoBytes(dto.getTamanoMaximoBytes())
                .visibilidad(dto.getVisibilidad() != null ? dto.getVisibilidad() : Visibilidad.OCULTO)
                .permiteReenvio(dto.getPermiteReenvio() == null || dto.getPermiteReenvio())
                .estructuraZip(dto.getEstructuraZip())
                .validacionZipEstricta(dto.getValidacionZipEstricta() != null ? dto.getValidacionZipEstricta() : false)
                .nombreZipEsperado(dto.getNombreZipEsperado())
                .actividad(actividad)
                .build();

        entregable = entregableRepository.save(entregable);
        return mapper.toDTO(entregable);
    }

    /**
     * SYSOP-011: Lista entregables de una actividad.
     */
    @Transactional(readOnly = true)
    public List<EntregableDTO> listarEntregablesActividad(Long actividadId) {
        if (!actividadRepository.existsById(actividadId)) {
            throw new EntityNotFoundException(ENTREGABLE_NOT_FOUND + actividadId);
        }
        
        return entregableRepository.findByActividadId(actividadId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * SYSOP-012: Obtiene detalle de un entregable.
     */
    @Transactional(readOnly = true)
    public EntregableDTO obtenerEntregable(Long entregableId) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new EntityNotFoundException(ENTREGABLE_NOT_FOUND + entregableId));
        return mapper.toDTO(entregable);
    }

    /**
     * Lista entregables visibles de una actividad.
     */
    @Transactional(readOnly = true)
    public List<EntregableDTO> listarEntregablesVisibles(Long actividadId) {
        if (!actividadRepository.existsById(actividadId)) {
            throw new EntityNotFoundException(ENTREGABLE_NOT_FOUND + actividadId);
        }
        
        return entregableRepository.findByActividadIdAndVisibilidad(actividadId, Visibilidad.VISIBLE).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Actualiza un entregable existente.
     */
    public EntregableDTO actualizarEntregable(Long id, CrearEntregableDTO dto) {
        Entregable entregable = entregableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTREGABLE_NOT_FOUND + id));

        entregable.setTitulo(dto.getTitulo());
        entregable.setDescripcion(dto.getDescripcion());
        entregable.setFechaInicio(dto.getFechaInicio());
        entregable.setFechaLimite(dto.getFechaLimite());
        entregable.setNotaMaxima(dto.getNotaMaxima());
        entregable.setTipoArchivoEsperado(dto.getTipoArchivoEsperado());
        entregable.setTamanoMaximoBytes(dto.getTamanoMaximoBytes());
        if (dto.getVisibilidad() != null) {
            entregable.setVisibilidad(dto.getVisibilidad());
        }
        if (dto.getPermiteReenvio() != null) {
            entregable.setPermiteReenvio(dto.getPermiteReenvio());
        }
        entregable.setEstructuraZip(dto.getEstructuraZip());
        if (dto.getValidacionZipEstricta() != null) {
            entregable.setValidacionZipEstricta(dto.getValidacionZipEstricta());
        }
        entregable.setNombreZipEsperado(dto.getNombreZipEsperado());

        entregable = entregableRepository.save(entregable);
        return mapper.toDTO(entregable);
    }

    /**
     * Cambia la visibilidad de un entregable.
     */
    public EntregableDTO cambiarVisibilidad(Long entregableId, Visibilidad visibilidad) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new EntityNotFoundException(ENTREGABLE_NOT_FOUND + entregableId));
        
        entregable.setVisibilidad(visibilidad);
        entregable = entregableRepository.save(entregable);
        return mapper.toDTO(entregable);
    }

    /**
     * Elimina un entregable.
     */
    public void eliminarEntregable(Long id) {
        if (!entregableRepository.existsById(id)) {
            throw new EntityNotFoundException(ENTREGABLE_NOT_FOUND + id);
        }
        entregableRepository.deleteById(id);
    }

    /**
     * Lista entregables en plazo de una actividad.
     */
    @Transactional(readOnly = true)
    public List<EntregableDTO> listarEntregablesEnPlazo(Long actividadId) {
        LocalDateTime ahora = LocalDateTime.now();
        return entregableRepository.findByActividadIdAndFechaLimiteAfter(actividadId, ahora).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Lista entregables próximos a vencer.
     */
    @Transactional(readOnly = true)
    public List<EntregableDTO> listarEntregablesProximosVencer(Long actividadId, int dias) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.plusDays(dias);
        return entregableRepository.findByActividadIdAndFechaLimiteBetween(actividadId, ahora, limite).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Lista entregables pendientes de un estudiante en una actividad.
     */
    @Transactional(readOnly = true)
    public List<EntregableDTO> listarEntregablesPendientesEstudiante(Long actividadId, Long estudianteId) {
        // Obtener todos los entregables de la actividad
        return entregableRepository.findByActividadId(actividadId).stream()
                .map(mapper::toDTO)
                .toList();
    }
}
