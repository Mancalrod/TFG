package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.service.CursoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión de cursos.
 * Implementa endpoints para SYSOP-002 a SYSOP-004.
 */
@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class CursoController {

    private final CursoService cursoService;

    @GetMapping
    public ResponseEntity<List<CursoDTO>> listarTodosCursos() {
        return ResponseEntity.ok(cursoService.listarTodosCursos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CursoDTO> obtenerCurso(@PathVariable Long id) {
        return ResponseEntity.ok(cursoService.obtenerCursoPorId(id));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<CursoDTO> obtenerCursoPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(cursoService.obtenerCursoPorCodigo(codigo));
    }

    /**
     * SYSOP-002: Crear un nuevo curso.
     */
    @PostMapping("/profesor/{profesorId}")
    public ResponseEntity<CursoDTO> crearCurso(
            @PathVariable Long profesorId,
            @Valid @RequestBody CrearCursoDTO dto) {
        CursoDTO curso = cursoService.crearCurso(dto, profesorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(curso);
    }

    @PostMapping("/usuario/{usuarioId}")
    public ResponseEntity<CursoDTO> crearCursoPorUsuario(
            @PathVariable Long usuarioId,
            @Valid @RequestBody CrearCursoDTO dto) {
        CursoDTO curso = cursoService.crearCursoPorUsuario(dto, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(curso);
    }

    /**
     * SYSOP-003: Listar cursos de un profesor.
     */
    @GetMapping("/profesor/{profesorId}")
    public ResponseEntity<List<CursoDTO>> listarCursosProfesor(@PathVariable Long profesorId) {
        return ResponseEntity.ok(cursoService.listarCursosProfesor(profesorId));
    }

    /**
     * SYSOP-004: Listar cursos de un estudiante.
     */
    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<CursoDTO>> listarCursosEstudiante(@PathVariable Long estudianteId) {
        return ResponseEntity.ok(cursoService.listarCursosEstudiante(estudianteId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CursoDTO> actualizarCurso(
            @PathVariable Long id,
            @Valid @RequestBody CrearCursoDTO dto) {
        return ResponseEntity.ok(cursoService.actualizarCurso(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCurso(@PathVariable Long id) {
        cursoService.eliminarCurso(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{cursoId}/profesores/{profesorId}")
    public ResponseEntity<CursoDTO> agregarProfesor(
            @PathVariable Long cursoId,
            @PathVariable Long profesorId) {
        return ResponseEntity.ok(cursoService.agregarProfesor(cursoId, profesorId));
    }

    @DeleteMapping("/{cursoId}/profesores/{profesorId}")
    public ResponseEntity<CursoDTO> quitarProfesor(
            @PathVariable Long cursoId,
            @PathVariable Long profesorId) {
        return ResponseEntity.ok(cursoService.quitarProfesor(cursoId, profesorId));
    }

    @PostMapping("/{cursoId}/usuarios/{usuarioId}/profesor")
    public ResponseEntity<CursoDTO> agregarProfesorPorUsuario(
            @PathVariable Long cursoId,
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(cursoService.agregarProfesorPorUsuario(cursoId, usuarioId));
    }

    @DeleteMapping("/{cursoId}/usuarios/{usuarioId}/profesor")
    public ResponseEntity<CursoDTO> quitarProfesorPorUsuario(
            @PathVariable Long cursoId,
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(cursoService.quitarProfesorPorUsuario(cursoId, usuarioId));
    }

    @PostMapping("/{cursoId}/grupos")
    public ResponseEntity<GrupoDTO> crearGrupo(
            @PathVariable Long cursoId,
            @RequestParam String titulo) {
        GrupoDTO grupo = cursoService.crearGrupo(cursoId, titulo);
        return ResponseEntity.status(HttpStatus.CREATED).body(grupo);
    }

    @GetMapping("/grupos")
    public ResponseEntity<List<GrupoDTO>> listarTodosGrupos() {
        return ResponseEntity.ok(cursoService.listarTodosGrupos());
    }

    @PostMapping("/grupos")
    public ResponseEntity<GrupoDTO> crearGrupoConCursos(@Valid @RequestBody GuardarGrupoDTO dto) {
        GrupoDTO grupo = cursoService.crearGrupoConCursos(dto.getTitulo(), dto.getCursoIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(grupo);
    }

    @GetMapping("/{cursoId}/grupos")
    public ResponseEntity<List<GrupoDTO>> listarGrupos(@PathVariable Long cursoId) {
        return ResponseEntity.ok(cursoService.listarGrupos(cursoId));
    }

    @PutMapping("/grupos/{grupoId}")
    public ResponseEntity<GrupoDTO> actualizarGrupo(
            @PathVariable Long grupoId,
            @RequestParam String titulo) {
        return ResponseEntity.ok(cursoService.actualizarGrupo(grupoId, titulo));
    }

    @PutMapping("/grupos/{grupoId}/cursos")
    public ResponseEntity<GrupoDTO> actualizarGrupoConCursos(
            @PathVariable Long grupoId,
            @Valid @RequestBody GuardarGrupoDTO dto) {
        return ResponseEntity.ok(cursoService.actualizarGrupoConCursos(grupoId, dto.getTitulo(), dto.getCursoIds()));
    }

    @DeleteMapping("/grupos/{grupoId}")
    public ResponseEntity<Void> eliminarGrupo(@PathVariable Long grupoId) {
        cursoService.eliminarGrupo(grupoId);
        return ResponseEntity.noContent().build();
    }
}
