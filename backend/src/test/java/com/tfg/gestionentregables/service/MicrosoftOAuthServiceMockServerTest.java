package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.config.OneDriveProperties;
import com.tfg.gestionentregables.entity.MicrosoftToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.MicrosoftTokenRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Tests de MicrosoftOAuthService usando MockRestServiceServer para simular las APIs de Microsoft.
 * Cubre los happy paths y ramas que requieren respuestas HTTP controladas
 * (el RestTemplate es new RestTemplate() inline, así que se intercepta via reflexión).
 */
@ExtendWith(MockitoExtension.class)
class MicrosoftOAuthServiceMockServerTest {

    @Mock private OneDriveProperties properties;
    @Mock private MicrosoftTokenRepository tokenRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private MicrosoftOAuthService oAuthService;
    private MockRestServiceServer mockServer;

    private Usuario usuario;
    private MicrosoftToken token;

    @BeforeEach
    void setUp() throws Exception {
        oAuthService = new MicrosoftOAuthService(properties, tokenRepository, usuarioRepository);

        // Vincular MockRestServiceServer al RestTemplate privado del servicio
        Field restTemplateField = MicrosoftOAuthService.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) restTemplateField.get(oAuthService);
        mockServer = MockRestServiceServer.createServer(restTemplate);

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
    // exchangeCodeForTokens — flujos exitosos
    // =============================================

    @Nested
    @DisplayName("exchangeCodeForTokens - flujo exitoso")
    class ExchangeCodeForTokensExitoso {

