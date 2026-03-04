package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.config.OneDriveProperties;
import com.tfg.gestionentregables.entity.MicrosoftToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.MicrosoftTokenRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio que gestiona el flujo OAuth2 Authorization Code con Microsoft Identity Platform.
 * Maneja la generación de URLs de autorización, intercambio de código por tokens,
 * refresco de tokens y almacenamiento seguro.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MicrosoftOAuthService {

    private final OneDriveProperties properties;
    private final MicrosoftTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /** Endpoint de autorización de Microsoft v2.0 */
    private static final String AUTHORIZE_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize";
    /** Endpoint de token de Microsoft v2.0 */
    private static final String TOKEN_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";

    /** Scopes requeridos para acceder a OneDrive del usuario */
    private static final String SCOPES = "Files.ReadWrite User.Read offline_access";

    /**
     * Genera la URL de autorización de Microsoft para iniciar el flujo OAuth2.
     *
     * @param usuarioId ID del usuario de la plataforma
     * @return URL completa de autorización
     */
    public String buildAuthorizationUrl(Long usuarioId) {
        String state = usuarioId + ":" + UUID.randomUUID();

        return String.format(AUTHORIZE_URL, properties.getTenantId()) +
                "?client_id=" + properties.getClientId() +
                "&response_type=code" +
                "&redirect_uri=" + encodeUrl(properties.getRedirectUri()) +
                "&response_mode=query" +
                "&scope=" + encodeUrl(SCOPES) +
                "&state=" + encodeUrl(state) +
                "&prompt=consent";
    }

    /**
     * Intercambia el código de autorización por tokens de acceso y refresco.
     * Almacena los tokens asociados al usuario.
     *
     * @param code  Código de autorización recibido de Microsoft
     * @param state Parámetro state (contiene usuarioId:uuid)
     * @return MicrosoftToken almacenado
     */
    @SuppressWarnings("unchecked")
    public MicrosoftToken exchangeCodeForTokens(String code, String state) {
        // Extraer el usuarioId del state
        Long usuarioId = extractUsuarioIdFromState(state);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        // Preparar la petición de token
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("code", code);
        params.add("redirect_uri", properties.getRedirectUri());
        params.add("grant_type", "authorization_code");
        params.add("scope", SCOPES);

        String tokenUrl = String.format(TOKEN_URL, properties.getTenantId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Error al intercambiar código por tokens con Microsoft");
        }

        Map<String, Object> body = response.getBody();
        return saveTokens(usuario, body);
    }

    /**
     * Refresca el access token usando el refresh token almacenado.
     *
     * @param usuarioId ID del usuario
     * @return Token actualizado, o empty si no hay token almacenado
     */
    @SuppressWarnings("unchecked")
    public Optional<MicrosoftToken> refreshAccessToken(Long usuarioId) {
        Optional<MicrosoftToken> tokenOpt = tokenRepository.findByUsuarioId(usuarioId);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        MicrosoftToken token = tokenOpt.get();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", properties.getClientId());
        params.add("client_secret", properties.getClientSecret());
        params.add("refresh_token", token.getRefreshToken());
        params.add("grant_type", "refresh_token");
        params.add("scope", SCOPES);

        String tokenUrl = String.format(TOKEN_URL, properties.getTenantId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("No se pudo refrescar el token de Microsoft para usuario {}", usuarioId);
                return Optional.empty();
            }

            Map<String, Object> body = response.getBody();

            // Actualizar tokens
            token.setAccessToken((String) body.get("access_token"));
            if (body.containsKey("refresh_token")) {
                token.setRefreshToken((String) body.get("refresh_token"));
            }
            int expiresIn = ((Number) body.get("expires_in")).intValue();
            token.setExpiraEn(LocalDateTime.now().plusSeconds(expiresIn - 60)); // 60s margen
            token.setUltimoRefresco(LocalDateTime.now());

            tokenRepository.save(token);
            log.info("Token de Microsoft refrescado para usuario {}", usuarioId);

            return Optional.of(token);
        } catch (Exception e) {
            log.error("Error al refrescar token de Microsoft para usuario {}: {}", usuarioId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Obtiene un access token válido para el usuario.
     * Si está expirado, lo refresca automáticamente.
     *
     * @param usuarioId ID del usuario
     * @return Access token válido, o empty si no conectado
     */
    public Optional<String> getValidAccessToken(Long usuarioId) {
        Optional<MicrosoftToken> tokenOpt = tokenRepository.findByUsuarioId(usuarioId);
        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        MicrosoftToken token = tokenOpt.get();

        if (token.isExpired()) {
            // Refrescar
            Optional<MicrosoftToken> refreshed = refreshAccessToken(usuarioId);
            if (refreshed.isEmpty()) {
                // El refresh falló, desconectar
                tokenRepository.delete(token);
                return Optional.empty();
            }
            token = refreshed.get();
        }

        return Optional.of(token.getAccessToken());
    }

    /**
     * Comprueba si un usuario tiene OneDrive conectado.
     */
    @Transactional(readOnly = true)
    public boolean isConnected(Long usuarioId) {
        return tokenRepository.existsByUsuarioId(usuarioId);
    }

    /**
     * Obtiene el email de Microsoft vinculado de un usuario.
     */
    @Transactional(readOnly = true)
    public Optional<String> getMicrosoftEmail(Long usuarioId) {
        return tokenRepository.findByUsuarioId(usuarioId)
                .map(MicrosoftToken::getMicrosoftEmail);
    }

    /**
     * Desconecta la cuenta de Microsoft del usuario.
     */
    public void disconnect(Long usuarioId) {
        tokenRepository.deleteByUsuarioId(usuarioId);
        log.info("OneDrive desconectado para usuario {}", usuarioId);
    }

    // ── Métodos privados ──

    @SuppressWarnings("unchecked")
    private MicrosoftToken saveTokens(Usuario usuario, Map<String, Object> tokenResponse) {
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        int expiresIn = ((Number) tokenResponse.get("expires_in")).intValue();

        // Obtener email de Microsoft del perfil del usuario
        String msEmail = fetchMicrosoftEmail(accessToken);

        // Buscar si ya existe un registro
        MicrosoftToken token = tokenRepository.findByUsuarioId(usuario.getId())
                .orElse(MicrosoftToken.builder()
                        .usuario(usuario)
                        .fechaConexion(LocalDateTime.now())
                        .build());

        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiraEn(LocalDateTime.now().plusSeconds(expiresIn - 60));
        token.setScopes(SCOPES);
        token.setMicrosoftEmail(msEmail);
        token.setUltimoRefresco(LocalDateTime.now());

        token = tokenRepository.save(token);
        log.info("Tokens de Microsoft almacenados para usuario {} (ms email: {})", usuario.getId(), msEmail);

        return token;
    }

    /**
     * Obtiene el email de Microsoft usando el access token para /me.
     */
    @SuppressWarnings("unchecked")
    private String fetchMicrosoftEmail(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://graph.microsoft.com/v1.0/me",
                    HttpMethod.GET, request, Map.class);

            if (response.getBody() != null) {
                String email = (String) response.getBody().get("mail");
                if (email == null) {
                    email = (String) response.getBody().get("userPrincipalName");
                }
                return email != null ? email : "unknown@microsoft.com";
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener email de Microsoft: {}", e.getMessage());
        }
        return "unknown@microsoft.com";
    }

    private Long extractUsuarioIdFromState(String state) {
        try {
            String[] parts = state.split(":");
            return Long.parseLong(parts[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("State inválido en callback OAuth2: " + state);
        }
    }

    private String encodeUrl(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
