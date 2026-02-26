package com.tfg.gestionentregables.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.FeedbackService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

@WebMvcTest(FeedbackController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeedbackControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private FeedbackService feedbackService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;
    private FeedbackDTO feedbackDTO;
    private CrearFeedbackDTO crearFeedbackDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        feedbackDTO = FeedbackDTO.builder()
                .id(1L).comentario("Buen trabajo")
                .fechaCreacion(LocalDateTime.now())
                .entregaId(1L).profesorId(1L).profesorNombre("Prof. García").build();

        crearFeedbackDTO = CrearFeedbackDTO.builder().comentario("Buen trabajo").build();
    }

    @Nested
    @DisplayName("GET /api/feedback/{id}")
    class ObtenerFeedback {

        @Test
        @DisplayName("200 - Obtiene feedback")
        void obtener_ok() throws Exception {
            when(feedbackService.obtenerFeedback(1L)).thenReturn(feedbackDTO);

            mockMvc.perform(get("/api/feedback/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.comentario").value("Buen trabajo"));
        }

        @Test
        @DisplayName("404 - No encontrado")
        void obtener_notFound() throws Exception {
            when(feedbackService.obtenerFeedback(99L))
                    .thenThrow(new EntityNotFoundException("No encontrado"));

            mockMvc.perform(get("/api/feedback/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/feedback/entrega/{entregaId}/profesor/{profesorId}")
    class CrearFeedback {

        @Test
        @DisplayName("201 - Crea feedback")
        void crear_ok() throws Exception {
            when(feedbackService.crearFeedback(eq(1L), eq(1L), any())).thenReturn(feedbackDTO);

            mockMvc.perform(post("/api/feedback/entrega/1/profesor/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearFeedbackDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.comentario").value("Buen trabajo"));
        }
    }

    @Nested
    @DisplayName("GET /api/feedback/entrega/{entregaId}")
    class ListarFeedbacks {

        @Test
        @DisplayName("200 - Lista feedbacks de la entrega")
        void listar_ok() throws Exception {
            when(feedbackService.listarFeedbacksEntrega(1L)).thenReturn(List.of(feedbackDTO));

            mockMvc.perform(get("/api/feedback/entrega/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/feedback/{id}/profesor/{profesorId}")
    class ActualizarFeedback {

        @Test
        @DisplayName("200 - Actualiza feedback")
        void actualizar_ok() throws Exception {
            when(feedbackService.actualizarFeedback(eq(1L), eq(1L), any())).thenReturn(feedbackDTO);

            mockMvc.perform(put("/api/feedback/1/profesor/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearFeedbackDTO)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("409 - Otro profesor intenta modificar")
        void actualizar_otroProfesor() throws Exception {
            when(feedbackService.actualizarFeedback(eq(1L), eq(99L), any()))
                    .thenThrow(new IllegalStateException("Solo el profesor que creó el feedback"));

            mockMvc.perform(put("/api/feedback/1/profesor/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(crearFeedbackDTO)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/feedback/{id}/profesor/{profesorId}")
    class EliminarFeedback {

        @Test
        @DisplayName("204 - Elimina feedback")
        void eliminar_ok() throws Exception {
            doNothing().when(feedbackService).eliminarFeedback(1L, 1L);

            mockMvc.perform(delete("/api/feedback/1/profesor/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("409 - Otro profesor intenta eliminar")
        void eliminar_otroProfesor() throws Exception {
            doThrow(new IllegalStateException("Solo el profesor que creó"))
                    .when(feedbackService).eliminarFeedback(1L, 99L);

            mockMvc.perform(delete("/api/feedback/1/profesor/99"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/feedback/profesor/{profesorId}")
    class ListarPorProfesor {

        @Test
        @DisplayName("200 - Lista feedbacks del profesor")
        void listar_ok() throws Exception {
            when(feedbackService.listarFeedbacksProfesor(1L)).thenReturn(List.of(feedbackDTO));

            mockMvc.perform(get("/api/feedback/profesor/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/feedback/estudiante/{estudianteId}/recientes")
    class ContarRecientes {

        @Test
        @DisplayName("200 - Cuenta feedbacks recientes")
        void contar_ok() throws Exception {
            when(feedbackService.contarFeedbacksRecientes(eq(1L), any(LocalDateTime.class)))
                    .thenReturn(5L);

            mockMvc.perform(get("/api/feedback/estudiante/1/recientes").param("dias", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(5));
        }
    }
}
