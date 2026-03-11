package com.tfg.gestionentregables.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfg.gestionentregables.config.OneDriveConfig;
import com.tfg.gestionentregables.entity.OneDriveToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.OneDriveTokenRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Servicio para integración con Microsoft OneDrive vía Graph API.
 * <p>
 * Gestiona:
 * - Flujo OAuth2 (autorización, intercambio de tokens, refresh)
 * - Subida de archivos al OneDrive del usuario
 * - Creación de estructura de carpetas organizada
 * - Descarga de archivos desde OneDrive
 * - Desconexión de la cuenta
 */
@Service
@Slf4j
public class OneDriveService {

    private final OneDriveConfig config;
    private final OneDriveTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final RestClient restClient = RestClient.create();

    public OneDriveService(OneDriveConfig config, OneDriveTokenRepository tokenRepository,
                           UsuarioRepository usuarioRepository) {
        this.config = config;
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ========================================
    // FLUJO OAuth2
    // ========================================

    /**
     * Genera la URL de autorización de Microsoft para iniciar el flujo OAuth2.
     *
     * @param usuarioId ID del usuario que quiere conectar su OneDrive
     * @return URL de autorización de Microsoft
     */
    public String generarUrlAutorizacion(Long usuarioId) {
        return config.getAuthorizeUrl() +
                "?client_id=" + config.getClientId() +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(config.getRedirectUri(), StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(config.getScopes(), StandardCharsets.UTF_8) +
                "&response_mode=query" +
                "&state=" + usuarioId;
    }

    /**
     * Procesa el callback de Microsoft después de la autorización.
     * Intercambia el código de autorización por tokens de acceso.
     *
     * @param code      Código de autorización recibido de Microsoft
     * @param usuarioId ID del usuario (pasado en state)
     * @return OneDriveToken guardado en la base de datos
     */
    @Transactional
    public OneDriveToken procesarCallback(String code, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        // Intercambiar código por tokens
        JsonNode tokenResponse = intercambiarCodigoPorTokens(code);

        String accessToken = tokenResponse.get("access_token").asText();
        String refreshToken = tokenResponse.get("refresh_token").asText();
        int expiresIn = tokenResponse.get("expires_in").asInt();

        // Obtener información del perfil de Microsoft
        JsonNode profile = obtenerPerfilMicrosoft(accessToken);

        // Crear o actualizar token
        OneDriveToken token = tokenRepository.findByUsuarioId(usuarioId)
                .orElse(OneDriveToken.builder()
                        .usuario(usuario)
                        .fechaConexion(LocalDateTime.now())
                        .build());

        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiraEn(LocalDateTime.now().plusSeconds(expiresIn));
        token.setMicrosoftUserId(profile.has("id") ? profile.get("id").asText() : null);
        token.setMicrosoftEmail(profile.has("mail") ? profile.get("mail").asText() :
                profile.has("userPrincipalName") ? profile.get("userPrincipalName").asText() : null);
        token.setFechaUltimoUso(LocalDateTime.now());

        token = tokenRepository.save(token);

        // Crear carpeta raíz en OneDrive
        crearCarpetaSiNoExiste(accessToken, config.getRootFolder());

        log.info("OneDrive conectado para usuario {} (Microsoft: {})", usuarioId, token.getMicrosoftEmail());
        return token;
    }

    /**
     * Intercambia el código de autorización por tokens.
     */
    private JsonNode intercambiarCodigoPorTokens(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", config.getRedirectUri());
        body.add("grant_type", "authorization_code");
        body.add("scope", config.getScopes());

        try {
            String response = restClient.post()
                    .uri(config.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return objectMapper.readTree(response);
        } catch (RestClientException | IOException e) {
            log.error("Error al intercambiar código por tokens: {}", e.getMessage());
            throw new RuntimeException("Error al conectar con Microsoft: " + e.getMessage(), e);
        }
    }

    /**
     * Refresca el access token usando el refresh token almacenado.
     *
     * @param token Token a refrescar
     * @return Token actualizado
     */
    public OneDriveToken refrescarToken(OneDriveToken token) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("refresh_token", token.getRefreshToken());
        body.add("grant_type", "refresh_token");
        body.add("scope", config.getScopes());

        try {
            String response = restClient.post()
                    .uri(config.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode tokenResponse = objectMapper.readTree(response);

            token.setAccessToken(tokenResponse.get("access_token").asText());
            if (tokenResponse.has("refresh_token")) {
                token.setRefreshToken(tokenResponse.get("refresh_token").asText());
            }
            token.setExpiraEn(LocalDateTime.now().plusSeconds(tokenResponse.get("expires_in").asInt()));
            token.setFechaUltimoUso(LocalDateTime.now());

            return tokenRepository.save(token);
        } catch (RestClientException | IOException e) {
            log.error("Error al refrescar token de OneDrive para usuario {}: {}",
                    token.getUsuario().getId(), e.getMessage());
            throw new RuntimeException("Error al refrescar token de OneDrive", e);
        }
    }

    /**
     * Obtiene un access token válido para un usuario, refrescándolo si es necesario.
     *
     * @param usuarioId ID del usuario
     * @return Access token válido
     */
    public String obtenerAccessTokenValido(Long usuarioId) {
        OneDriveToken token = tokenRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("El usuario no tiene OneDrive conectado"));

        if (token.isExpired()) {
            token = refrescarToken(token);
        }

        return token.getAccessToken();
    }

    // ========================================
    // OPERACIONES CON ARCHIVOS
    // ========================================

    /**
     * Sube un archivo al OneDrive de un usuario en una ruta organizada.
     * Estructura: TFG-Entregables/{cursoTitulo}/{actividadTitulo}/{entregableTitulo}/{estudianteNombre}/archivo
     *
     * @param usuarioId         ID del usuario dueño del OneDrive
     * @param archivo           Archivo a subir
     * @param cursoTitulo       Título del curso
     * @param actividadTitulo   Título de la actividad
     * @param entregableTitulo  Título del entregable
     * @param estudianteNombre  Nombre del estudiante (para organizar en carpetas)
     * @param nombreArchivo     Nombre del archivo a guardar
     * @return Mapa con fileId, webUrl y downloadUrl del archivo subido
     */
    public Map<String, String> subirArchivo(Long usuarioId, MultipartFile archivo,
                                             String cursoTitulo, String actividadTitulo,
                                             String entregableTitulo, String estudianteNombre,
                                             String nombreArchivo) {
        String accessToken = obtenerAccessTokenValido(usuarioId);

        // Construir ruta en OneDrive: TFG-Entregables/Curso/Actividad/Entregable/Alumno/archivo.ext
        String rutaOneDrive = String.format("%s/%s/%s/%s/%s/%s",
                config.getRootFolder(),
                sanitizarNombreCarpeta(cursoTitulo),
                sanitizarNombreCarpeta(actividadTitulo),
                sanitizarNombreCarpeta(entregableTitulo),
                sanitizarNombreCarpeta(estudianteNombre),
                nombreArchivo);

        return subirArchivoAOneDrive(accessToken, rutaOneDrive, archivo);
    }

    /**
     * Sube un archivo a OneDrive usando la API de Microsoft Graph.
     * Usa upload simple para archivos < 4MB, upload session para archivos más grandes.
     *
     * @param accessToken Token de acceso válido
     * @param rutaOneDrive Ruta completa del archivo en OneDrive
     * @param archivo Archivo a subir
     * @return Mapa con fileId, webUrl del archivo subido
     */
    private Map<String, String> subirArchivoAOneDrive(String accessToken, String rutaOneDrive,
                                                       MultipartFile archivo) {
        try {
            String encodedPath = URLEncoder.encode(rutaOneDrive, StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("%2F", "/");

            String url = config.getGraphApiUrl() + "/me/drive/root:/" + encodedPath + ":/content";

            byte[] content = archivo.getBytes();

            if (content.length > 4 * 1024 * 1024) {
                // Para archivos > 4MB, usar upload session
                return subirArchivoGrande(accessToken, rutaOneDrive, archivo);
            }

            String response = restClient.put()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", archivo.getContentType() != null ?
                            archivo.getContentType() : "application/octet-stream")
                    .body(content)
                    .retrieve()
                    .body(String.class);

            JsonNode responseJson = objectMapper.readTree(response);

            String fileId = responseJson.get("id").asText();
            String webUrl = responseJson.has("webUrl") ? responseJson.get("webUrl").asText() : null;

            log.info("Archivo subido a OneDrive: {} (ID: {})", rutaOneDrive, fileId);

            return Map.of(
                    "fileId", fileId,
                    "webUrl", webUrl != null ? webUrl : ""
            );
        } catch (RestClientException | IOException e) {
            log.error("Error al subir archivo a OneDrive: {}", e.getMessage());
            throw new RuntimeException("Error al subir archivo a OneDrive: " + e.getMessage(), e);
        }
    }

    /**
     * Sube archivos grandes (>4MB) usando upload sessions de Microsoft Graph.
     */
    private Map<String, String> subirArchivoGrande(String accessToken, String rutaOneDrive,
                                                    MultipartFile archivo) {
        try {
            String encodedPath = URLEncoder.encode(rutaOneDrive, StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("%2F", "/");

            // 1. Crear upload session
            String createSessionUrl = config.getGraphApiUrl() +
                    "/me/drive/root:/" + encodedPath + ":/createUploadSession";

            String sessionBody = """
                {
                    "item": {
                        "@microsoft.graph.conflictBehavior": "replace"
                    }
                }
                """;

            String sessionResponse = restClient.post()
                    .uri(createSessionUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(sessionBody)
                    .retrieve()
                    .body(String.class);

            JsonNode session = objectMapper.readTree(sessionResponse);
            String uploadUrl = session.get("uploadUrl").asText();

            // 2. Subir archivo por bloques de 5MB
            byte[] content = archivo.getBytes();
            int chunkSize = 5 * 1024 * 1024; // 5MB
            int totalSize = content.length;
            JsonNode lastResponse = null;

            for (int offset = 0; offset < totalSize; offset += chunkSize) {
                int end = Math.min(offset + chunkSize, totalSize);
                byte[] chunk = new byte[end - offset];
                System.arraycopy(content, offset, chunk, 0, end - offset);

                String contentRange = String.format("bytes %d-%d/%d", offset, end - 1, totalSize);

                String chunkResponse = restClient.put()
                        .uri(uploadUrl)
                        .header("Content-Range", contentRange)
                        .header("Content-Length", String.valueOf(chunk.length))
                        .body(chunk)
                        .retrieve()
                        .body(String.class);

                lastResponse = objectMapper.readTree(chunkResponse);
            }

            if (lastResponse != null && lastResponse.has("id")) {
                String fileId = lastResponse.get("id").asText();
                String webUrl = lastResponse.has("webUrl") ? lastResponse.get("webUrl").asText() : "";

                log.info("Archivo grande subido a OneDrive: {} (ID: {})", rutaOneDrive, fileId);
                return Map.of("fileId", fileId, "webUrl", webUrl);
            }

            throw new RuntimeException("No se recibió respuesta final de la subida");
        } catch (RestClientException | IOException e) {
            log.error("Error al subir archivo grande a OneDrive: {}", e.getMessage());
            throw new RuntimeException("Error al subir archivo grande a OneDrive: " + e.getMessage(), e);
        }
    }

    /**
     * Descarga un archivo desde OneDrive.
     *
     * @param usuarioId ID del usuario dueño del OneDrive
     * @param fileId    ID del archivo en OneDrive
     * @return Contenido del archivo como byte[]
     */
    public byte[] descargarArchivo(Long usuarioId, String fileId) {
        String accessToken = obtenerAccessTokenValido(usuarioId);

        try {
            String url = config.getGraphApiUrl() + "/me/drive/items/" + fileId + "/content";

            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException e) {
            log.error("Error al descargar archivo de OneDrive (fileId: {}): {}", fileId, e.getMessage());
            throw new RuntimeException("Error al descargar archivo de OneDrive: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene una URL de descarga temporal (pre-autenticada) para un archivo.
     *
     * @param usuarioId ID del usuario dueño del OneDrive
     * @param fileId    ID del archivo en OneDrive
     * @return URL de descarga temporal
     */
    public String obtenerUrlDescarga(Long usuarioId, String fileId) {
        String accessToken = obtenerAccessTokenValido(usuarioId);

        try {
            String url = config.getGraphApiUrl() + "/me/drive/items/" + fileId;

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode responseJson = objectMapper.readTree(response);

            if (responseJson.has("@microsoft.graph.downloadUrl")) {
                return responseJson.get("@microsoft.graph.downloadUrl").asText();
            }

            // Fallback: usar el content endpoint
            return config.getGraphApiUrl() + "/me/drive/items/" + fileId + "/content";
        } catch (RestClientException | IOException e) {
            log.error("Error al obtener URL de descarga: {}", e.getMessage());
            throw new RuntimeException("Error al obtener URL de descarga de OneDrive: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un archivo de OneDrive.
     *
     * @param usuarioId ID del usuario dueño del OneDrive
     * @param fileId    ID del archivo en OneDrive
     */
    public void eliminarArchivo(Long usuarioId, String fileId) {
        String accessToken = obtenerAccessTokenValido(usuarioId);

        try {
            String url = config.getGraphApiUrl() + "/me/drive/items/" + fileId;

            restClient.delete()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Archivo eliminado de OneDrive (fileId: {})", fileId);
        } catch (RestClientException e) {
            log.warn("Error al eliminar archivo de OneDrive (fileId: {}): {}", fileId, e.getMessage());
            // No lanzar excepción si no se puede eliminar (puede ya no existir)
        }
    }

    // ========================================
    // OPERACIONES CON CARPETAS
    // ========================================

    /**
     * Crea una carpeta en OneDrive si no existe.
     */
    private void crearCarpetaSiNoExiste(String accessToken, String nombreCarpeta) {
        try {
            String url = config.getGraphApiUrl() + "/me/drive/root/children";

            String body = String.format("""
                {
                    "name": "%s",
                    "folder": {},
                    "@microsoft.graph.conflictBehavior": "fail"
                }
                """, nombreCarpeta);

            restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            log.info("Carpeta '{}' creada en OneDrive", nombreCarpeta);
        } catch (RestClientException e) {
            // La carpeta puede ya existir (conflicto), lo cual es normal
            log.debug("La carpeta '{}' ya existe o no se pudo crear: {}", nombreCarpeta, e.getMessage());
        }
    }

    // ========================================
    // GESTIÓN DE CONEXIÓN
    // ========================================

    /**
     * Verifica si un usuario tiene OneDrive conectado.
     */
    @Transactional(readOnly = true)
    public boolean estaConectado(Long usuarioId) {
        return tokenRepository.existsByUsuarioId(usuarioId);
    }

    /**
     * Obtiene información de la conexión OneDrive de un usuario.
     */
    @Transactional(readOnly = true)
    public OneDriveToken obtenerConexion(Long usuarioId) {
        return tokenRepository.findByUsuarioId(usuarioId).orElse(null);
    }

    /**
     * Desconecta la cuenta de OneDrive de un usuario.
     */
    @Transactional
    public void desconectar(Long usuarioId) {
        tokenRepository.deleteByUsuarioId(usuarioId);
        log.info("OneDrive desconectado para usuario {}", usuarioId);
    }

    /**
     * Obtiene el perfil del usuario de Microsoft.
     */
    private JsonNode obtenerPerfilMicrosoft(String accessToken) {
        try {
            String url = config.getGraphApiUrl() + "/me";

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            return objectMapper.readTree(response);
        } catch (RestClientException | IOException e) {
            log.warn("No se pudo obtener perfil de Microsoft: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    /**
     * Verifica si la integración con OneDrive está habilitada.
     */
    public boolean isEnabled() {
        return config.isEnabled() &&
                config.getClientId() != null && !config.getClientId().isBlank() &&
                config.getClientSecret() != null && !config.getClientSecret().isBlank();
    }

    // ========================================
    // UTILIDADES
    // ========================================

    /**
     * Sanitiza el nombre de una carpeta para OneDrive.
     * Elimina caracteres no permitidos en nombres de archivo/carpeta.
     */
    private String sanitizarNombreCarpeta(String nombre) {
        if (nombre == null) return "Sin-nombre";
        // OneDrive no permite: " * : < > ? / \ |
        return nombre.replaceAll("[\"*:<>?/\\\\|]", "_")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
