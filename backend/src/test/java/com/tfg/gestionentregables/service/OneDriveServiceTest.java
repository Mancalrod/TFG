package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.config.OneDriveConfig;
import com.tfg.gestionentregables.entity.OneDriveToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.OneDriveTokenRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneDriveServiceTest {

    @Mock private OneDriveConfig config;
    @Mock private OneDriveTokenRepository tokenRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private OneDriveService oneDriveService;

    private Usuario usuario;
    private OneDriveToken token;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Test User")
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
    // generarUrlAutorizacion
    // =============================================

    @Nested
    @DisplayName("generarUrlAutorizacion")
    class GenerarUrlAutorizacion {

        @Test
        @DisplayName("Genera URL con los parámetros correctos")
        void genera_url_correcta() {
            when(config.getAuthorizeUrl()).thenReturn("https://login.microsoftonline.com/common/oauth2/v2.0/authorize");
            when(config.getClientId()).thenReturn("test-client-id");
            when(config.getRedirectUri()).thenReturn("http://localhost:8080/api/onedrive/callback");
            when(config.getScopes()).thenReturn("files.readwrite offline_access user.read");

            String url = oneDriveService.generarUrlAutorizacion(1L);

            assertThat(url).contains("client_id=test-client-id");
            assertThat(url).contains("response_type=code");
            assertThat(url).contains("state=1");
            assertThat(url).startsWith("https://login.microsoftonline.com");
        }
    }

    // =============================================
    // procesarCallback
    // =============================================

    @Nested
    @DisplayName("procesarCallback")
    class ProcesarCallback {

        @Test
        @DisplayName("Lanza excepción si usuario no existe")
        void callback_usuarioNoExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oneDriveService.procesarCallback("code", 99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    // =============================================
    // obtenerAccessTokenValido
    // =============================================

    @Nested
    @DisplayName("obtenerAccessTokenValido")
    class ObtenerAccessTokenValido {

        @Test
        @DisplayName("Devuelve token si no ha expirado")
        void token_valido() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

            String result = oneDriveService.obtenerAccessTokenValido(1L);

            assertThat(result).isEqualTo("access-token-123");
        }

        @Test
        @DisplayName("Lanza excepción si usuario no tiene OneDrive conectado")
        void sin_conexion() {
            when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oneDriveService.obtenerAccessTokenValido(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no tiene OneDrive conectado");
        }
    }

    // =============================================
    // estaConectado
    // =============================================

    @Nested
    @DisplayName("estaConectado")
    class EstaConectado {

        @Test
        @DisplayName("Devuelve true si el usuario tiene token almacenado")
        void conectado_true() {
            when(tokenRepository.existsByUsuarioId(1L)).thenReturn(true);

            assertThat(oneDriveService.estaConectado(1L)).isTrue();
        }

        @Test
        @DisplayName("Devuelve false si el usuario no tiene token almacenado")
        void conectado_false() {
            when(tokenRepository.existsByUsuarioId(99L)).thenReturn(false);

            assertThat(oneDriveService.estaConectado(99L)).isFalse();
        }
    }

    // =============================================
    // obtenerConexion
    // =============================================

    @Nested
    @DisplayName("obtenerConexion")
    class ObtenerConexion {

        @Test
        @DisplayName("Devuelve token si existe")
        void conexion_existe() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

            OneDriveToken result = oneDriveService.obtenerConexion(1L);

            assertThat(result).isNotNull();
            assertThat(result.getMicrosoftEmail()).isEqualTo("test@microsoft.com");
        }

        @Test
        @DisplayName("Devuelve null si no existe conexión")
        void conexion_noExiste() {
            when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            OneDriveToken result = oneDriveService.obtenerConexion(99L);

            assertThat(result).isNull();
        }
    }

    // =============================================
    // desconectar
    // =============================================

    @Nested
    @DisplayName("desconectar")
    class Desconectar {

        @Test
        @DisplayName("Elimina el token del usuario")
        void desconectar_ok() {
            doNothing().when(tokenRepository).deleteByUsuarioId(1L);

            oneDriveService.desconectar(1L);

            verify(tokenRepository).deleteByUsuarioId(1L);
        }
    }

    // =============================================
    // isEnabled
    // =============================================

    @Nested
    @DisplayName("isEnabled")
    class IsEnabled {

        @Test
        @DisplayName("Habilitado si config enabled y clientId/Secret presentes")
        void enabled_true() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getClientId()).thenReturn("client-id");
            when(config.getClientSecret()).thenReturn("client-secret");

            assertThat(oneDriveService.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Deshabilitado si config.enabled es false")
        void enabled_false() {
            when(config.isEnabled()).thenReturn(false);

            assertThat(oneDriveService.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Deshabilitado si clientId está vacío")
        void enabled_noClientId() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getClientId()).thenReturn("");

            assertThat(oneDriveService.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Deshabilitado si clientSecret es null")
        void enabled_nullSecret() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getClientId()).thenReturn("client-id");
            when(config.getClientSecret()).thenReturn(null);

            assertThat(oneDriveService.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Deshabilitado si clientSecret está en blanco")
        void enabled_blankSecret() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getClientId()).thenReturn("client-id");
            when(config.getClientSecret()).thenReturn("   ");

            assertThat(oneDriveService.isEnabled()).isFalse();
        }
    }
}
