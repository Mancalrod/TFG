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
import java.util.HashSet;
import java.util.List;

/**
 * Servicio para gestión de actividades.
 * Implementa operaciones SYSOP-005 a SYSOP-009.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ActividadService {

    private static final String ACTIVIDAD_NOT_FOUND = "Actividad no encontrada con ID: ";

    private final ActividadRepository actividadRepository;
    private final CursoRepository cursoRepository;
    private final GrupoRepository grupoRepository;
    private final EntityMapper mapper;

    /**
     * SYSOP-005: Crea una nueva actividad en un curso.
     */
    public ActividadDTO crearActividad(CrearActividadDTO dto, Long cursoId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado con ID: " + cursoId));

        Actividad actividad = Actividad.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .tipoActividad(dto.getTipoActividad())
                .fechaCreacion(LocalDateTime.now())
                .fechaInicio(dto.getFechaInicio())
                .fechaLimite(dto.getFechaLimite())
                .visibilidad(dto.getVisibilidad() != null ? dto.getVisibilidad() : Visibilidad.OCULTO)
                .notaMaxima(dto.getNotaMaxima())
                .subirAOneDrive(Boolean.TRUE.equals(dto.getSubirAOneDrive()))
                .oneDriveUsuarioId(Boolean.TRUE.equals(dto.getSubirAOneDrive()) ? dto.getOneDriveUsuarioId() : null)
                .carpetaOneDrive(Boolean.TRUE.equals(dto.getSubirAOneDrive()) ? dto.getCarpetaOneDrive() : null)
                .curso(curso)
                .build();

        // Asignar grupos si se especifican
        if (dto.getGrupoIds() != null && !dto.getGrupoIds().isEmpty()) {
            List<Grupo> grupos = grupoRepository.findAllById(dto.getGrupoIds());
            actividad.setGrupos(new HashSet<>(grupos));
        }

        actividad = actividadRepository.save(actividad);
        return mapper.toDTO(actividad);
    }

    /**
     * SYSOP-006: Lista actividades de un curso (para profesor).
     */
    @Transactional(readOnly = true)
    public List<ActividadDTO> listarActividadesCurso(Long cursoId) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new EntityNotFoundException("Curso no encontrado con ID: " + cursoId);
        }

        return actividadRepository.findByCursoId(cursoId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * SYSOP-007: Lista actividades visibles para un grupo específico.
     */
    @Transactional(readOnly = true)
    public List<ActividadDTO> listarActividadesVisiblesGrupo(Long grupoId) {
        if (!grupoRepository.existsById(grupoId)) {
            throw new EntityNotFoundException("Grupo no encontrado con ID: " + grupoId);
        }

        return actividadRepository.findByGrupoIdAndVisibilidad(grupoId, Visibilidad.VISIBLE).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * SYSOP-008: Obtiene detalle de una actividad con sus entregables.
     */
    @Transactional(readOnly = true)
    public ActividadDTO obtenerActividadConEntregables(Long actividadId) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new EntityNotFoundException(ACTIVIDAD_NOT_FOUND + actividadId));
        return mapper.toDTOWithEntregables(actividad);
    }

    /**
     * SYSOP-009: Publica/oculta una actividad.
     */
    public ActividadDTO cambiarVisibilidad(Long actividadId, Visibilidad visibilidad) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada con ID: " + actividadId));

        actividad.setVisibilidad(visibilidad);
        actividad = actividadRepository.save(actividad);
        return mapper.toDTO(actividad);
    }

    /**
     * Obtiene una actividad por su ID.
     */
    @Transactional(readOnly = true)
    public ActividadDTO obtenerActividadPorId(Long id) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ACTIVIDAD_NOT_FOUND + id));
        return mapper.toDTO(actividad);
    }

    /**
     * Actualiza una actividad existente.
     */
    public ActividadDTO actualizarActividad(Long id, CrearActividadDTO dto) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada con ID: " + id));

        actividad.setTitulo(dto.getTitulo());
        actividad.setDescripcion(dto.getDescripcion());
        actividad.setTipoActividad(dto.getTipoActividad());
        actividad.setFechaInicio(dto.getFechaInicio());
        actividad.setFechaLimite(dto.getFechaLimite());
        if (dto.getVisibilidad() != null) {
            actividad.setVisibilidad(dto.getVisibilidad());
        }
        actividad.setNotaMaxima(dto.getNotaMaxima());

        // OneDrive
        actividad.setSubirAOneDrive(Boolean.TRUE.equals(dto.getSubirAOneDrive()));
        actividad.setOneDriveUsuarioId(
                Boolean.TRUE.equals(dto.getSubirAOneDrive()) ? dto.getOneDriveUsuarioId() : null);
        actividad.setCarpetaOneDrive(
                Boolean.TRUE.equals(dto.getSubirAOneDrive()) ? dto.getCarpetaOneDrive() : null);

        // Actualizar grupos si se especifican
        if (dto.getGrupoIds() != null) {
            List<Grupo> grupos = grupoRepository.findAllById(dto.getGrupoIds());
            actividad.setGrupos(new HashSet<>(grupos));
        }

        actividad = actividadRepository.save(actividad);
        return mapper.toDTO(actividad);
    }

    /**
     * Elimina una actividad.
     */
    public void eliminarActividad(Long id) {
        if (!actividadRepository.existsById(id)) {
            throw new EntityNotFoundException(ACTIVIDAD_NOT_FOUND + id);
        }
        actividadRepository.deleteById(id);
    }

    /**
     * Lista actividades en plazo de un curso.
     */
    @Transactional(readOnly = true)
    public List<ActividadDTO> listarActividadesEnPlazo(Long cursoId) {
        LocalDateTime ahora = LocalDateTime.now();
        return actividadRepository.findByCursoIdAndFechaLimiteAfter(cursoId, ahora).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Lista actividades próximas a su fecha límite.
     */
    @Transactional(readOnly = true)
    public List<ActividadDTO> listarActividadesProximasLimite(Long cursoId, int dias) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.plusDays(dias);
        return actividadRepository.findByCursoIdAndFechaLimiteBetween(cursoId, ahora, limite).stream()
                .map(mapper::toDTO)
                .toList();
    }
}
