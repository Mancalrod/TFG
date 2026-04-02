package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Servicio para gestión de usuarios.
 * Implementa operaciones SYSOP-001 y relacionadas.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UsuarioService {

    private static final String USUARIO_NOT_FOUND = "Usuario no encontrado con ID: ";

    private static final Set<String> MIME_TYPES_PERMITIDOS = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FOTO_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final String PROFILE_PHOTOS_FOLDER = "profile-photos";

    private final UsuarioRepository usuarioRepository;
    private final ProfesorRepository profesorRepository;
    private final EstudianteRepository estudianteRepository;
    private final GrupoRepository grupoRepository;
    private final EntityMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    @Value("${app.upload.dir:${user.home}/tfg-uploads}")
    private String uploadBaseDir;

    /**
     * SYSOP-001: Obtiene un usuario por su ID.
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(USUARIO_NOT_FOUND + id));
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
                .esAdmin(Boolean.TRUE.equals(dto.getEsAdmin()))
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
            throw new EntityNotFoundException(USUARIO_NOT_FOUND + id);
        }
        usuarioRepository.deleteById(id);
    }

    /**
     * Cambia la contraseña de un usuario.
     * Verifica la contraseña actual, valida la fortaleza de la nueva y hace hash con BCrypt.
     */
    public void cambiarContrasena(Long usuarioId, CambiarContrasenaDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException(USUARIO_NOT_FOUND + usuarioId));

        // Verificar contraseña actual
        if (!passwordEncoder.matches(dto.getContrasenaActual(), usuario.getContrasena())) {
            throw new AccessDeniedException("La contraseña actual no es correcta");
        }

        // No permitir que la nueva sea igual a la actual
        if (passwordEncoder.matches(dto.getContrasenaNueva(), usuario.getContrasena())) {
            throw new IllegalArgumentException("La nueva contraseña no puede ser igual a la actual");
        }

        usuario.setContrasena(passwordEncoder.encode(dto.getContrasenaNueva()));
        usuarioRepository.save(usuario);
        log.info("Contraseña cambiada para usuario ID: {}", usuarioId);
    }

    /**
     * Sube una foto de perfil para el usuario.
     * Valida estrictamente el MIME type (real, no solo extensión) y el tamaño.
     * Previene subida de webshells o archivos maliciosos.
     */
    public UsuarioDTO subirFotoPerfil(Long usuarioId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("No se ha proporcionado ningún archivo");
        }

        // Validación de tamaño
        if (archivo.getSize() > MAX_FOTO_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar los 2 MB");
        }

        // Validación estricta de MIME type (verificación real del contenido, no solo extensión)
        String contentType = archivo.getContentType();
        if (contentType == null || !MIME_TYPES_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Tipo de archivo no permitido. Solo se aceptan: JPEG, PNG, WebP, GIF");
        }

        try {
            byte[] firma = archivo.getBytes();
            if (!esFirmaImagenValida(firma)) {
                throw new IllegalArgumentException("El archivo no contiene una imagen válida");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo procesar la imagen subida");
        }

        // Validar extensión del nombre del archivo
        String originalFilename = archivo.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isBlank()) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
                throw new IllegalArgumentException("El archivo debe incluir una extensión válida");
            }
            String extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
            Set<String> extensionesPermitidas = Set.of("jpg", "jpeg", "png", "webp", "gif");
            if (!extensionesPermitidas.contains(extension)) {
                throw new IllegalArgumentException(
                    "Extensión de archivo no permitida. Solo se aceptan: jpg, jpeg, png, webp, gif");
            }
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException(USUARIO_NOT_FOUND + usuarioId));

        String url = cloudinaryService.isEnabled()
            ? subirFotoCloudinary(usuarioId, archivo)
            : guardarFotoLocal(usuarioId, archivo, originalFilename, contentType);

        if (url.isBlank()) {
            throw new IllegalStateException("No se pudo obtener la URL de la imagen subida");
        }
        usuario.setFotoPerfilUrl(url);
        usuario = usuarioRepository.save(usuario);

        log.info("Foto de perfil actualizada para usuario ID: {}", usuarioId);
        return mapper.toDTO(usuario);
    }

    private String subirFotoCloudinary(Long usuarioId, MultipartFile archivo) {
        Map<String, String> upload = cloudinaryService.subirArchivo(archivo, PROFILE_PHOTOS_FOLDER + "/" + usuarioId);
        return upload.getOrDefault("secureUrl", upload.getOrDefault("url", ""));
    }

    private String guardarFotoLocal(Long usuarioId,
                                    MultipartFile archivo,
                                    String originalFilename,
                                    String contentType) {
        String extension = resolverExtension(originalFilename, contentType);

        try {
            Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
            Path fotoDir = basePath.resolve(PROFILE_PHOTOS_FOLDER).resolve(String.valueOf(usuarioId)).normalize();
            Files.createDirectories(fotoDir);

            String fileName = UUID.randomUUID() + "." + extension;
            Path destino = fotoDir.resolve(fileName).normalize();
            if (!destino.startsWith(fotoDir)) {
                throw new IllegalArgumentException("Ruta de archivo no válida");
            }

            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + PROFILE_PHOTOS_FOLDER + "/" + usuarioId + "/" + fileName;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la foto de perfil en almacenamiento local", e);
        }
    }

    private String resolverExtension(String originalFilename, String contentType) {
        if (originalFilename != null && !originalFilename.isBlank()) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalFilename.length() - 1) {
                return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
            }
        }

        if ("image/jpeg".equalsIgnoreCase(contentType)) return "jpg";
        if ("image/png".equalsIgnoreCase(contentType)) return "png";
        if ("image/webp".equalsIgnoreCase(contentType)) return "webp";
        if ("image/gif".equalsIgnoreCase(contentType)) return "gif";
        return "img";
    }

    private boolean esFirmaImagenValida(byte[] contenido) {
        if (contenido == null || contenido.length < 12) {
            return false;
        }

        boolean esJpeg = (contenido[0] & 0xFF) == 0xFF && (contenido[1] & 0xFF) == 0xD8;
        boolean esPng = (contenido[0] & 0xFF) == 0x89
                && contenido[1] == 0x50
                && contenido[2] == 0x4E
                && contenido[3] == 0x47;
        boolean esGif = contenido[0] == 0x47
                && contenido[1] == 0x49
                && contenido[2] == 0x46
                && contenido[3] == 0x38;
        boolean esWebp = contenido[0] == 0x52
                && contenido[1] == 0x49
                && contenido[2] == 0x46
                && contenido[3] == 0x46
                && contenido[8] == 0x57
                && contenido[9] == 0x45
                && contenido[10] == 0x42
                && contenido[11] == 0x50;

        return esJpeg || esPng || esGif || esWebp;
    }

    /**
     * Registra un usuario como profesor.
     */
    public void registrarComoProfesor(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException(USUARIO_NOT_FOUND + usuarioId));

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

    /**
     * Elimina el rol de profesor de un usuario.
     */
    public void eliminarRolProfesor(Long usuarioId) {
        List<Profesor> profesores = profesorRepository.findByUsuarioId(usuarioId);
        if (profesores.isEmpty()) {
            throw new EntityNotFoundException("El usuario no es profesor");
        }
        profesorRepository.deleteAll(profesores);
    }

    /**
     * Elimina el rol de estudiante de un usuario.
     */
    public void eliminarRolEstudiante(Long usuarioId) {
        List<Estudiante> estudiantes = estudianteRepository.findByUsuarioId(usuarioId);
        if (estudiantes.isEmpty()) {
            throw new EntityNotFoundException("El usuario no es estudiante");
        }
        estudianteRepository.deleteAll(estudiantes);
    }

    /**
     * Lista los estudiantes de un grupo con información de usuario.
     */
    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarEstudiantesDeGrupo(Long grupoId) {
        return estudianteRepository.findByGrupoId(grupoId).stream()
                .map(e -> mapper.toDTO(e.getUsuario()))
                .toList();
    }

    /**
     * Elimina a un estudiante de un grupo específico.
     */
    public void eliminarEstudianteDeGrupo(Long usuarioId, Long grupoId) {
        Estudiante estudiante = estudianteRepository.findByUsuarioIdAndGrupoId(usuarioId, grupoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "El usuario " + usuarioId + " no es estudiante del grupo " + grupoId));
        estudianteRepository.delete(estudiante);
    }
}
