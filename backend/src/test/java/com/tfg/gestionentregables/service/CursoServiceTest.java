package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock private CursoRepository cursoRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private EntityMapper mapper;

    @InjectMocks
    private CursoService cursoService;

    private Curso curso;
    private CursoDTO cursoDTO;
    private CrearCursoDTO crearCursoDTO;
    private Profesor profesor;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Prof").correoElectronico("prof@test.com")
                .contrasena("pass").build();

        curso = Curso.builder().id(1L).titulo("IS").descripcion("Desc").codigo("IS-001").build();

        profesor = Profesor.builder().id(1L).usuario(usuario).curso(curso).build();

        cursoDTO = CursoDTO.builder().id(1L).titulo("IS").codigo("IS-001")
                .grupos(List.of()).numeroActividades(0).numeroProfesores(1).numeroEstudiantes(0).build();

        crearCursoDTO = CrearCursoDTO.builder().titulo("IS").descripcion("Desc").codigo("IS-001").build();
    }

    @Nested
    @DisplayName("crearCurso")
    class CrearCurso {

        @Test
        @DisplayName("Crea curso correctamente")
        void crear_ok() {
            when(profesorRepository.findById(1L)).thenReturn(Optional.of(profesor));
            when(cursoRepository.existsByCodigo("IS-001")).thenReturn(false);
            when(cursoRepository.save(any(Curso.class))).thenReturn(curso);
            when(mapper.toDTO(any(Curso.class))).thenReturn(cursoDTO);

            CursoDTO result = cursoService.crearCurso(crearCursoDTO, 1L);

            assertThat(result.getTitulo()).isEqualTo("IS");
            verify(cursoRepository).save(any(Curso.class));
        }

        @Test
        @DisplayName("Lanza excepción si profesor no existe")
        void crear_profesorNoExiste() {
            when(profesorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.crearCurso(crearCursoDTO, 99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza excepción si código duplicado")
        void crear_codigoDuplicado() {
            when(profesorRepository.findById(1L)).thenReturn(Optional.of(profesor));
            when(cursoRepository.existsByCodigo("IS-001")).thenReturn(true);

            assertThatThrownBy(() -> cursoService.crearCurso(crearCursoDTO, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya existe un curso con ese código");
        }
    }

    @Nested
    @DisplayName("crearCursoPorUsuario")
    class CrearCursoPorUsuario {

        @Test
        @DisplayName("Crea curso y profesor asociados al usuario")
        void crearPorUsuario_ok() {
            Curso cursoPersistido = Curso.builder().id(55L).titulo("IS").descripcion("Desc").codigo("IS-001").build();

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(cursoRepository.existsByCodigo("IS-001")).thenReturn(false);
            when(cursoRepository.save(any(Curso.class))).thenReturn(cursoPersistido);
            when(cursoRepository.findById(55L)).thenReturn(Optional.of(curso));
            when(mapper.toDTO(curso)).thenReturn(cursoDTO);

            CursoDTO result = cursoService.crearCursoPorUsuario(crearCursoDTO, 1L);

            assertThat(result).isNotNull();
            verify(profesorRepository).save(any(Profesor.class));
        }

        @Test
        @DisplayName("Lanza excepción si usuario no existe")
        void crearPorUsuario_usuarioNoExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.crearCursoPorUsuario(crearCursoDTO, 99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    @Nested
    @DisplayName("listarCursosProfesor")
    class ListarCursosProfesor {

        @Test
        @DisplayName("Lista cursos del profesor")
        void listar_ok() {
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(true);
            when(cursoRepository.findByProfesorUsuarioId(1L)).thenReturn(List.of(curso));
            when(mapper.toDTO(curso)).thenReturn(cursoDTO);

            List<CursoDTO> result = cursoService.listarCursosProfesor(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si profesor no existe")
        void listar_noExiste() {
            when(profesorRepository.existsByUsuarioId(99L)).thenReturn(false);

            assertThatThrownBy(() -> cursoService.listarCursosProfesor(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarCursosEstudiante")
    class ListarCursosEstudiante {

        @Test
        @DisplayName("Lista cursos del estudiante")
        void listar_ok() {
            when(cursoRepository.findByEstudianteUsuarioId(1L)).thenReturn(List.of(curso));
            when(mapper.toDTO(curso)).thenReturn(cursoDTO);

            List<CursoDTO> result = cursoService.listarCursosEstudiante(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Devuelve lista vacía si no tiene cursos")
        void listar_sinCursos() {
            when(cursoRepository.findByEstudianteUsuarioId(1L)).thenReturn(List.of());

            List<CursoDTO> result = cursoService.listarCursosEstudiante(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("obtenerCursoPorId")
    class ObtenerPorId {

        @Test
        @DisplayName("Obtiene curso existente")
        void obtener_ok() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(mapper.toDTO(curso)).thenReturn(cursoDTO);

            CursoDTO result = cursoService.obtenerCursoPorId(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void obtener_noExiste() {
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.obtenerCursoPorId(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("obtenerCursoPorCodigo")
    class ObtenerPorCodigo {

        @Test
        @DisplayName("Obtiene curso por código")
        void obtener_ok() {
            when(cursoRepository.findByCodigo("IS-001")).thenReturn(Optional.of(curso));
            when(mapper.toDTO(curso)).thenReturn(cursoDTO);

            CursoDTO result = cursoService.obtenerCursoPorCodigo("IS-001");

            assertThat(result.getCodigo()).isEqualTo("IS-001");
        }

        @Test
        @DisplayName("Lanza excepción si código no existe")
        void obtener_noExiste() {
            when(cursoRepository.findByCodigo("XXX")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.obtenerCursoPorCodigo("XXX"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarTodosCursos")
    class ListarTodos {

        @Test
        @DisplayName("Lista todos los cursos")
        void listarTodos_ok() {
            when(cursoRepository.findAll()).thenReturn(List.of(curso));
            when(mapper.toDTO(curso)).thenReturn(cursoDTO);

            List<CursoDTO> result = cursoService.listarTodosCursos();

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("actualizarCurso")
    class ActualizarCurso {

        @Test
        @DisplayName("Actualiza curso correctamente")
        void actualizar_ok() {
            CrearCursoDTO updateDTO = CrearCursoDTO.builder()
                    .titulo("IS Actualizado").codigo("IS-001").build();

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(cursoRepository.save(any(Curso.class))).thenReturn(curso);
            when(mapper.toDTO(any(Curso.class))).thenReturn(cursoDTO);

            CursoDTO result = cursoService.actualizarCurso(1L, updateDTO);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Lanza excepción si código duplicado en otro curso")
        void actualizar_codigoDuplicado() {
            CrearCursoDTO updateDTO = CrearCursoDTO.builder()
                    .titulo("IS").codigo("OTRO-CODE").build();

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(cursoRepository.existsByCodigo("OTRO-CODE")).thenReturn(true);

            assertThatThrownBy(() -> cursoService.actualizarCurso(1L, updateDTO))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("No lanza excepción si código es el mismo")
        void actualizar_mismoCodigo() {
            CrearCursoDTO updateDTO = CrearCursoDTO.builder()
                    .titulo("IS Actualizado").codigo("IS-001").build();

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(cursoRepository.save(any(Curso.class))).thenReturn(curso);
            when(mapper.toDTO(any(Curso.class))).thenReturn(cursoDTO);

            assertThatNoException().isThrownBy(() -> cursoService.actualizarCurso(1L, updateDTO));
        }

        @Test
        @DisplayName("Lanza excepción si curso no existe")
        void actualizar_noExiste() {
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.actualizarCurso(99L, crearCursoDTO))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("eliminarCurso")
    class EliminarCurso {

        @Test
        @DisplayName("Elimina curso existente")
        void eliminar_ok() {
            when(cursoRepository.existsById(1L)).thenReturn(true);

            cursoService.eliminarCurso(1L);

            verify(cursoRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void eliminar_noExiste() {
            when(cursoRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> cursoService.eliminarCurso(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("agregarProfesor")
    class AgregarProfesor {

        @Test
        @DisplayName("Agrega profesor a curso")
        void agregar_ok() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(profesorRepository.findById(1L)).thenReturn(Optional.of(profesor));
            when(cursoRepository.save(any(Curso.class))).thenReturn(curso);
            when(mapper.toDTO(any(Curso.class))).thenReturn(cursoDTO);

            CursoDTO result = cursoService.agregarProfesor(1L, 1L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Lanza excepción si curso no existe")
        void agregar_cursoNoExiste() {
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.agregarProfesor(99L, 1L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza excepción si profesor no existe")
        void agregar_profesorNoExiste() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(profesorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.agregarProfesor(1L, 99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Bloquea si el usuario ya es estudiante del mismo curso")
        void agregar_bloqueaSiEsEstudianteDelMismoCurso() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(profesorRepository.findById(1L)).thenReturn(Optional.of(profesor));
            when(estudianteRepository.existsByUsuarioIdAndGrupoCursoId(1L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> cursoService.agregarProfesor(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede ser profesor y estudiante del mismo curso");
        }
    }

    @Nested
    @DisplayName("quitarProfesor")
    class QuitarProfesor {

        @Test
        @DisplayName("Quita profesor del curso")
        void quitar_ok() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(profesorRepository.findById(1L)).thenReturn(Optional.of(profesor));
            when(cursoRepository.save(any(Curso.class))).thenReturn(curso);
            when(mapper.toDTO(any(Curso.class))).thenReturn(cursoDTO);

            CursoDTO result = cursoService.quitarProfesor(1L, 1L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Lanza excepción si curso no existe")
        void quitar_cursoNoExiste() {
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.quitarProfesor(99L, 1L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza excepción si profesor no existe")
        void quitar_profesorNoExiste() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(profesorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.quitarProfesor(1L, 99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("crearGrupo")
    class CrearGrupo {

        @Test
        @DisplayName("Crea grupo en curso")
        void crear_ok() {
            Grupo grupo = Grupo.builder().id(1L).titulo("Grupo A").curso(curso).build();
            GrupoDTO grupoDTO = GrupoDTO.builder().id(1L).titulo("Grupo A").cursoId(1L).numeroEstudiantes(0).build();

            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(grupoRepository.save(any(Grupo.class))).thenReturn(grupo);
            when(mapper.toDTO(any(Grupo.class))).thenReturn(grupoDTO);

            GrupoDTO result = cursoService.crearGrupo(1L, "Grupo A");

            assertThat(result.getTitulo()).isEqualTo("Grupo A");
        }

        @Test
        @DisplayName("Lanza excepción si curso no existe")
        void crear_cursoNoExiste() {
            when(cursoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.crearGrupo(99L, "G"))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarGrupos")
    class ListarGrupos {

        @Test
        @DisplayName("Lista grupos del curso")
        void listar_ok() {
            Grupo grupo = Grupo.builder().id(1L).titulo("G1").curso(curso).build();
            GrupoDTO grupoDTO = GrupoDTO.builder().id(1L).titulo("G1").build();

            when(cursoRepository.existsById(1L)).thenReturn(true);
            when(grupoRepository.findByCursoRelacionadoId(1L)).thenReturn(List.of());
            when(grupoRepository.findByCursoId(1L)).thenReturn(List.of(grupo));
            when(mapper.toDTO(grupo)).thenReturn(grupoDTO);

            List<GrupoDTO> result = cursoService.listarGrupos(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Usa resultados de relación curso-grupo cuando existen")
        void listar_priorizaCursoRelacionado() {
            Grupo grupo = Grupo.builder().id(2L).titulo("Relacionado").curso(curso).build();
            GrupoDTO grupoDTO = GrupoDTO.builder().id(2L).titulo("Relacionado").build();

            when(cursoRepository.existsById(1L)).thenReturn(true);
            when(grupoRepository.findByCursoRelacionadoId(1L)).thenReturn(List.of(grupo));
            when(mapper.toDTO(grupo)).thenReturn(grupoDTO);

            List<GrupoDTO> result = cursoService.listarGrupos(1L);

            assertThat(result).hasSize(1);
            verify(grupoRepository, never()).findByCursoId(1L);
        }

        @Test
        @DisplayName("Lanza excepción si curso no existe")
        void listar_cursoNoExiste() {
            when(cursoRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> cursoService.listarGrupos(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("actualizarGrupo")
    class ActualizarGrupo {

        @Test
        @DisplayName("Actualiza título del grupo")
        void actualizar_ok() {
            Grupo grupo = Grupo.builder().id(1L).titulo("Viejo").curso(curso).build();
            GrupoDTO grupoDTO = GrupoDTO.builder().id(1L).titulo("Nuevo").cursoId(1L).build();

            when(grupoRepository.findById(1L)).thenReturn(Optional.of(grupo));
            when(grupoRepository.save(any(Grupo.class))).thenReturn(grupo);
            when(mapper.toDTO(grupo)).thenReturn(grupoDTO);

            GrupoDTO result = cursoService.actualizarGrupo(1L, "Nuevo");

            assertThat(result.getTitulo()).isEqualTo("Nuevo");
            verify(grupoRepository).save(argThat(g -> "Nuevo".equals(g.getTitulo())));
        }

        @Test
        @DisplayName("Lanza excepción si grupo no existe")
        void actualizar_noExiste() {
            when(grupoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.actualizarGrupo(99L, "Nuevo"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Grupo no encontrado");
        }
    }

    @Nested
    @DisplayName("eliminarGrupo")
    class EliminarGrupo {

        @Test
        @DisplayName("Elimina grupo existente")
        void eliminar_ok() {
            when(grupoRepository.existsById(1L)).thenReturn(true);

            cursoService.eliminarGrupo(1L);

            verify(grupoRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Lanza excepción si grupo no existe")
        void eliminar_noExiste() {
            when(grupoRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> cursoService.eliminarGrupo(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Grupo no encontrado");
        }
    }

    @Nested
    @DisplayName("agregarProfesorPorUsuario")
    class AgregarProfesorPorUsuario {

        @Test
        @DisplayName("Agrega profesor por usuario cuando no existe relación")
        void agregarPorUsuario_ok() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioIdAndCursoId(1L, 1L)).thenReturn(false);
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(mapper.toDTO(curso)).thenReturn(cursoDTO);

            CursoDTO result = cursoService.agregarProfesorPorUsuario(1L, 1L);

            assertThat(result).isNotNull();
            verify(profesorRepository).save(any(Profesor.class));
        }

        @Test
        @DisplayName("Falla si usuario ya es profesor en ese curso")
        void agregarPorUsuario_duplicado() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioIdAndCursoId(1L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> cursoService.agregarProfesorPorUsuario(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya está asignado como profesor");
        }
    }

    @Nested
    @DisplayName("quitarProfesorPorUsuario")
    class QuitarProfesorPorUsuario {

        @Test
        @DisplayName("Elimina relación y limpia filas residuales cuando aplica")
        void quitarPorUsuario_okYLimpiaResiduales() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(profesorRepository.findByUsuarioIdAndCursoId(1L, 1L)).thenReturn(Optional.of(profesor));
            when(profesorRepository.countByUsuarioIdAndCursoIsNotNull(1L)).thenReturn(0L);
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(mapper.toDTO(curso)).thenReturn(cursoDTO);

            CursoDTO result = cursoService.quitarProfesorPorUsuario(1L, 1L);

            assertThat(result).isNotNull();
            verify(profesorRepository).delete(profesor);
            verify(profesorRepository).deleteByUsuarioId(1L);
        }

        @Test
        @DisplayName("Falla si no existe relación profesor-usuario en curso")
        void quitarPorUsuario_noExisteRelacion() {
            when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
            when(profesorRepository.findByUsuarioIdAndCursoId(1L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.quitarProfesorPorUsuario(1L, 1L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("no está asignado como profesor");
        }
    }
}
