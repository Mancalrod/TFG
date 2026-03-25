package com.tfg.gestionentregables.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Servicio para gestión de archivos en Cloudinary.
 * Se usa como almacenamiento persistente en producción (Render).
 * Si Cloudinary no está configurado (desarrollo), isEnabled() devuelve false
 * y el sistema usa almacenamiento local como fallback.
 */
@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final boolean enabled;

    public CloudinaryService(@Autowired(required = false) Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
        this.enabled = cloudinary != null;
        if (enabled) {
            log.info("CloudinaryService habilitado");
        } else {
            log.info("CloudinaryService deshabilitado (usando almacenamiento local)");
        }
    }

    /**
     * Indica si Cloudinary está configurado y disponible.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sube un archivo a Cloudinary.
     *
     * @param archivo  archivo a subir
     * @param carpeta  carpeta dentro de Cloudinary (ej: "entregas/curso-IS/practica-1")
     * @return mapa con "publicId", "url" y "secureUrl"
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> subirArchivo(MultipartFile archivo, String carpeta) {
        if (!enabled) {
            throw new IllegalStateException("Cloudinary no está habilitado");
        }

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "tfg-entregables/" + carpeta,
                            "resource_type", "raw"
                    )
            );

            String publicId = (String) uploadResult.get("public_id");
            String secureUrl = (String) uploadResult.get("secure_url");
            String url = (String) uploadResult.get("url");

            log.info("Archivo subido a Cloudinary: publicId={}, url={}", publicId, secureUrl);

            return Map.of(
                    "publicId", publicId,
                    "url", url != null ? url : "",
                    "secureUrl", secureUrl != null ? secureUrl : ""
            );
        } catch (IOException e) {
            throw new RuntimeException("Error al subir archivo a Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Descarga el contenido de un archivo desde Cloudinary.
     *
     * @param secureUrl URL segura del archivo en Cloudinary
     * @return byte[] con el contenido del archivo
     */
    public byte[] descargarArchivo(String secureUrl) {
        if (!enabled) {
            throw new IllegalStateException("Cloudinary no está habilitado");
        }

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(secureUrl))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return response.body();
            }
            throw new RuntimeException("Error al descargar de Cloudinary. Status: " + response.statusCode());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al descargar archivo de Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un archivo de Cloudinary.
     *
     * @param publicId public_id del archivo en Cloudinary
     */
    @SuppressWarnings("unchecked")
    public void eliminarArchivo(String publicId) {
        if (!enabled) {
            throw new IllegalStateException("Cloudinary no está habilitado");
        }

        try {
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId, ObjectUtils.asMap("resource_type", "raw"));
            log.info("Archivo eliminado de Cloudinary: publicId={}, result={}", publicId, result.get("result"));
        } catch (IOException e) {
            log.warn("Error al eliminar archivo de Cloudinary: publicId={}, error={}", publicId, e.getMessage());
        }
    }
}
