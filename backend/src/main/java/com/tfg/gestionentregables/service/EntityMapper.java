package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;


/**
 * Mapper para convertir entidades a DTOs y viceversa.
 */
@Component
public class EntityMapper {

    public UsuarioDTO toDTO(Usuario usuario) {
        if (usuario == null) return null;
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .telefono(usuario.getTelefono())
                .correoElectronico(usuario.getCorreoElectronico())
                .esAdmin(usuario.getEsAdmin())
                .build();
    }

    public CursoDTO toDTO(Curso curso) {
        if (curso == null) return null;
        return CursoDTO.builder()
                .id(curso.getId())
                .titulo(curso.getTitulo())
                .descripcion(curso.getDescripcion())
                .codigo(curso.getCodigo())
                .grupos(curso.getGrupos().stream().map(this::toSimpleDTO).toList())
                .numeroActividades(curso.getActividades().size())
                .numeroProfesores(curso.getProfesores().size())
                .numeroEstudiantes(curso.getGrupos().stream()
                        .mapToInt(g -> g.getEstudiantes().size())
                        .sum())
                .build();
    }

    public GrupoDTO toDTO(Grupo grupo) {
        if (grupo == null) return null;
        return GrupoDTO.builder()
                .id(grupo.getId())
                .titulo(grupo.getTitulo())
                .cursoId(grupo.getCurso().getId())
                .cursoTitulo(grupo.getCurso().getTitulo())
                .numeroEstudiantes(grupo.getEstudiantes().size())
                .build();
    }

    public GrupoDTO toSimpleDTO(Grupo grupo) {
        if (grupo == null) return null;
        return GrupoDTO.builder()
                .id(grupo.getId())
                .titulo(grupo.getTitulo())
                .numeroEstudiantes(grupo.getEstudiantes().size())
                .build();
    }

    public ActividadDTO toDTO(Actividad actividad) {
        if (actividad == null) return null;
        return ActividadDTO.builder()
                .id(actividad.getId())
                .titulo(actividad.getTitulo())
                .descripcion(actividad.getDescripcion())
                .tipoActividad(actividad.getTipoActividad())
                .fechaCreacion(actividad.getFechaCreacion())
                .fechaInicio(actividad.getFechaInicio())
                .fechaLimite(actividad.getFechaLimite())
                .visibilidad(actividad.getVisibilidad())
                .notaMaxima(actividad.getNotaMaxima())
                .cursoId(actividad.getCurso().getId())
                .cursoTitulo(actividad.getCurso().getTitulo())
                .grupoIds(actividad.getGrupos().stream().map(Grupo::getId).toList())
                .numeroEntregables(actividad.getEntregables().size())
                .enPlazo(actividad.estaEnPlazo())
                .build();
    }

    public ActividadDTO toDTOWithEntregables(Actividad actividad) {
        ActividadDTO dto = toDTO(actividad);
        if (dto != null && actividad.getEntregables() != null) {
            dto.setEntregables(actividad.getEntregables().stream()
                    .map(this::toDTO)
                    .toList());
        }
        return dto;
    }

    public EntregableDTO toDTO(Entregable entregable) {
        if (entregable == null) return null;
        return EntregableDTO.builder()
                .id(entregable.getId())
                .titulo(entregable.getTitulo())
                .descripcion(entregable.getDescripcion())
                .fechaInicio(entregable.getFechaInicio())
                .fechaLimite(entregable.getFechaLimite())
                .notaMaxima(entregable.getNotaMaxima())
                .tipoArchivoEsperado(entregable.getTipoArchivoEsperado())
                .tamanoMaximoBytes(entregable.getTamanoMaximoBytes())
                .visibilidad(entregable.getVisibilidad())
                .permiteReenvio(entregable.getPermiteReenvio())
                .actividadId(entregable.getActividad().getId())
                .actividadTitulo(entregable.getActividad().getTitulo())
                .numeroEntregas((long) entregable.getEntregas().size())
                .enPlazo(entregable.estaEnPlazo())
                .build();
    }

    public EntregaDTO toDTO(Entrega entrega) {
        if (entrega == null) return null;
        return EntregaDTO.builder()
                .id(entrega.getId())
                .nombre(entrega.getNombre())
                .version(entrega.getVersion())
                .fechaEntrega(entrega.getFechaEntrega())
                .estado(entrega.getEstado())
                .calificacion(entrega.getCalificacion())
                .fechaCalificacion(entrega.getFechaCalificacion())
                .esVersionActiva(entrega.getEsVersionActiva())
                .entregableId(entrega.getEntregable().getId())
                .entregableTitulo(entrega.getEntregable().getTitulo())
                .estudianteId(entrega.getEstudiante().getId())
                .estudianteNombre(entrega.getEstudiante().getUsuario().getNombre())
                .fueATiempo(entrega.fueATiempo())
                .archivos(entrega.getArchivos() != null ? 
                        entrega.getArchivos().stream().map(this::toDTO).toList() : 
                        Collections.emptyList())
                .feedbacks(entrega.getFeedbacks() != null ?
                        entrega.getFeedbacks().stream().map(this::toDTO).toList() :
                        Collections.emptyList())
                .build();
    }

    public EntregaResumenDTO toResumenDTO(Entrega entrega) {
        if (entrega == null) return null;
        return EntregaResumenDTO.builder()
                .entregaId(entrega.getId())
                .estudianteId(entrega.getEstudiante().getId())
                .estudianteNombre(entrega.getEstudiante().getUsuario().getNombre())
                .estudianteCorreo(entrega.getEstudiante().getUsuario().getCorreoElectronico())
                .grupoTitulo(entrega.getEstudiante().getGrupo().getTitulo())
                .fechaEntrega(entrega.getFechaEntrega())
                .estado(entrega.getEstado())
                .calificacion(entrega.getCalificacion())
                .fueATiempo(entrega.fueATiempo())
                .version(entrega.getVersion())
                .build();
    }

    public MaterialDTO toDTO(Material material) {
        if (material == null) return null;
        return MaterialDTO.builder()
                .id(material.getId())
                .nombre(material.getNombre())
                .tipoMaterial(material.getTipoMaterial())
                .ruta(material.getRuta())
                .tamanoBytes(material.getTamanoBytes())
                .oneDriveWebUrl(material.getOneDriveWebUrl())
                .oneDriveItemId(material.getOneDriveItemId())
                .build();
    }

    public FeedbackDTO toDTO(Feedback feedback) {
        if (feedback == null) return null;
        return FeedbackDTO.builder()
                .id(feedback.getId())
                .comentario(feedback.getComentario())
                .fechaCreacion(feedback.getFechaCreacion())
                .fechaModificacion(feedback.getFechaModificacion())
                .entregaId(feedback.getEntrega().getId())
                .profesorId(feedback.getProfesor().getId())
                .profesorNombre(feedback.getProfesor().getNombre())
                .build();
    }
}
