package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.service.ZipValidationService.ResultadoValidacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ZipValidationServiceTest {

    private ZipValidationService zipValidationService;

    @BeforeEach
    void setUp() {
        zipValidationService = new ZipValidationService();
    }

    /**
     * Crea un ZIP en memoria con las rutas indicadas.
     */
    private MockMultipartFile crearZip(String nombreArchivo, String... rutas) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String ruta : rutas) {
                zos.putNextEntry(new ZipEntry(ruta));
                if (!ruta.endsWith("/")) {
                    zos.write(("contenido de " + ruta).getBytes());
                }
                zos.closeEntry();
            }
        }
        return new MockMultipartFile("archivo", nombreArchivo, "application/zip", baos.toByteArray());
    }

    @Nested
    @DisplayName("Validación del nombre del ZIP")
    class ValidacionNombre {

        @Test
        @DisplayName("Acepta cualquier nombre cuando nombreEsperado es null")
        void nombre_null() throws IOException {
            MockMultipartFile zip = crearZip("cualquier.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Acepta cualquier nombre cuando nombreEsperado es '*'")
        void nombre_wildcard() throws IOException {
            MockMultipartFile zip = crearZip("cualquier.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "*");

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Acepta nombre correcto")
        void nombre_correcto() throws IOException {
            MockMultipartFile zip = crearZip("practica1.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "practica1");

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Rechaza nombre incorrecto")
        void nombre_incorrecto() throws IOException {
            MockMultipartFile zip = crearZip("mi_entrega.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "practica1");

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("practica1.zip"));
        }

        @Test
        @DisplayName("Comparación case-insensitive del nombre")
        void nombre_caseInsensitive() throws IOException {
            MockMultipartFile zip = crearZip("Practica1.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "practica1");

            assertThat(result.valido()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validación de estructura - Modo mínimo")
    class ValidacionMinima {

        @Test
        @DisplayName("Estructura null es válida")
        void estructura_null() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "cualquier.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Estructura vacía es válida")
        void estructura_vacia() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "cualquier.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, "[]", false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Archivo requerido presente es válido")
        void archivo_presente() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "memoria.pdf");
            String estructura = """
                [{"id":"1","nombre":"memoria","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Archivo requerido ausente da error")
        void archivo_ausente() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "otro.txt");
            String estructura = """
                [{"id":"1","nombre":"memoria","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("memoria"));
        }

        @Test
        @DisplayName("Carpeta requerida presente es válido")
        void carpeta_presente() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "src/", "src/Main.java", "readme.txt");
            String estructura = """
                [{"id":"1","nombre":"src","tipo":"CARPETA","extensiones":[],"hijos":[
                  {"id":"2","nombre":"Main","tipo":"ARCHIVO","extensiones":["java"],"hijos":[]}
                ]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Carpeta requerida ausente da error")
        void carpeta_ausente() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "archivo.txt");
            String estructura = """
                [{"id":"1","nombre":"src","tipo":"CARPETA","extensiones":[],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("src"));
        }

        @Test
        @DisplayName("Permite archivos extra en modo mínimo")
        void archivos_extra_permitidos() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "memoria.pdf", "readme.md", "extra.txt");
            String estructura = """
                [{"id":"1","nombre":"memoria","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Wildcard '*' acepta cualquier nombre de archivo")
        void wildcard_archivo() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "cualquier_nombre.pdf");
            String estructura = """
                [{"id":"1","nombre":"*","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Cualquier extensión cuando extensiones está vacío")
        void extension_cualquiera() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "memoria.docx");
            String estructura = """
                [{"id":"1","nombre":"memoria","tipo":"ARCHIVO","extensiones":[],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validación de estructura - Modo estricto")
    class ValidacionEstricta {

        @Test
        @DisplayName("Rechaza archivos extra en modo estricto")
        void archivos_extra_rechazados() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "memoria.pdf", "extra.txt");
            String estructura = """
                [{"id":"1","nombre":"memoria","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, true, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("extra.txt"));
        }

        @Test
        @DisplayName("Acepta estructura exacta en modo estricto")
        void estructura_exacta() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "memoria.pdf");
            String estructura = """
                [{"id":"1","nombre":"memoria","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, true, null);

            assertThat(result.valido()).isTrue();
        }
    }

    @Nested
    @DisplayName("Normalización de carpeta raíz")
    class NormalizacionRaiz {

        @Test
        @DisplayName("Quita carpeta raíz común del ZIP")
        void quita_raiz_comun() throws IOException {
            MockMultipartFile zip = crearZip("test.zip",
                    "proyecto/",
                    "proyecto/src/",
                    "proyecto/src/Main.java",
                    "proyecto/memoria.pdf");
            String estructura = """
                [
                  {"id":"1","nombre":"src","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"2","nombre":"Main","tipo":"ARCHIVO","extensiones":["java"],"hijos":[]}
                  ]},
                  {"id":"3","nombre":"memoria","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}
                ]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validación combinada (nombre + estructura)")
    class ValidacionCombinada {

        @Test
        @DisplayName("Nombre incorrecto + estructura correcta: error con mensaje de nombre")
        void nombre_mal_estructura_ok() throws IOException {
            MockMultipartFile zip = crearZip("mal_nombre.zip", "memoria.pdf");
            String estructura = """
                [{"id":"1","nombre":"memoria","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, "practica1");

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("practica1.zip"));
        }

        @Test
        @DisplayName("Nombre correcto + estructura incorrecta: error de estructura")
        void nombre_ok_estructura_mal() throws IOException {
            MockMultipartFile zip = crearZip("practica1.zip", "otro.txt");
            String estructura = """
                [{"id":"1","nombre":"memoria","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, "practica1");

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("memoria"));
        }
    }

    @Nested
    @DisplayName("Errores de parseo")
    class ErroresParseo {

        @Test
        @DisplayName("JSON inválido devuelve error")
        void json_invalido() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, "no es json", false, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("estructura esperada"));
        }
    }
}
