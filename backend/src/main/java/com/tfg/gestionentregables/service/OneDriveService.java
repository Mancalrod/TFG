package com.tfg.gestionentregables.service;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.tfg.gestionentregables.config.OneDriveProperties;
import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;

/**
 * Servicio para interactuar con OneDrive mediante Microsoft Graph API.
 * Usa tokens delegados (OAuth2 Authorization Code) obtenidos del usuario.
 * Cada operación recibe un access token del usuario correspondiente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OneDriveService {

    private final OneDriveProperties properties;

    /** Umbral para usar upload session en vez de simple upload (4 MB) */
    private static final long LARGE_FILE_THRESHOLD = 4 * 1024 * 1024;

    /**
     * Verifica si la integración con OneDrive está habilitada en la configuración.
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * Sube un archivo al OneDrive del usuario autenticado.
     *
     * @param accessToken Token OAuth2 del usuario
     * @param folderPath  Ruta de la carpeta (ej: "Entregas/IS-301/Práctica 1")
     * @param fileName    Nombre del archivo
     * @param content     Contenido del archivo
     * @param size        Tamaño en bytes
     * @return Información del archivo subido
     */
    public OneDriveFileInfo uploadFile(String accessToken, String folderPath,
                                       String fileName, byte[] content, long size) {
        try {
            GraphServiceClient client = buildClient(accessToken);

            String safeFolderPath = sanitizePath(folderPath);
            String safeFileName = sanitizeFileName(fileName);
            String fullPath = safeFolderPath + "/" + safeFileName;

            log.debug("Uploading file to OneDrive: path={}", fullPath);

            // Obtener driveId del usuario autenticado ("me")
            String driveId = getUserDriveId(client);

            DriveItem driveItem;
            if (size <= LARGE_FILE_THRESHOLD) {
                driveItem = uploadSmallFile(client, driveId, fullPath, content);
            } else {
                driveItem = uploadLargeFile(client, driveId, fullPath, content, size);
            }

            OneDriveFileInfo info = new OneDriveFileInfo(
                    driveItem.getId(),
                    driveItem.getWebUrl(),
                    driveItem.getName(),
                    driveItem.getSize()
            );

            log.info("File uploaded to OneDrive: itemId={}, url={}", info.itemId(), info.webUrl());
            return info;

        } catch (Exception e) {
            log.error("Error uploading file to OneDrive: {}", e.getMessage(), e);
            throw new RuntimeException("Error al subir archivo a OneDrive: " + e.getMessage(), e);
        }
    }

    /**
     * Descarga un archivo del OneDrive del usuario.
     *
     * @param accessToken Token OAuth2 del usuario propietario
     * @param itemId      ID del item en OneDrive
     * @return InputStream con el contenido
     */
    public InputStream downloadFile(String accessToken, String itemId) {
        try {
            GraphServiceClient client = buildClient(accessToken);
            String driveId = getUserDriveId(client);

            log.debug("Downloading file from OneDrive: itemId={}", itemId);
            return client.drives().byDriveId(driveId)
                    .items().byDriveItemId(itemId)
                    .content().get();
        } catch (Exception e) {
            log.error("Error downloading file from OneDrive: {}", e.getMessage(), e);
            throw new RuntimeException("Error al descargar archivo de OneDrive: " + e.getMessage(), e);
        }
    }

    /**
     * Construye la ruta de carpeta para la entrega del alumno.
     * Formato: {studentFolder}/{codigoCurso}/{tituloActividad}/{tituloEntregable}/v{version}
     */
    public String buildStudentFolderPath(String codigoCurso, String tituloActividad,
                                         String tituloEntregable, int version) {
        return String.join("/",
                properties.getStudentFolder(),
                sanitizePath(codigoCurso),
                sanitizePath(tituloActividad),
                sanitizePath(tituloEntregable),
                "v" + version
        );
    }

    /**
     * Construye la ruta de carpeta para la copia en el OneDrive del profesor.
     * Formato: {professorFolder}/{codigoCurso}/{tituloActividad}/{tituloEntregable}/{nombreEstudiante}_v{version}
     */
    public String buildProfessorFolderPath(String codigoCurso, String tituloActividad,
                                           String tituloEntregable,
                                           String nombreEstudiante, int version) {
        return String.join("/",
                properties.getProfessorFolder(),
                sanitizePath(codigoCurso),
                sanitizePath(tituloActividad),
                sanitizePath(tituloEntregable),
                sanitizePath(nombreEstudiante) + "_v" + version
        );
    }

    // ══════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS
    // ══════════════════════════════════════════════════════════════

    /**
     * Crea un GraphServiceClient usando el access token delegado del usuario.
     */
    private GraphServiceClient buildClient(String accessToken) {
        TokenCredential credential = (request) ->
                Mono.just(new AccessToken(accessToken, OffsetDateTime.now().plusHours(1)));

        return new GraphServiceClient(credential, "https://graph.microsoft.com/.default");
    }

    /**
     * Obtiene el driveId del usuario autenticado (usa /me/drive).
     */
    private String getUserDriveId(GraphServiceClient client) {
        var drive = client.me().drive().get();
        if (drive == null || drive.getId() == null) {
            throw new RuntimeException("No se pudo obtener el drive del usuario");
        }
        return drive.getId();
    }

    private DriveItem uploadSmallFile(GraphServiceClient client, String driveId,
                                      String fullPath, byte[] content) {
        String itemPath = "root:/" + fullPath + ":";
        return client.drives().byDriveId(driveId)
                .items().byDriveItemId(itemPath)
                .content()
                .put(new ByteArrayInputStream(content));
    }

    private DriveItem uploadLargeFile(GraphServiceClient client, String driveId,
                                      String fullPath, byte[] content, long size) {
        String itemPath = "root:/" + fullPath + ":";

        var uploadSessionBody =
                new com.microsoft.graph.drives.item.items.item.createuploadsession.CreateUploadSessionPostRequestBody();

        UploadSession uploadSession = client.drives().byDriveId(driveId)
                .items().byDriveItemId(itemPath)
                .createUploadSession()
                .post(uploadSessionBody);

        if (uploadSession == null || uploadSession.getUploadUrl() == null) {
            throw new RuntimeException("No se pudo crear la sesión de carga en OneDrive");
        }

        // Fallback: subir contenido completo via content PUT
        return client.drives().byDriveId(driveId)
                .items().byDriveItemId(itemPath)
                .content()
                .put(new ByteArrayInputStream(content));
    }

    private String sanitizePath(String path) {
        if (path == null) return "sin-nombre";
        return path
                .replace("\\", "/")
                .replaceAll("[<>:\"|?*#%]", "_")
                .replaceAll("/+", "/")
                .replaceAll("^/|/$", "")
                .trim();
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "archivo";
        return fileName
                .replaceAll("[<>:\"|?*#%/\\\\]", "_")
                .trim();
    }

    /**
     * Registro inmutable con la información de un archivo subido a OneDrive.
     */
    public record OneDriveFileInfo(
            String itemId,
            String webUrl,
            String fileName,
            Long size
    ) {}
}
