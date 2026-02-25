package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * Servicio para gestión de usuarios.
 * Implementa operaciones SYSOP-001 y relacionadas.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ProfesorRepository profesorRepository;
    private final EstudianteRepository estudianteRepository;
    private final GrupoRepository grupoRepository;
    private final EntityMapper mapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * SYSOP-001: Obtiene un usuario por su ID.
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));
        return mapper.toDTO(usuario);
    }

    /**
     * Obtiene un usuario por correo electrónico.
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correo)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con correo: " + correo));
        return mapper.toDTO(usuario);
    }

    /**
     * Lista todos los usuarios.
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Crea un nuevo usuario.
     */
    public UsuarioDTO crearUsuario(CrearUsuarioDTO dto) {
        if (usuarioRepository.existsByCorreoElectronico(dto.getCorreoElectronico())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo electrónico");
        }

        Usuario usuario = Usuario.builder()
                .nombre(dto.getNombre())
                .telefono(dto.getTelefono())
                .correoElectronico(dto.getCorreoElectronico())
                .contrasena(passwordEncoder.encode(dto.getContrasena()))
                .esAdmin(dto.getEsAdmin() != null ? dto.getEsAdmin() : false)
                .build();

        usuario = usuarioRepository.save(usuario);
        return mapper.toDTO(usuario);
    }

    /**
     * Actualiza un usuario existente.
     */
    public UsuarioDTO actualizarUsuario(Long id, CrearUsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));

        // Verificar si el nuevo correo ya existe en otro usuario
        if (!usuario.getCorreoElectronico().equals(dto.getCorreoElectronico()) &&
                usuarioRepository.existsByCorreoElectronico(dto.getCorreoElectronico())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo electrónico");
        }

        usuario.setNombre(dto.getNombre());
        usuario.setTelefono(dto.getTelefono());
        usuario.setCorreoElectronico(dto.getCorreoElectronico());
        if (dto.getContrasena() != null && !dto.getContrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        }
        if (dto.getEsAdmin() != null) {
            usuario.setEsAdmin(dto.getEsAdmin());
        }

        usuario = usuarioRepository.save(usuario);
        return mapper.toDTO(usuario);
    }

    /**
     * Elimina un usuario.
     */
    public void eliminarUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new EntityNotFoundException("Usuario no encontrado con ID: " + id);
        }
        usuarioRepository.deleteById(id);
    }

    /**
     * Registra un usuario como profesor.
     */
    public void registrarComoProfesor(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        if (profesorRepository.existsByUsuarioId(usuarioId)) {
            throw new IllegalStateException("El usuario ya está registrado como profesor");
        }

        Profesor profesor = Profesor.builder()
                .usuario(usuario)
                .build();
        profesorRepository.save(profesor);
    }

    /**
     * Registra un usuario como estudiante en un grupo.
     */
    public void registrarComoEstudiante(Long usuarioId, Long grupoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado con ID: " + grupoId));

        if (estudianteRepository.existsByUsuarioId(usuarioId)) {
            throw new IllegalStateException("El usuario ya está registrado como estudiante");
        }

        Estudiante estudiante = Estudiante.builder()
                .usuario(usuario)
                .grupo(grupo)
                .build();
        estudianteRepository.save(estudiante);
    }

    /**
     * Verifica si un usuario es profesor.
     */
    @Transactional(readOnly = true)
    public boolean esProfesor(Long usuarioId) {
        return profesorRepository.existsByUsuarioId(usuarioId);
    }

    /**
     * Verifica si un usuario es estudiante.
     */
    @Transactional(readOnly = true)
    public boolean esEstudiante(Long usuarioId) {
        return estudianteRepository.existsByUsuarioId(usuarioId);
    }

    /**
     * Obtiene el ID de profesor dado un usuario ID.
     */
    @Transactional(readOnly = true)
    public Long obtenerProfesorId(Long usuarioId) {
        Profesor profesor = profesorRepository.findFirstByUsuarioId(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("No es profesor"));
        return profesor.getId();
    }

    /**
     * Obtiene el ID de estudiante dado un usuario ID.
     */
    @Transactional(readOnly = true)
    public Long obtenerEstudianteId(Long usuarioId) {
        Estudiante estudiante = estudianteRepository.findFirstByUsuarioId(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("No es estudiante"));
        return estudiante.getId();
    }
}
