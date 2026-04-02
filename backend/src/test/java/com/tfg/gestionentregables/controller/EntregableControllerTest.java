package com.tfg.gestionentregables.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.EntregableService;
import com.tfg.gestionentregables.service.SecurityContextUserService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EntregableController.class)
@AutoConfigureMockMvc(addFilters = false)
class EntregableControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EntregableService entregableService;
    @MockitoBean private SecurityContextUserService securityContextUserService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private EntregableDTO entregableDTO;
    private CrearEntregableDTO crearEntregableDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        when(securityContextUserService.getCurrentUserId(any())).thenReturn(1L);
        when(securityContextUserService.hasRole(any(), eq("ADMIN"))).thenReturn(true);
        when(securityContextUserService.hasRole(any(), eq("ESTUDIANTE"))).thenReturn(false);

        entregableDTO = EntregableDTO.builder()
                .id(1L).titulo("Entregable 1").descripcion("Desc")
                .tipoArchivoEsperado(TipoMaterial.PDF)
                .visibilidad(Visibilidad.VISIBLE).permiteReenvio(true)
                .actividadId(1L).actividadTitulo("PR1")
                .numeroEntregas(0L).enPlazo(true).build();

        crearEntregableDTO = CrearEntregableDTO.builder()
                .titulo("Entregable 1").descripcion("Desc")
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .tipoArchivoEsperado(TipoMaterial.PDF)
                .visibilidad(Visibilidad.VISIBLE)
                .build();
    }

    @Nested
    @DisplayName("GET /api/entregables/{id}")
    class ObtenerEntregable {

        @Test
        @DisplayName("200 - Obtiene entregable")
        void obtener_ok() throws Exception {
            when(entregableService.obtenerEntregable(1L)).thenReturn(entregableDTO);

            mockMvc.perform(get("/api/entregables/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Entregable 1"));
        }

        @Test
        @DisplayName("404 - No encontrado")
        void obtener_notFound() throws Exception {
            when(entregableService.obtenerEntregable(99L))
                    .thenThrow(new EntityNotFoundException("No encontrado"));

            mockMvc.perform(get("/api/entregables/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/entregables/actividad/{actividadId}")
    class CrearEntregable {

        @Test
        @DisplayName("201 - Crea entregable")
        void crear_ok() throws Exception {
            when(entregableService.crearEntregable(any(), eq(1L), anyLong(), anyBoolean())).thenReturn(entregableDTO);

            mockMvc.perform(post("/api/entregables/actividad/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearEntregableDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.titulo").value("Entregable 1"));
        }

                @Test
                @DisplayName("403 - Acceso denegado al crear entregable")
                void crear_forbidden() throws Exception {
                    when(entregableService.crearEntregable(any(), eq(1L), anyLong(), anyBoolean()))
                        .thenThrow(new AccessDeniedException("No tienes permisos sobre este curso"));

                    mockMvc.perform(post("/api/entregables/actividad/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearEntregableDTO)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.message").value("No tienes permisos sobre este curso"));
                }
    }

    @Nested
    @DisplayName("GET /api/entregables/actividad/{actividadId}")
    class ListarEntregables {

        @Test
        @DisplayName("200 - Lista entregables de actividad")
        void listar_ok() throws Exception {
            when(entregableService.listarEntregablesActividad(1L)).thenReturn(List.of(entregableDTO));

            mockMvc.perform(get("/api/entregables/actividad/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/entregables/actividad/{actividadId}/visibles")
    class ListarVisibles {

        @Test
        @DisplayName("200 - Lista entregables visibles")
        void listar_ok() throws Exception {
            when(entregableService.listarEntregablesVisibles(1L)).thenReturn(List.of(entregableDTO));

            mockMvc.perform(get("/api/entregables/actividad/1/visibles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/entregables/{id}")
    class ActualizarEntregable {

        @Test
        @DisplayName("200 - Actualiza entregable")
        void actualizar_ok() throws Exception {
            when(entregableService.actualizarEntregable(eq(1L), any(), anyLong(), anyBoolean())).thenReturn(entregableDTO);

            mockMvc.perform(put("/api/entregables/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearEntregableDTO)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH /api/entregables/{id}/visibilidad")
    class CambiarVisibilidad {

        @Test
        @DisplayName("200 - Cambia visibilidad")
        void cambiar_ok() throws Exception {
            when(entregableService.cambiarVisibilidad(eq(1L), eq(Visibilidad.OCULTO), anyLong(), anyBoolean()))
                    .thenReturn(entregableDTO);

            mockMvc.perform(patch("/api/entregables/1/visibilidad")
                            .param("visibilidad", "OCULTO"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH /api/entregables/{id}/notas-visibles")
    class CambiarVisibilidadNotas {

        @Test
        @DisplayName("200 - Cambia visibilidad de notas")
        void cambiar_notas_ok() throws Exception {
            when(entregableService.cambiarVisibilidadNotasEstudiante(eq(1L), eq(true), anyLong(), anyBoolean()))
                    .thenReturn(entregableDTO);

            mockMvc.perform(patch("/api/entregables/1/notas-visibles")
                            .param("visible", "true"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("403 - Acceso denegado al cambiar visibilidad de notas")
        void cambiar_notas_forbidden() throws Exception {
            when(entregableService.cambiarVisibilidadNotasEstudiante(eq(1L), eq(false), anyLong(), anyBoolean()))
                    .thenThrow(new AccessDeniedException("No tienes permisos sobre este curso"));

            mockMvc.perform(patch("/api/entregables/1/notas-visibles")
                            .param("visible", "false"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("No tienes permisos sobre este curso"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/entregables/{id}")
    class EliminarEntregable {

        @Test
        @DisplayName("204 - Elimina entregable")
        void eliminar_ok() throws Exception {
            doNothing().when(entregableService).eliminarEntregable(eq(1L), anyLong(), anyBoolean());

            mockMvc.perform(delete("/api/entregables/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 - No encontrado")
        void eliminar_notFound() throws Exception {
            doThrow(new EntityNotFoundException("No encontrado"))
                    .when(entregableService).eliminarEntregable(eq(99L), anyLong(), anyBoolean());

            mockMvc.perform(delete("/api/entregables/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/entregables/actividad/{actividadId}/en-plazo")
    class EnPlazo {

        @Test
        @DisplayName("200 - Lista entregables en plazo")
        void listar_ok() throws Exception {
            when(entregableService.listarEntregablesEnPlazo(1L)).thenReturn(List.of(entregableDTO));

            mockMvc.perform(get("/api/entregables/actividad/1/en-plazo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/entregables/actividad/{actividadId}/proximos")
    class Proximos {

        @Test
        @DisplayName("200 - Lista entregables próximos a vencer")
        void listar_ok() throws Exception {
            when(entregableService.listarEntregablesProximosVencer(1L, 7)).thenReturn(List.of(entregableDTO));

            mockMvc.perform(get("/api/entregables/actividad/1/proximos").param("dias", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/entregables/actividad/{actividadId}/pendientes/{estudianteId}")
    class Pendientes {

        @Test
        @DisplayName("200 - Lista entregables pendientes")
        void listar_ok() throws Exception {
            when(entregableService.listarEntregablesPendientesEstudiante(1L, 1L))
                    .thenReturn(List.of(entregableDTO));

            mockMvc.perform(get("/api/entregables/actividad/1/pendientes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("403 - Estudiante no puede consultar pendientes de otro estudiante")
        void listar_forbidden_estudiante_otro_id() throws Exception {
            when(securityContextUserService.getCurrentUserId(any())).thenReturn(1L);
            when(securityContextUserService.hasRole(any(), eq("ADMIN"))).thenReturn(false);
            when(securityContextUserService.hasRole(any(), eq("ESTUDIANTE"))).thenReturn(true);

            mockMvc.perform(get("/api/entregables/actividad/1/pendientes/2"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("No puedes consultar entregables pendientes de otro estudiante"));

            verify(entregableService, never()).listarEntregablesPendientesEstudiante(anyLong(), anyLong());
        }
    }
}
