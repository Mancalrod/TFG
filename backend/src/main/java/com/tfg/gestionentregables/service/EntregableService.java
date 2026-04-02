package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private final EntregaRepository entregaRepository;
    private final ProfesorRepository profesorRepository;
    private final EntityMapper mapper;
    private final NotificacionService notificacionService;

    /**
     * SYSOP-010: Crea un nuevo entregable en una actividad.
     */
    public EntregableDTO crearEntregable(CrearEntregableDTO dto, Long actividadId) {
        return crearEntregable(dto, actividadId, null, false);
    }

    public EntregableDTO crearEntregable(CrearEntregableDTO dto, Long actividadId, Long actorUsuarioId, boolean actorEsAdmin) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada con ID: " + actividadId));

        verificarAccesoProfesorACurso(actividad.getCurso().getId(), actorUsuarioId, actorEsAdmin);

        Entregable entregable = Entregable.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .fechaInicio(dto.getFechaInicio())
                .fechaLimite(dto.getFechaLimite())
                .notaMaxima(dto.getNotaMaxima())
                .tipoArchivoEsperado(dto.getTipoArchivoEsperado())
                .tamanoMaximoBytes(dto.getTamanoMaximoBytes())
                .visibilidad(dto.getVisibilidad() != null ? dto.getVisibilidad() : Visibilidad.OCULTO)
                .permiteReenvio(!Boolean.FALSE.equals(dto.getPermiteReenvio()))
                .estructuraZip(dto.getEstructuraZip())
                .validacionZipEstricta(Boolean.TRUE.equals(dto.getValidacionZipEstricta()))
                .nombreZipEsperado(dto.getNombreZipEsperado())
                .actividad(actividad)
                .build();

        entregable = entregableRepository.save(entregable);
        if (entregable.getVisibilidad() == Visibilidad.VISIBLE) {
            notificacionService.notificarNuevoEntregable(entregable);
        }
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
        return actualizarEntregable(id, dto, null, false);
    }

    public EntregableDTO actualizarEntregable(Long id, CrearEntregableDTO dto, Long actorUsuarioId, boolean actorEsAdmin) {
        Entregable entregable = entregableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTREGABLE_NOT_FOUND + id));

        verificarAccesoProfesorACurso(entregable.getActividad().getCurso().getId(), actorUsuarioId, actorEsAdmin);

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
        return cambiarVisibilidad(entregableId, visibilidad, null, false);
    }

    public EntregableDTO cambiarVisibilidad(Long entregableId, Visibilidad visibilidad, Long actorUsuarioId, boolean actorEsAdmin) {
        Entregable entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new EntityNotFoundException(ENTREGABLE_NOT_FOUND + entregableId));

        verificarAccesoProfesorACurso(entregable.getActividad().getCurso().getId(), actorUsuarioId, actorEsAdmin);

        entregable.setVisibilidad(visibilidad);
        entregable = entregableRepository.save(entregable);
        return mapper.toDTO(entregable);
    }

    public EntregableDTO cambiarVisibilidadNotasEstudiante(Long entregableId,
                                                           boolean visible,
                                                           Long actorUsuarioId,
                                                           boolean actorEsAdmin) {
        Entregable entregable = entregableRepository.findById(entregableId)
            .orElseThrow(() -> new EntityNotFoundException(ENTREGABLE_NOT_FOUND + entregableId));

        verificarAccesoProfesorACurso(entregable.getActividad().getCurso().getId(), actorUsuarioId, actorEsAdmin);

        entregable.setNotasVisiblesEstudiante(visible);
        entregable = entregableRepository.save(entregable);

        List<Entrega> entregasActivas = entregaRepository.findByEntregableIdAndEsVersionActiva(entregableId, true);
        for (Entrega entrega : entregasActivas) {
            if (entrega.getCalificacion() == null) {
                continue;
            }
            if (visible) {
                entrega.setEstado(EstadoEntrega.PUBLICADO);
                notificacionService.notificarEntregaEvaluada(entrega, true);
            } else if (entrega.getEstado() == EstadoEntrega.PUBLICADO) {
                entrega.setEstado(EstadoEntrega.CALIFICADO);
            }
        }
        entregaRepository.saveAll(entregasActivas);

        return mapper.toDTO(entregable);
    }

    /**
     * Elimina un entregable.
     */
    public void eliminarEntregable(Long id) {
        eliminarEntregable(id, null, false);
    }

    public void eliminarEntregable(Long id, Long actorUsuarioId, boolean actorEsAdmin) {
        if (!entregableRepository.existsById(id)) {
            throw new EntityNotFoundException(ENTREGABLE_NOT_FOUND + id);
        }
        if (!actorEsAdmin && actorUsuarioId != null) {
            Entregable entregable = entregableRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException(ENTREGABLE_NOT_FOUND + id));
            verificarAccesoProfesorACurso(entregable.getActividad().getCurso().getId(), actorUsuarioId, false);
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

    private void verificarAccesoProfesorACurso(Long cursoId, Long actorUsuarioId, boolean actorEsAdmin) {
        if (actorEsAdmin || actorUsuarioId == null) {
            return;
        }
        boolean esProfesorDelCurso = profesorRepository.existsByUsuarioIdAndCursoId(actorUsuarioId, cursoId);
        if (!esProfesorDelCurso) {
            throw new AccessDeniedException("No tienes permisos sobre este curso");
        }
    }
}
