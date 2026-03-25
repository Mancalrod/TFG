package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.entity.MicrosoftToken;
import com.tfg.gestionentregables.service.MicrosoftOAuthService;
import com.tfg.gestionentregables.service.SecurityContextUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * Controlador REST para el flujo OAuth2 con Microsoft (OneDrive).
 * Gestiona la conexión/desconexión de la cuenta Microsoft del usuario.
 */
@RestController
@RequestMapping("/api/oauth/microsoft")
@RequiredArgsConstructor
@Slf4j
public class MicrosoftOAuthController {

    private final MicrosoftOAuthService oAuthService;
    private final SecurityContextUserService securityContextUserService;

    /**
     * Genera la URL de autorización de Microsoft y la devuelve al frontend.
     * El frontend abrirá esta URL en una ventana/pestaña nueva.
     *
     * @param usuarioId ID del usuario que quiere conectar su OneDrive
     * @return URL de autorización de Microsoft
     */
    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> authorize(@RequestParam Long usuarioId,
                                                         Authentication authentication) {
        assertSameUserOrAdmin(authentication, usuarioId);
        String authUrl = oAuthService.buildAuthorizationUrl(usuarioId);
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    /**
     * Callback de Microsoft tras la autorización del usuario.
     * Intercambia el código por tokens y redirige al frontend.
     *
     * @param code  Código de autorización
     * @param state Parámetro state (contiene usuarioId:uuid)
     * @param error Error de Microsoft (si el usuario canceló o hubo error)
     * @param errorDescription Descripción del error
     * @return Redirección al frontend
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        // Si hubo error (ej: usuario canceló)
        if (error != null) {
            log.warn("OAuth2 Microsoft error: {} - {}", error, errorDescription);
            return ResponseEntity.status(302)
                    .location(URI.create("http://localhost:3000/dashboard?onedrive=error&reason=" + error))
                    .build();
        }

        if (code == null || state == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            MicrosoftToken token = oAuthService.exchangeCodeForTokens(code, state);
            log.info("OneDrive conectado exitosamente para usuario {}", token.getUsuario().getId());

            return ResponseEntity.status(302)
                    .location(URI.create("http://localhost:3000/dashboard?onedrive=success"))
                    .build();
        } catch (Exception e) {
            log.error("Error en callback OAuth2 Microsoft: {}", e.getMessage(), e);
            return ResponseEntity.status(302)
                    .location(URI.create("http://localhost:3000/dashboard?onedrive=error&reason=token_exchange_failed"))
                    .build();
        }
    }

    /**
     * Verifica si el usuario ya tiene su OneDrive conectado.
     *
     * @param usuarioId ID del usuario
     * @return Estado de conexión con email de Microsoft si conectado
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam Long usuarioId,
                                                      Authentication authentication) {
        assertSameUserOrAdmin(authentication, usuarioId);
        boolean connected = oAuthService.isConnected(usuarioId);
        if (connected) {
            String msEmail = oAuthService.getMicrosoftEmail(usuarioId).orElse(null);
            return ResponseEntity.ok(Map.of(
                    "connected", true,
                    "microsoftEmail", msEmail != null ? msEmail : ""
            ));
        }
        return ResponseEntity.ok(Map.of("connected", false, "microsoftEmail", ""));
    }

    /**
     * Desconecta la cuenta de Microsoft del usuario.
     *
     * @param usuarioId ID del usuario
     * @return 204 No Content
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@RequestParam Long usuarioId,
                                           Authentication authentication) {
        assertSameUserOrAdmin(authentication, usuarioId);
        oAuthService.disconnect(usuarioId);
        return ResponseEntity.noContent().build();
    }

    private void assertSameUserOrAdmin(Authentication authentication, Long usuarioId) {
        if (authentication == null) {
            return;
        }
        Long actorId = securityContextUserService.getCurrentUserId(authentication);
        boolean actorEsAdmin = securityContextUserService.hasRole(authentication, "ADMIN");
        if (!actorEsAdmin && actorId != null && !actorId.equals(usuarioId)) {
            throw new AccessDeniedException("No puedes operar sobre la cuenta de otro usuario");
        }
    }
}
