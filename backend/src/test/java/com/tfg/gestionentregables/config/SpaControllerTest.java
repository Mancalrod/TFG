package com.tfg.gestionentregables.config;

import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpaController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /dashboard - Redirige a index.html (ruta nivel 1)")
    void redirectRoot_forwardsToIndex() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @DisplayName("GET /admin/users - Redirige a index.html (ruta nivel 2)")
    void redirectSubPath_forwardsToIndex() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @DisplayName("GET /admin/users/123 - Redirige a index.html (ruta nivel 3)")
    void redirectDeepPath_forwardsToIndex() throws Exception {
        mockMvc.perform(get("/admin/users/123"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }
}
