package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.service.ZipValidationService.NodoEstructura;
import com.tfg.gestionentregables.service.ZipValidationService.ResultadoValidacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    // =============================================
    // Nombre del ZIP — ramas no cubiertas
    // =============================================

    @Nested
    @DisplayName("Validación del nombre — ramas adicionales")
    class ValidacionNombreExtra {

        @Test
        @DisplayName("Acepta cualquier nombre cuando nombreEsperado es cadena vacía")
        void nombre_vacio() throws IOException {
            MockMultipartFile zip = crearZip("cualquier.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "");

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Acepta cualquier nombre cuando nombreEsperado es solo espacios en blanco")
        void nombre_blancos() throws IOException {
            MockMultipartFile zip = crearZip("cualquier.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "   ");

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("No falla si getOriginalFilename devuelve null")
        void nombre_originalFilenameNull() throws IOException {
            byte[] zipBytes = crearZipBytes("archivo.txt");
            // MockMultipartFile convierte null a "", así que usamos un mock real
            MultipartFile zip = mock(MultipartFile.class);
            when(zip.getOriginalFilename()).thenReturn(null);
            when(zip.getInputStream()).thenReturn(new ByteArrayInputStream(zipBytes));

            // nombreEsperado dado pero originalFilename es null → no entra en el bloque de comparación
            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "practica1");

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Archivo sin extensión .zip se compara tal cual con el nombre esperado")
        void nombre_sinExtensionZip() throws IOException {
            // Nombre no termina en .zip → nombreSinExt = nombreArchivo entero
            byte[] zipBytes = crearZipBytes("archivo.txt");
            MockMultipartFile zip = new MockMultipartFile("archivo", "practica1.tar.gz", "application/zip", zipBytes);

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "practica1");

            // "practica1.tar.gz" != "practica1" → error
            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("practica1.zip") && e.contains("practica1.tar.gz"));
        }

        @Test
        @DisplayName("Archivo sin extensión .zip pero nombre coincide exactamente")
        void nombre_sinExtensionZip_coincide() throws IOException {
            byte[] zipBytes = crearZipBytes("archivo.txt");
            MockMultipartFile zip = new MockMultipartFile("archivo", "practica1", "application/zip", zipBytes);

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "practica1");

            assertThat(result.valido()).isTrue();
        }
    }

    // =============================================
    // Protecciones del ZIP (Zip Slip, MAX_ENTRIES)
    // =============================================

    @Nested
    @DisplayName("Protecciones de seguridad del ZIP")
    class ProteccionesZip {

        @Test
        @DisplayName("Rechaza ZIP con entrada de path traversal (Zip Slip)")
        void zipSlip_detectado() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                // Entrada maliciosa con path traversal
                zos.putNextEntry(new ZipEntry("../../etc/passwd"));
                zos.write("malicioso".getBytes());
                zos.closeEntry();
            }
            MockMultipartFile zip = new MockMultipartFile("archivo", "test.zip", "application/zip", baos.toByteArray());
            String estructura = """
                [{"id":"1","nombre":"archivo","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("No se pudo leer"));
        }

        @Test
        @DisplayName("Rechaza ZIP con ruta relativa ascendente que empieza con ../")
        void zipSlip_relativoConDosPuntos() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("../sibling/evil.txt"));
                zos.write("malicioso".getBytes());
                zos.closeEntry();
            }
            MockMultipartFile zip = new MockMultipartFile("archivo", "test.zip", "application/zip", baos.toByteArray());
            String estructura = """
                [{"id":"1","nombre":"archivo","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("No se pudo leer"));
        }

        @Test
        @DisplayName("Rechaza ZIP con demasiadas entradas (MAX_ENTRIES)")
        void maxEntries_excedido() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (int i = 0; i <= 10_000; i++) {
                    zos.putNextEntry(new ZipEntry("file" + i + ".txt"));
                    zos.write(("c" + i).getBytes());
                    zos.closeEntry();
                }
            }
            MockMultipartFile zip = new MockMultipartFile("archivo", "test.zip", "application/zip", baos.toByteArray());
            String estructura = """
                [{"id":"1","nombre":"*","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("No se pudo leer"));
        }

        @Test
        @DisplayName("ZIP corrupto devuelve error de lectura")
        void zip_corrupto() {
            MockMultipartFile zip = new MockMultipartFile("archivo", "test.zip", "application/zip",
                    "esto no es un zip".getBytes());
            String estructura = """
                [{"id":"1","nombre":"archivo","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            // Un ZIP corrupto no tiene entradas → rutas vacías → falta archivo
            assertThat(result.valido()).isFalse();
        }
    }

    // =============================================
    // Carpetas wildcard "*"
    // =============================================

    @Nested
    @DisplayName("Carpeta wildcard '*'")
    class CarpetaWildcard {

        @Test
        @DisplayName("Carpeta '*' acepta cualquier carpeta y valida sus hijos")
        void wildcard_carpeta_conHijos() throws IOException {
            MockMultipartFile zip = crearZip("test.zip",
                    "alumno1/", "alumno1/entrega.pdf",
                    "alumno2/", "alumno2/entrega.pdf");
            String estructura = """
                [{"id":"1","nombre":"*","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"2","nombre":"entrega","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}
                ]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Carpeta '*' da error cuando no hay carpetas en el nivel y tiene hijos")
        void wildcard_carpeta_sinCarpetasEnNivel() throws IOException {
            // Solo archivos en raíz, sin carpetas
            MockMultipartFile zip = crearZip("test.zip", "archivo.txt");
            String estructura = """
                [{"id":"1","nombre":"*","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"2","nombre":"entrega","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}
                ]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("al menos una carpeta"));
        }

        @Test
        @DisplayName("Carpeta '*' sin hijos no da error aunque no haya carpetas")
        void wildcard_carpeta_sinHijos() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "archivo.txt");
            String estructura = """
                [{"id":"1","nombre":"*","tipo":"CARPETA","extensiones":[],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Carpeta '*' detecta hijos faltantes en alguna subcarpeta")
        void wildcard_carpeta_conHijosFaltantes() throws IOException {
            MockMultipartFile zip = crearZip("test.zip",
                    "alumno1/", "alumno1/entrega.pdf",
                    "alumno2/", "alumno2/otro.txt");  // faltan entrega.pdf aquí
            String estructura = """
                [{"id":"1","nombre":"*","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"2","nombre":"entrega","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}
                ]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("entrega"));
        }
    }

    // =============================================
    // Carpeta nombrada con hijos vacíos
    // =============================================

    @Nested
    @DisplayName("Carpeta nombrada sin hijos esperados")
    class CarpetaNombradaSinHijos {

        @Test
        @DisplayName("Carpeta existe sin hijos esperados → válido")
        void carpeta_sinHijos_existe() throws IOException {
            // Añadir archivo en raíz para evitar que se normalice la carpeta raíz
            MockMultipartFile zip = crearZip("test.zip",
                    "readme.txt", "lib/", "lib/algo.jar");
            String estructura = """
                [{"id":"1","nombre":"lib","tipo":"CARPETA","extensiones":[],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }
    }

    // =============================================
    // Modo estricto — ramas adicionales
    // =============================================

    @Nested
    @DisplayName("Modo estricto — ramas adicionales")
    class ModoEstrictoExtra {

        @Test
        @DisplayName("Estricto con carpeta wildcard '*' acepta archivos dentro de cualquier carpeta")
        void estricto_wildcardCarpeta() throws IOException {
            // Dos carpetas raíz para evitar normalización
            MockMultipartFile zip = crearZip("test.zip",
                    "grupo1/", "grupo1/entrega.pdf",
                    "grupo2/", "grupo2/entrega.pdf");
            String estructura = """
                [{"id":"1","nombre":"*","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"2","nombre":"entrega","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}
                ]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, true, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Estricto con carpeta wildcard '*' rechaza archivos extra")
        void estricto_wildcardCarpeta_archivoExtra() throws IOException {
            // Dos carpetas raíz para evitar normalización; grupo1 tiene archivo extra
            MockMultipartFile zip = crearZip("test.zip",
                    "grupo1/", "grupo1/entrega.pdf", "grupo1/extra.txt",
                    "grupo2/", "grupo2/entrega.pdf");
            String estructura = """
                [{"id":"1","nombre":"*","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"2","nombre":"entrega","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}
                ]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, true, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("extra.txt"));
        }

        @Test
        @DisplayName("Estricto con carpeta nombrada y hijos vacíos acepta todo su contenido")
        void estricto_carpetaNombrada_hijosVacios() throws IOException {
            // Archivo en raíz para evitar normalización
            MockMultipartFile zip = crearZip("test.zip",
                    "readme.txt", "lib/", "lib/dep.jar");
            String estructura = """
                [
                  {"id":"0","nombre":"readme","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]},
                  {"id":"1","nombre":"lib","tipo":"CARPETA","extensiones":[],"hijos":[]}
                ]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, true, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Estricto con carpeta nombrada y hijos, rechaza archivos extra dentro")
        void estricto_carpetaNombrada_archivoExtraDentro() throws IOException {
            // Archivo en raíz para evitar normalización
            MockMultipartFile zip = crearZip("test.zip",
                    "readme.txt", "src/", "src/Main.java", "src/Extra.java");
            String estructura = """
                [
                  {"id":"0","nombre":"readme","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]},
                  {"id":"1","nombre":"src","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"2","nombre":"Main","tipo":"ARCHIVO","extensiones":["java"],"hijos":[]}
                  ]}
                ]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, true, null);

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).anyMatch(e -> e.contains("Extra.java"));
        }

        @Test
        @DisplayName("Estricto no reporta extra si la validación de nodos ya falló")
        void estricto_noReportaExtra_siYaHayErrores() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "extra.txt");
            String estructura = """
                [{"id":"1","nombre":"obligatorio","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, true, null);

            assertThat(result.valido()).isFalse();
            // Solo hay error de archivo faltante, no de extra (se salta comprobación estricta si hay errores previos)
            assertThat(result.errores()).hasSize(1);
            assertThat(result.errores().getFirst()).contains("obligatorio");
        }
    }

    // =============================================
    // Normalización de rutas — ramas adicionales
    // =============================================

    @Nested
    @DisplayName("Normalización de rutas — ramas adicionales")
    class NormalizacionExtra {

        @Test
        @DisplayName("No normaliza si hay múltiples carpetas raíz diferentes")
        void multiples_raices() throws IOException {
            MockMultipartFile zip = crearZip("test.zip",
                    "carpetaA/", "carpetaA/archivo.txt",
                    "carpetaB/", "carpetaB/archivo.txt");
            String estructura = """
                [
                  {"id":"1","nombre":"carpetaA","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"2","nombre":"archivo","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]}
                  ]},
                  {"id":"3","nombre":"carpetaB","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"4","nombre":"archivo","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]}
                  ]}
                ]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("No normaliza si hay archivo suelto en la raíz del ZIP")
        void archivo_en_raiz_previene_normalizacion() throws IOException {
            MockMultipartFile zip = crearZip("test.zip",
                    "readme.txt", "src/", "src/Main.java");
            String estructura = """
                [
                  {"id":"1","nombre":"readme","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]},
                  {"id":"2","nombre":"src","tipo":"CARPETA","extensiones":[],"hijos":[
                    {"id":"3","nombre":"Main","tipo":"ARCHIVO","extensiones":["java"],"hijos":[]}
                  ]}
                ]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }
    }

    // =============================================
    // coincideNombreArchivo — ramas de extensión
    // =============================================

    @Nested
    @DisplayName("Matching de archivos — ramas adicionales")
    class MatchingArchivosExtra {

        @Test
        @DisplayName("Archivo sin extensión (sin punto) coincide si nombre esperado es igual")
        void archivo_sinExtension() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "Makefile");
            String estructura = """
                [{"id":"1","nombre":"Makefile","tipo":"ARCHIVO","extensiones":[],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Archivo sin extensión no coincide con extensión requerida")
        void archivo_sinExtension_conExtensionRequerida() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "Makefile");
            String estructura = """
                [{"id":"1","nombre":"Makefile","tipo":"ARCHIVO","extensiones":["txt"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
        }

        @Test
        @DisplayName("Extensión case-insensitive permite PDF vs pdf")
        void extension_caseInsensitive() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "memoria.PDF");
            String estructura = """
                [{"id":"1","nombre":"memoria","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Múltiples extensiones permitidas, archivo cumple una de ellas")
        void multiples_extensiones() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "documento.docx");
            String estructura = """
                [{"id":"1","nombre":"documento","tipo":"ARCHIVO","extensiones":["pdf","docx","odt"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Múltiples extensiones permitidas, archivo no cumple ninguna")
        void multiples_extensiones_ninguna_coincide() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "documento.txt");
            String estructura = """
                [{"id":"1","nombre":"documento","tipo":"ARCHIVO","extensiones":["pdf","docx","odt"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
        }

        @Test
        @DisplayName("Wildcard '*' con extensión requerida rechaza extensión incorrecta")
        void wildcard_extension_incorrecta() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "foto.png");
            String estructura = """
                [{"id":"1","nombre":"*","tipo":"ARCHIVO","extensiones":["pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isFalse();
        }

        @Test
        @DisplayName("Extensión '*' heredada se interpreta como cualquier extensión")
        void extension_wildcard_heredada() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "informe.pdf");
            String estructura = """
                [{"id":"1","nombre":"informe","tipo":"ARCHIVO","extensiones":["*"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }

        @Test
        @DisplayName("Normaliza extensiones con punto inicial")
        void extension_con_punto_inicial() throws IOException {
            MockMultipartFile zip = crearZip("test.zip", "informe.pdf");
            String estructura = """
                [{"id":"1","nombre":"informe","tipo":"ARCHIVO","extensiones":[".pdf"],"hijos":[]}]
                """;

            ResultadoValidacion result = zipValidationService.validarZip(zip, estructura, false, null);

            assertThat(result.valido()).isTrue();
        }
    }

    // =============================================
    // NodoEstructura record — compact constructor
    // =============================================

    @Nested
    @DisplayName("NodoEstructura record")
    class NodoEstructuraTest {

        @Test
        @DisplayName("Constructor compacto rellena extensiones null con lista vacía")
        void extensiones_null_default() {
            NodoEstructura nodo = new NodoEstructura("1", "test", "ARCHIVO", null, null);

            assertThat(nodo.extensiones()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Constructor compacto rellena hijos null con lista vacía")
        void hijos_null_default() {
            NodoEstructura nodo = new NodoEstructura("1", "test", "CARPETA", null, null);

            assertThat(nodo.hijos()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Constructor compacto mantiene valores no-null")
        void valores_no_null() {
            List<String> exts = List.of("pdf", "docx");
            List<NodoEstructura> hijos = List.of(
                    new NodoEstructura("2", "hijo", "ARCHIVO", null, null));

            NodoEstructura nodo = new NodoEstructura("1", "test", "CARPETA", exts, hijos);

            assertThat(nodo.extensiones()).containsExactly("pdf", "docx");
            assertThat(nodo.hijos()).hasSize(1);
        }
    }

    // =============================================
    // ResultadoValidacion record
    // =============================================

    @Nested
    @DisplayName("ResultadoValidacion record")
    class ResultadoValidacionTest {

        @Test
        @DisplayName("ok() devuelve resultado válido sin errores")
        void ok_sinErrores() {
            ResultadoValidacion result = ResultadoValidacion.ok();

            assertThat(result.valido()).isTrue();
            assertThat(result.errores()).isEmpty();
        }

        @Test
        @DisplayName("error() devuelve resultado inválido con errores")
        void error_conErrores() {
            ResultadoValidacion result = ResultadoValidacion.error(List.of("error1", "error2"));

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).containsExactly("error1", "error2");
        }
    }

    // =============================================
    // Nombre incorrecto + JSON null/vacío → error solo de nombre
    // =============================================

    @Nested
    @DisplayName("Error de nombre con estructura null")
    class ErrorNombreConEstructuraNull {

        @Test
        @DisplayName("Nombre incorrecto con jsonEstructura null devuelve error de nombre")
        void nombre_incorrecto_sinEstructura() throws IOException {
            MockMultipartFile zip = crearZip("malo.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, null, false, "correcto");

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).hasSize(1);
            assertThat(result.errores().getFirst()).contains("correcto.zip");
        }

        @Test
        @DisplayName("Nombre incorrecto + estructura vacía → error solo de nombre")
        void nombre_incorrecto_estructuraVacia() throws IOException {
            MockMultipartFile zip = crearZip("malo.zip", "archivo.txt");

            ResultadoValidacion result = zipValidationService.validarZip(zip, "[]", false, "correcto");

            assertThat(result.valido()).isFalse();
            assertThat(result.errores()).hasSize(1);
            assertThat(result.errores().getFirst()).contains("correcto.zip");
        }
    }

    // =============================================
    // Helper para crear bytes de ZIP (sin nombre de archivo)
    // =============================================

    private byte[] crearZipBytes(String... rutas) throws IOException {
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
        return baos.toByteArray();
    }
}
