package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.entity.MicrosoftToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import com.tfg.gestionentregables.service.MicrosoftOAuthService;
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
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MicrosoftOAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class MicrosoftOAuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MicrosoftOAuthService oAuthService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    private MicrosoftToken microsoftToken;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().id(1L).nombre("Test User")
                .correoElectronico("test@test.com").contrasena("pass").build();

        microsoftToken = MicrosoftToken.builder()
                .id(1L)
                .usuario(usuario)
                .microsoftEmail("test@outlook.com")
                .accessToken("access-123")
                .refreshToken("refresh-123")
                .expiraEn(LocalDateTime.now().plusHours(1))
                .scopes("Files.ReadWrite User.Read offline_access")
                .fechaConexion(LocalDateTime.now())
                .build();
    }

    // =============================================
    // GET /api/oauth/microsoft/authorize
    // =============================================

    @Nested
    @DisplayName("GET /api/oauth/microsoft/authorize")
    class Authorize {

        @Test
        @DisplayName("200 - Genera URL de autorización")
        void authorize_ok() throws Exception {
            when(oAuthService.buildAuthorizationUrl(1L))
                    .thenReturn("https://login.microsoftonline.com/auth?state=1:uuid");

            mockMvc.perform(get("/api/oauth/microsoft/authorize")
                            .param("usuarioId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authUrl").value("https://login.microsoftonline.com/auth?state=1:uuid"));
        }
    }

    // =============================================
    // GET /api/oauth/microsoft/callback
    // =============================================

    @Nested
    @DisplayName("GET /api/oauth/microsoft/callback")
    class Callback {

        @Test
        @DisplayName("302 - Callback exitoso redirige al dashboard con success")
        void callback_ok() throws Exception {
            when(oAuthService.exchangeCodeForTokens("auth-code", "1:uuid-123"))
                    .thenReturn(microsoftToken);

            mockMvc.perform(get("/api/oauth/microsoft/callback")
                            .param("code", "auth-code")
                            .param("state", "1:uuid-123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", "http://localhost:3000/dashboard?onedrive=success"));
        }

        @Test
        @DisplayName("302 - Callback con error redirige con error")
        void callback_error() throws Exception {
            mockMvc.perform(get("/api/oauth/microsoft/callback")
                            .param("error", "access_denied")
                            .param("error_description", "User cancelled"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.containsString("onedrive=error")));
        }

        @Test
        @DisplayName("400 - Parámetros faltantes")
        void callback_missingParams() throws Exception {
            mockMvc.perform(get("/api/oauth/microsoft/callback"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("302 - Error al intercambiar código redirige con error")
        void callback_exchangeError() throws Exception {
            when(oAuthService.exchangeCodeForTokens("bad-code", "1:uuid"))
                    .thenThrow(new RuntimeException("Token exchange failed"));

            mockMvc.perform(get("/api/oauth/microsoft/callback")
                            .param("code", "bad-code")
                            .param("state", "1:uuid"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.containsString("onedrive=error")));
        }
    }

    // =============================================
    // GET /api/oauth/microsoft/status
    // =============================================

    @Nested
    @DisplayName("GET /api/oauth/microsoft/status")
    class Status {

        @Test
        @DisplayName("200 - Usuario conectado")
        void status_connected() throws Exception {
            when(oAuthService.isConnected(1L)).thenReturn(true);
            when(oAuthService.getMicrosoftEmail(1L)).thenReturn(Optional.of("test@outlook.com"));

            mockMvc.perform(get("/api/oauth/microsoft/status")
                            .param("usuarioId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.connected").value(true))
                    .andExpect(jsonPath("$.microsoftEmail").value("test@outlook.com"));
        }

        @Test
        @DisplayName("200 - Usuario no conectado")
        void status_notConnected() throws Exception {
            when(oAuthService.isConnected(1L)).thenReturn(false);

            mockMvc.perform(get("/api/oauth/microsoft/status")
                            .param("usuarioId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.connected").value(false));
        }

        @Test
        @DisplayName("200 - Conectado sin email")
        void status_connectedNoEmail() throws Exception {
            when(oAuthService.isConnected(1L)).thenReturn(true);
            when(oAuthService.getMicrosoftEmail(1L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/oauth/microsoft/status")
                            .param("usuarioId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.connected").value(true))
                    .andExpect(jsonPath("$.microsoftEmail").value(""));
        }
    }

    // =============================================
    // DELETE /api/oauth/microsoft/disconnect
    // =============================================

    @Nested
    @DisplayName("DELETE /api/oauth/microsoft/disconnect")
    class Disconnect {

        @Test
        @DisplayName("204 - Desconecta correctamente")
        void disconnect_ok() throws Exception {
            doNothing().when(oAuthService).disconnect(1L);

            mockMvc.perform(delete("/api/oauth/microsoft/disconnect")
                            .param("usuarioId", "1"))
                    .andExpect(status().isNoContent());

            verify(oAuthService).disconnect(1L);
        }
    }
}
