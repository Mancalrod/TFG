package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.config.OneDriveConfig;
import com.tfg.gestionentregables.entity.OneDriveToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.OneDriveTokenRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de OneDriveService usando MockWebServer para simular las APIs de Microsoft.
 * Esto permite testear la lógica real (parseo JSON, construcción de URLs, flujos completos)
 * sin depender de la API real de Microsoft Graph.
 */
@ExtendWith(MockitoExtension.class)
class OneDriveServiceMockServerTest {

    private MockWebServer mockServer;

    @Mock private OneDriveConfig config;
    @Mock private OneDriveTokenRepository tokenRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private OneDriveService oneDriveService;

    private Usuario usuario;
    private OneDriveToken token;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        oneDriveService = new OneDriveService(config, tokenRepository, usuarioRepository);

        usuario = Usuario.builder().id(1L).nombre("Test User")
                .correoElectronico("test@test.com").contrasena("pass").build();

        token = OneDriveToken.builder()
                .id(1L)
                .usuario(usuario)
                .accessToken("valid-access-token")
                .refreshToken("valid-refresh-token")
                .expiraEn(LocalDateTime.now().plusHours(1))
                .microsoftEmail("test@microsoft.com")
                .fechaConexion(LocalDateTime.now().minusDays(1))
                .fechaUltimoUso(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    private String baseUrl() {
        return mockServer.url("/").toString();
    }

    // =============================================
    // procesarCallback — flujo completo exitoso
    // =============================================

    @Nested
    @DisplayName("procesarCallback - flujo completo")
    class ProcesarCallbackCompleto {

        @Test
        @DisplayName("Intercambia código, obtiene perfil, guarda token y crea carpeta")
        void callback_exitoso() throws InterruptedException {
            // 1. Respuesta del token endpoint
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {
                            "access_token": "new-access-token",
                            "refresh_token": "new-refresh-token",
                            "expires_in": 3600,
                            "token_type": "Bearer"
                        }
                        """)
                    .addHeader("Content-Type", "application/json"));

            // 2. Respuesta del perfil /me
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {
                            "id": "ms-user-id-123",
                            "mail": "usuario@outlook.com",
                            "displayName": "Test User"
                        }
                        """)
                    .addHeader("Content-Type", "application/json"));

            // 3. Respuesta de crear carpeta raíz
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {
                            "id": "folder-id-123",
                            "name": "TFG-Entregables"
                        }
                        """)
                    .addHeader("Content-Type", "application/json"));

            // Configurar mocks
            String base = baseUrl();
            when(config.getTokenUrl()).thenReturn(base + "oauth2/v2.0/token");
            when(config.getClientId()).thenReturn("test-client-id");
            when(config.getClientSecret()).thenReturn("test-client-secret");
            when(config.getRedirectUri()).thenReturn("http://localhost:8080/callback");
            when(config.getScopes()).thenReturn("Files.ReadWrite offline_access");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");
            when(config.getRootFolder()).thenReturn("TFG-Entregables");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
            when(tokenRepository.save(any(OneDriveToken.class))).thenAnswer(inv -> inv.getArgument(0));

            // Ejecutar
            OneDriveToken result = oneDriveService.procesarCallback("auth-code-xyz", 1L);

            // Verificar token guardado
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("new-access-token");
            assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
            assertThat(result.getMicrosoftUserId()).isEqualTo("ms-user-id-123");
            assertThat(result.getMicrosoftEmail()).isEqualTo("usuario@outlook.com");
            assertThat(result.getExpiraEn()).isAfter(LocalDateTime.now().plusMinutes(50));

            verify(tokenRepository).save(any(OneDriveToken.class));

            // Verificar petición de token
            RecordedRequest tokenRequest = mockServer.takeRequest();
            String tokenBody = tokenRequest.getBody().readUtf8();
            assertThat(tokenBody).contains("grant_type=authorization_code");
            assertThat(tokenBody).contains("code=auth-code-xyz");
            assertThat(tokenBody).contains("client_id=test-client-id");

            // Verificar petición de perfil
            RecordedRequest profileRequest = mockServer.takeRequest();
            assertThat(profileRequest.getPath()).endsWith("/v1.0/me");
            assertThat(profileRequest.getHeader("Authorization")).isEqualTo("Bearer new-access-token");

            // Verificar petición de crear carpeta
            RecordedRequest folderRequest = mockServer.takeRequest();
            assertThat(folderRequest.getPath()).endsWith("/v1.0/me/drive/root/children");
            String folderBody = folderRequest.getBody().readUtf8();
            assertThat(folderBody).contains("TFG-Entregables");
        }

        @Test
        @DisplayName("Usa userPrincipalName si mail es null en perfil")
        void callback_usaUserPrincipalName() {
            // Token response
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"access_token":"at","refresh_token":"rt","expires_in":3600}
                        """)
                    .addHeader("Content-Type", "application/json"));

            // Perfil sin "mail", con "userPrincipalName"
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"id":"ms-123","userPrincipalName":"upn@outlook.com"}
                        """)
                    .addHeader("Content-Type", "application/json"));

            // Carpeta raíz
            mockServer.enqueue(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(config.getTokenUrl()).thenReturn(base + "token");
            when(config.getClientId()).thenReturn("cid");
            when(config.getClientSecret()).thenReturn("cs");
            when(config.getRedirectUri()).thenReturn("http://localhost/cb");
            when(config.getScopes()).thenReturn("scope");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");
            when(config.getRootFolder()).thenReturn("Root");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OneDriveToken result = oneDriveService.procesarCallback("code", 1L);

            assertThat(result.getMicrosoftEmail()).isEqualTo("upn@outlook.com");
        }

        @Test
        @DisplayName("Actualiza token existente en vez de crear nuevo")
        void callback_actualizaTokenExistente() {
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"access_token":"at-new","refresh_token":"rt-new","expires_in":7200}
                        """)
                    .addHeader("Content-Type", "application/json"));
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"id":"ms-id","mail":"new@outlook.com"}
                        """)
                    .addHeader("Content-Type", "application/json"));
            mockServer.enqueue(new MockResponse().setBody("{}").addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(config.getTokenUrl()).thenReturn(base + "token");
            when(config.getClientId()).thenReturn("cid");
            when(config.getClientSecret()).thenReturn("cs");
            when(config.getRedirectUri()).thenReturn("http://localhost/cb");
            when(config.getScopes()).thenReturn("scope");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");
            when(config.getRootFolder()).thenReturn("Root");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OneDriveToken result = oneDriveService.procesarCallback("code", 1L);

            // Debe ser el mismo objeto token actualizado
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAccessToken()).isEqualTo("at-new");
            assertThat(result.getRefreshToken()).isEqualTo("rt-new");
            assertThat(result.getMicrosoftEmail()).isEqualTo("new@outlook.com");
        }
    }

    // =============================================
    // refrescarToken — flujo completo exitoso
    // =============================================

    @Nested
    @DisplayName("refrescarToken - flujo completo")
    class RefrescarTokenCompleto {

        @Test
        @DisplayName("Refresca token correctamente y guarda en BD")
        void refrescar_exitoso() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {
                            "access_token": "refreshed-access-token",
                            "refresh_token": "refreshed-refresh-token",
                            "expires_in": 7200
                        }
                        """)
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(config.getTokenUrl()).thenReturn(base + "oauth2/v2.0/token");
            when(config.getClientId()).thenReturn("cid");
            when(config.getClientSecret()).thenReturn("cs");
            when(config.getScopes()).thenReturn("Files.ReadWrite");
            when(tokenRepository.save(any(OneDriveToken.class))).thenAnswer(inv -> inv.getArgument(0));

            OneDriveToken result = oneDriveService.refrescarToken(token);

            assertThat(result.getAccessToken()).isEqualTo("refreshed-access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refreshed-refresh-token");
            assertThat(result.getExpiraEn()).isAfter(LocalDateTime.now().plusMinutes(100));
            assertThat(result.getFechaUltimoUso()).isNotNull();

            verify(tokenRepository).save(any(OneDriveToken.class));

            RecordedRequest request = mockServer.takeRequest();
            String body = request.getBody().readUtf8();
            assertThat(body).contains("grant_type=refresh_token");
            assertThat(body).contains("refresh_token=valid-refresh-token");
        }

        @Test
        @DisplayName("Mantiene refresh_token anterior si no viene en respuesta")
        void refrescar_sinNuevoRefreshToken() {
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"access_token":"new-at","expires_in":3600}
                        """)
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(config.getTokenUrl()).thenReturn(base + "token");
            when(config.getClientId()).thenReturn("cid");
            when(config.getClientSecret()).thenReturn("cs");
            when(config.getScopes()).thenReturn("scope");
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OneDriveToken result = oneDriveService.refrescarToken(token);

            assertThat(result.getAccessToken()).isEqualTo("new-at");
            // refresh_token se mantiene igual
            assertThat(result.getRefreshToken()).isEqualTo("valid-refresh-token");
        }

        @Test
        @DisplayName("Lanza RuntimeException si el servidor responde con error HTTP")
        void refrescar_errorHttp() {
            mockServer.enqueue(new MockResponse().setResponseCode(400)
                    .setBody("{\"error\":\"invalid_grant\"}")
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(config.getTokenUrl()).thenReturn(base + "token");
            when(config.getClientId()).thenReturn("cid");
            when(config.getClientSecret()).thenReturn("cs");
            when(config.getScopes()).thenReturn("scope");

            assertThatThrownBy(() -> oneDriveService.refrescarToken(token))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al refrescar token");
        }
    }

    // =============================================
    // obtenerAccessTokenValido — con refresh exitoso
    // =============================================

    @Nested
    @DisplayName("obtenerAccessTokenValido — refresh exitoso")
    class ObtenerAccessTokenRefreshExitoso {

        @Test
        @DisplayName("Refresca token expirado y devuelve el nuevo access token")
        void token_expirado_refrescaYDevuelveNuevo() {
            token.setExpiraEn(LocalDateTime.now().minusHours(1)); // expirado

            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"access_token":"nuevo-token-refrescado","refresh_token":"rt","expires_in":3600}
                        """)
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getTokenUrl()).thenReturn(base + "token");
            when(config.getClientId()).thenReturn("cid");
            when(config.getClientSecret()).thenReturn("cs");
            when(config.getScopes()).thenReturn("scope");
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            String result = oneDriveService.obtenerAccessTokenValido(1L);

            assertThat(result).isEqualTo("nuevo-token-refrescado");
        }
    }

    // =============================================
    // subirArchivo — flujo completo (< 4MB)
    // =============================================

    @Nested
    @DisplayName("subirArchivo - flujo completo")
    class SubirArchivoCompleto {

        @Test
        @DisplayName("Sube archivo usando carpeta opcional personalizada")
        void subir_conCarpetaOpcional() throws InterruptedException {
            MockMultipartFile archivo = new MockMultipartFile(
                "file", "doc.txt", "text/plain", "contenido".getBytes());

            mockServer.enqueue(new MockResponse()
                .setBody("""
                {"id":"file-id-opt","webUrl":"https://onedrive.example.com/file-opt"}
                """)
                .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            Map<String, String> result = oneDriveService.subirArchivo(
                1L, archivo,
                "Curso", "Actividad", "Entregable", "Alumno Uno",
                "doc.txt", "Carpeta Personalizada");

            assertThat(result).containsEntry("fileId", "file-id-opt");

            RecordedRequest request = mockServer.takeRequest();
            assertThat(request.getMethod()).isEqualTo("PUT");
            assertThat(request.getPath()).contains("Carpeta%20Personalizada");
            assertThat(request.getPath()).contains("Alumno%20Uno");
            assertThat(request.getPath()).doesNotContain("%2520");
            assertThat(request.getPath()).contains("doc.txt");
        }

        @Test
        @DisplayName("Sube archivo grande por upload session cuando supera 4MB")
        void subir_archivoGrande_uploadSession() throws InterruptedException {
            byte[] contenidoGrande = new byte[6 * 1024 * 1024]; // 6MB
            MockMultipartFile archivo = new MockMultipartFile(
                "file", "video.bin", "application/octet-stream", contenidoGrande);

            String uploadUrl = baseUrl() + "upload-session-url";

            // 1) createUploadSession
            mockServer.enqueue(new MockResponse()
                .setBody("""
                {"uploadUrl":"%s"}
                """.formatted(uploadUrl))
                .addHeader("Content-Type", "application/json"));

            // 2) primer chunk (respuesta intermedia)
            mockServer.enqueue(new MockResponse()
                .setBody("""
                {"nextExpectedRanges":["5242880-"]}
                """)
                .addHeader("Content-Type", "application/json"));

            // 3) segundo chunk (respuesta final)
            mockServer.enqueue(new MockResponse()
                .setBody("""
                {"id":"large-file-id","webUrl":"https://onedrive.example.com/large"}
                """)
                .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getRootFolder()).thenReturn("TFG-Entregables");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            Map<String, String> result = oneDriveService.subirArchivo(
                1L, archivo, "Curso", "Actividad", "Entregable", "Alumno", "video.bin");

            assertThat(result).containsEntry("fileId", "large-file-id");
            assertThat(result).containsEntry("webUrl", "https://onedrive.example.com/large");

            RecordedRequest createSessionRequest = mockServer.takeRequest();
            assertThat(createSessionRequest.getMethod()).isEqualTo("POST");
            assertThat(createSessionRequest.getPath()).contains("createUploadSession");

            RecordedRequest chunk1Request = mockServer.takeRequest();
            assertThat(chunk1Request.getMethod()).isEqualTo("PUT");
            assertThat(chunk1Request.getPath()).isEqualTo("/upload-session-url");
            assertThat(chunk1Request.getHeader("Content-Range")).isEqualTo("bytes 0-5242879/6291456");

            RecordedRequest chunk2Request = mockServer.takeRequest();
            assertThat(chunk2Request.getMethod()).isEqualTo("PUT");
            assertThat(chunk2Request.getPath()).isEqualTo("/upload-session-url");
            assertThat(chunk2Request.getHeader("Content-Range")).isEqualTo("bytes 5242880-6291455/6291456");
        }

        @Test
        @DisplayName("Sube archivo pequeño (< 4MB) correctamente")
        void subir_archivoPequeno() throws InterruptedException {
            byte[] contenido = "contenido del archivo PDF de prueba".getBytes();
            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "informe.pdf", "application/pdf", contenido);

            // Respuesta de Graph API al subir archivo
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {
                            "id": "file-id-abc123",
                            "name": "informe.pdf",
                            "webUrl": "https://onedrive.live.com/edit.aspx?id=abc123"
                        }
                        """)
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getRootFolder()).thenReturn("TFG-Entregables");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            Map<String, String> result = oneDriveService.subirArchivo(
                    1L, archivo,
                    "Ingeniería del Software",
                    "Actividad 1",
                    "Entregable Final",
                    "Juan García",
                    "informe.pdf");

            assertThat(result).containsEntry("fileId", "file-id-abc123");
            assertThat(result).containsEntry("webUrl", "https://onedrive.live.com/edit.aspx?id=abc123");

            RecordedRequest request = mockServer.takeRequest();
            assertThat(request.getMethod()).isEqualTo("PUT");
            assertThat(request.getPath()).contains("/v1.0/me/drive/root:/");
            assertThat(request.getPath()).contains("TFG-Entregables");
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer valid-access-token");
            assertThat(request.getHeader("Content-Type")).isEqualTo("application/pdf");
            assertThat(request.getBody().readByteArray()).isEqualTo(contenido);
        }

        @Test
        @DisplayName("Sube archivo con webUrl ausente en respuesta")
        void subir_sinWebUrl() {
            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "doc.txt", "text/plain", "texto".getBytes());

            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"id":"file-id-999"}
                        """)
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getRootFolder()).thenReturn("Root");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            Map<String, String> result = oneDriveService.subirArchivo(
                    1L, archivo, "Curso", "Actividad", "Entregable", "Alumno", "doc.txt");

            assertThat(result).containsEntry("fileId", "file-id-999");
            assertThat(result).containsEntry("webUrl", "");
        }

        @Test
        @DisplayName("Sanitiza caracteres especiales en ruta y construye URL correcta")
        void subir_sanitizaRutaCorrectamente() throws InterruptedException {
            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "abc".getBytes());

            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"id":"fid","webUrl":"http://example.com"}
                        """)
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getRootFolder()).thenReturn("TFG-Entregables");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            oneDriveService.subirArchivo(
                    1L, archivo,
                    "Curso: IS <2026>",   // caracteres ilegales
                    "Actividad *1*",       // asterisco
                    "Entregable?",         // interrogación
                    "Alumno|Test",         // pipe
                    "test.pdf");

            RecordedRequest request = mockServer.takeRequest();
            String path = request.getPath();
            // Los caracteres ilegales deben haber sido reemplazados por _
            assertThat(path).doesNotContain("*");
            assertThat(path).doesNotContain("?"); // como query param no, como char en ruta
            assertThat(path).contains("TFG-Entregables");
        }

        @Test
        @DisplayName("Usa application/octet-stream si contentType es null")
        void subir_contentTypeNull() throws InterruptedException {
            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "data.bin", null, "binary".getBytes());

            mockServer.enqueue(new MockResponse()
                    .setBody("{\"id\":\"fid\",\"webUrl\":\"\"}")
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getRootFolder()).thenReturn("Root");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            oneDriveService.subirArchivo(1L, archivo, "C", "A", "E", "Al", "data.bin");

            RecordedRequest request = mockServer.takeRequest();
            assertThat(request.getHeader("Content-Type")).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("Lanza excepción si Graph API responde con error HTTP")
        void subir_errorHttp() {
            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "abc".getBytes());

            mockServer.enqueue(new MockResponse().setResponseCode(403)
                    .setBody("{\"error\":{\"code\":\"accessDenied\"}}")
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getRootFolder()).thenReturn("Root");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            assertThatThrownBy(() -> oneDriveService.subirArchivo(
                    1L, archivo, "C", "A", "E", "Al", "test.pdf"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al subir archivo");
        }
    }

    // =============================================
    // descargarArchivo — flujo completo
    // =============================================

    @Nested
    @DisplayName("descargarArchivo - flujo completo")
    class DescargarArchivoCompleto {

        @Test
        @DisplayName("Descarga archivo correctamente y devuelve bytes")
        void descargar_exitoso() throws InterruptedException {
            byte[] contenidoEsperado = "contenido binario del archivo".getBytes();

            mockServer.enqueue(new MockResponse()
                    .setBody(new okio.Buffer().write(contenidoEsperado))
                    .addHeader("Content-Type", "application/octet-stream"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            byte[] result = oneDriveService.descargarArchivo(1L, "file-id-abc");

            assertThat(result).isEqualTo(contenidoEsperado);

            RecordedRequest request = mockServer.takeRequest();
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).isEqualTo("/v1.0/me/drive/items/file-id-abc/content");
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer valid-access-token");
        }

        @Test
        @DisplayName("Lanza excepción si Graph API devuelve error")
        void descargar_errorHttp() {
            mockServer.enqueue(new MockResponse().setResponseCode(404)
                    .setBody("{\"error\":{\"code\":\"itemNotFound\"}}")
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            assertThatThrownBy(() -> oneDriveService.descargarArchivo(1L, "inexistente"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al descargar archivo");
        }
    }

    // =============================================
    // obtenerUrlDescarga — flujo completo
    // =============================================

    @Nested
    @DisplayName("obtenerUrlDescarga - flujo completo")
    class ObtenerUrlDescargaCompleto {

        @Test
        @DisplayName("Devuelve downloadUrl de @microsoft.graph.downloadUrl")
        void urlDescarga_conDownloadUrl() throws InterruptedException {
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {
                            "id": "file-id-abc",
                            "name": "informe.pdf",
                            "@microsoft.graph.downloadUrl": "https://download.example.com/file?token=xyz"
                        }
                        """)
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            String url = oneDriveService.obtenerUrlDescarga(1L, "file-id-abc");

            assertThat(url).isEqualTo("https://download.example.com/file?token=xyz");

            RecordedRequest request = mockServer.takeRequest();
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).isEqualTo("/v1.0/me/drive/items/file-id-abc");
        }

        @Test
        @DisplayName("Devuelve fallback URL si no existe @microsoft.graph.downloadUrl")
        void urlDescarga_fallback() {
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"id":"file-id","name":"doc.pdf"}
                        """)
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            String url = oneDriveService.obtenerUrlDescarga(1L, "file-id");

            // Fallback: GraphApiUrl + /me/drive/items/{id}/content
            assertThat(url).isEqualTo(base + "v1.0/me/drive/items/file-id/content");
        }

        @Test
        @DisplayName("Lanza excepción si Graph API devuelve error")
        void urlDescarga_errorHttp() {
            mockServer.enqueue(new MockResponse().setResponseCode(401)
                    .setBody("{\"error\":{\"code\":\"InvalidAuthenticationToken\"}}")
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            assertThatThrownBy(() -> oneDriveService.obtenerUrlDescarga(1L, "file-id"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al obtener URL de descarga");
        }
    }

        @Nested
        @DisplayName("listarCarpetas - flujo completo")
        class ListarCarpetasCompleto {

                @Test
                @DisplayName("Lista carpetas desde root y construye path completo")
                void listar_root_ok() throws InterruptedException {
                        mockServer.enqueue(new MockResponse()
                                        .setBody("""
                                                {
                                                    "value": [
                                                        {
                                                            "id": "folder-1",
                                                            "name": "Entregas",
                                                            "parentReference": {"path": "/drive/root:"}
                                                        },
                                                        {
                                                            "id": "folder-2",
                                                            "name": "Pruebas",
                                                            "parentReference": {"path": "/drive/root:/Curso A"}
                                                        }
                                                    ]
                                                }
                                                """)
                                        .addHeader("Content-Type", "application/json"));

                        String base = baseUrl();
                        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
                        when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

                        var carpetas = oneDriveService.listarCarpetas(1L, null);

                        assertThat(carpetas).hasSize(2);
                        assertThat(carpetas.get(0)).containsEntry("id", "folder-1");
                        assertThat(carpetas.get(0)).containsEntry("path", "Entregas");
                        assertThat(carpetas.get(1)).containsEntry("path", "Curso A/Pruebas");

                        RecordedRequest request = mockServer.takeRequest();
                        assertThat(request.getMethod()).isEqualTo("GET");
                        assertThat(request.getPath()).contains("/v1.0/me/drive/root/children");
                }

                @Test
                @DisplayName("Lista carpetas hijas desde parentId")
                void listar_child_ok() throws InterruptedException {
                        mockServer.enqueue(new MockResponse()
                                        .setBody("""
                                                {"value": [{"id":"child-1","name":"Subcarpeta","parentReference":{"path":"/drive/root:/Base"}}]}
                                                """)
                                        .addHeader("Content-Type", "application/json"));

                        String base = baseUrl();
                        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
                        when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

                        var carpetas = oneDriveService.listarCarpetas(1L, "parent-xyz");

                        assertThat(carpetas).hasSize(1);
                        assertThat(carpetas.get(0)).containsEntry("id", "child-1");

                        RecordedRequest request = mockServer.takeRequest();
                        assertThat(request.getPath()).contains("/v1.0/me/drive/items/parent-xyz/children");
                }

                @Test
                @DisplayName("Lanza excepción cuando Graph API falla")
                void listar_errorHttp() {
                        mockServer.enqueue(new MockResponse().setResponseCode(500)
                                        .setBody("{" + "\"error\":\"server_error\"}"));

                        String base = baseUrl();
                        when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
                        when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

                        assertThatThrownBy(() -> oneDriveService.listarCarpetas(1L, null))
                                        .isInstanceOf(RuntimeException.class)
                                        .hasMessageContaining("Error al listar carpetas");
                }
        }

    // =============================================
    // eliminarArchivo — flujo completo
    // =============================================

    @Nested
    @DisplayName("eliminarArchivo - flujo completo")
    class EliminarArchivoCompleto {

        @Test
        @DisplayName("Elimina archivo correctamente (204 No Content)")
        void eliminar_exitoso() throws InterruptedException {
            mockServer.enqueue(new MockResponse().setResponseCode(204));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            assertThatNoException().isThrownBy(
                    () -> oneDriveService.eliminarArchivo(1L, "file-id-to-delete"));

            RecordedRequest request = mockServer.takeRequest();
            assertThat(request.getMethod()).isEqualTo("DELETE");
            assertThat(request.getPath()).isEqualTo("/v1.0/me/drive/items/file-id-to-delete");
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer valid-access-token");
        }

        @Test
        @DisplayName("No lanza excepción si el archivo ya no existe (404)")
        void eliminar_archivoNoExiste() {
            mockServer.enqueue(new MockResponse().setResponseCode(404)
                    .setBody("{\"error\":{\"code\":\"itemNotFound\"}}")
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");

            // El método traga la excepción
            assertThatNoException().isThrownBy(
                    () -> oneDriveService.eliminarArchivo(1L, "file-id-inexistente"));
        }
    }

    // =============================================
    // Flujo completo: subir con token expirado → refresh → subir
    // =============================================

    @Nested
    @DisplayName("Flujo completo - token expirado")
    class FlujoTokenExpirado {

        @Test
        @DisplayName("Refresca token automáticamente antes de subir archivo")
        void subir_conTokenExpirado_refrescaYSube() {
            token.setExpiraEn(LocalDateTime.now().minusHours(1)); // expirado

            // 1. Respuesta refresh token
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"access_token":"fresh-token","refresh_token":"rt","expires_in":3600}
                        """)
                    .addHeader("Content-Type", "application/json"));

            // 2. Respuesta subir archivo
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"id":"uploaded-file-id","webUrl":"https://onedrive.com/file"}
                        """)
                    .addHeader("Content-Type", "application/json"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getTokenUrl()).thenReturn(base + "token");
            when(config.getClientId()).thenReturn("cid");
            when(config.getClientSecret()).thenReturn("cs");
            when(config.getScopes()).thenReturn("scope");
            when(config.getRootFolder()).thenReturn("Root");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "data".getBytes());

            Map<String, String> result = oneDriveService.subirArchivo(
                    1L, archivo, "Curso", "Act", "Ent", "Alumno", "doc.pdf");

            assertThat(result).containsEntry("fileId", "uploaded-file-id");
            verify(tokenRepository).save(any(OneDriveToken.class));
        }

        @Test
        @DisplayName("Refresca token antes de descargar archivo")
        void descargar_conTokenExpirado() {
            token.setExpiraEn(LocalDateTime.now().minusMinutes(5)); // expirado

            // 1. Refresh
            mockServer.enqueue(new MockResponse()
                    .setBody("""
                        {"access_token":"fresh-token","expires_in":3600}
                        """)
                    .addHeader("Content-Type", "application/json"));

            // 2. Descarga
            mockServer.enqueue(new MockResponse()
                    .setBody(new okio.Buffer().write("file-content".getBytes()))
                    .addHeader("Content-Type", "application/octet-stream"));

            String base = baseUrl();
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getTokenUrl()).thenReturn(base + "token");
            when(config.getClientId()).thenReturn("cid");
            when(config.getClientSecret()).thenReturn("cs");
            when(config.getScopes()).thenReturn("scope");
            when(config.getGraphApiUrl()).thenReturn(base + "v1.0");
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            byte[] result = oneDriveService.descargarArchivo(1L, "fid");

            assertThat(result).isEqualTo("file-content".getBytes());
        }
    }
}
