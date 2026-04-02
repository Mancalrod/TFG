package com.tfg.gestionentregables.controller;

import com.tfg.gestionentregables.dto.NotificacionDTO;
import com.tfg.gestionentregables.dto.PreferenciaNotificacionDTO;
import com.tfg.gestionentregables.service.NotificacionService;
import com.tfg.gestionentregables.service.SecurityContextUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para notificaciones del usuario autenticado.
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final SecurityContextUserService securityContextUserService;

    @GetMapping
    public ResponseEntity<List<NotificacionDTO>> listarNotificaciones(Authentication authentication) {
        Long usuarioId = securityContextUserService.getCurrentUserId(authentication);
        return ResponseEntity.ok(notificacionService.obtenerNotificaciones(usuarioId));
    }

    @PutMapping("/{id}/leida")
    public ResponseEntity<Void> marcarComoLeida(@PathVariable Long id, Authentication authentication) {
        Long usuarioId = securityContextUserService.getCurrentUserId(authentication);
        notificacionService.marcarComoLeida(id, usuarioId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/no-leidas/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(Authentication authentication) {
        Long usuarioId = securityContextUserService.getCurrentUserId(authentication);
        Long count = notificacionService.contarNoLeidas(usuarioId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/preferencias")
    public ResponseEntity<PreferenciaNotificacionDTO> obtenerPreferencias(Authentication authentication) {
        Long usuarioId = securityContextUserService.getCurrentUserId(authentication);
        return ResponseEntity.ok(notificacionService.obtenerPreferencias(usuarioId));
    }

    @PutMapping("/preferencias")
    public ResponseEntity<PreferenciaNotificacionDTO> actualizarPreferencias(
            @Valid @RequestBody PreferenciaNotificacionDTO dto,
            Authentication authentication) {
        Long usuarioId = securityContextUserService.getCurrentUserId(authentication);
        return ResponseEntity.ok(notificacionService.actualizarPreferencias(usuarioId, dto));
    }
}
