package com.tfg.gestionentregables.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.CursoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CursoController.class)
@AutoConfigureMockMvc(addFilters = false)
class CursoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CursoService cursoService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private CursoDTO cursoDTO;
    private CrearCursoDTO crearCursoDTO;
    private GrupoDTO grupoDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        cursoDTO = CursoDTO.builder()
                .id(1L).titulo("IS").descripcion("Desc")
                .codigo("IS-001").grupos(List.of())
                .numeroActividades(0).numeroProfesores(1).numeroEstudiantes(0).build();

        crearCursoDTO = CrearCursoDTO.builder()
                .titulo("IS").descripcion("Desc").codigo("IS-001").build();

        grupoDTO = GrupoDTO.builder().id(1L).titulo("Grupo A").cursoId(1L).numeroEstudiantes(0).build();
    }

    @Nested
    @DisplayName("GET /api/cursos")
    class ListarTodos {

        @Test
        @DisplayName("200 - Lista todos los cursos")
        void listar_ok() throws Exception {
            when(cursoService.listarTodosCursos()).thenReturn(List.of(cursoDTO));

            mockMvc.perform(get("/api/cursos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].titulo").value("IS"));
        }
    }

    @Nested
    @DisplayName("GET /api/cursos/{id}")
    class ObtenerCurso {

        @Test
        @DisplayName("200 - Obtiene curso")
        void obtener_ok() throws Exception {
            when(cursoService.obtenerCursoPorId(1L)).thenReturn(cursoDTO);

            mockMvc.perform(get("/api/cursos/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.codigo").value("IS-001"));
        }

        @Test
        @DisplayName("404 - Curso no encontrado")
        void obtener_notFound() throws Exception {
            when(cursoService.obtenerCursoPorId(99L))
                    .thenThrow(new EntityNotFoundException("No encontrado"));

            mockMvc.perform(get("/api/cursos/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/cursos/codigo/{codigo}")
    class ObtenerPorCodigo {

        @Test
        @DisplayName("200 - Obtiene curso por código")
        void obtener_ok() throws Exception {
            when(cursoService.obtenerCursoPorCodigo("IS-001")).thenReturn(cursoDTO);

            mockMvc.perform(get("/api/cursos/codigo/IS-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.codigo").value("IS-001"));
        }
    }

    @Nested
    @DisplayName("POST /api/cursos/profesor/{profesorId}")
    class CrearCurso {

        @Test
        @DisplayName("201 - Crea curso")
        void crear_ok() throws Exception {
            when(cursoService.crearCurso(any(), eq(1L))).thenReturn(cursoDTO);

            mockMvc.perform(post("/api/cursos/profesor/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearCursoDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.titulo").value("IS"));
        }
    }

    @Nested
    @DisplayName("GET /api/cursos/profesor/{profesorId}")
    class ListarCursosProfesor {

        @Test
        @DisplayName("200 - Lista cursos del profesor")
        void listar_ok() throws Exception {
            when(cursoService.listarCursosProfesor(1L)).thenReturn(List.of(cursoDTO));

            mockMvc.perform(get("/api/cursos/profesor/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/cursos/estudiante/{estudianteId}")
    class ListarCursosEstudiante {

        @Test
        @DisplayName("200 - Lista cursos del estudiante")
        void listar_ok() throws Exception {
            when(cursoService.listarCursosEstudiante(1L)).thenReturn(List.of(cursoDTO));

            mockMvc.perform(get("/api/cursos/estudiante/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/cursos/{id}")
    class ActualizarCurso {

        @Test
        @DisplayName("200 - Actualiza curso")
        void actualizar_ok() throws Exception {
            when(cursoService.actualizarCurso(eq(1L), any())).thenReturn(cursoDTO);

            mockMvc.perform(put("/api/cursos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearCursoDTO)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/cursos/{id}")
    class EliminarCurso {

        @Test
        @DisplayName("204 - Elimina curso")
        void eliminar_ok() throws Exception {
            doNothing().when(cursoService).eliminarCurso(1L);

            mockMvc.perform(delete("/api/cursos/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/cursos/{cursoId}/profesores/{profesorId}")
    class AgregarProfesor {

        @Test
        @DisplayName("200 - Agrega profesor a curso")
        void agregar_ok() throws Exception {
            when(cursoService.agregarProfesor(1L, 2L)).thenReturn(cursoDTO);

            mockMvc.perform(post("/api/cursos/1/profesores/2"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/cursos/{cursoId}/profesores/{profesorId}")
    class QuitarProfesor {

        @Test
        @DisplayName("200 - Quita profesor del curso")
        void quitar_ok() throws Exception {
            when(cursoService.quitarProfesor(1L, 2L)).thenReturn(cursoDTO);

            mockMvc.perform(delete("/api/cursos/1/profesores/2"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/cursos/{cursoId}/grupos")
    class CrearGrupo {

        @Test
        @DisplayName("201 - Crea grupo en curso")
        void crear_ok() throws Exception {
            when(cursoService.crearGrupo(1L, "Grupo A")).thenReturn(grupoDTO);

            mockMvc.perform(post("/api/cursos/1/grupos").param("titulo", "Grupo A"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.titulo").value("Grupo A"));
        }
    }

    @Nested
    @DisplayName("GET /api/cursos/{cursoId}/grupos")
    class ListarGrupos {

        @Test
        @DisplayName("200 - Lista grupos del curso")
        void listar_ok() throws Exception {
            when(cursoService.listarGrupos(1L)).thenReturn(List.of(grupoDTO));

            mockMvc.perform(get("/api/cursos/1/grupos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
