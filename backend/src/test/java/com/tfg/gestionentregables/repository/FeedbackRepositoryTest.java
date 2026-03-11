package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class FeedbackRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private FeedbackRepository feedbackRepository;

    private Entrega entrega;
    private Usuario profesor;

    @BeforeEach
    void setUp() {
        Curso curso = em.persistAndFlush(Curso.builder()
                .titulo("Curso").descripcion("Desc").codigo("C-001").build());
        Grupo grupo = em.persistAndFlush(Grupo.builder().titulo("Grupo A").curso(curso).build());

        Actividad actividad = em.persistAndFlush(Actividad.builder()
                .titulo("Actividad 1").tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(LocalDateTime.now()).fechaLimite(LocalDateTime.now().plusDays(7))
                .visibilidad(Visibilidad.VISIBLE).curso(curso).build());

        Entregable entregable = em.persistAndFlush(Entregable.builder()
                .titulo("Entregable 1").fechaLimite(LocalDateTime.now().plusDays(7))
                .visibilidad(Visibilidad.VISIBLE).actividad(actividad).build());

        Usuario alumno = em.persistAndFlush(Usuario.builder()
                .nombre("Ana").correoElectronico("ana@test.com").contrasena("pass123").esAdmin(false).build());
        Estudiante estudiante = em.persistAndFlush(Estudiante.builder().usuario(alumno).grupo(grupo).build());

        profesor = em.persistAndFlush(Usuario.builder()
                .nombre("Prof").correoElectronico("prof@test.com").contrasena("pass123").esAdmin(false).build());

        entrega = em.persistAndFlush(Entrega.builder()
                .nombre("Entrega 1").version(1).fechaEntrega(LocalDateTime.now())
                .estado(EstadoEntrega.ENTREGADO).esVersionActiva(true)
                .entregable(entregable).estudiante(estudiante).build());
    }

    @Test
    @DisplayName("findByEntregaId devuelve feedbacks de la entrega")
    void findByEntregaId() {
        em.persistAndFlush(Feedback.builder()
                .comentario("Bien").fechaCreacion(LocalDateTime.now())
                .entrega(entrega).profesor(profesor).build());

        List<Feedback> result = feedbackRepository.findByEntregaId(entrega.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getComentario()).isEqualTo("Bien");
    }

    @Test
    @DisplayName("findByEntregaIdOrderByFechaCreacionDesc ordena por fecha descendente")
    void findByEntregaIdOrderByFechaCreacionDesc() {
        em.persistAndFlush(Feedback.builder()
                .comentario("Primero").fechaCreacion(LocalDateTime.now().minusHours(2))
                .entrega(entrega).profesor(profesor).build());
        em.persistAndFlush(Feedback.builder()
                .comentario("Segundo").fechaCreacion(LocalDateTime.now())
                .entrega(entrega).profesor(profesor).build());

        List<Feedback> result = feedbackRepository.findByEntregaIdOrderByFechaCreacionDesc(entrega.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getComentario()).isEqualTo("Segundo");
    }

    @Test
    @DisplayName("findByProfesorId devuelve feedbacks del profesor")
    void findByProfesorId() {
        em.persistAndFlush(Feedback.builder()
                .comentario("Bien").fechaCreacion(LocalDateTime.now())
                .entrega(entrega).profesor(profesor).build());

        List<Feedback> result = feedbackRepository.findByProfesorId(profesor.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("countFeedbacksRecientesParaEstudiante cuenta feedbacks recientes")
    void countFeedbacksRecientes() {
        em.persistAndFlush(Feedback.builder()
                .comentario("Reciente").fechaCreacion(LocalDateTime.now())
                .entrega(entrega).profesor(profesor).build());
        em.persistAndFlush(Feedback.builder()
                .comentario("Antiguo").fechaCreacion(LocalDateTime.now().minusDays(10))
                .entrega(entrega).profesor(profesor).build());

        long count = feedbackRepository.countFeedbacksRecientesParaEstudiante(
                entrega.getEstudiante().getId(), LocalDateTime.now().minusDays(7));

        assertThat(count).isEqualTo(1);
    }
}
