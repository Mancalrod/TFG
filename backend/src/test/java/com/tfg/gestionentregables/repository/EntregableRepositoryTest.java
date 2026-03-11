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
class EntregableRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EntregableRepository entregableRepository;

    private Actividad actividad;

    @BeforeEach
    void setUp() {
        Curso curso = em.persistAndFlush(Curso.builder()
                .titulo("Curso").descripcion("Desc").codigo("C-001").build());

        actividad = em.persistAndFlush(Actividad.builder()
                .titulo("Actividad 1").tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(LocalDateTime.now()).fechaLimite(LocalDateTime.now().plusDays(30))
                .visibilidad(Visibilidad.VISIBLE).curso(curso).build());
    }

    private Entregable crearEntregable(String titulo, Visibilidad vis, LocalDateTime limite) {
        return em.persistAndFlush(Entregable.builder()
                .titulo(titulo).fechaLimite(limite)
                .visibilidad(vis).actividad(actividad).build());
    }

    @Test
    @DisplayName("findByActividadId devuelve entregables de la actividad")
    void findByActividadId() {
        crearEntregable("Ent 1", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7));
        crearEntregable("Ent 2", Visibilidad.OCULTO, LocalDateTime.now().plusDays(14));

        List<Entregable> result = entregableRepository.findByActividadId(actividad.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByActividadIdAndVisibilidad filtra por visibilidad")
    void findByActividadIdAndVisibilidad() {
        crearEntregable("Visible", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7));
        crearEntregable("Oculto", Visibilidad.OCULTO, LocalDateTime.now().plusDays(7));

        List<Entregable> result = entregableRepository.findByActividadIdAndVisibilidad(
                actividad.getId(), Visibilidad.VISIBLE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitulo()).isEqualTo("Visible");
    }

    @Test
    @DisplayName("findByActividadIdAndFechaLimiteAfter filtra por fecha")
    void findByActividadIdAndFechaLimiteAfter() {
        crearEntregable("Pasado", Visibilidad.VISIBLE, LocalDateTime.now().minusDays(1));
        crearEntregable("Futuro", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7));

        List<Entregable> result = entregableRepository.findByActividadIdAndFechaLimiteAfter(
                actividad.getId(), LocalDateTime.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitulo()).isEqualTo("Futuro");
    }

    @Test
    @DisplayName("findByActividadIdAndFechaLimiteBetween filtra por rango")
    void findByActividadIdAndFechaLimiteBetween() {
        crearEntregable("Muy pronto", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(1));
        crearEntregable("En rango", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(5));
        crearEntregable("Muy tarde", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(30));

        List<Entregable> result = entregableRepository.findByActividadIdAndFechaLimiteBetween(
                actividad.getId(), LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitulo()).isEqualTo("En rango");
    }
}
