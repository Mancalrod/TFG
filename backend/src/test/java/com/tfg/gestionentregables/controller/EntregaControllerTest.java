package com.tfg.gestionentregables.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.EntregaService;
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

@WebMvcTest(EntregaController.class)
@AutoConfigureMockMvc(addFilters = false)
class EntregaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EntregaService entregaService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private EntregaDTO entregaDTO;
    private EntregaResumenDTO entregaResumenDTO;
    private EntregaEstadisticasDTO estadisticasDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        entregaDTO = EntregaDTO.builder()
                .id(1L).nombre("Mi entrega").version(1)
                .fechaEntrega(LocalDateTime.now())
                .estado(EstadoEntrega.ENTREGADO).esVersionActiva(true)
                .entregableId(1L).entregableTitulo("E1")
                .estudianteId(1L).estudianteNombre("Alumno")
                .archivos(List.of()).feedbacks(List.of()).build();

        entregaResumenDTO = EntregaResumenDTO.builder()
                .entregaId(1L).estudianteId(1L).estudianteNombre("Alumno")
                .estado(EstadoEntrega.ENTREGADO).version(1).build();

        estadisticasDTO = EntregaEstadisticasDTO.builder()
                .entregableId(1L).totalEntregas(5L).entregasATiempo(4L)
                .entregasTardias(1L).entregasCalificadas(3L).entregasPendientes(2L)
                .promedioCalificacion(7.5).build();
    }

    @Nested
    @DisplayName("GET /api/entregas/{id}")
    class ObtenerEntrega {

        @Test
        @DisplayName("200 - Obtiene entrega")
        void obtener_ok() throws Exception {
            when(entregaService.obtenerEntrega(1L)).thenReturn(entregaDTO);

            mockMvc.perform(get("/api/entregas/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Mi entrega"));
        }

        @Test
        @DisplayName("404 - No encontrada")
        void obtener_notFound() throws Exception {
            when(entregaService.obtenerEntrega(99L))
                    .thenThrow(new EntityNotFoundException("No encontrada"));

            mockMvc.perform(get("/api/entregas/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/entregas/entregable/{entregableId}/estudiante/{estudianteId}")
    class RealizarEntrega {

        @Test
        @DisplayName("201 - Realiza entrega")
        void realizar_ok() throws Exception {
            when(entregaService.realizarEntrega(eq(1L), eq(1L), eq("Mi entrega"), any()))
                    .thenReturn(entregaDTO);

            mockMvc.perform(post("/api/entregas/entregable/1/estudiante/1")
                            .param("nombre", "Mi entrega"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("Mi entrega"));
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/entregable/{entregableId}")
    class ListarParaEvaluar {

        @Test
        @DisplayName("200 - Lista entregas para evaluar")
        void listar_ok() throws Exception {
            when(entregaService.listarEntregasParaEvaluar(1L)).thenReturn(List.of(entregaResumenDTO));

            mockMvc.perform(get("/api/entregas/entregable/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/entregable/{entregableId}/estudiante/{estudianteId}")
    class ListarHistorial {

        @Test
        @DisplayName("200 - Lista historial de versiones")
        void listar_ok() throws Exception {
            when(entregaService.listarEntregasEstudiante(1L, 1L)).thenReturn(List.of(entregaDTO));

            mockMvc.perform(get("/api/entregas/entregable/1/estudiante/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/entregas/{id}/calificar")
    class CalificarEntrega {

        @Test
        @DisplayName("200 - Califica entrega")
        void calificar_ok() throws Exception {
            CalificacionDTO cal = CalificacionDTO.builder().nota(8.5).build();
            when(entregaService.calificarEntrega(eq(1L), any())).thenReturn(entregaDTO);

            mockMvc.perform(post("/api/entregas/1/calificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cal)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("400 - Nota supera máxima")
        void calificar_notaInvalida() throws Exception {
            CalificacionDTO cal = CalificacionDTO.builder().nota(15.0).build();
            when(entregaService.calificarEntrega(eq(1L), any()))
                    .thenThrow(new IllegalArgumentException("La calificación no puede ser mayor"));

            mockMvc.perform(post("/api/entregas/1/calificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cal)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/estudiante/{estudianteId}")
    class ListarTodasEstudiante {

        @Test
        @DisplayName("200 - Lista todas entregas del estudiante")
        void listar_ok() throws Exception {
            when(entregaService.listarTodasEntregasEstudiante(1L)).thenReturn(List.of(entregaDTO));

            mockMvc.perform(get("/api/entregas/estudiante/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/profesor/{profesorId}/pendientes")
    class ListarPendientes {

        @Test
        @DisplayName("200 - Lista pendientes de calificar")
        void listar_ok() throws Exception {
            when(entregaService.listarEntregasPendientesCalificar(1L))
                    .thenReturn(List.of(entregaResumenDTO));

            mockMvc.perform(get("/api/entregas/profesor/1/pendientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/entregas/entregable/{entregableId}/estadisticas")
    class Estadisticas {

        @Test
        @DisplayName("200 - Obtiene estadísticas")
        void obtener_ok() throws Exception {
            when(entregaService.obtenerEstadisticas(1L)).thenReturn(estadisticasDTO);

            mockMvc.perform(get("/api/entregas/entregable/1/estadisticas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEntregas").value(5));
        }
    }

    @Nested
    @DisplayName("DELETE /api/entregas/{id}")
    class EliminarEntrega {

        @Test
        @DisplayName("204 - Elimina entrega")
        void eliminar_ok() throws Exception {
            doNothing().when(entregaService).eliminarEntrega(1L);

            mockMvc.perform(delete("/api/entregas/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("409 - Entrega calificada no se puede eliminar")
        void eliminar_calificada() throws Exception {
            doThrow(new IllegalStateException("No se puede eliminar una entrega ya calificada"))
                    .when(entregaService).eliminarEntrega(1L);

            mockMvc.perform(delete("/api/entregas/1"))
                    .andExpect(status().isConflict());
        }
    }
}
