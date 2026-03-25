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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

        @Test
        @DisplayName("State contiene usuarioId seguido de UUID")
        void state_contiene_userId() {
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("cid");
            when(properties.getRedirectUri()).thenReturn("http://localhost/cb");

            String url = oAuthService.buildAuthorizationUrl(42L);

            // state= se codifica como "42:<uuid>"
            assertThat(url).contains("state=");
            // Extraer el valor de state del URL
            String stateParam = url.substring(url.indexOf("state=") + 6);
            if (stateParam.contains("&")) {
                stateParam = stateParam.substring(0, stateParam.indexOf("&"));
            }
            assertThat(java.net.URLDecoder.decode(stateParam, java.nio.charset.StandardCharsets.UTF_8))
                    .startsWith("42:");
        }

        @Test
        @DisplayName("URL codifica redirect_uri y scopes")
        void codifica_parametros() {
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("cid");
            when(properties.getRedirectUri()).thenReturn("http://localhost:8080/api/callback");

            String url = oAuthService.buildAuthorizationUrl(1L);

            // scope contiene espacios que deben estar codificados
            assertThat(url).contains("scope=");
            // La parte de scope no debe contener espacios sin codificar
            String scopePart = url.substring(url.indexOf("scope=") + 6);
            if (scopePart.contains("&")) {
                scopePart = scopePart.substring(0, scopePart.indexOf("&"));
            }
            assertThat(scopePart).doesNotContain(" ");
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

        @Test
        @DisplayName("Lanza excepción si state es null")
        void exchange_stateNull() {
            assertThatThrownBy(() -> oAuthService.exchangeCodeForTokens("code", null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Lanza excepción si state tiene formato no numérico")
        void exchange_stateNoNumerico() {
            assertThatThrownBy(() -> oAuthService.exchangeCodeForTokens("code", "abc:uuid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("State inválido");
        }

        @Test
        @DisplayName("Lanza excepción si usuario existe pero la llamada REST falla")
        void exchange_restFalla() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(properties.getClientId()).thenReturn("fake-client");
            when(properties.getClientSecret()).thenReturn("fake-secret");
            when(properties.getRedirectUri()).thenReturn("http://localhost/cb");
            when(properties.getTenantId()).thenReturn("common");

            assertThatThrownBy(() -> oAuthService.exchangeCodeForTokens("fake-code", "1:uuid"))
                    .isInstanceOf(Exception.class);
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

        @Test
        @DisplayName("Devuelve empty si la llamada HTTP falla (restTemplate lanza excepción)")
        void refresh_httpFalla() {
            // El restTemplate interno no es mockeable, pero al intentar llamar
            // a la URL real con datos falsos, lanzará excepción que es capturada
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(properties.getClientId()).thenReturn("fake-client-id");
            when(properties.getClientSecret()).thenReturn("fake-secret");
            when(properties.getTenantId()).thenReturn("common");

            // La llamada REST a Microsoft fallará → catch → return Optional.empty()
            Optional<MicrosoftToken> result = oAuthService.refreshAccessToken(1L);

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

        @Test
        @DisplayName("Intenta refrescar si token expirado, devuelve empty y elimina si falla")
        void token_expirado_refreshFalla() {
            // Token con expiración en el pasado
            MicrosoftToken tokenExpirado = MicrosoftToken.builder()
                    .id(1L)
                    .usuario(usuario)
                    .microsoftEmail("test@outlook.com")
                    .accessToken("expired-token")
                    .refreshToken("refresh-token-123")
                    .expiraEn(LocalDateTime.now().minusHours(1)) // ¡Expirado!
                    .scopes("Files.ReadWrite User.Read offline_access")
                    .fechaConexion(LocalDateTime.now())
                    .build();

            // Primera llamada a findByUsuarioId (getValidAccessToken)
            // Segunda llamada (refreshAccessToken internamente)
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(tokenExpirado));
            when(properties.getClientId()).thenReturn("fake-client-id");
            when(properties.getClientSecret()).thenReturn("fake-secret");
            when(properties.getTenantId()).thenReturn("common");

            // refreshAccessToken fallará (restTemplate real lanza excepción)
            // → getValidAccessToken elimina token y devuelve empty
            Optional<String> result = oAuthService.getValidAccessToken(1L);

            assertThat(result).isEmpty();
            verify(tokenRepository).delete(tokenExpirado);
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

        @Test
        @DisplayName("Devuelve empty si el email del token es null")
        void email_null() {
            MicrosoftToken tokenSinEmail = MicrosoftToken.builder()
                    .id(2L).usuario(usuario)
                    .microsoftEmail(null)
                    .accessToken("at").refreshToken("rt")
                    .expiraEn(LocalDateTime.now().plusHours(1))
                    .fechaConexion(LocalDateTime.now())
                    .build();

            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(tokenSinEmail));

            Optional<String> result = oAuthService.getMicrosoftEmail(1L);

            // map(MicrosoftToken::getMicrosoftEmail) devuelve Optional.ofNullable(null) → empty
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

        @Test
        @DisplayName("No falla si usuario no tenía OneDrive conectado")
        void disconnect_sinConexionPrevia() {
            doNothing().when(tokenRepository).deleteByUsuarioId(99L);

            assertThatNoException().isThrownBy(() -> oAuthService.disconnect(99L));

            verify(tokenRepository).deleteByUsuarioId(99L);
        }
    }

    // =============================================
    // extractUsuarioIdFromState (método privado, via reflexión)
    // =============================================

    @Nested
    @DisplayName("extractUsuarioIdFromState")
    class ExtractUsuarioIdFromState {

        private Long invocarExtract(String state) {
            try {
                Method method = MicrosoftOAuthService.class.getDeclaredMethod("extractUsuarioIdFromState", String.class);
                method.setAccessible(true);
                return (Long) method.invoke(oAuthService, state);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException re) throw re;
                throw new RuntimeException(e.getCause());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Extrae ID correctamente de state válido")
        void extract_ok() {
            Long result = invocarExtract("42:some-uuid-value");
            assertThat(result).isEqualTo(42L);
        }

        @Test
        @DisplayName("Lanza excepción si state tiene texto no numérico")
        void extract_noNumerico() {
            assertThatThrownBy(() -> invocarExtract("abc:uuid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("State inválido");
        }

        @Test
        @DisplayName("Lanza excepción si state es vacío")
        void extract_vacio() {
            assertThatThrownBy(() -> invocarExtract(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Extrae ID cuando state solo tiene ID sin UUID")
        void extract_sinUuid() {
            // "5" → parts[0] = "5" → Long.parseLong("5") = 5L
            Long result = invocarExtract("5");
            assertThat(result).isEqualTo(5L);
        }
    }

    // =============================================
    // encodeUrl (método privado, via reflexión)
    // =============================================

    @Nested
    @DisplayName("encodeUrl")
    class EncodeUrl {

        private String invocarEncode(String value) {
            try {
                Method method = MicrosoftOAuthService.class.getDeclaredMethod("encodeUrl", String.class);
                method.setAccessible(true);
                return (String) method.invoke(oAuthService, value);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Codifica espacios como +")
        void encode_espacios() {
            String result = invocarEncode("Files.ReadWrite User.Read offline_access");
            assertThat(result).contains("+").doesNotContain(" ");
        }

        @Test
        @DisplayName("URL sin caracteres especiales se mantiene igual")
        void encode_sinEspeciales() {
            String result = invocarEncode("test-value");
            assertThat(result).isEqualTo("test-value");
        }

        @Test
        @DisplayName("Codifica caracteres especiales de URL")
        void encode_especiales() {
            String result = invocarEncode("http://localhost:8080/api/callback");
            assertThat(result).doesNotContain(":");
            assertThat(result).doesNotContain("/");
        }
    }

    // =============================================
    // fetchMicrosoftEmail (método privado, via reflexión)
    // =============================================

    @Nested
    @DisplayName("fetchMicrosoftEmail")
    class FetchMicrosoftEmail {

        private String invocarFetchEmail(String accessToken) {
            try {
                Method method = MicrosoftOAuthService.class.getDeclaredMethod("fetchMicrosoftEmail", String.class);
                method.setAccessible(true);
                return (String) method.invoke(oAuthService, accessToken);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Devuelve 'unknown@microsoft.com' cuando la API de Graph falla")
        void retornaFallbackCuandoApiFalla() {
            // restTemplate real lanza excepción al llamar graph API con token inválido
            String result = invocarFetchEmail("token-invalido");

            assertThat(result).isEqualTo("unknown@microsoft.com");
        }
    }
}
