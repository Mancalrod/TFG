package com.tfg.gestionentregables.controller;
import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.service.SecurityContextUserService;
import com.tfg.gestionentregables.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controlador REST para gestión de usuarios.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Validated
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final SecurityContextUserService securityContextUserService;

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
    }

    @GetMapping("/correo/{correo}")
    public ResponseEntity<UsuarioDTO> obtenerUsuarioPorCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorCorreo(correo));
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody CrearUsuarioDTO dto) {
        UsuarioDTO usuario = usuarioService.crearUsuario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody CrearUsuarioDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/profesor")
    public ResponseEntity<Void> registrarComoProfesor(
            @PathVariable Long id,
            @RequestParam @Positive Long cursoId) {
        usuarioService.registrarComoProfesor(id, cursoId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{id}/estudiante/{grupoId}")
    public ResponseEntity<Void> registrarComoEstudiante(
            @PathVariable Long id,
            @PathVariable Long grupoId) {
        usuarioService.registrarComoEstudiante(id, grupoId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}/es-profesor")
    public ResponseEntity<Boolean> esProfesor(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.esProfesor(id));
    }

    @GetMapping("/{id}/es-estudiante")
    public ResponseEntity<Boolean> esEstudiante(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.esEstudiante(id));
    }

    @DeleteMapping("/{id}/profesor")
    public ResponseEntity<Void> eliminarRolProfesor(@PathVariable Long id) {
        usuarioService.eliminarRolProfesor(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/estudiante")
    public ResponseEntity<Void> eliminarRolEstudiante(@PathVariable Long id) {
        usuarioService.eliminarRolEstudiante(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/estudiante/{grupoId}")
    public ResponseEntity<Void> eliminarEstudianteDeGrupo(
            @PathVariable Long id,
            @PathVariable Long grupoId) {
        usuarioService.eliminarEstudianteDeGrupo(id, grupoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<UsuarioDTO>> listarEstudiantesDeGrupo(@PathVariable Long grupoId) {
        return ResponseEntity.ok(usuarioService.listarEstudiantesDeGrupo(grupoId));
    }

    @GetMapping("/{id}/grupos")
    public ResponseEntity<List<GrupoDTO>> listarGruposDeEstudiante(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.listarGruposDeEstudiante(id));
    }

    @GetMapping("/{id}/profesor-id")
    public ResponseEntity<Long> obtenerProfesorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerProfesorId(id));
    }

    // ==================== PERFIL DE USUARIO ====================

    /**
     * Endpoint para cambiar la contraseña del usuario autenticado.
     * Verifica que el usuario solicitante sea el mismo que el target.
     */
    @PutMapping("/{id}/contrasena")
    public ResponseEntity<Void> cambiarContrasena(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CambiarContrasenaDTO dto,
            Authentication authentication) {
        verificarPermisoUsuario(id, authentication);
        usuarioService.cambiarContrasena(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint para subir/actualizar la foto de perfil.
     * Acepta multipart/form-data con el campo "archivo".
     */
    @PostMapping(value = "/{id}/foto-perfil", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioDTO> subirFotoPerfil(
            @PathVariable @Positive Long id,
            @RequestPart("archivo") MultipartFile archivo,
            Authentication authentication) {
        verificarPermisoUsuario(id, authentication);
        return ResponseEntity.ok(usuarioService.subirFotoPerfil(id, archivo));
    }

    /**
     * Verifica que el usuario autenticado sea el mismo que el target.
     * Previene que un usuario modifique el perfil de otro.
     */
    private void verificarPermisoUsuario(Long targetUserId, Authentication authentication) {
        Long currentUserId = securityContextUserService.getCurrentUserId(authentication);
        if (currentUserId == null || !currentUserId.equals(targetUserId)) {
            throw new AccessDeniedException("No tienes permiso para modificar este perfil");
        }
    }
}
