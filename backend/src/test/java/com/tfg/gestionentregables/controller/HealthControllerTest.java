package com.tfg.gestionentregables.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/health - Devuelve estado UP")
    void healthCheck_ok() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("AcademicFlow"));
    }

    @Test
    @DisplayName("GET /api/public/info - Devuelve info pública")
    void publicInfo_ok() throws Exception {
        mockMvc.perform(get("/api/public/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.documentation").exists());
    }

    @Test
    @DisplayName("GET /api/health/liveness - Devuelve estado UP")
    void liveness_ok() throws Exception {
        mockMvc.perform(get("/api/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.type").value("LIVENESS"));
    }

    @Test
    @DisplayName("GET /api/health/readiness - Incluye estado de BD")
    void readiness_ok() throws Exception {
        mockMvc.perform(get("/api/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.type").value("READINESS"))
                .andExpect(jsonPath("$.database").value("UP"));
    }
}
