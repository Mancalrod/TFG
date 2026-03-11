package com.tfg.gestionentregables.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Endpoints públicos")
    class EndpointsPublicos {

        @Test
        @DisplayName("GET /api/health es accesible sin autenticación")
        void health_publico() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /api/auth/login es accesible sin autenticación")
        void login_publico() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"correoElectronico\":\"test@test.com\",\"contrasena\":\"pass\"}"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("GET /api/onedrive/enabled es accesible sin autenticación")
        void onedriveEnabled_publico() throws Exception {
            mockMvc.perform(get("/api/onedrive/enabled"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Endpoints protegidos - sin JWT no se obtiene acceso autenticado")
    class EndpointsProtegidos {

        @Test
        @DisplayName("GET /api/admin/algo requiere autenticación (no 200)")
        void admin_requiereAuth() throws Exception {
            mockMvc.perform(get("/api/admin/algo"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("CORS y CSRF deshabilitado")
    class CsrfDeshabilitado {

        @Test
        @DisplayName("POST sin token CSRF no da 403 por CSRF")
        void post_sinCsrf_noForbidden() throws Exception {
            int statusCode = mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"correoElectronico\":\"a@b.com\",\"contrasena\":\"x\"}"))
                    .andReturn().getResponse().getStatus();
            // CSRF está deshabilitado, así que no debería ser 403 por CSRF
            org.assertj.core.api.Assertions.assertThat(statusCode).isNotEqualTo(403);
        }
    }
}
