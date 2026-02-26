package com.tfg.gestionentregables.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.enums.TipoActividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.ActividadService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActividadController.class)
@AutoConfigureMockMvc(addFilters = false)
class ActividadControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ActividadService actividadService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private ActividadDTO actividadDTO;
    private CrearActividadDTO crearActividadDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        actividadDTO = ActividadDTO.builder()
                .id(1L).titulo("Práctica 1").descripcion("Desc")
                .tipoActividad(TipoActividad.EVALUABLE)
                .visibilidad(Visibilidad.VISIBLE)
                .cursoId(1L).cursoTitulo("IS")
                .grupoIds(List.of(1L)).numeroEntregables(0).build();

        crearActividadDTO = CrearActividadDTO.builder()
                .titulo("Práctica 1").descripcion("Desc")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .visibilidad(Visibilidad.VISIBLE)
                .grupoIds(List.of(1L)).build();
    }

    @Nested
    @DisplayName("GET /api/actividades/{id}")
    class ObtenerActividad {

        @Test
        @DisplayName("200 - Obtiene actividad")
        void obtener_ok() throws Exception {
            when(actividadService.obtenerActividadPorId(1L)).thenReturn(actividadDTO);

            mockMvc.perform(get("/api/actividades/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Práctica 1"));
        }

        @Test
        @DisplayName("404 - Actividad no encontrada")
        void obtener_notFound() throws Exception {
            when(actividadService.obtenerActividadPorId(99L))
                    .thenThrow(new EntityNotFoundException("No encontrada"));

            mockMvc.perform(get("/api/actividades/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/actividades/curso/{cursoId}")
    class CrearActividad {

        @Test
        @DisplayName("201 - Crea actividad")
        void crear_ok() throws Exception {
            when(actividadService.crearActividad(any(), eq(1L))).thenReturn(actividadDTO);

            mockMvc.perform(post("/api/actividades/curso/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearActividadDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.titulo").value("Práctica 1"));
        }
    }

    @Nested
    @DisplayName("GET /api/actividades/curso/{cursoId}")
    class ListarActividades {

        @Test
        @DisplayName("200 - Lista actividades del curso")
        void listar_ok() throws Exception {
            when(actividadService.listarActividadesCurso(1L)).thenReturn(List.of(actividadDTO));

            mockMvc.perform(get("/api/actividades/curso/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/actividades/grupo/{grupoId}")
    class ListarActividadesGrupo {

        @Test
        @DisplayName("200 - Lista actividades del grupo")
        void listar_ok() throws Exception {
            when(actividadService.listarActividadesVisiblesGrupo(1L)).thenReturn(List.of(actividadDTO));

            mockMvc.perform(get("/api/actividades/grupo/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/actividades/{id}/detalle")
    class ObtenerConEntregables {

        @Test
        @DisplayName("200 - Obtiene actividad con entregables")
        void obtener_ok() throws Exception {
            when(actividadService.obtenerActividadConEntregables(1L)).thenReturn(actividadDTO);

            mockMvc.perform(get("/api/actividades/1/detalle"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Práctica 1"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/actividades/{id}/visibilidad")
    class CambiarVisibilidad {

        @Test
        @DisplayName("200 - Cambia visibilidad")
        void cambiar_ok() throws Exception {
            when(actividadService.cambiarVisibilidad(1L, Visibilidad.OCULTO)).thenReturn(actividadDTO);

            mockMvc.perform(patch("/api/actividades/1/visibilidad")
                            .param("visibilidad", "OCULTO"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/actividades/{id}")
    class ActualizarActividad {

        @Test
        @DisplayName("200 - Actualiza actividad")
        void actualizar_ok() throws Exception {
            when(actividadService.actualizarActividad(eq(1L), any())).thenReturn(actividadDTO);

            mockMvc.perform(put("/api/actividades/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearActividadDTO)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/actividades/{id}")
    class EliminarActividad {

        @Test
        @DisplayName("204 - Elimina actividad")
        void eliminar_ok() throws Exception {
            doNothing().when(actividadService).eliminarActividad(1L);

            mockMvc.perform(delete("/api/actividades/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 - Actividad no encontrada")
        void eliminar_notFound() throws Exception {
            doThrow(new EntityNotFoundException("No encontrada"))
                    .when(actividadService).eliminarActividad(99L);

            mockMvc.perform(delete("/api/actividades/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/actividades/curso/{cursoId}/en-plazo")
    class EnPlazo {

        @Test
        @DisplayName("200 - Lista actividades en plazo")
        void listar_ok() throws Exception {
            when(actividadService.listarActividadesEnPlazo(1L)).thenReturn(List.of(actividadDTO));

            mockMvc.perform(get("/api/actividades/curso/1/en-plazo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/actividades/curso/{cursoId}/proximas")
    class Proximas {

        @Test
        @DisplayName("200 - Lista actividades próximas")
        void listar_ok() throws Exception {
            when(actividadService.listarActividadesProximasLimite(1L, 7)).thenReturn(List.of(actividadDTO));

            mockMvc.perform(get("/api/actividades/curso/1/proximas").param("dias", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
