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
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
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
            assertThat(url).contains("response_mode=query");
            assertThat(url).startsWith("https://login.microsoftonline.com");
        }

        @Test
        @DisplayName("URL codifica redirect_uri y scopes correctamente")
        void genera_url_codificada() {
            when(config.getAuthorizeUrl()).thenReturn("https://login.microsoftonline.com/common/oauth2/v2.0/authorize");
            when(config.getClientId()).thenReturn("client-id");
            when(config.getRedirectUri()).thenReturn("http://localhost:8080/api/onedrive/callback");
            when(config.getScopes()).thenReturn("files.readwrite offline_access");

            String url = oneDriveService.generarUrlAutorizacion(5L);

            assertThat(url).contains("state=5");
            assertThat(url).contains("redirect_uri=");
            assertThat(url).contains("scope=");
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

        @Test
        @DisplayName("Intenta refrescar token si está expirado y falla sin servidor")
        void token_expirado_intentaRefrescar() {
            token.setExpiraEn(LocalDateTime.now().minusHours(1));
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getTokenUrl()).thenReturn("https://login.microsoftonline.com/common/oauth2/v2.0/token");

            assertThatThrownBy(() -> oneDriveService.obtenerAccessTokenValido(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al refrescar token");
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

        @Test
        @DisplayName("Deshabilitado si clientId es null")
        void enabled_nullClientId() {
            when(config.isEnabled()).thenReturn(true);
            when(config.getClientId()).thenReturn(null);

            assertThat(oneDriveService.isEnabled()).isFalse();
        }
    }

    // =============================================
    // sanitizarNombreCarpeta (método privado, testeado via reflexión)
    // =============================================

    @Nested
    @DisplayName("sanitizarNombreCarpeta")
    class SanitizarNombreCarpeta {

        private String invocarSanitizar(String nombre) {
            try {
                Method method = OneDriveService.class.getDeclaredMethod("sanitizarNombreCarpeta", String.class);
                method.setAccessible(true);
                return (String) method.invoke(oneDriveService, nombre);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Devuelve 'Sin-nombre' para null")
        void sanitizar_null() {
            assertThat(invocarSanitizar(null)).isEqualTo("Sin-nombre");
        }

        @Test
        @DisplayName("Reemplaza caracteres especiales de OneDrive (:, <, >)")
        void sanitizar_caracteresEspeciales() {
            String resultado = invocarSanitizar("Curso: Programación <2026>");
            assertThat(resultado).doesNotContain(":");
            assertThat(resultado).doesNotContain("<");
            assertThat(resultado).doesNotContain(">");
        }

        @Test
        @DisplayName("Nombre normal se mantiene igual")
        void sanitizar_normal() {
            assertThat(invocarSanitizar("Ingeniería del Software")).isEqualTo("Ingeniería del Software");
        }

        @Test
        @DisplayName("Reemplaza asterisco, barra y pipe")
        void sanitizar_masCaracteres() {
            String resultado = invocarSanitizar("Archivo*nombre/raro|test");
            assertThat(resultado).doesNotContain("*");
            assertThat(resultado).doesNotContain("/");
            assertThat(resultado).doesNotContain("|");
        }

        @Test
        @DisplayName("Elimina espacios extra y trims")
        void sanitizar_espaciosExtra() {
            String resultado = invocarSanitizar("  Nombre   con   espacios  ");
            assertThat(resultado).isEqualTo("Nombre con espacios");
        }

        @Test
        @DisplayName("Reemplaza comillas y signo de interrogación")
        void sanitizar_comillasYPregunta() {
            String resultado = invocarSanitizar("Archivo \"importante\"? sí");
            assertThat(resultado).doesNotContain("\"");
            assertThat(resultado).doesNotContain("?");
        }

        @Test
        @DisplayName("Cadena vacía devuelve cadena vacía")
        void sanitizar_vacio() {
            assertThat(invocarSanitizar("")).isEmpty();
        }

        @Test
        @DisplayName("Solo caracteres especiales devuelve underscores")
        void sanitizar_soloEspeciales() {
            String resultado = invocarSanitizar("*:<>?/\\|");
            assertThat(resultado).matches("^[_]+$");
        }

        @Test
        @DisplayName("Barra invertida se reemplaza")
        void sanitizar_barraInvertida() {
            String resultado = invocarSanitizar("carpeta\\subcarpeta");
            assertThat(resultado).doesNotContain("\\");
        }
    }

    // =============================================
    // subirArchivo
    // =============================================

    @Nested
    @DisplayName("subirArchivo")
    class SubirArchivo {

        @Test
        @DisplayName("Lanza excepción si usuario no tiene OneDrive conectado")
        void subir_sinConexion() {
            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "contenido".getBytes());

            when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oneDriveService.subirArchivo(
                    99L, archivo, "Curso", "Actividad", "Entregable", "Alumno", "test.pdf"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no tiene OneDrive conectado");
        }

        @Test
        @DisplayName("Falla al subir cuando Graph API no está disponible")
        void subir_graphApiFalla() {
            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "contenido".getBytes());

            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getRootFolder()).thenReturn("TFG-Entregables");
            when(config.getGraphApiUrl()).thenReturn("https://graph.microsoft.com/v1.0");

            assertThatThrownBy(() -> oneDriveService.subirArchivo(
                    1L, archivo, "Curso", "Actividad", "Entregable", "Alumno", "test.pdf"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al subir archivo");
        }

        @Test
        @DisplayName("Sanitiza caracteres especiales en la ruta de carpetas")
        void subir_sanitizaRuta() {
            MockMultipartFile archivo = new MockMultipartFile(
                    "file", "test.pdf", "application/pdf", "contenido".getBytes());

            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getRootFolder()).thenReturn("TFG-Entregables");
            when(config.getGraphApiUrl()).thenReturn("https://graph.microsoft.com/v1.0");

            // La llamada fallará por HTTP, pero no debería fallar por caracteres especiales
            assertThatThrownBy(() -> oneDriveService.subirArchivo(
                    1L, archivo, "Curso: IS <2026>", "Actividad *1*", "Entregable?", "Alumno|Test", "test.pdf"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al subir archivo");
        }
    }

    // =============================================
    // descargarArchivo
    // =============================================

    @Nested
    @DisplayName("descargarArchivo")
    class DescargarArchivo {

        @Test
        @DisplayName("Lanza excepción si usuario no tiene OneDrive conectado")
        void descargar_sinConexion() {
            when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oneDriveService.descargarArchivo(99L, "file-id"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no tiene OneDrive conectado");
        }

        @Test
        @DisplayName("Falla al descargar cuando Graph API no está disponible")
        void descargar_graphApiFalla() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn("https://graph.microsoft.com/v1.0");

            assertThatThrownBy(() -> oneDriveService.descargarArchivo(1L, "file-id-123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al descargar archivo");
        }
    }

    // =============================================
    // obtenerUrlDescarga
    // =============================================

    @Nested
    @DisplayName("obtenerUrlDescarga")
    class ObtenerUrlDescarga {

        @Test
        @DisplayName("Lanza excepción si usuario no tiene OneDrive conectado")
        void urlDescarga_sinConexion() {
            when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oneDriveService.obtenerUrlDescarga(99L, "file-id"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no tiene OneDrive conectado");
        }

        @Test
        @DisplayName("Falla al obtener URL cuando Graph API no está disponible")
        void urlDescarga_graphApiFalla() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn("https://graph.microsoft.com/v1.0");

            assertThatThrownBy(() -> oneDriveService.obtenerUrlDescarga(1L, "file-id-123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al obtener URL de descarga");
        }
    }

    // =============================================
    // eliminarArchivo
    // =============================================

    @Nested
    @DisplayName("eliminarArchivo")
    class EliminarArchivo {

        @Test
        @DisplayName("Lanza excepción si usuario no tiene OneDrive conectado")
        void eliminar_sinConexion() {
            when(tokenRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> oneDriveService.eliminarArchivo(99L, "file-id"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("no tiene OneDrive conectado");
        }

        @Test
        @DisplayName("No lanza excepción si Graph API falla al eliminar (swallows error)")
        void eliminar_graphApiFallaNoLanza() {
            when(tokenRepository.findByUsuarioId(1L)).thenReturn(Optional.of(token));
            when(config.getGraphApiUrl()).thenReturn("https://graph.microsoft.com/v1.0");

            // eliminarArchivo captura RestClientException y no la relanza
            assertThatNoException().isThrownBy(
                    () -> oneDriveService.eliminarArchivo(1L, "file-id-inexistente"));
        }
    }

    // =============================================
    // refrescarToken
    // =============================================

    @Nested
    @DisplayName("refrescarToken")
    class RefrescarToken {

        @Test
        @DisplayName("Lanza RuntimeException cuando no puede contactar el servidor de tokens")
        void refrescar_falla() {
            when(config.getTokenUrl()).thenReturn("https://login.microsoftonline.com/common/oauth2/v2.0/token");

            assertThatThrownBy(() -> oneDriveService.refrescarToken(token))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al refrescar token");
        }
    }

    // =============================================
    // desconectar - tests adicionales
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

        @Test
        @DisplayName("No falla si el usuario no tenía OneDrive conectado")
        void desconectar_sinConexionPrevia() {
            doNothing().when(tokenRepository).deleteByUsuarioId(99L);

            assertThatNoException().isThrownBy(
                    () -> oneDriveService.desconectar(99L));

            verify(tokenRepository).deleteByUsuarioId(99L);
        }
    }
}