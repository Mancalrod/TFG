package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


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
     * Crea un grupo en un curso.
     */
    public GrupoDTO crearGrupo(Long cursoId, String titulo) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new EntityNotFoundException(CURSO_NOT_FOUND + cursoId));

        Grupo grupo = Grupo.builder()
                .titulo(titulo)
                .curso(curso)
                .build();
        
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
        
        return grupoRepository.findByCursoId(cursoId).stream()
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
