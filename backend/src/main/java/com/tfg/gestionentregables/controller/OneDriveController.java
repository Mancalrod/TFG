package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.dto.OneDriveConnectionDTO;
import com.tfg.gestionentregables.entity.OneDriveToken;
import com.tfg.gestionentregables.service.OneDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la integración con Microsoft OneDrive.
 * <p>
 * Endpoints:
 * - GET  /api/onedrive/status/{usuarioId}     → Estado de conexión
 * - GET  /api/onedrive/auth-url/{usuarioId}    → URL para iniciar OAuth2
 * - GET  /api/onedrive/callback                → Callback de Microsoft (público)
 * - POST /api/onedrive/disconnect/{usuarioId}  → Desconectar OneDrive
 * - GET  /api/onedrive/enabled                 → Verificar si la integración está habilitada
 */
@RestController
@RequestMapping("/api/onedrive")
@RequiredArgsConstructor
@Slf4j
public class OneDriveController {

    private final OneDriveService oneDriveService;

    /**
     * Verifica si la integración con OneDrive está habilitada en el servidor.
     */
    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Boolean>> isEnabled() {
        return ResponseEntity.ok(Map.of("enabled", oneDriveService.isEnabled()));
    }

    /**
     * Obtiene el estado de conexión de OneDrive para un usuario.
     */
    @GetMapping("/status/{usuarioId}")
    public ResponseEntity<OneDriveConnectionDTO> getConnectionStatus(@PathVariable Long usuarioId) {
        if (!oneDriveService.isEnabled()) {
            return ResponseEntity.ok(OneDriveConnectionDTO.builder()
                    .conectado(false)
                    .integrationEnabled(false)
                    .build());
        }

        OneDriveToken token = oneDriveService.obtenerConexion(usuarioId);

        if (token == null) {
            return ResponseEntity.ok(OneDriveConnectionDTO.builder()
                    .conectado(false)
                    .integrationEnabled(true)
                    .build());
        }

        return ResponseEntity.ok(OneDriveConnectionDTO.builder()
                .conectado(true)
                .integrationEnabled(true)
                .microsoftEmail(token.getMicrosoftEmail())
                .fechaConexion(token.getFechaConexion())
                .fechaUltimoUso(token.getFechaUltimoUso())
                .build());
    }

    /**
     * Genera la URL de autorización de Microsoft para que el usuario conecte su OneDrive.
     * El frontend debe redirigir al usuario a esta URL.
     */
    @GetMapping("/auth-url/{usuarioId}")
    public ResponseEntity<Map<String, String>> getAuthUrl(@PathVariable Long usuarioId) {
        if (!oneDriveService.isEnabled()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La integración con OneDrive no está habilitada"));
        }

        String authUrl = oneDriveService.generarUrlAutorizacion(usuarioId);
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    /**
     * Callback de Microsoft OAuth2.
     * Microsoft redirige aquí después de que el usuario autoriza la aplicación.
     * Este endpoint es público (no requiere JWT).
     *
     * @param code  Código de autorización de Microsoft
     * @param state Contiene el usuarioId
     * @return Redirección al frontend con resultado
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        if (error != null) {
            log.error("Error en callback de OneDrive: {} - {}", error, errorDescription);
            // Redirigir al frontend con error
            return ResponseEntity.ok(generarHtmlRedirect(false, "Error: " + errorDescription));
        }

        if (code == null || state == null) {
            return ResponseEntity.badRequest().body("Parámetros inválidos");
        }

        try {
            Long usuarioId = Long.parseLong(state);
            oneDriveService.procesarCallback(code, usuarioId);

            // Devolver página HTML que cierra la ventana o redirige al frontend
            return ResponseEntity.ok(generarHtmlRedirect(true, "OneDrive conectado correctamente"));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Estado inválido");
        } catch (Exception e) {
            log.error("Error procesando callback de OneDrive: {}", e.getMessage());
            return ResponseEntity.ok(generarHtmlRedirect(false, "Error al conectar: " + e.getMessage()));
        }
    }

    /**
     * Desconecta la cuenta de OneDrive de un usuario.
     */
    @PostMapping("/disconnect/{usuarioId}")
    public ResponseEntity<Map<String, String>> disconnect(@PathVariable Long usuarioId) {
        oneDriveService.desconectar(usuarioId);
        return ResponseEntity.ok(Map.of("message", "OneDrive desconectado correctamente"));
    }

    /**
     * Genera HTML que notifica al frontend (ventana opener) el resultado de la conexión
     * y cierra la ventana popup.
     */
    private String generarHtmlRedirect(boolean success, String message) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><title>OneDrive - Conexión</title></head>
            <body>
                <h2>%s</h2>
                <p>%s</p>
                <p>Esta ventana se cerrará automáticamente...</p>
                <script>
                    if (window.opener) {
                        window.opener.postMessage({
                            type: 'onedrive-auth',
                            success: %s,
                            message: '%s'
                        }, '*');
                        setTimeout(() => window.close(), 2000);
                    } else {
                        // Si no es popup, redirigir al frontend
                        setTimeout(() => {
                            window.location.href = '/';
                        }, 3000);
                    }
                </script>
            </body>
            </html>
            """,
                success ? "✅ Conexión exitosa" : "❌ Error",
                message,
                success,
                message.replace("'", "\\'"));
    }
}
