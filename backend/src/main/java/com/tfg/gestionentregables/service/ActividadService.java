package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ProfesorRepository profesorRepository;
    private final EstudianteRepository estudianteRepository;
    private final EntityMapper mapper;
    private final NotificacionService notificacionService;

    /**
     * SYSOP-005: Crea una nueva actividad en un curso.
     */
    public ActividadDTO crearActividad(CrearActividadDTO dto, Long cursoId) {
        return crearActividad(dto, cursoId, null, false);
    }

    public ActividadDTO crearActividad(CrearActividadDTO dto, Long cursoId, Long actorUsuarioId, boolean actorEsAdmin) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado con ID: " + cursoId));

        verificarAccesoProfesorACurso(cursoId, actorUsuarioId, actorEsAdmin);

        boolean subirAOneDrive = Boolean.TRUE.equals(dto.getSubirAOneDrive());
        ModoOneDrive modoOneDrive = subirAOneDrive
            ? (dto.getModoOneDrive() != null ? dto.getModoOneDrive() : ModoOneDrive.ACTIVIDAD)
            : null;
        String carpetaOneDrive = (subirAOneDrive && modoOneDrive == ModoOneDrive.ACTIVIDAD)
            ? dto.getCarpetaOneDrive()
            : null;

        Actividad actividad = Actividad.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .tipoActividad(dto.getTipoActividad())
                .fechaCreacion(LocalDateTime.now())
                .fechaInicio(dto.getFechaInicio())
                .fechaLimite(dto.getFechaLimite())
                .visibilidad(dto.getVisibilidad() != null ? dto.getVisibilidad() : Visibilidad.OCULTO)
                .notaMaxima(dto.getNotaMaxima())
                .subirAOneDrive(subirAOneDrive)
                .oneDriveUsuarioId(subirAOneDrive ? dto.getOneDriveUsuarioId() : null)
                .carpetaOneDrive(carpetaOneDrive)
                .modoOneDrive(modoOneDrive)
                .curso(curso)
                .build();

        actividad.setGrupos(resolverGruposAsignados(dto.getGrupoIds(), cursoId));

        actividad = actividadRepository.save(actividad);
        if (actividad.getVisibilidad() == Visibilidad.VISIBLE) {
            notificacionService.notificarNuevaActividad(actividad);
        }
        return mapper.toDTO(actividad);
    }

    /**
     * SYSOP-006: Lista actividades de un curso (para profesor).
     */
    @Transactional(readOnly = true)
    public List<ActividadDTO> listarActividadesCurso(Long cursoId) {
        return listarActividadesCurso(cursoId, null, false, false, false);
    }

    @Transactional(readOnly = true)
    public List<ActividadDTO> listarActividadesCurso(Long cursoId,
                                                     Long actorUsuarioId,
                                                     boolean actorEsAdmin,
                                                     boolean actorEsProfesor,
                                                     boolean actorEsEstudiante) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new EntityNotFoundException("Curso no encontrado con ID: " + cursoId);
        }

        boolean accesoComoProfesor = false;
        boolean accesoComoEstudiante = false;

        if (!actorEsAdmin && actorUsuarioId != null) {
            if (actorEsProfesor) {
                accesoComoProfesor = profesorRepository.existsByUsuarioIdAndCursoId(actorUsuarioId, cursoId);
            }
            if (actorEsEstudiante) {
                accesoComoEstudiante = estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(actorUsuarioId, cursoId).isPresent();
            }
            if (!accesoComoProfesor && !accesoComoEstudiante) {
                throw new AccessDeniedException("No tienes acceso a este curso");
            }
        }

        List<Actividad> actividades = (actorUsuarioId != null && !actorEsAdmin && !accesoComoProfesor && accesoComoEstudiante)
            ? actividadRepository.findByCursoIdAndVisibilidad(cursoId, Visibilidad.VISIBLE)
            : actividadRepository.findByCursoId(cursoId);

        return actividades.stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * SYSOP-007: Lista actividades visibles para un grupo específico.
     */
    @Transactional(readOnly = true)
    public List<ActividadDTO> listarActividadesVisiblesGrupo(Long grupoId) {
        return listarActividadesVisiblesGrupo(grupoId, null, false, false, false);
    }

    @Transactional(readOnly = true)
    public List<ActividadDTO> listarActividadesVisiblesGrupo(Long grupoId,
                                                              Long actorUsuarioId,
                                                              boolean actorEsAdmin,
                                                              boolean actorEsProfesor,
                                                              boolean actorEsEstudiante) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado con ID: " + grupoId));

        if (!actorEsAdmin && actorUsuarioId != null) {
            if (actorEsEstudiante) {
                boolean pertenece = estudianteRepository.existsByUsuarioIdAndGrupoId(actorUsuarioId, grupoId);
                if (!pertenece) {
                    throw new AccessDeniedException("No tienes acceso a este grupo");
                }
            } else if (actorEsProfesor) {
                verificarAccesoProfesorACurso(grupo.getCurso().getId(), actorUsuarioId, false);
            }
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
        return cambiarVisibilidad(actividadId, visibilidad, null, false);
    }

    public ActividadDTO cambiarVisibilidad(Long actividadId, Visibilidad visibilidad, Long actorUsuarioId, boolean actorEsAdmin) {
        Actividad actividad = actividadRepository.findById(actividadId)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada con ID: " + actividadId));

        verificarAccesoProfesorACurso(actividad.getCurso().getId(), actorUsuarioId, actorEsAdmin);

        Visibilidad visibilidadAnterior = actividad.getVisibilidad();
        actividad.setVisibilidad(visibilidad);
        actividad = actividadRepository.save(actividad);
        if (visibilidadAnterior != Visibilidad.VISIBLE && visibilidad == Visibilidad.VISIBLE) {
            notificacionService.notificarNuevaActividad(actividad);
        }
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
        return actualizarActividad(id, dto, null, false);
    }

    public ActividadDTO actualizarActividad(Long id, CrearActividadDTO dto, Long actorUsuarioId, boolean actorEsAdmin) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Actividad no encontrada con ID: " + id));

        verificarAccesoProfesorACurso(actividad.getCurso().getId(), actorUsuarioId, actorEsAdmin);

        Visibilidad visibilidadAnterior = actividad.getVisibilidad();

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
        boolean subirAOneDrive = Boolean.TRUE.equals(dto.getSubirAOneDrive());
        ModoOneDrive modoOneDrive = subirAOneDrive
            ? (dto.getModoOneDrive() != null ? dto.getModoOneDrive() : ModoOneDrive.ACTIVIDAD)
            : null;

        actividad.setSubirAOneDrive(subirAOneDrive);
        actividad.setOneDriveUsuarioId(subirAOneDrive ? dto.getOneDriveUsuarioId() : null);
        actividad.setModoOneDrive(modoOneDrive);
        actividad.setCarpetaOneDrive(
            (subirAOneDrive && modoOneDrive == ModoOneDrive.ACTIVIDAD)
                ? dto.getCarpetaOneDrive()
                : null);

        // Actualizar grupos si se especifican
        if (dto.getGrupoIds() != null) {
            actividad.setGrupos(resolverGruposAsignados(dto.getGrupoIds(), actividad.getCurso().getId()));
        }

        actividad = actividadRepository.save(actividad);
        if (visibilidadAnterior != Visibilidad.VISIBLE && actividad.getVisibilidad() == Visibilidad.VISIBLE) {
            notificacionService.notificarNuevaActividad(actividad);
        }
        return mapper.toDTO(actividad);
    }

    private Set<Grupo> resolverGruposAsignados(List<Long> grupoIds, Long cursoId) {
        if (grupoIds == null || grupoIds.isEmpty()) {
            return new HashSet<>(grupoRepository.findByCursoId(cursoId));
        }
        List<Grupo> grupos = grupoRepository.findAllById(grupoIds);
        validarGruposDelCurso(grupoIds, grupos, cursoId);
        return new HashSet<>(grupos);
    }

    /**
     * Elimina una actividad.
     */
    public void eliminarActividad(Long id) {
        eliminarActividad(id, null, false);
    }

    public void eliminarActividad(Long id, Long actorUsuarioId, boolean actorEsAdmin) {
        if (!actividadRepository.existsById(id)) {
            throw new EntityNotFoundException(ACTIVIDAD_NOT_FOUND + id);
        }
        if (!actorEsAdmin && actorUsuarioId != null) {
            Actividad actividad = actividadRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException(ACTIVIDAD_NOT_FOUND + id));
            verificarAccesoProfesorACurso(actividad.getCurso().getId(), actorUsuarioId, false);
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

    private void validarGruposDelCurso(List<Long> grupoIdsSolicitados, List<Grupo> grupos, Long cursoId) {
        Set<Long> idsEncontrados = grupos.stream().map(Grupo::getId).collect(Collectors.toSet());
        List<Long> faltantes = grupoIdsSolicitados.stream()
                .filter(id -> !idsEncontrados.contains(id))
                .toList();
        if (!faltantes.isEmpty()) {
            throw new EntityNotFoundException("Grupos no encontrados: " + faltantes);
        }

        boolean hayGrupoDeOtroCurso = grupos.stream()
                .anyMatch(g -> g.getCurso() == null || !cursoId.equals(g.getCurso().getId()));
        if (hayGrupoDeOtroCurso) {
            throw new IllegalArgumentException("Todos los grupos deben pertenecer al mismo curso de la actividad");
        }
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
