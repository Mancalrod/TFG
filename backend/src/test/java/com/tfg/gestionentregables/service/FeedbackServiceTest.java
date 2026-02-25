package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private EntregaRepository entregaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EntityMapper mapper;

    @InjectMocks
    private FeedbackService feedbackService;

    private Entrega entrega;
    private Usuario profesor;
    private Feedback feedback;
    private FeedbackDTO feedbackDTO;
    private CrearFeedbackDTO crearFeedbackDTO;

    @BeforeEach
    void setUp() {
        profesor = Usuario.builder().id(1L).nombre("Prof. García")
                .correoElectronico("prof@test.com").contrasena("pass").build();

        Curso curso = Curso.builder().id(1L).titulo("IS").codigo("IS-001").build();
        Actividad actividad = Actividad.builder().id(1L).titulo("P1").curso(curso)
                .grupos(new HashSet<>()).entregables(new HashSet<>()).build();
        Entregable entregable = Entregable.builder().id(1L).titulo("E1").actividad(actividad)
                .entregas(new HashSet<>()).build();

        Usuario alumno = Usuario.builder().id(2L).nombre("Alumno").correoElectronico("a@t.com").contrasena("p").build();
        Grupo grupo = Grupo.builder().id(1L).titulo("G1").curso(curso).estudiantes(new HashSet<>()).build();
        Estudiante estudiante = Estudiante.builder().id(1L).usuario(alumno).grupo(grupo).build();

        entrega = Entrega.builder().id(1L).nombre("Entrega 1").version(1)
                .entregable(entregable).estudiante(estudiante)
                .archivos(new HashSet<>()).feedbacks(new HashSet<>()).build();

        LocalDateTime ahora = LocalDateTime.now();
        feedback = Feedback.builder().id(1L).comentario("Buen trabajo")
                .fechaCreacion(ahora).fechaModificacion(ahora)
                .entrega(entrega).profesor(profesor).build();

        feedbackDTO = FeedbackDTO.builder().id(1L).comentario("Buen trabajo")
                .fechaCreacion(ahora).entregaId(1L).profesorId(1L).profesorNombre("Prof. García").build();

        crearFeedbackDTO = CrearFeedbackDTO.builder().comentario("Buen trabajo").build();
    }

    @Nested
    @DisplayName("crearFeedback")
    class CrearFeedback {

        @Test
        @DisplayName("Crea feedback correctamente")
        void crear_ok() {
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(profesor));
            when(feedbackRepository.save(any(Feedback.class))).thenReturn(feedback);
            when(mapper.toDTO(any(Feedback.class))).thenReturn(feedbackDTO);

            FeedbackDTO result = feedbackService.crearFeedback(1L, 1L, crearFeedbackDTO);

            assertThat(result.getComentario()).isEqualTo("Buen trabajo");
            verify(feedbackRepository).save(any(Feedback.class));
        }

        @Test
        @DisplayName("Lanza excepción si entrega no existe")
        void crear_entregaNoExiste() {
            when(entregaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feedbackService.crearFeedback(99L, 1L, crearFeedbackDTO))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza excepción si profesor no existe")
        void crear_profesorNoExiste() {
            when(entregaRepository.findById(1L)).thenReturn(Optional.of(entrega));
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feedbackService.crearFeedback(1L, 99L, crearFeedbackDTO))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarFeedbacksEntrega")
    class ListarFeedbacks {

        @Test
        @DisplayName("Lista feedbacks de entrega existente")
        void listar_ok() {
            when(entregaRepository.existsById(1L)).thenReturn(true);
            when(feedbackRepository.findByEntregaIdOrderByFechaCreacionDesc(1L))
                    .thenReturn(List.of(feedback));
            when(mapper.toDTO(feedback)).thenReturn(feedbackDTO);

            List<FeedbackDTO> result = feedbackService.listarFeedbacksEntrega(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si entrega no existe")
        void listar_entregaNoExiste() {
            when(entregaRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> feedbackService.listarFeedbacksEntrega(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("actualizarFeedback")
    class ActualizarFeedback {

        @Test
        @DisplayName("Actualiza feedback del mismo profesor")
        void actualizar_ok() {
            when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
            when(feedbackRepository.save(any(Feedback.class))).thenReturn(feedback);
            when(mapper.toDTO(any(Feedback.class))).thenReturn(feedbackDTO);

            FeedbackDTO result = feedbackService.actualizarFeedback(1L, 1L, crearFeedbackDTO);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Lanza excepción si otro profesor intenta modificar")
        void actualizar_otroProfesor() {
            when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

            assertThatThrownBy(() -> feedbackService.actualizarFeedback(1L, 99L, crearFeedbackDTO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Solo el profesor que creó");
        }

        @Test
        @DisplayName("Lanza excepción si feedback no existe")
        void actualizar_noExiste() {
            when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feedbackService.actualizarFeedback(99L, 1L, crearFeedbackDTO))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("obtenerFeedback")
    class ObtenerFeedback {

        @Test
        @DisplayName("Obtiene feedback existente")
        void obtener_ok() {
            when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
            when(mapper.toDTO(feedback)).thenReturn(feedbackDTO);

            FeedbackDTO result = feedbackService.obtenerFeedback(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lanza excepción si no existe")
        void obtener_noExiste() {
            when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feedbackService.obtenerFeedback(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("eliminarFeedback")
    class EliminarFeedback {

        @Test
        @DisplayName("Elimina feedback del mismo profesor")
        void eliminar_ok() {
            when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

            feedbackService.eliminarFeedback(1L, 1L);

            verify(feedbackRepository).delete(feedback);
        }

        @Test
        @DisplayName("Bloquea eliminación por otro profesor")
        void eliminar_otroProfesor() {
            when(feedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

            assertThatThrownBy(() -> feedbackService.eliminarFeedback(1L, 99L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Solo el profesor que creó");
        }

        @Test
        @DisplayName("Lanza excepción si feedback no existe")
        void eliminar_noExiste() {
            when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feedbackService.eliminarFeedback(99L, 1L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listarFeedbacksProfesor")
    class ListarFeedbacksProfesor {

        @Test
        @DisplayName("Lista feedbacks del profesor")
        void listar_ok() {
            when(usuarioRepository.existsById(1L)).thenReturn(true);
            when(feedbackRepository.findByProfesorIdOrderByFechaCreacionDesc(1L))
                    .thenReturn(List.of(feedback));
            when(mapper.toDTO(feedback)).thenReturn(feedbackDTO);

            List<FeedbackDTO> result = feedbackService.listarFeedbacksProfesor(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Lanza excepción si profesor no existe")
        void listar_noExiste() {
            when(usuarioRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> feedbackService.listarFeedbacksProfesor(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("contarFeedbacksRecientes")
    class ContarRecientes {

        @Test
        @DisplayName("Cuenta feedbacks recientes")
        void contar_ok() {
            LocalDateTime desde = LocalDateTime.now().minusDays(7);
            when(feedbackRepository.countFeedbacksRecientesParaEstudiante(1L, desde)).thenReturn(3L);

            long result = feedbackService.contarFeedbacksRecientes(1L, desde);

            assertThat(result).isEqualTo(3L);
        }
    }
}
