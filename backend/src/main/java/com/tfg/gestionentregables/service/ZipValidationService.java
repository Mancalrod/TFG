package com.tfg.gestionentregables.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Servicio para validar la estructura interna de archivos ZIP
 * contra una estructura esperada definida en el entregable.
 */
@Service
@Slf4j
public class ZipValidationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Nodo de la estructura esperada, parseado del JSON almacenado en Entregable.
     */
    public record NodoEstructura(
            String id,
            String nombre,
            String tipo,             // "ARCHIVO" o "CARPETA"
            List<String> extensiones, // para archivos: extensiones permitidas (vacío = cualquiera)
            List<NodoEstructura> hijos // para carpetas: nodos hijos
    ) {
        public NodoEstructura {
            if (extensiones == null) extensiones = Collections.emptyList();
            if (hijos == null) hijos = Collections.emptyList();
        }
    }

    /**
     * Resultado de la validación con detalles de errores.
     */
    public record ResultadoValidacion(
            boolean valido,
            List<String> errores
    ) {
        public static ResultadoValidacion ok() {
            return new ResultadoValidacion(true, Collections.emptyList());
        }

        public static ResultadoValidacion error(List<String> errores) {
            return new ResultadoValidacion(false, errores);
        }
    }

    /**
     * Valida un archivo ZIP contra el nombre esperado y la estructura esperada.
     *
     * @param archivo         el archivo ZIP subido
     * @param jsonEstructura  JSON con la estructura esperada (array de nodos), puede ser nulo
     * @param estricta        true = solo los archivos definidos, false = al menos esos archivos
     * @param nombreEsperado  nombre esperado del ZIP sin extensión; nulo/"*"/vacío = cualquiera
     * @return resultado de la validación
     */
    public ResultadoValidacion validarZip(MultipartFile archivo, String jsonEstructura,
                                          boolean estricta, String nombreEsperado) {
        List<String> errores = new ArrayList<>();

        // Validar nombre del archivo ZIP
        if (nombreEsperado != null && !nombreEsperado.isBlank() && !"*".equals(nombreEsperado.trim())) {
            String nombreArchivo = archivo.getOriginalFilename();
            if (nombreArchivo != null) {
                // Quitar extensión .zip para comparar
                String nombreSinExt = nombreArchivo.toLowerCase().endsWith(".zip")
                        ? nombreArchivo.substring(0, nombreArchivo.length() - 4)
                        : nombreArchivo;
                if (!nombreSinExt.equalsIgnoreCase(nombreEsperado.trim())) {
                    errores.add("El nombre del archivo ZIP debe ser \"" + nombreEsperado.trim()
                            + ".zip\" (se recibió \"" + nombreArchivo + "\")");
                }
            }
        }

        if (jsonEstructura == null || jsonEstructura.isBlank()) {
            return errores.isEmpty() ? ResultadoValidacion.ok() : ResultadoValidacion.error(errores);
        }

        List<NodoEstructura> estructuraEsperada;
        try {
            estructuraEsperada = objectMapper.readValue(jsonEstructura, new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Error al parsear la estructura ZIP esperada: {}", e.getMessage());
            errores.add("Error interno al leer la estructura esperada del ZIP");
            return ResultadoValidacion.error(errores);
        }

        if (estructuraEsperada.isEmpty()) {
            return errores.isEmpty() ? ResultadoValidacion.ok() : ResultadoValidacion.error(errores);
        }

        // Extraer las rutas del ZIP
        Set<String> rutasZip;
        try {
            rutasZip = extraerRutasZip(archivo);
        } catch (IOException e) {
            log.error("Error al leer el archivo ZIP: {}", e.getMessage());
            errores.add("No se pudo leer el contenido del archivo ZIP");
            return ResultadoValidacion.error(errores);
        }

        // Construir el set de rutas esperadas
        Set<String> rutasEsperadas = new HashSet<>();
        Set<String> carpetasEsperadas = new HashSet<>();
        recogerRutasEsperadas(estructuraEsperada, "", rutasEsperadas, carpetasEsperadas);

        // Validar que cada nodo esperado tiene correspondencia en el ZIP
        validarNodos(estructuraEsperada, "", rutasZip, errores);

        // Si es estricta, verificar que no hay archivos extra
        if (estricta && errores.isEmpty()) {
            Set<String> archivosZip = new HashSet<>();
            for (String ruta : rutasZip) {
                if (!ruta.endsWith("/")) {
                    archivosZip.add(ruta);
                }
            }

            for (String archivoZip : archivosZip) {
                if (!coincideConAlgunaRutaEsperada(archivoZip, estructuraEsperada, "")) {
                    errores.add("Archivo no esperado: " + archivoZip);
                }
            }
        }

        if (errores.isEmpty()) {
            return ResultadoValidacion.ok();
        }
        return ResultadoValidacion.error(errores);
    }

    /**
     * Extrae todas las rutas (archivos y directorios) de un ZIP.
     * Las rutas de directorio terminan en "/".
     */
    private static final int MAX_ENTRIES = 10_000;
    private static final long MAX_TOTAL_SIZE = 100L * 1024 * 1024; // 100 MB

    private Set<String> extraerRutasZip(MultipartFile archivo) throws IOException {
        Set<String> rutas = new HashSet<>();
        try (InputStream is = archivo.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {
            int totalEntries = 0;
            long totalSize = 0;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) { // NOSONAR - protecciones aplicadas abajo
                totalEntries++;
                if (totalEntries > MAX_ENTRIES) {
                    throw new IOException("El archivo ZIP excede el número máximo de entradas permitidas");
                }

                String name = entry.getName();
                // Protección contra Zip Slip: rechazar entradas con path traversal
                Path entryPath = Path.of(name).normalize();
                if (entryPath.isAbsolute() || entryPath.startsWith("..")) {
                    throw new IOException("Entrada ZIP maliciosa detectada: " + name);
                }

                // Protección contra Zip Bomb: limitar tamaño total descomprimido
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = zis.read(buffer)) >= 0) {
                    totalSize += bytesRead;
                    if (totalSize > MAX_TOTAL_SIZE) {
                        throw new IOException("El archivo ZIP excede el tamaño máximo descomprimido permitido");
                    }
                }

                rutas.add(name);
                zis.closeEntry();
            }
        }

        // Detectar carpeta raíz común (ej: "proyecto/src/..." -> quitar "proyecto/")
        return normalizarRutas(rutas);
    }

    /**
     * Si todas las rutas comparten una carpeta raíz común, la elimina.
     * Esto es habitual en ZIPs creados desde control de versiones.
     */
    private Set<String> normalizarRutas(Set<String> rutas) {
        if (rutas.isEmpty()) return rutas;

        // Buscar si hay un prefijo común tipo "carpeta/"
        String prefijo = null;
        for (String ruta : rutas) {
            int idx = ruta.indexOf('/');
            if (idx < 0) {
                // Hay un archivo en la raíz → no hay carpeta raíz común
                return rutas;
            }
            String candidato = ruta.substring(0, idx + 1);
            if (prefijo == null) {
                prefijo = candidato;
            } else if (!prefijo.equals(candidato)) {
                return rutas; // No hay carpeta raíz común
            }
        }

        if (prefijo == null) return rutas;

        // Quitar el prefijo común
        Set<String> normalizadas = new HashSet<>();
        for (String ruta : rutas) {
            String sinPrefijo = ruta.substring(prefijo.length());
            if (!sinPrefijo.isEmpty()) {
                normalizadas.add(sinPrefijo);
            }
        }
        return normalizadas;
    }

    /**
     * Valida recursivamente que los nodos esperados existen en el ZIP.
     */
    private void validarNodos(List<NodoEstructura> nodos, String pathActual,
                              Set<String> rutasZip, List<String> errores) {
        for (NodoEstructura nodo : nodos) {
            if ("CARPETA".equals(nodo.tipo())) {
                validarCarpeta(nodo, pathActual, rutasZip, errores);
            } else {
                validarArchivo(nodo, pathActual, rutasZip, errores);
            }
        }
    }

    private void validarCarpeta(NodoEstructura nodo, String pathActual,
                                Set<String> rutasZip, List<String> errores) {
        boolean nombreCualquiera = "*".equals(nodo.nombre());

        if (nombreCualquiera) {
            // Cualquier carpeta en este nivel debe tener los hijos esperados
            Set<String> carpetasEncontradas = encontrarCarpetasEnNivel(pathActual, rutasZip);
            if (carpetasEncontradas.isEmpty() && !nodo.hijos().isEmpty()) {
                errores.add("Se esperaba al menos una carpeta en: " + (pathActual.isEmpty() ? "raíz" : pathActual));
            }
            for (String carpeta : carpetasEncontradas) {
                validarNodos(nodo.hijos(), carpeta, rutasZip, errores);
            }
        } else {
            String carpetaPath = pathActual + nodo.nombre() + "/";
            boolean existe = rutasZip.stream().anyMatch(r -> r.startsWith(carpetaPath));
            if (!existe) {
                errores.add("Falta la carpeta: " + carpetaPath);
            } else if (!nodo.hijos().isEmpty()) {
                validarNodos(nodo.hijos(), carpetaPath, rutasZip, errores);
            }
        }
    }

    private void validarArchivo(NodoEstructura nodo, String pathActual,
                                Set<String> rutasZip, List<String> errores) {
        boolean nombreCualquiera = "*".equals(nodo.nombre());
        List<String> extensiones = nodo.extensiones();
        boolean extensionCualquiera = extensiones == null || extensiones.isEmpty();

        // Buscar archivos en el nivel actual
        Set<String> archivosEnNivel = encontrarArchivosEnNivel(pathActual, rutasZip);

        boolean encontrado = false;
        for (String archivoZip : archivosEnNivel) {
            String nombreArchivo = archivoZip.substring(pathActual.length());
            if (coincideNombreArchivo(nombreArchivo, nodo.nombre(), extensiones, nombreCualquiera, extensionCualquiera)) {
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            String descripcionEsperado = construirDescripcionArchivo(nodo, pathActual);
            errores.add("Falta archivo: " + descripcionEsperado);
        }
    }

    private boolean coincideNombreArchivo(String nombreReal, String nombreEsperado,
                                          List<String> extensiones, boolean nombreCualquiera,
                                          boolean extensionCualquiera) {
        int dotIdx = nombreReal.lastIndexOf('.');
        String baseName = dotIdx >= 0 ? nombreReal.substring(0, dotIdx) : nombreReal;
        String ext = dotIdx >= 0 ? nombreReal.substring(dotIdx + 1).toLowerCase() : "";

        // Verificar nombre
        if (!nombreCualquiera && !baseName.equalsIgnoreCase(nombreEsperado)) {
            return false;
        }

        // Verificar extensión
        if (!extensionCualquiera) {
            return extensiones.stream().anyMatch(e -> e.equalsIgnoreCase(ext));
        }

        return true;
    }

    private String construirDescripcionArchivo(NodoEstructura nodo, String pathActual) {
        StringBuilder sb = new StringBuilder(pathActual);
        if ("*".equals(nodo.nombre())) {
            sb.append("(cualquier nombre)");
        } else {
            sb.append(nodo.nombre());
        }
        if (nodo.extensiones() != null && !nodo.extensiones().isEmpty()) {
            sb.append(".").append(String.join("|", nodo.extensiones()));
        } else {
            sb.append(".(cualquier extensión)");
        }
        return sb.toString();
    }

    /**
     * Encuentra carpetas directas en un nivel dado del ZIP.
     */
    private Set<String> encontrarCarpetasEnNivel(String pathActual, Set<String> rutasZip) {
        Set<String> carpetas = new HashSet<>();
        for (String ruta : rutasZip) {
            if (ruta.startsWith(pathActual) && ruta.length() > pathActual.length()) {
                String resto = ruta.substring(pathActual.length());
                int slashIdx = resto.indexOf('/');
                if (slashIdx >= 0) {
                    carpetas.add(pathActual + resto.substring(0, slashIdx + 1));
                }
            }
        }
        return carpetas;
    }

    /**
     * Encuentra archivos (no directorios) directos en un nivel dado.
     */
    private Set<String> encontrarArchivosEnNivel(String pathActual, Set<String> rutasZip) {
        Set<String> archivos = new HashSet<>();
        for (String ruta : rutasZip) {
            if (!ruta.endsWith("/") && ruta.startsWith(pathActual)) {
                String resto = ruta.substring(pathActual.length());
                // Solo archivos en este nivel, no en subcarpetas
                if (!resto.contains("/")) {
                    archivos.add(ruta);
                }
            }
        }
        return archivos;
    }

    /**
     * Para modo estricto: verifica si un archivo del ZIP coincide con alguna ruta esperada.
     */
    private boolean coincideConAlgunaRutaEsperada(String archivoZip, List<NodoEstructura> nodos, String pathActual) {
        for (NodoEstructura nodo : nodos) {
            if ("CARPETA".equals(nodo.tipo())) {
                if ("*".equals(nodo.nombre())) {
                    String resto = archivoZip.substring(pathActual.length());
                    int slashIdx = resto.indexOf('/');
                    if (slashIdx >= 0) {
                        String carpeta = pathActual + resto.substring(0, slashIdx + 1);
                        if (coincideConAlgunaRutaEsperada(archivoZip, nodo.hijos(), carpeta)) {
                            return true;
                        }
                    }
                } else {
                    String carpetaPath = pathActual + nodo.nombre() + "/";
                    if (archivoZip.startsWith(carpetaPath)) {
                        if (nodo.hijos().isEmpty()) {
                            return true;
                        }
                        if (coincideConAlgunaRutaEsperada(archivoZip, nodo.hijos(), carpetaPath)) {
                            return true;
                        }
                    }
                }
            } else {
                // Archivo
                if (archivoZip.startsWith(pathActual)) {
                    String nombreArchivo = archivoZip.substring(pathActual.length());
                    if (!nombreArchivo.contains("/")) {
                        boolean nombreCualquiera = "*".equals(nodo.nombre());
                        boolean extensionCualquiera = nodo.extensiones() == null || nodo.extensiones().isEmpty();
                        if (coincideNombreArchivo(nombreArchivo, nodo.nombre(), nodo.extensiones(),
                                nombreCualquiera, extensionCualquiera)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Recoge las rutas esperadas (para logging/debug).
     */
    private void recogerRutasEsperadas(List<NodoEstructura> nodos, String pathActual,
                                        Set<String> rutasArchivos, Set<String> rutasCarpetas) {
        for (NodoEstructura nodo : nodos) {
            if ("CARPETA".equals(nodo.tipo())) {
                String carpeta = pathActual + nodo.nombre() + "/";
                rutasCarpetas.add(carpeta);
                recogerRutasEsperadas(nodo.hijos(), carpeta, rutasArchivos, rutasCarpetas);
            } else {
                rutasArchivos.add(pathActual + nodo.nombre());
            }
        }
    }
}
