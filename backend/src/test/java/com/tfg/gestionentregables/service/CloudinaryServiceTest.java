package com.tfg.gestionentregables.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private MockWebServer mockServer;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabled {

        @Test
        @DisplayName("Devuelve false cuando no hay Cloudinary configurado")
        void disabledWhenCloudinaryIsNull() {
            CloudinaryService service = new CloudinaryService(null);
            assertThat(service.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Devuelve true cuando Cloudinary está configurado")
        void enabledWhenCloudinaryExists() {
            CloudinaryService service = new CloudinaryService(cloudinary);
            assertThat(service.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("subirArchivo")
    class SubirArchivo {

        @Test
        @DisplayName("Lanza excepción si Cloudinary no está habilitado")
        void subirDisabled() {
            CloudinaryService service = new CloudinaryService(null);
            MockMultipartFile archivo = new MockMultipartFile("file", "doc.txt", "text/plain", "hola".getBytes());

            assertThatThrownBy(() -> service.subirArchivo(archivo, "curso/act"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está habilitado");
        }

        @Test
        @DisplayName("Sube archivo correctamente y devuelve publicId/url/secureUrl")
        void subirOk() throws Exception {
            when(cloudinary.uploader()).thenReturn(uploader);

            Map<String, Object> uploadResult = new HashMap<>();
            uploadResult.put("public_id", "tfg-entregables/curso/act/file123");
            uploadResult.put("url", "http://res.cloudinary.com/demo/raw/upload/file123");
            uploadResult.put("secure_url", "https://res.cloudinary.com/demo/raw/upload/file123");

            when(uploader.upload(any(byte[].class), anyMap())).thenReturn(uploadResult);

            CloudinaryService service = new CloudinaryService(cloudinary);
            MockMultipartFile archivo = new MockMultipartFile("file", "doc.txt", "text/plain", "hola".getBytes());

            Map<String, String> result = service.subirArchivo(archivo, "curso/act");

            assertThat(result).containsEntry("publicId", "tfg-entregables/curso/act/file123");
            assertThat(result).containsEntry("url", "http://res.cloudinary.com/demo/raw/upload/file123");
            assertThat(result).containsEntry("secureUrl", "https://res.cloudinary.com/demo/raw/upload/file123");
        }

        @Test
        @DisplayName("Lanza RuntimeException cuando uploader falla con IOException")
        void subirIOException() throws Exception {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new IOException("fallo upload"));

            CloudinaryService service = new CloudinaryService(cloudinary);
            MockMultipartFile archivo = new MockMultipartFile("file", "doc.txt", "text/plain", "hola".getBytes());

            assertThatThrownBy(() -> service.subirArchivo(archivo, "curso/act"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al subir archivo a Cloudinary");
        }
    }

    @Nested
    @DisplayName("descargarArchivo")
    class DescargarArchivo {

        @Test
        @DisplayName("Lanza excepción si Cloudinary no está habilitado")
        void descargarDisabled() {
            CloudinaryService service = new CloudinaryService(null);

            assertThatThrownBy(() -> service.descargarArchivo("https://x"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está habilitado");
        }

        @SuppressWarnings("resource")
        @Test
        @DisplayName("Descarga archivo correctamente con status 200")
        void descargarOk() {
            byte[] contenido = "contenido-cloudinary".getBytes();
            mockServer.enqueue(new MockResponse().setResponseCode(200)
                    .setBody(new okio.Buffer().write(contenido))
                    .addHeader("Content-Type", "application/octet-stream"));

            CloudinaryService service = new CloudinaryService(cloudinary);
            String url = mockServer.url("/file.bin").toString();

            byte[] result = service.descargarArchivo(url);
            assertThat(result).isEqualTo(contenido);
        }

        @Test
        @DisplayName("Lanza RuntimeException cuando status HTTP no es 200")
        void descargarStatusError() {
            mockServer.enqueue(new MockResponse().setResponseCode(500).setBody("error"));

            CloudinaryService service = new CloudinaryService(cloudinary);
            String url = mockServer.url("/error.bin").toString();

            assertThatThrownBy(() -> service.descargarArchivo(url))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error al descargar de Cloudinary");
        }
    }

    @Nested
    @DisplayName("eliminarArchivo")
    class EliminarArchivo {

        @Test
        @DisplayName("Lanza excepción si Cloudinary no está habilitado")
        void eliminarDisabled() {
            CloudinaryService service = new CloudinaryService(null);

            assertThatThrownBy(() -> service.eliminarArchivo("public-id"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no está habilitado");
        }

        @Test
        @DisplayName("No lanza excepción si destroy falla con IOException")
        void eliminarIOExceptionNoLanza() throws Exception {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.destroy(any(String.class), anyMap())).thenThrow(new IOException("fallo destroy"));

            CloudinaryService service = new CloudinaryService(cloudinary);

            assertThatNoException().isThrownBy(() -> service.eliminarArchivo("public-id"));
        }

        @Test
        @DisplayName("Elimina correctamente cuando Cloudinary responde")
        void eliminarOk() throws Exception {
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.destroy(any(String.class), anyMap())).thenReturn(Map.of("result", "ok"));

            CloudinaryService service = new CloudinaryService(cloudinary);

            assertThatNoException().isThrownBy(() -> service.eliminarArchivo("public-id"));
        }
    }
}
