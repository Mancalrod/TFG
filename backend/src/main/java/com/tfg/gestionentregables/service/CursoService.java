package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Servicio para gestión de cursos.
 * Implementa operaciones SYSOP-002, SYSOP-003, SYSOP-004.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CursoService {

    private static final String PROFESOR_NOT_FOUND = "Profesor no encontrado con ID: ";
    private static final String CURSO_NOT_FOUND = "Curso no encontrado con ID: ";

    private final CursoRepository cursoRepository;
    private final ProfesorRepository profesorRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final GrupoRepository grupoRepository;
    private final EntityMapper mapper;

    /**
     * SYSOP-002: Crea un nuevo curso.
     */
    public CursoDTO crearCurso(CrearCursoDTO dto, Long profesorId) {
        Profesor profesor = profesorRepository.findById(profesorId)
                .orElseThrow(() -> new EntityNotFoundException(PROFESOR_NOT_FOUND + profesorId));

        if (cursoRepository.existsByCodigo(dto.getCodigo())) {
            throw new IllegalArgumentException("Ya existe un curso con ese código");
        }

        Curso curso = Curso.builder()
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .codigo(dto.getCodigo())
                .build();
        
        curso.addProfesor(profesor);
        curso = cursoRepository.save(curso);
        return mapper.toDTO(curso);
    }

        /**
         * Crea un curso asignando como profesor a un usuario (sin requerir profesorId previo).
         */
        public CursoDTO crearCursoPorUsuario(CrearCursoDTO dto, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        if (Boolean.TRUE.equals(usuario.getEsAdmin())) {
            throw new IllegalStateException("Un administrador no puede ser profesor");
        }

        if (cursoRepository.existsByCodigo(dto.getCodigo())) {
            throw new IllegalArgumentException("Ya existe un curso con ese código");
        }

        Curso curso = Curso.builder()
            .titulo(dto.getTitulo())
            .descripcion(dto.getDescripcion())
            .codigo(dto.getCodigo())
            .build();

        curso = cursoRepository.save(curso);

        Profesor profesor = Profesor.builder()
            .usuario(usuario)
            .curso(curso)
            .build();
        profesorRepository.save(profesor);

        Long nuevoCursoId = curso.getId();
        Curso actualizado = cursoRepository.findById(nuevoCursoId)
            .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + nuevoCursoId));
        return mapper.toDTO(actualizado);
        }

    /**
     * SYSOP-003: Lista cursos de un profesor por su usuarioId.
     */
    @Transactional(readOnly = true)
    public List<CursoDTO> listarCursosProfesor(Long usuarioId) {
        if (!profesorRepository.existsByUsuarioId(usuarioId)) {
            throw new EntityNotFoundException("Profesor no encontrado para usuario con ID: " + usuarioId);
        }
        
        return cursoRepository.findByProfesorUsuarioId(usuarioId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * SYSOP-004: Lista cursos de un estudiante.
     */
    @Transactional(readOnly = true)
    public List<CursoDTO> listarCursosEstudiante(Long estudianteUsuarioId) {
        return cursoRepository.findByEstudianteUsuarioId(estudianteUsuarioId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Obtiene un curso por su ID.
     */
    @Transactional(readOnly = true)
    public CursoDTO obtenerCursoPorId(Long id) {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + id));
        return mapper.toDTO(curso);
    }

    /**
     * Obtiene un curso por su código.
     */
    @Transactional(readOnly = true)
    public CursoDTO obtenerCursoPorCodigo(String codigo) {
        Curso curso = cursoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado con código: " + codigo));
        return mapper.toDTO(curso);
    }

    /**
     * Lista todos los cursos.
     */
    @Transactional(readOnly = true)
    public List<CursoDTO> listarTodosCursos() {
        return cursoRepository.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Actualiza un curso existente.
     */
    public CursoDTO actualizarCurso(Long id, CrearCursoDTO dto) {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + id));

        // Verificar si el nuevo código ya existe en otro curso
        if (!curso.getCodigo().equals(dto.getCodigo()) && cursoRepository.existsByCodigo(dto.getCodigo())) {
            throw new IllegalArgumentException("Ya existe un curso con ese código");
        }

        curso.setTitulo(dto.getTitulo());
        curso.setDescripcion(dto.getDescripcion());
        curso.setCodigo(dto.getCodigo());

        curso = cursoRepository.save(curso);
        return mapper.toDTO(curso);
    }

    /**
     * Elimina un curso.
     */
    public void eliminarCurso(Long id) {
        if (!cursoRepository.existsById(id)) {
            throw new EntityNotFoundException(CURSO_NOT_FOUND + id);
        }
        cursoRepository.deleteById(id);
    }

    /**
     * Añade un profesor a un curso.
     */
    public CursoDTO agregarProfesor(Long cursoId, Long profesorId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + cursoId));
        Profesor profesor = profesorRepository.findById(profesorId)
                .orElseThrow(() -> new EntityNotFoundException(PROFESOR_NOT_FOUND + profesorId));

        Long usuarioId = profesor.getUsuario().getId();
        if (estudianteRepository.existsByUsuarioIdAndGrupoCursoId(usuarioId, cursoId)) {
            throw new IllegalStateException("Un usuario no puede ser profesor y estudiante del mismo curso");
        }

        curso.addProfesor(profesor);
        curso = cursoRepository.save(curso);
        return mapper.toDTO(curso);
    }

    /**
     * Quita un profesor de un curso.
     */
    public CursoDTO quitarProfesor(Long cursoId, Long profesorId) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + cursoId));
        Profesor profesor = profesorRepository.findById(profesorId)
                .orElseThrow(() -> new EntityNotFoundException(PROFESOR_NOT_FOUND + profesorId));

        curso.removeProfesor(profesor);
        curso = cursoRepository.save(curso);
        return mapper.toDTO(curso);
    }

        /**
         * Añade un profesor a un curso usando usuarioId.
         * Crea una relación Profesor-Usuario-Curso independiente por curso.
         */
        public CursoDTO agregarProfesorPorUsuario(Long cursoId, Long usuarioId) {
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + cursoId));
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        if (Boolean.TRUE.equals(usuario.getEsAdmin())) {
            throw new IllegalStateException("Un administrador no puede ser profesor");
        }

        if (profesorRepository.existsByUsuarioIdAndCursoId(usuarioId, cursoId)) {
            throw new IllegalStateException("El usuario ya está asignado como profesor en este curso");
        }

        Profesor profesor = Profesor.builder()
            .usuario(usuario)
            .curso(curso)
            .build();
        profesorRepository.save(profesor);

        Curso actualizado = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + cursoId));
        return mapper.toDTO(actualizado);
        }

        /**
         * Quita un profesor de un curso usando usuarioId.
         */
        public CursoDTO quitarProfesorPorUsuario(Long cursoId, Long usuarioId) {
        Curso curso = cursoRepository.findById(cursoId)
            .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + cursoId));

        Profesor profesor = profesorRepository.findByUsuarioIdAndCursoId(usuarioId, cursoId)
            .orElseThrow(() -> new EntityNotFoundException(
                "El usuario " + usuarioId + " no está asignado como profesor en el curso " + cursoId));

        profesorRepository.delete(profesor);

        // Si ya no tiene cursos asignados, eliminar cualquier fila residual de profesor
        // para que no siga apareciendo con rol de profesor.
        if (profesorRepository.countByUsuarioIdAndCursoIsNotNull(usuarioId) == 0) {
            profesorRepository.deleteByUsuarioId(usuarioId);
        }

        Curso actualizado = cursoRepository.findById(curso.getId())
            .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + cursoId));
        return mapper.toDTO(actualizado);
        }

    /**
     * Crea un grupo en un curso.
     */
    public GrupoDTO crearGrupo(Long cursoId, String titulo) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + cursoId));

        Grupo grupo = Grupo.builder()
                .titulo(titulo)
                .curso(curso)
                .build();

        grupo.asignarCurso(curso);
        
        grupo = grupoRepository.save(grupo);
        return mapper.toDTO(grupo);
    }

    /**
     * Crea un grupo asociado a varios cursos.
     */
    public GrupoDTO crearGrupoConCursos(String titulo, List<Long> cursoIds) {
        if (cursoIds == null || cursoIds.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar al menos un curso");
        }

        List<Curso> cursos = cursoRepository.findAllById(cursoIds);
        Set<Long> encontrados = cursos.stream().map(Curso::getId).collect(Collectors.toSet());
        List<Long> faltantes = cursoIds.stream().filter(id -> !encontrados.contains(id)).toList();
        if (!faltantes.isEmpty()) {
            throw new EntityNotFoundException("Cursos no encontrados: " + faltantes);
        }

        Curso cursoPrincipal = cursos.getFirst();
        Grupo grupo = Grupo.builder()
                .titulo(titulo)
                .curso(cursoPrincipal)
                .build();
        cursos.forEach(grupo::asignarCurso);

        grupo = grupoRepository.save(grupo);
        return mapper.toDTO(grupo);
    }

    /**
     * Lista grupos de un curso.
     */
    @Transactional(readOnly = true)
    public List<GrupoDTO> listarGrupos(Long cursoId) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new EntityNotFoundException(CURSO_NOT_FOUND + cursoId);
        }
        
        List<Grupo> grupos = grupoRepository.findByCursoRelacionadoId(cursoId);
        if (grupos.isEmpty()) {
            // Compatibilidad con tests y datos legado.
            grupos = grupoRepository.findByCursoId(cursoId);
        }

        return grupos.stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GrupoDTO> listarTodosGrupos() {
        return grupoRepository.findAllWithCursos().stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Actualiza el título de un grupo.
     */
    public GrupoDTO actualizarGrupo(Long grupoId, String titulo) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado con ID: " + grupoId));
        grupo.setTitulo(titulo);
        grupo = grupoRepository.save(grupo);
        return mapper.toDTO(grupo);
    }

    public GrupoDTO actualizarGrupoConCursos(Long grupoId, String titulo, List<Long> cursoIds) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado con ID: " + grupoId));

        if (cursoIds == null || cursoIds.isEmpty()) {
            throw new IllegalArgumentException("Debes seleccionar al menos un curso");
        }

        List<Curso> cursos = cursoRepository.findAllById(cursoIds);
        Set<Long> encontrados = cursos.stream().map(Curso::getId).collect(Collectors.toSet());
        List<Long> faltantes = cursoIds.stream().filter(id -> !encontrados.contains(id)).toList();
        if (!faltantes.isEmpty()) {
            throw new EntityNotFoundException("Cursos no encontrados: " + faltantes);
        }

        grupo.setTitulo(titulo);
        Curso cursoPrincipal = cursos.getFirst();
        grupo.setCurso(cursoPrincipal);
        grupo.getCursos().clear();
        cursos.forEach(grupo::asignarCurso);

        grupo = grupoRepository.save(grupo);
        return mapper.toDTO(grupo);
    }

    /**
     * Elimina un grupo.
     */
    public void eliminarGrupo(Long grupoId) {
        if (!grupoRepository.existsById(grupoId)) {
            throw new EntityNotFoundException("Grupo no encontrado con ID: " + grupoId);
        }
        grupoRepository.deleteById(grupoId);
    }
}
