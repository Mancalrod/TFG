package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.config.OneDriveProperties;
import com.tfg.gestionentregables.entity.MicrosoftToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.MicrosoftTokenRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MicrosoftOAuthServiceTest {

    @Mock private OneDriveProperties properties;
    @Mock private MicrosoftTokenRepository tokenRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MicrosoftOAuthService oAuthService;

    private Usuario usuario;
    private MicrosoftToken token;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Test User")
                .correoElectronico("test@test.com").contrasena("pass").build();

        token = MicrosoftToken.builder()
                .id(1L)
                .usuario(usuario)
                .microsoftEmail("test@outlook.com")
                .accessToken("access-token-123")
                .refreshToken("refresh-token-123")
                .expiraEn(LocalDateTime.now().plusHours(1))
                .scopes("Files.ReadWrite User.Read offline_access")
                .fechaConexion(LocalDateTime.now())
                .build();
    }

    // =============================================
    // buildAuthorizationUrl
    // =============================================

    @Nested
    @DisplayName("buildAuthorizationUrl")
    class BuildAuthorizationUrl {

        @Test
        @DisplayName("Genera URL con parámetros correctos")
        void genera_url_correcta() {
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("test-client-id");
            when(properties.getRedirectUri()).thenReturn("http://localhost:8080/api/oauth/microsoft/callback");

            String url = oAuthService.buildAuthorizationUrl(1L);

            assertThat(url).contains("client_id=test-client-id");
            assertThat(url).contains("response_type=code");
            assertThat(url).contains("prompt=consent");
            assertThat(url).startsWith("https://login.microsoftonline.com/common/oauth2/v2.0/authorize");
        }
    }

    // =============================================
    // exchangeCodeForTokens
    // =============================================

    @Nested
    @DisplayName("exchangeCodeForTokens")
    class ExchangeCodeForTokens {

        @Test
        @DisplayName("Lanza excepción si usuario no existe")
        void exchange_usuarioNoExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oAuthService.exchangeCodeForTokens("code", "99:uuid-123"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }

        @Test
        @DisplayName("Lanza excepción si state es inválido")
        void exchange_stateInvalido() {
            assertThatThrownBy(() -> oAuthService.exchangeCodeForTokens("code", "invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("State inválido");
        }
    }

    // =============================================
    // refreshAccessToken
    // =============================================

    @Nested
    @DisplayName("refreshAccessToken")
    class RefreshAccessToken {

        @Test
        @DisplayName("Devuelve empty si no hay token almacenado")
        void refresh_sinToken() {
            when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            Optional<MicrosoftToken> result = oAuthService.refreshAccessToken(99L);

            assertThat(result).isEmpty();
        }
    }

    // =============================================
    // getValidAccessToken
    // =============================================

    @Nested
    @DisplayName("getValidAccessToken")
    class GetValidAccessToken {

        @Test
        @DisplayName("Devuelve token si no ha expirado")
        void token_valido() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

            Optional<String> result = oAuthService.getValidAccessToken(1L);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("access-token-123");
        }

        @Test
        @DisplayName("Devuelve empty si usuario no conectado")
        void sin_conexion() {
            when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            Optional<String> result = oAuthService.getValidAccessToken(99L);

            assertThat(result).isEmpty();
        }
    }

    // =============================================
    // isConnected
    // =============================================

    @Nested
    @DisplayName("isConnected")
    class IsConnected {

        @Test
        @DisplayName("Devuelve true si existe token")
        void conectado_true() {
            when(tokenRepository.existsByUsuarioId(1L)).thenReturn(true);

            assertThat(oAuthService.isConnected(1L)).isTrue();
        }

        @Test
        @DisplayName("Devuelve false si no existe token")
        void conectado_false() {
            when(tokenRepository.existsByUsuarioId(99L)).thenReturn(false);

            assertThat(oAuthService.isConnected(99L)).isFalse();
        }
    }

    // =============================================
    // getMicrosoftEmail
    // =============================================

    @Nested
    @DisplayName("getMicrosoftEmail")
    class GetMicrosoftEmail {

        @Test
        @DisplayName("Devuelve email si existe token")
        void email_ok() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));

            Optional<String> result = oAuthService.getMicrosoftEmail(1L);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("test@outlook.com");
        }

        @Test
        @DisplayName("Devuelve empty si no existe token")
        void email_noToken() {
            when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            Optional<String> result = oAuthService.getMicrosoftEmail(99L);

            assertThat(result).isEmpty();
        }
    }

    // =============================================
    // disconnect
    // =============================================

    @Nested
    @DisplayName("disconnect")
    class Disconnect {

        @Test
        @DisplayName("Elimina token del usuario")
        void disconnect_ok() {
            doNothing().when(tokenRepository).deleteByUsuarioId(1L);

            oAuthService.disconnect(1L);

            verify(tokenRepository).deleteByUsuarioId(1L);
        }
    }
}