        @Test
        @DisplayName("Intercambia código por tokens y almacena nuevo token con email")
        void exchange_exitoso_nuevoToken() {
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("test-client-id");
            when(properties.getClientSecret()).thenReturn("test-client-secret");
            when(properties.getRedirectUri()).thenReturn("http://localhost:8080/api/callback");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
            when(tokenRepository.save(any(MicrosoftToken.class))).thenAnswer(inv -> inv.getArgument(0));

            // 1. Respuesta del token endpoint
            mockServer.expect(requestTo(containsString("oauth2/v2.0/token")))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                    .andRespond(withSuccess("""
                        {"access_token":"new-at","refresh_token":"new-rt","expires_in":3600}
                        """, MediaType.APPLICATION_JSON));

            // 2. Respuesta del perfil /me (fetchMicrosoftEmail)
            mockServer.expect(requestTo("https://graph.microsoft.com/v1.0/me"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header("Authorization", "Bearer new-at"))
                    .andRespond(withSuccess("""
                        {"mail":"user@outlook.com","displayName":"User"}
                        """, MediaType.APPLICATION_JSON));

            MicrosoftToken result = oAuthService.exchangeCodeForTokens("auth-code", "1:uuid-123");

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("new-at");
            assertThat(result.getRefreshToken()).isEqualTo("new-rt");
            assertThat(result.getMicrosoftEmail()).isEqualTo("user@outlook.com");
            assertThat(result.getScopes()).isEqualTo("Files.ReadWrite User.Read offline_access");
            assertThat(result.getUsuario()).isEqualTo(usuario);
            assertThat(result.getExpiraEn()).isAfter(LocalDateTime.now().plusMinutes(50));

            verify(tokenRepository).save(any(MicrosoftToken.class));
            mockServer.verify();
        }

        @Test
        @DisplayName("Actualiza token existente en lugar de crear uno nuevo")
        void exchange_exitoso_tokenExistente() {
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("test-client-id");
            when(properties.getClientSecret()).thenReturn("test-client-secret");
            when(properties.getRedirectUri()).thenReturn("http://localhost:8080/api/callback");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(tokenRepository.save(any(MicrosoftToken.class))).thenAnswer(inv -> inv.getArgument(0));

            mockServer.expect(requestTo(containsString("oauth2/v2.0/token")))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                        {"access_token":"updated-at","refresh_token":"updated-rt","expires_in":7200}
                        """, MediaType.APPLICATION_JSON));

            mockServer.expect(requestTo("https://graph.microsoft.com/v1.0/me"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("""
                        {"mail":"updated@outlook.com"}
                        """, MediaType.APPLICATION_JSON));

            MicrosoftToken result = oAuthService.exchangeCodeForTokens("new-code", "1:uuid-456");

            // Mismo objeto (token existente), campos actualizados
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAccessToken()).isEqualTo("updated-at");
            assertThat(result.getRefreshToken()).isEqualTo("updated-rt");
            assertThat(result.getMicrosoftEmail()).isEqualTo("updated@outlook.com");

            mockServer.verify();
        }

        @Test
        @DisplayName("Lanza RuntimeException cuando token endpoint devuelve body null")
        void exchange_respuestaBodyNull() {
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("test-client-id");
            when(properties.getClientSecret()).thenReturn("test-client-secret");
            when(properties.getRedirectUri()).thenReturn("http://localhost:8080/api/callback");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            // Respuesta 200 con body JSON "null" → body == null
            mockServer.expect(requestTo(containsString("oauth2/v2.0/token")))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> oAuthService.exchangeCodeForTokens("code", "1:uuid"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al intercambiar código por tokens");
        }
    }

    // =============================================
    // refreshAccessToken — flujos con respuestas controladas
    // =============================================

    @Nested
    @DisplayName("refreshAccessToken - con MockServer")
    class RefreshAccessTokenMock {

        @Test
        @DisplayName("Refresca token exitosamente con nuevo refresh_token en respuesta")
        void refresh_exitoso_conNuevoRefreshToken() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("test-client-id");
            when(properties.getClientSecret()).thenReturn("test-client-secret");
            when(tokenRepository.save(any(MicrosoftToken.class))).thenAnswer(inv -> inv.getArgument(0));

            mockServer.expect(requestTo(containsString("oauth2/v2.0/token")))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                    .andRespond(withSuccess("""
                        {"access_token":"refreshed-at","refresh_token":"new-rt","expires_in":3600}
                        """, MediaType.APPLICATION_JSON));

            Optional<MicrosoftToken> result = oAuthService.refreshAccessToken(1L);

            assertThat(result).isPresent();
            MicrosoftToken refreshed = result.get();
            assertThat(refreshed.getAccessToken()).isEqualTo("refreshed-at");
            assertThat(refreshed.getRefreshToken()).isEqualTo("new-rt");
            assertThat(refreshed.getExpiraEn()).isAfter(LocalDateTime.now().plusMinutes(50));
            assertThat(refreshed.getUltimoRefresco()).isNotNull();

            verify(tokenRepository).save(any(MicrosoftToken.class));
            mockServer.verify();
        }

        @Test
        @DisplayName("Refresca token exitosamente sin nuevo refresh_token (mantiene el anterior)")
        void refresh_exitoso_sinNuevoRefreshToken() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("test-client-id");
            when(properties.getClientSecret()).thenReturn("test-client-secret");
            when(tokenRepository.save(any(MicrosoftToken.class))).thenAnswer(inv -> inv.getArgument(0));

            // Respuesta sin "refresh_token" → se mantiene el original
            mockServer.expect(requestTo(containsString("oauth2/v2.0/token")))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                        {"access_token":"refreshed-at","expires_in":3600}
                        """, MediaType.APPLICATION_JSON));

            Optional<MicrosoftToken> result = oAuthService.refreshAccessToken(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getAccessToken()).isEqualTo("refreshed-at");
            // refresh_token se mantiene igual
            assertThat(result.get().getRefreshToken()).isEqualTo("refresh-token-123");

            mockServer.verify();
        }

        @Test
        @DisplayName("Devuelve empty cuando respuesta es 2xx pero body es null")
        void refresh_bodyNull() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("test-client-id");
            when(properties.getClientSecret()).thenReturn("test-client-secret");

            // JSON "null" → body se deserializa como null
            mockServer.expect(requestTo(containsString("oauth2/v2.0/token")))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

            Optional<MicrosoftToken> result = oAuthService.refreshAccessToken(1L);

