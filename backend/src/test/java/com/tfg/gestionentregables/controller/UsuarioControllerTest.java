package com.tfg.gestionentregables.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.SecurityContextUserService;
import com.tfg.gestionentregables.service.UsuarioService;
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

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private SecurityContextUserService securityContextUserService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private UsuarioDTO usuarioDTO;
    private CrearUsuarioDTO crearUsuarioDTO;
    private ActualizarUsuarioDTO actualizarUsuarioDTO;
    private GrupoDTO grupoDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        usuarioDTO = UsuarioDTO.builder()
                .id(1L).nombre("Juan").correoElectronico("juan@test.com")
                .esAdmin(false).build();

        crearUsuarioDTO = CrearUsuarioDTO.builder()
                .nombre("Juan").correoElectronico("juan@test.com")
                .contrasena("password123").build();

        actualizarUsuarioDTO = ActualizarUsuarioDTO.builder()
            .nombre("Juan").correoElectronico("juan@test.com")
            .contrasena("password123").build();

        grupoDTO = GrupoDTO.builder()
            .id(10L)
            .titulo("G1")
            .numeroEstudiantes(5)
            .build();
    }

    @Nested
    @DisplayName("GET /api/usuarios")
    class ListarUsuarios {

        @Test
        @DisplayName("200 - Lista todos los usuarios")
        void listar_ok() throws Exception {
            when(usuarioService.listarUsuarios()).thenReturn(List.of(usuarioDTO));

            mockMvc.perform(get("/api/usuarios"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].nombre").value("Juan"));
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/{id}")
    class ObtenerUsuario {

        @Test
        @DisplayName("200 - Obtiene usuario")
        void obtener_ok() throws Exception {
            when(usuarioService.obtenerUsuarioPorId(1L)).thenReturn(usuarioDTO);

            mockMvc.perform(get("/api/usuarios/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Juan"));
        }

        @Test
        @DisplayName("404 - Usuario no encontrado")
        void obtener_notFound() throws Exception {
            when(usuarioService.obtenerUsuarioPorId(99L))
                    .thenThrow(new EntityNotFoundException("No encontrado"));

            mockMvc.perform(get("/api/usuarios/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/correo/{correo}")
    class ObtenerPorCorreo {

        @Test
        @DisplayName("200 - Obtiene usuario por correo")
        void obtener_ok() throws Exception {
            when(usuarioService.obtenerUsuarioPorCorreo("juan@test.com")).thenReturn(usuarioDTO);

            mockMvc.perform(get("/api/usuarios/correo/juan@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correoElectronico").value("juan@test.com"));
        }
    }

    @Nested
    @DisplayName("POST /api/usuarios")
    class CrearUsuario {

        @Test
        @DisplayName("201 - Crea usuario")
        void crear_ok() throws Exception {
            when(usuarioService.crearUsuario(any())).thenReturn(usuarioDTO);

            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearUsuarioDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("Juan"));
        }

        @Test
        @DisplayName("400 - Correo duplicado")
        void crear_correoDuplicado() throws Exception {
            when(usuarioService.crearUsuario(any()))
                    .thenThrow(new IllegalArgumentException("Ya existe un usuario con ese correo"));

            mockMvc.perform(post("/api/usuarios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearUsuarioDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/usuarios/{id}")
    class ActualizarUsuario {

        @Test
        @DisplayName("200 - Actualiza usuario")
        void actualizar_ok() throws Exception {
            when(usuarioService.actualizarUsuario(eq(1L), any())).thenReturn(usuarioDTO);

            mockMvc.perform(put("/api/usuarios/1")
                            .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizarUsuarioDTO)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/usuarios/{id}")
    class EliminarUsuario {

        @Test
        @DisplayName("204 - Elimina usuario")
        void eliminar_ok() throws Exception {
            doNothing().when(usuarioService).eliminarUsuario(1L);

            mockMvc.perform(delete("/api/usuarios/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/usuarios/{id}/profesor")
    class RegistrarProfesor {

        @Test
        @DisplayName("201 - Registra como profesor")
        void registrar_ok() throws Exception {
            doNothing().when(usuarioService).registrarComoProfesor(1L, 3L);

            mockMvc.perform(post("/api/usuarios/1/profesor")
                    .param("cursoId", "3"))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("409 - Ya es profesor")
        void registrar_yaEsProfesor() throws Exception {
            doThrow(new IllegalStateException("Ya está registrado como profesor"))
                .when(usuarioService).registrarComoProfesor(1L, 3L);

            mockMvc.perform(post("/api/usuarios/1/profesor")
                    .param("cursoId", "3"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/usuarios/{id}/estudiante/{grupoId}")
    class RegistrarEstudiante {

        @Test
        @DisplayName("201 - Registra como estudiante")
        void registrar_ok() throws Exception {
            doNothing().when(usuarioService).registrarComoEstudiante(1L, 1L);

            mockMvc.perform(post("/api/usuarios/1/estudiante/1"))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/{id}/es-profesor")
    class EsProfesor {

        @Test
        @DisplayName("200 - Es profesor")
        void esProfesor_true() throws Exception {
            when(usuarioService.esProfesor(1L)).thenReturn(true);

            mockMvc.perform(get("/api/usuarios/1/es-profesor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
        }

        @Test
        @DisplayName("200 - No es profesor")
        void esProfesor_false() throws Exception {
            when(usuarioService.esProfesor(1L)).thenReturn(false);

            mockMvc.perform(get("/api/usuarios/1/es-profesor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/{id}/es-estudiante")
    class EsEstudiante {

        @Test
        @DisplayName("200 - Es estudiante")
        void esEstudiante_true() throws Exception {
            when(usuarioService.esEstudiante(1L)).thenReturn(true);

            mockMvc.perform(get("/api/usuarios/1/es-estudiante"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
        }

        @Test
        @DisplayName("200 - No es estudiante")
        void esEstudiante_false() throws Exception {
            when(usuarioService.esEstudiante(1L)).thenReturn(false);

            mockMvc.perform(get("/api/usuarios/1/es-estudiante"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/usuarios/{id}/profesor")
    class EliminarRolProfesor {

        @Test
        @DisplayName("204 - Elimina rol profesor")
        void eliminar_ok() throws Exception {
            doNothing().when(usuarioService).eliminarRolProfesor(1L);

            mockMvc.perform(delete("/api/usuarios/1/profesor"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("DELETE /api/usuarios/{id}/estudiante")
    class EliminarRolEstudiante {

        @Test
        @DisplayName("204 - Elimina rol estudiante")
        void eliminar_ok() throws Exception {
            doNothing().when(usuarioService).eliminarRolEstudiante(1L);

            mockMvc.perform(delete("/api/usuarios/1/estudiante"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("DELETE /api/usuarios/{id}/estudiante/{grupoId}")
    class EliminarEstudianteDeGrupo {

        @Test
        @DisplayName("204 - Elimina estudiante del grupo")
        void eliminar_ok() throws Exception {
            doNothing().when(usuarioService).eliminarEstudianteDeGrupo(1L, 10L);

            mockMvc.perform(delete("/api/usuarios/1/estudiante/10"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/grupo/{grupoId}")
    class ListarEstudiantesDeGrupo {

        @Test
        @DisplayName("200 - Lista estudiantes del grupo")
        void listar_ok() throws Exception {
            when(usuarioService.listarEstudiantesDeGrupo(3L)).thenReturn(List.of(usuarioDTO));

            mockMvc.perform(get("/api/usuarios/grupo/3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/{id}/grupos")
    class ListarGruposDeEstudiante {

        @Test
        @DisplayName("200 - Lista grupos del estudiante")
        void listar_ok() throws Exception {
            when(usuarioService.listarGruposDeEstudiante(1L)).thenReturn(List.of(grupoDTO));

            mockMvc.perform(get("/api/usuarios/1/grupos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(10));
        }
    }

    @Nested
    @DisplayName("GET /api/usuarios/{id}/profesor-id")
    class ObtenerProfesorId {

        @Test
        @DisplayName("200 - Obtiene id de profesor")
        void obtener_ok() throws Exception {
            when(usuarioService.obtenerProfesorId(1L)).thenReturn(42L);

            mockMvc.perform(get("/api/usuarios/1/profesor-id"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("42"));
        }
    }
}
