package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.PreferenciaNotificacionDTO;
import com.tfg.gestionentregables.dto.NotificacionDTO;
import com.tfg.gestionentregables.entity.Actividad;
import com.tfg.gestionentregables.entity.Curso;
import com.tfg.gestionentregables.entity.Entregable;
import com.tfg.gestionentregables.entity.Entrega;
import com.tfg.gestionentregables.entity.Estudiante;
import com.tfg.gestionentregables.entity.Grupo;
import com.tfg.gestionentregables.entity.Notificacion;
import com.tfg.gestionentregables.entity.PreferenciaNotificacion;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.entity.enums.CanalNotificacion;
import com.tfg.gestionentregables.entity.enums.TipoNotificacion;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.repository.EntregableRepository;
import com.tfg.gestionentregables.repository.EstudianteRepository;
import com.tfg.gestionentregables.repository.NotificacionRepository;
import com.tfg.gestionentregables.repository.PreferenciaNotificacionRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private PreferenciaNotificacionRepository preferenciaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EntregableRepository entregableRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private EntityMapper mapper;
    @Mock private EmailService emailService;

    @InjectMocks
    private NotificacionService notificacionService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(10L)
                .correoElectronico("ana@ull.edu.es")
                .nombre("Ana")
                .build();
    }

    @Test
    @DisplayName("Guarda in-app cuando el canal es APP")
    void enviarNotificacion_appOnly() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.APP).build()
        ));

        notificacionService.enviarNotificacion(10L, TipoNotificacion.NUEVO_ENTREGABLE,
                "Nuevo", "Mensaje", 1L);

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(emailService, never()).enviarCorreo(any(), any(), any());
    }

    @Test
    @DisplayName("Envia email cuando el canal es EMAIL")
    void enviarNotificacion_emailOnly() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.EMAIL).build()
        ));

        notificacionService.enviarNotificacion(10L, TipoNotificacion.DEADLINE_CERCANO,
                "Recordatorio", "Mensaje", 1L);

        verify(notificacionRepository, never()).save(any(Notificacion.class));
        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), any(), contains("Mensaje"));
        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), any(), contains("/cursos/1"));
    }

    @Test
    @DisplayName("Incluye deep-link a entrega en correo cuando entregaId está disponible")
    void enviarNotificacion_emailIncluyeLinkEntrega() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.EMAIL).build()
        ));

        notificacionService.enviarNotificacion(
                10L,
                TipoNotificacion.NOTA_PUBLICADA,
                "Nota publicada",
                "Tu nota está lista",
                5L,
                6L,
                7L,
                8L
        );

        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), any(), contains("/entregas/8"));
    }

    @Test
    @DisplayName("Canal EMAIL con base URL null usa fallback localhost")
    void enviarNotificacion_emailBaseUrlNull_usaFallback() {
        ReflectionTestUtils.setField(notificacionService, "frontendBaseUrl", null);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.EMAIL).build()
        ));

        notificacionService.enviarNotificacion(10L, TipoNotificacion.DEADLINE_CERCANO,
                "Recordatorio", "Mensaje", 9L);

        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), any(), contains("http://localhost:3000/cursos/9"));
    }

    @Test
    @DisplayName("Canal EMAIL con base URL vacia usa fallback localhost")
    void enviarNotificacion_emailBaseUrlVacia_usaFallback() {
        ReflectionTestUtils.setField(notificacionService, "frontendBaseUrl", "   ");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.EMAIL).build()
        ));

        notificacionService.enviarNotificacion(10L, TipoNotificacion.DEADLINE_CERCANO,
                "Recordatorio", "Mensaje", 10L);

        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), any(), contains("http://localhost:3000/cursos/10"));
    }

    @Test
    @DisplayName("Canal EMAIL recorta espacios y barras finales en base URL")
    void enviarNotificacion_emailBaseUrlConEspaciosYBarras_normaliza() {
        ReflectionTestUtils.setField(notificacionService, "frontendBaseUrl", "  https://front.test///   ");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.EMAIL).build()
        ));

        notificacionService.enviarNotificacion(10L, TipoNotificacion.DEADLINE_CERCANO,
                "Recordatorio", "Mensaje", 11L);

        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), any(), contains("https://front.test/cursos/11"));
    }

    @Test
    @DisplayName("Canal EMAIL usa ruta de entregable cuando no hay entrega")
    void enviarNotificacion_emailIncluyeLinkEntregable() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.EMAIL).build()
        ));

        notificacionService.enviarNotificacion(
                10L,
                TipoNotificacion.NOTA_PUBLICADA,
                "Nota",
                "Mensaje",
                5L,
                null,
                7L,
                null
        );

        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), any(), contains("/entregables/7"));
    }

    @Test
    @DisplayName("Canal EMAIL usa ruta de actividad cuando no hay entrega ni entregable")
    void enviarNotificacion_emailIncluyeLinkActividad() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.EMAIL).build()
        ));

        notificacionService.enviarNotificacion(
                10L,
                TipoNotificacion.NOTA_PUBLICADA,
                "Nota",
                "Mensaje",
                null,
                6L,
                null,
                null
        );

        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), any(), contains("/actividades/6"));
    }

    @Test
    @DisplayName("Canal EMAIL sin IDs envia solo mensaje sin deep-link")
    void enviarNotificacion_emailSinIds_noIncluyeDeepLink() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.EMAIL).build()
        ));

        notificacionService.enviarNotificacion(
                10L,
                TipoNotificacion.NOTA_PUBLICADA,
                "Nota",
                "Mensaje plano",
                null,
                null,
                null,
                null
        );

        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), any(), eq("Mensaje plano"));
    }

    @Test
    @DisplayName("Canal AMBOS guarda in-app y envia email con texto sanitizado")
    void enviarNotificacion_ambos_sanitizaTexto() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.of(
                PreferenciaNotificacion.builder().usuario(usuario).canal(CanalNotificacion.AMBOS).build()
        ));

        notificacionService.enviarNotificacion(
                10L,
                TipoNotificacion.NUEVO_ENTREGABLE,
                "  Titulo\u0000 limpio  ",
                "\u0007Mensaje seguro\t",
                1L,
                2L,
                3L,
                4L
        );

        verify(notificacionRepository).save(argThat(n ->
                "Titulo limpio".equals(n.getTitulo())
                        && "Mensaje seguro".equals(n.getMensaje())
                        && Long.valueOf(2L).equals(n.getActividadId())
                        && Long.valueOf(3L).equals(n.getEntregableId())
                        && Long.valueOf(4L).equals(n.getEntregaId())
        ));
        verify(emailService).enviarCorreo(eq("ana@ull.edu.es"), eq("[TFG Entregables] Titulo limpio"), argThat(m ->
                m.startsWith("Mensaje seguro") && m.contains("/entregas/4")
        ));
    }

    @Test
    @DisplayName("Actualiza preferencia de notificacion")
    void actualizarPreferencias_ok() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());

        PreferenciaNotificacionDTO result = notificacionService.actualizarPreferencias(10L,
                new PreferenciaNotificacionDTO("AMBOS"));

        assertThat(result.getCanal()).isEqualTo("AMBOS");
        verify(preferenciaRepository).save(any(PreferenciaNotificacion.class));
    }

    @Test
    @DisplayName("Bloquea marcar leida de notificacion ajena")
    void marcarLeida_sinPermiso() {
        Usuario otro = Usuario.builder().id(99L).build();
        Notificacion n = Notificacion.builder().id(5L).usuario(otro).build();
        when(notificacionRepository.findById(5L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificacionService.marcarComoLeida(5L, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Lanza error al obtener usuario inexistente")
    void enviarNotificacion_usuarioNoExiste() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.enviarNotificacion(10L,
                TipoNotificacion.NUEVO_ENTREGABLE, "t", "m", 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

        @Test
        @DisplayName("Obtiene notificaciones ordenadas y mapeadas a DTO")
        void obtenerNotificaciones_ok() {
                Notificacion n1 = Notificacion.builder().id(1L).usuario(usuario).build();
                Notificacion n2 = Notificacion.builder().id(2L).usuario(usuario).build();
                when(notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(10L)).thenReturn(List.of(n1, n2));
                when(mapper.toDTO(n1)).thenReturn(new NotificacionDTO());
                when(mapper.toDTO(n2)).thenReturn(new NotificacionDTO());

                List<NotificacionDTO> result = notificacionService.obtenerNotificaciones(10L);

                assertThat(result).hasSize(2);
                verify(mapper).toDTO(n1);
                verify(mapper).toDTO(n2);
        }

        @Test
        @DisplayName("Marca notificacion propia como leida")
        void marcarLeida_ok() {
                Notificacion n = Notificacion.builder().id(5L).usuario(usuario).leida(false).build();
                when(notificacionRepository.findById(5L)).thenReturn(Optional.of(n));

                notificacionService.marcarComoLeida(5L, 10L);

                assertThat(n.getLeida()).isTrue();
                verify(notificacionRepository).save(n);
        }

        @Test
        @DisplayName("Lanza error al marcar como leida si no existe")
        void marcarLeida_noExiste() {
                when(notificacionRepository.findById(5L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> notificacionService.marcarComoLeida(5L, 10L))
                                .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Cuenta notificaciones no leidas")
        void contarNoLeidas_ok() {
                when(notificacionRepository.countByUsuarioIdAndLeidaFalse(10L)).thenReturn(4L);

                Long count = notificacionService.contarNoLeidas(10L);

                assertThat(count).isEqualTo(4L);
        }

        @Test
        @DisplayName("Obtiene preferencias con valor por defecto APP")
        void obtenerPreferencias_defaultApp() {
                when(preferenciaRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());

                PreferenciaNotificacionDTO dto = notificacionService.obtenerPreferencias(10L);

                assertThat(dto.getCanal()).isEqualTo("APP");
        }

        @Test
        @DisplayName("Falla al actualizar preferencia con canal invalido")
        void actualizarPreferencias_canalInvalido() {
                PreferenciaNotificacionDTO dtoInvalido = new PreferenciaNotificacionDTO("NO_EXISTE");

                assertThatThrownBy(() -> notificacionService.actualizarPreferencias(10L, dtoInvalido))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Canal de notificación no válido");
        }

        @Test
        @DisplayName("Falla al actualizar preferencia si usuario no existe")
        void actualizarPreferencias_usuarioNoExiste() {
                when(usuarioRepository.findById(10L)).thenReturn(Optional.empty());
                PreferenciaNotificacionDTO dto = new PreferenciaNotificacionDTO("APP");

                assertThatThrownBy(() -> notificacionService.actualizarPreferencias(10L, dto))
                                .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("No notifica nuevo entregable si no es visible")
        void notificarNuevoEntregable_noVisible() {
                Entregable entregable = Entregable.builder().visibilidad(Visibilidad.OCULTO).build();

                notificacionService.notificarNuevoEntregable(entregable);

                verify(notificacionRepository, never()).save(any(Notificacion.class));
                verify(emailService, never()).enviarCorreo(any(), any(), any());
        }

        @Test
        @DisplayName("Notifica nuevo entregable visible a estudiantes de grupos")
        void notificarNuevoEntregable_visible() {
                Usuario usuarioEst = Usuario.builder().id(20L).correoElectronico("est@ull.edu.es").build();
                Estudiante estudiante = Estudiante.builder().id(30L).usuario(usuarioEst).build();
                Grupo grupo = Grupo.builder().id(40L).estudiantes(Set.of(estudiante)).build();
                Curso curso = Curso.builder().id(50L).titulo("Curso").build();
                Actividad actividad = Actividad.builder().id(60L).titulo("Act").curso(curso).grupos(Set.of(grupo)).build();
                Entregable entregable = Entregable.builder()
                                .id(70L)
                                .titulo("Entrega 1")
                                .visibilidad(Visibilidad.VISIBLE)
                                .fechaLimite(LocalDateTime.now().plusDays(1))
                                .actividad(actividad)
                                .build();

                NotificacionService spyService = spy(notificacionService);
                doNothing().when(spyService)
                        .enviarNotificacion(anyLong(), any(TipoNotificacion.class), any(), any(), anyLong(), any(), any(), any());

                spyService.notificarNuevoEntregable(entregable);

                verify(spyService).enviarNotificacion(
                        eq(20L), eq(TipoNotificacion.NUEVO_ENTREGABLE), any(), any(), eq(50L), eq(60L), eq(70L), isNull());
        }

        @Test
        @DisplayName("Notifica nueva actividad usando grupos del curso cuando actividad no trae grupos")
        void notificarNuevaActividad_usaGruposDelCurso() {
                Usuario usuarioEst = Usuario.builder().id(25L).correoElectronico("est4@ull.edu.es").build();
                Estudiante estudiante = Estudiante.builder().id(35L).usuario(usuarioEst).build();
                Grupo grupoCurso = Grupo.builder().id(45L).estudiantes(Set.of(estudiante)).build();
                Curso curso = Curso.builder().id(55L).titulo("Curso X").grupos(Set.of(grupoCurso)).build();
                Actividad actividad = Actividad.builder()
                                .id(65L)
                                .titulo("Act X")
                                .visibilidad(Visibilidad.VISIBLE)
                                .fechaLimite(LocalDateTime.now().plusDays(2))
                                .curso(curso)
                                .grupos(Set.of())
                                .build();

                NotificacionService spyService = spy(notificacionService);
                doNothing().when(spyService)
                                .enviarNotificacion(anyLong(), any(TipoNotificacion.class), any(), any(), anyLong(), any(), any(), any());

                spyService.notificarNuevaActividad(actividad);

                verify(spyService).enviarNotificacion(
                                eq(25L), eq(TipoNotificacion.NUEVA_ACTIVIDAD), any(), any(), eq(55L), eq(65L), isNull(), isNull());
        }

        @Test
        @DisplayName("No notifica nueva actividad si está oculta")
        void notificarNuevaActividad_oculta() {
                Actividad actividad = Actividad.builder().id(66L).visibilidad(Visibilidad.OCULTO).build();

                NotificacionService spyService = spy(notificacionService);
                spyService.notificarNuevaActividad(actividad);

                verify(spyService, never()).enviarNotificacion(anyLong(), any(TipoNotificacion.class), any(), any(), anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("No notifica nota publicada cuando no es visible")
        void notificarEntregaEvaluada_notaNoVisible() {
                Estudiante estudiante = Estudiante.builder().id(44L).usuario(usuario).build();
                Entrega entrega = Entrega.builder().id(90L).estudiante(estudiante).calificacion(9.5).build();

                NotificacionService spyService = spy(notificacionService);
                spyService.notificarEntregaEvaluada(entrega, false);

                verify(spyService, never()).enviarNotificacion(anyLong(), any(TipoNotificacion.class), any(), any(), anyLong(), any(), any(), any());
        }

        @Test
        @DisplayName("Notifica nota publicada cuando es visible y calificada")
        void notificarEntregaEvaluada_ok() {
                Estudiante estudiante = Estudiante.builder().id(46L).usuario(usuario).build();
                Curso curso = Curso.builder().id(58L).titulo("Curso Y").build();
                Actividad actividad = Actividad.builder().id(68L).titulo("Act Y").curso(curso).build();
                Entregable entregable = Entregable.builder().id(78L).titulo("Ent Y").actividad(actividad).build();
                Entrega entrega = Entrega.builder()
                                .id(88L)
                                .estudiante(estudiante)
                                .entregable(entregable)
                                .calificacion(8.75)
                                .build();

                NotificacionService spyService = spy(notificacionService);
                doNothing().when(spyService)
                                .enviarNotificacion(anyLong(), any(TipoNotificacion.class), any(), any(), anyLong(), any(), any(), any());

                spyService.notificarEntregaEvaluada(entrega, true);

                verify(spyService).enviarNotificacion(
                                eq(10L), eq(TipoNotificacion.NOTA_PUBLICADA), any(), any(), eq(58L), eq(68L), eq(78L), eq(88L));
        }

        @Test
        @DisplayName("Cron de deadlines notifica solo si no entrego y no estaba notificado")
        void notificarDeadlinesCercanos_notificaPendientes() {
                Usuario usuarioEst = Usuario.builder().id(21L).correoElectronico("est2@ull.edu.es").build();
                Estudiante estudiante = Estudiante.builder().id(31L).usuario(usuarioEst).build();
                Grupo grupo = Grupo.builder().id(41L).estudiantes(Set.of(estudiante)).build();
                Curso curso = Curso.builder().id(51L).titulo("Curso B").build();
                Actividad actividad = Actividad.builder().id(61L).titulo("Act B").curso(curso).grupos(Set.of(grupo)).build();
                Entregable entregable = Entregable.builder()
                                .id(71L)
                                .titulo("Entrega 2")
                                .visibilidad(Visibilidad.VISIBLE)
                                .fechaLimite(LocalDateTime.now().plusHours(2))
                                .actividad(actividad)
                                .entregas(Set.of())
                                .build();

                when(entregableRepository.findAll()).thenReturn(List.of(entregable));
                when(notificacionRepository.existsByUsuarioIdAndTipoAndCursoIdAndTituloAndFechaCreacionAfter(
                                eq(21L), eq(TipoNotificacion.DEADLINE_CERCANO), eq(51L), any(), any(LocalDateTime.class)
                )).thenReturn(false);

                NotificacionService spyService = spy(notificacionService);
                doNothing().when(spyService)
                        .enviarNotificacion(anyLong(), any(TipoNotificacion.class), any(), any(), anyLong(), any(), any(), any());

                spyService.notificarDeadlinesCercanos();

                verify(spyService).enviarNotificacion(
                        eq(21L), eq(TipoNotificacion.DEADLINE_CERCANO), any(), any(), eq(51L), eq(61L), eq(71L), isNull());
        }

        @Test
        @DisplayName("Cron de deadlines no notifica si estudiante ya entrego")
        void notificarDeadlinesCercanos_noNotificaSiYaEntrego() {
                Usuario usuarioEst = Usuario.builder().id(22L).correoElectronico("est3@ull.edu.es").build();
                Estudiante estudiante = Estudiante.builder().id(32L).usuario(usuarioEst).build();
                Grupo grupo = Grupo.builder().id(42L).estudiantes(Set.of(estudiante)).build();
                Curso curso = Curso.builder().id(52L).titulo("Curso C").build();
                Actividad actividad = Actividad.builder().id(62L).titulo("Act C").curso(curso).grupos(Set.of(grupo)).build();
                Entrega entregaActiva = Entrega.builder().id(80L).estudiante(estudiante).esVersionActiva(true).build();
                Entregable entregable = Entregable.builder()
                                .id(72L)
                                .titulo("Entrega 3")
                                .visibilidad(Visibilidad.VISIBLE)
                                .fechaLimite(LocalDateTime.now().plusHours(3))
                                .actividad(actividad)
                                .entregas(Set.of(entregaActiva))
                                .build();

                when(entregableRepository.findAll()).thenReturn(List.of(entregable));

                NotificacionService spyService = spy(notificacionService);

                spyService.notificarDeadlinesCercanos();

                verify(spyService, never())
                        .enviarNotificacion(eq(22L), eq(TipoNotificacion.DEADLINE_CERCANO), any(), any(), eq(52L), any(), any(), any());
        }

        @Test
        @DisplayName("Cron de deadlines no notifica si ya fue notificado en las últimas 24h")
        void notificarDeadlinesCercanos_noNotificaSiYaNotificado() {
                Usuario usuarioEst = Usuario.builder().id(23L).correoElectronico("est5@ull.edu.es").build();
                Estudiante estudiante = Estudiante.builder().id(33L).usuario(usuarioEst).build();
                Grupo grupo = Grupo.builder().id(43L).estudiantes(Set.of(estudiante)).build();
                Curso curso = Curso.builder().id(53L).titulo("Curso D").build();
                Actividad actividad = Actividad.builder().id(63L).titulo("Act D").curso(curso).grupos(Set.of(grupo)).build();
                Entregable entregable = Entregable.builder()
                                .id(73L)
                                .titulo("Entrega 4")
                                .visibilidad(Visibilidad.VISIBLE)
                                .fechaLimite(LocalDateTime.now().plusHours(4))
                                .actividad(actividad)
                                .entregas(Set.of())
                                .build();

                when(entregableRepository.findAll()).thenReturn(List.of(entregable));
                when(notificacionRepository.existsByUsuarioIdAndTipoAndCursoIdAndTituloAndFechaCreacionAfter(
                                eq(23L), eq(TipoNotificacion.DEADLINE_CERCANO), eq(53L), any(), any(LocalDateTime.class)
                )).thenReturn(true);

                NotificacionService spyService = spy(notificacionService);

                spyService.notificarDeadlinesCercanos();

                verify(spyService, never())
                                .enviarNotificacion(eq(23L), eq(TipoNotificacion.DEADLINE_CERCANO), any(), any(), eq(53L), any(), any(), any());
        }
}