            assertThat(result).isEmpty();
        }
    }

    // =============================================
    // getValidAccessToken — token expirado + refresh exitoso
    // =============================================

    @Nested
    @DisplayName("getValidAccessToken - refresh exitoso")
    class GetValidAccessTokenRefreshExitoso {

        @Test
        @DisplayName("Refresca token expirado y devuelve nuevo access token")
        void tokenExpirado_refreshExitoso() {
            MicrosoftToken tokenExpirado = MicrosoftToken.builder()
                    .id(1L).usuario(usuario)
                    .microsoftEmail("test@outlook.com")
                    .accessToken("expired-at")
                    .refreshToken("valid-rt")
                    .expiraEn(LocalDateTime.now().minusHours(1))
                    .scopes("Files.ReadWrite User.Read offline_access")
                    .fechaConexion(LocalDateTime.now())
                    .build();

            // findByUsuarioId se llama 2 veces: en getValidAccessToken y en refreshAccessToken
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(tokenExpirado));
            when(properties.getTenantId()).thenReturn("common");
            when(properties.getClientId()).thenReturn("test-client-id");
            when(properties.getClientSecret()).thenReturn("test-client-secret");
            when(tokenRepository.save(any(MicrosoftToken.class))).thenAnswer(inv -> inv.getArgument(0));

            mockServer.expect(requestTo(containsString("oauth2/v2.0/token")))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                        {"access_token":"new-valid-at","refresh_token":"new-rt","expires_in":3600}
                        """, MediaType.APPLICATION_JSON));

            Optional<String> result = oAuthService.getValidAccessToken(1L);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("new-valid-at");
            verify(tokenRepository, never()).delete(any());
            mockServer.verify();
        }
    }

    // =============================================
    // fetchMicrosoftEmail — respuestas controladas (via reflexión)
    // =============================================

    @Nested
    @DisplayName("fetchMicrosoftEmail - respuestas controladas")
    class FetchMicrosoftEmailMock {

        private String invocarFetchEmail(String accessToken) {
            try {
                Method method = MicrosoftOAuthService.class.getDeclaredMethod("fetchMicrosoftEmail", String.class);
                method.setAccessible(true);
                return (String) method.invoke(oAuthService, accessToken);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException re) throw re;
                throw new RuntimeException(e.getCause());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Retorna email del campo 'mail' cuando está presente")
        void retornaMail() {
            mockServer.expect(requestTo("https://graph.microsoft.com/v1.0/me"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header("Authorization", "Bearer valid-token"))
                    .andRespond(withSuccess("""
                        {"mail":"user@outlook.com","userPrincipalName":"upn@outlook.com"}
                        """, MediaType.APPLICATION_JSON));

            String result = invocarFetchEmail("valid-token");

            assertThat(result).isEqualTo("user@outlook.com");
            mockServer.verify();
        }

        @Test
        @DisplayName("Retorna userPrincipalName cuando 'mail' es null")
        void retornaUPN_cuandoMailEsNull() {
            mockServer.expect(requestTo("https://graph.microsoft.com/v1.0/me"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("""
                        {"userPrincipalName":"upn@microsoft.com"}
                        """, MediaType.APPLICATION_JSON));

            String result = invocarFetchEmail("valid-token");

            assertThat(result).isEqualTo("upn@microsoft.com");
            mockServer.verify();
        }

        @Test
        @DisplayName("Retorna 'unknown@microsoft.com' cuando mail y UPN son null")
        void retornaFallback_cuandoAmbosNull() {
            mockServer.expect(requestTo("https://graph.microsoft.com/v1.0/me"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("""
                        {"id":"user-id","displayName":"User"}
                        """, MediaType.APPLICATION_JSON));

            String result = invocarFetchEmail("valid-token");

            assertThat(result).isEqualTo("unknown@microsoft.com");
            mockServer.verify();
        }

        @Test
        @DisplayName("Retorna 'unknown@microsoft.com' cuando body de Graph API es null")
        void retornaFallback_cuandoBodyNull() {
            mockServer.expect(requestTo("https://graph.microsoft.com/v1.0/me"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

            String result = invocarFetchEmail("valid-token");

            assertThat(result).isEqualTo("unknown@microsoft.com");
            mockServer.verify();
        }
    }
}
