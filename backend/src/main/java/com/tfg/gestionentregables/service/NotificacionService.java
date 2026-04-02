package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.NotificacionDTO;
import com.tfg.gestionentregables.dto.PreferenciaNotificacionDTO;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.CanalNotificacion;
import com.tfg.gestionentregables.entity.enums.TipoNotificacion;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Servicio de notificaciones configurable.
 * Gestiona notificaciones in-app y por email según preferencias del usuario.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificacionService {

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FECHA_HUMANA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy 'a las' HH:mm", LOCALE_ES);

    private final NotificacionRepository notificacionRepository;
    private final PreferenciaNotificacionRepository preferenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntregableRepository entregableRepository;
    private final EntityMapper mapper;
    private final EmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl = "http://localhost:3000";

    /**
     * Envía una notificación respetando las preferencias del usuario.
     */
    public void enviarNotificacion(Long usuarioId, TipoNotificacion tipo,
                                    String titulo, String mensaje, Long cursoId) {
        enviarNotificacion(usuarioId, tipo, titulo, mensaje, cursoId, null, null, null);
    }

    @SuppressWarnings("java:S107")
    public void enviarNotificacion(Long usuarioId, TipoNotificacion tipo,
                                   String titulo, String mensaje, Long cursoId,
                                   Long actividadId, Long entregableId, Long entregaId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + usuarioId));

        CanalNotificacion canal = obtenerCanalPreferido(usuarioId);

        String tituloSeguro = limpiarTextoNotificacion(titulo);
        String mensajeSeguro = limpiarTextoNotificacion(mensaje);

        // Notificación in-app (si canal es APP o AMBOS)
        if (canal == CanalNotificacion.APP || canal == CanalNotificacion.AMBOS) {
            Notificacion notificacion = Notificacion.builder()
                    .usuario(usuario)
                    .tipo(tipo)
                .titulo(tituloSeguro)
                .mensaje(mensajeSeguro)
                    .cursoId(cursoId)
                    .actividadId(actividadId)
                    .entregableId(entregableId)
                    .entregaId(entregaId)
                    .build();
            notificacionRepository.save(notificacion);
        }

        // Notificación por email (si canal es EMAIL o AMBOS)
        if (canal == CanalNotificacion.EMAIL || canal == CanalNotificacion.AMBOS) {
            String urlDestino = construirUrlDestino(actividadId, entregableId, entregaId, cursoId);
            emailService.enviarCorreo(
                usuario.getCorreoElectronico(),
                "[TFG Entregables] " + tituloSeguro,
                construirCuerpoEmail(mensajeSeguro, urlDestino)
            );
        }
    }

    /**
     * Obtiene las notificaciones del usuario.
     */
    @Transactional(readOnly = true)
    public List<NotificacionDTO> obtenerNotificaciones(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    /**
     * Marca una notificación como leída (con verificación de propiedad).
     */
    public void marcarComoLeida(Long notificacionId, Long usuarioId) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new EntityNotFoundException("Notificación no encontrada: " + notificacionId));

        if (!notificacion.getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("No tienes permiso para modificar esta notificación");
        }

        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    /**
     * Cuenta las notificaciones no leídas del usuario.
     */
    @Transactional(readOnly = true)
    public Long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    /**
     * Obtiene las preferencias de notificación del usuario.
     */
    @Transactional(readOnly = true)
    public PreferenciaNotificacionDTO obtenerPreferencias(Long usuarioId) {
        CanalNotificacion canal = obtenerCanalPreferido(usuarioId);
        return new PreferenciaNotificacionDTO(canal.name());
    }

    /**
     * Actualiza las preferencias de notificación del usuario.
     */
    public PreferenciaNotificacionDTO actualizarPreferencias(Long usuarioId, PreferenciaNotificacionDTO dto) {
        CanalNotificacion canal;
        try {
            canal = CanalNotificacion.valueOf(dto.getCanal().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Canal de notificación no válido: " + dto.getCanal());
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + usuarioId));

        PreferenciaNotificacion preferencia = preferenciaRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> PreferenciaNotificacion.builder()
                        .usuario(usuario)
                        .build());

        preferencia.setCanal(canal);
        preferenciaRepository.save(preferencia);

        return new PreferenciaNotificacionDTO(canal.name());
    }

    /**
     * TRIGGER: Notifica a los estudiantes cuando un profesor sube un nuevo entregable.
     * Se llama desde EntregableService al crear un entregable visible.
     */
    public void notificarNuevoEntregable(Entregable entregable) {
        if (entregable.getVisibilidad() != Visibilidad.VISIBLE) {
            return; // Solo notificar entregables visibles
        }

        Actividad actividad = entregable.getActividad();
        Curso curso = actividad.getCurso();
        Set<Grupo> grupos = actividad.getGrupos();
        if (grupos == null || grupos.isEmpty()) {
            grupos = curso.getGrupos();
        }

        String titulo = "Nuevo entregable: " + entregable.getTitulo();
        String mensaje = String.format(
            "Se ha publicado un nuevo entregable '%s' en la actividad '%s' del curso '%s'. Fecha límite: %s",
            entregable.getTitulo(),
            actividad.getTitulo(),
            curso.getTitulo(),
            formatearFechaHumana(entregable.getFechaLimite())
        );

        // Notificar a todos los estudiantes de los grupos asignados
        for (Grupo grupo : grupos) {
            for (Estudiante estudiante : grupo.getEstudiantes()) {
                enviarNotificacion(
                    estudiante.getUsuario().getId(),
                    TipoNotificacion.NUEVO_ENTREGABLE,
                    titulo,
                    mensaje,
                    curso.getId(),
                    actividad.getId(),
                    entregable.getId(),
                    null
                );
            }
        }

        log.info("Notificaciones enviadas para nuevo entregable '{}' a {} grupos",
            entregable.getTitulo(), grupos.size());
    }

    /**
     * TRIGGER: Notifica a estudiantes cuando se publica una actividad visible.
     */
    public void notificarNuevaActividad(Actividad actividad) {
        if (actividad.getVisibilidad() != Visibilidad.VISIBLE) {
            return;
        }

        Curso curso = actividad.getCurso();
        Set<Grupo> grupos = actividad.getGrupos();
        if (grupos == null || grupos.isEmpty()) {
            grupos = curso.getGrupos();
        }

        String titulo = "Nueva actividad: " + actividad.getTitulo();
        String mensaje = String.format(
            "Se ha publicado la actividad '%s' en el curso '%s'. Fecha límite: %s",
            actividad.getTitulo(),
            curso.getTitulo(),
            formatearFechaHumana(actividad.getFechaLimite())
        );

        for (Grupo grupo : grupos) {
            for (Estudiante estudiante : grupo.getEstudiantes()) {
                enviarNotificacion(
                    estudiante.getUsuario().getId(),
                    TipoNotificacion.NUEVA_ACTIVIDAD,
                    titulo,
                    mensaje,
                    curso.getId(),
                    actividad.getId(),
                    null,
                    null
                );
            }
        }
    }

    /**
     * Notifica al alumno cuando su nota es visible/publicada.
     */
    public void notificarEntregaEvaluada(Entrega entrega, boolean notaVisible) {
        if (entrega == null || entrega.getEstudiante() == null || entrega.getEntregable() == null) {
            return;
        }

        if (!notaVisible || entrega.getCalificacion() == null) {
            return;
        }

        Long usuarioId = entrega.getEstudiante().getUsuario().getId();
        Entregable entregable = entrega.getEntregable();
        Actividad actividad = entregable.getActividad();
        Curso curso = actividad.getCurso();

        String titulo = "Nota publicada: " + entregable.getTitulo();
        String mensaje = String.format(
            "Tu entrega de '%s' ya tiene nota publicada: %.2f (publicada el %s)",
            entregable.getTitulo(),
            entrega.getCalificacion(),
            formatearFechaHumana(LocalDateTime.now())
        );
        enviarNotificacion(
            usuarioId,
            TipoNotificacion.NOTA_PUBLICADA,
            titulo,
            mensaje,
            curso.getId(),
            actividad.getId(),
            entregable.getId(),
            entrega.getId());
    }

    /**
     * TRIGGER CRON: Cada hora, busca entregables con deadline en las próximas 24h
     * y notifica a estudiantes que aún no han entregado.
     */
    @Scheduled(cron = "0 0 * * * *") // Cada hora en punto
    public void notificarDeadlinesCercanos() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.plusHours(24);

        // Buscar entregables visibles con deadline entre ahora y 24h
        List<Entregable> entregablesProximaDeadline = entregableRepository.findAll().stream()
                .filter(e -> e.getVisibilidad() == Visibilidad.VISIBLE)
                .filter(e -> e.getFechaLimite().isAfter(ahora) && e.getFechaLimite().isBefore(limite))
                .toList();

        for (Entregable entregable : entregablesProximaDeadline) {
            Actividad actividad = entregable.getActividad();
            Curso curso = actividad.getCurso();

            String titulo = "⏰ Deadline cercano: " + entregable.getTitulo();
            String mensaje = String.format(
                "El entregable '%s' de la actividad '%s' (curso '%s') vence el %s. ¡Recuerda entregar a tiempo!",
                entregable.getTitulo(),
                actividad.getTitulo(),
                curso.getTitulo(),
                formatearFechaHumana(entregable.getFechaLimite())
            );

            // Notificar solo a estudiantes que NO han entregado
            for (Grupo grupo : actividad.getGrupos()) {
                for (Estudiante estudiante : grupo.getEstudiantes()) {
                    boolean yaEntrego = entregable.getEntregas().stream()
                            .anyMatch(e -> e.getEstudiante().getId().equals(estudiante.getId())
                                        && Boolean.TRUE.equals(e.getEsVersionActiva()));
                    boolean yaNotificado = notificacionRepository
                        .existsByUsuarioIdAndTipoAndCursoIdAndTituloAndFechaCreacionAfter(
                            estudiante.getUsuario().getId(),
                            TipoNotificacion.DEADLINE_CERCANO,
                            curso.getId(),
                            titulo,
                            ahora.minusHours(24)
                        );
                    if (!yaEntrego && !yaNotificado) {
                        enviarNotificacion(
                            estudiante.getUsuario().getId(),
                            TipoNotificacion.DEADLINE_CERCANO,
                            titulo,
                            mensaje,
                            curso.getId(),
                            actividad.getId(),
                            entregable.getId(),
                            null
                        );
                    }
                }
            }
        }

        if (!entregablesProximaDeadline.isEmpty()) {
            log.info("Notificaciones de deadline enviadas para {} entregables", entregablesProximaDeadline.size());
        }
    }

    /**
     * Obtiene el canal preferido del usuario, o APP por defecto.
     */
    private CanalNotificacion obtenerCanalPreferido(Long usuarioId) {
        return preferenciaRepository.findByUsuarioId(usuarioId)
                .map(PreferenciaNotificacion::getCanal)
                .orElse(CanalNotificacion.APP);
    }

    private String limpiarTextoNotificacion(String value) {
        if (value == null) {
            return null;
        }
        return value
            .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
            .trim();
    }

    private String formatearFechaHumana(LocalDateTime fecha) {
        if (fecha == null) {
            return "fecha no disponible";
        }
        return fecha.format(FECHA_HUMANA);
    }

    private String construirCuerpoEmail(String mensaje, String urlDestino) {
        if (urlDestino == null || urlDestino.isBlank()) {
            return mensaje;
        }
        return mensaje + "\n\nAbrir en plataforma: " + urlDestino;
    }

    private String construirUrlDestino(Long actividadId, Long entregableId, Long entregaId, Long cursoId) {
        String base = frontendBaseUrl != null ? frontendBaseUrl.replaceAll("/+$", "") : "http://localhost:3000";
        if (entregaId != null) {
            return base + "/entregas/" + entregaId;
        }
        if (entregableId != null) {
            return base + "/entregables/" + entregableId;
        }
        if (actividadId != null) {
            return base + "/actividades/" + actividadId;
        }
        if (cursoId != null) {
            return base + "/cursos/" + cursoId;
        }
        return null;
    }
}
