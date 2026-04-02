package com.tfg.gestionentregables.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfg.gestionentregables.dto.NotificacionDTO;
import com.tfg.gestionentregables.dto.PreferenciaNotificacionDTO;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.NotificacionService;
import com.tfg.gestionentregables.service.SecurityContextUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificacionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private NotificacionService notificacionService;
    @MockitoBean private SecurityContextUserService securityContextUserService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(securityContextUserService.getCurrentUserId(any())).thenReturn(7L);
    }

    @Nested
    @DisplayName("GET /api/notificaciones")
    class Listar {

        @Test
        @DisplayName("200 - Lista notificaciones del usuario autenticado")
        void listar_ok() throws Exception {
            NotificacionDTO dto = NotificacionDTO.builder()
                    .id(10L)
                    .tipo("NUEVO_ENTREGABLE")
                    .titulo("Nuevo entregable")
                    .mensaje("Se publico una nueva tarea")
                    .leida(false)
                    .cursoId(3L)
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            when(notificacionService.obtenerNotificaciones(7L)).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/notificaciones"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].titulo").value("Nuevo entregable"));
        }
    }

    @Test
    @DisplayName("PUT /api/notificaciones/{id}/leida - 200")
    void marcarLeida_ok() throws Exception {
        mockMvc.perform(put("/api/notificaciones/11/leida"))
                .andExpect(status().isOk());

        verify(notificacionService).marcarComoLeida(11L, 7L);
    }

    @Test
    @DisplayName("GET /api/notificaciones/no-leidas/count - 200")
    void contarNoLeidas_ok() throws Exception {
        when(notificacionService.contarNoLeidas(7L)).thenReturn(4L);

        mockMvc.perform(get("/api/notificaciones/no-leidas/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    @DisplayName("GET /api/notificaciones/preferencias - 200")
    void obtenerPreferencias_ok() throws Exception {
        when(notificacionService.obtenerPreferencias(7L)).thenReturn(new PreferenciaNotificacionDTO("APP"));

        mockMvc.perform(get("/api/notificaciones/preferencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canal").value("APP"));
    }

    @Test
    @DisplayName("PUT /api/notificaciones/preferencias - 200")
    void actualizarPreferencias_ok() throws Exception {
        PreferenciaNotificacionDTO request = new PreferenciaNotificacionDTO("EMAIL");
        when(notificacionService.actualizarPreferencias(eq(7L), any(PreferenciaNotificacionDTO.class)))
                .thenReturn(new PreferenciaNotificacionDTO("EMAIL"));

        mockMvc.perform(put("/api/notificaciones/preferencias")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canal").value("EMAIL"));
    }
}