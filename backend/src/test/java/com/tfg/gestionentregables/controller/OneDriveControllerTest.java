package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.entity.OneDriveToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.OneDriveService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OneDriveController.class)
@AutoConfigureMockMvc(addFilters = false)
class OneDriveControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private OneDriveService oneDriveService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private OneDriveToken token;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().id(1L).nombre("Test User")
                .correoElectronico("test@test.com").contrasena("pass").build();

        token = OneDriveToken.builder()
                .id(1L)
                .usuario(usuario)
                .accessToken("access-token-123")
                .refreshToken("refresh-token-123")
                .expiraEn(LocalDateTime.now().plusHours(1))
                .microsoftEmail("test@microsoft.com")
                .fechaConexion(LocalDateTime.now().minusDays(1))
                .fechaUltimoUso(LocalDateTime.now())
                .build();
    }

    // =============================================
    // GET /api/onedrive/enabled
    // =============================================

    @Nested
    @DisplayName("GET /api/onedrive/enabled")
    class IsEnabled {

        @Test
        @DisplayName("200 - Integración habilitada")
        void enabled_true() throws Exception {
            when(oneDriveService.isEnabled()).thenReturn(true);

            mockMvc.perform(get("/api/onedrive/enabled"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("200 - Integración deshabilitada")
        void enabled_false() throws Exception {
            when(oneDriveService.isEnabled()).thenReturn(false);

            mockMvc.perform(get("/api/onedrive/enabled"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }
    }

    // =============================================
    // GET /api/onedrive/status/{usuarioId}
    // =============================================

    @Nested
    @DisplayName("GET /api/onedrive/status/{usuarioId}")
    class GetConnectionStatus {

        @Test
        @DisplayName("200 - OneDrive deshabilitado devuelve no conectado")
        void status_disabled() throws Exception {
            when(oneDriveService.isEnabled()).thenReturn(false);

            mockMvc.perform(get("/api/onedrive/status/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conectado").value(false))
                    .andExpect(jsonPath("$.integrationEnabled").value(false));
        }

        @Test
        @DisplayName("200 - Sin conexión OneDrive")
        void status_notConnected() throws Exception {
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.obtenerConexion(1L)).thenReturn(null);

            mockMvc.perform(get("/api/onedrive/status/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conectado").value(false))
                    .andExpect(jsonPath("$.integrationEnabled").value(true));
        }

        @Test
        @DisplayName("200 - Conectado a OneDrive")
        void status_connected() throws Exception {
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.obtenerConexion(1L)).thenReturn(token);

            mockMvc.perform(get("/api/onedrive/status/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.conectado").value(true))
                    .andExpect(jsonPath("$.integrationEnabled").value(true))
                    .andExpect(jsonPath("$.microsoftEmail").value("test@microsoft.com"));
        }
    }

    // =============================================
    // GET /api/onedrive/auth-url/{usuarioId}
    // =============================================

    @Nested
    @DisplayName("GET /api/onedrive/auth-url/{usuarioId}")
    class GetAuthUrl {

        @Test
        @DisplayName("200 - Genera URL de autorización")
        void authUrl_ok() throws Exception {
            when(oneDriveService.isEnabled()).thenReturn(true);
            when(oneDriveService.generarUrlAutorizacion(1L)).thenReturn("https://login.microsoft.com/auth?state=1");

            mockMvc.perform(get("/api/onedrive/auth-url/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authUrl").value("https://login.microsoft.com/auth?state=1"));
        }

        @Test
        @DisplayName("400 - Integración deshabilitada")
        void authUrl_disabled() throws Exception {
            when(oneDriveService.isEnabled()).thenReturn(false);

            mockMvc.perform(get("/api/onedrive/auth-url/1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    // =============================================
    // GET /api/onedrive/callback
    // =============================================

    @Nested
    @DisplayName("GET /api/onedrive/callback")
    class HandleCallback {

        @Test
        @DisplayName("200 - Callback exitoso")
        void callback_ok() throws Exception {
            when(oneDriveService.procesarCallback("auth-code-123", 1L)).thenReturn(token);

            mockMvc.perform(get("/api/onedrive/callback")
                            .param("code", "auth-code-123")
                            .param("state", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Conexión exitosa")));
        }

        @Test
        @DisplayName("400 - Callback con error de Microsoft")
        void callback_error() throws Exception {
            mockMvc.perform(get("/api/onedrive/callback")
                            .param("error", "access_denied")
                            .param("error_description", "User denied access"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Error")));
        }

        @Test
        @DisplayName("400 - Parámetros inválidos")
        void callback_missingParams() throws Exception {
            mockMvc.perform(get("/api/onedrive/callback"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("200 - State no numérico devuelve HTML de error")
        void callback_invalidState() throws Exception {
            mockMvc.perform(get("/api/onedrive/callback")
                            .param("code", "auth-code-123")
                            .param("state", "not-a-number"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("500 - Error al procesar callback")
        void callback_processingError() throws Exception {
            when(oneDriveService.procesarCallback("bad-code", 1L))
                    .thenThrow(new RuntimeException("Token exchange failed"));

            mockMvc.perform(get("/api/onedrive/callback")
                            .param("code", "bad-code")
                            .param("state", "1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Error")));
        }
    }

    // =============================================
    // POST /api/onedrive/disconnect/{usuarioId}
    // =============================================

    @Nested
    @DisplayName("POST /api/onedrive/disconnect/{usuarioId}")
    class Disconnect {

        @Test
        @DisplayName("200 - Desconecta correctamente")
        void disconnect_ok() throws Exception {
            doNothing().when(oneDriveService).desconectar(1L);

            mockMvc.perform(post("/api/onedrive/disconnect/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("OneDrive desconectado correctamente"));

            verify(oneDriveService).desconectar(1L);
        }
    }
}
