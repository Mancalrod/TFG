package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.*;
import com.tfg.gestionentregables.entity.enums.TipoActividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ActividadRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ActividadRepository actividadRepository;

    private Curso curso;
    private Grupo grupo1;
    private Grupo grupo2;

    @BeforeEach
    void setUp() {
        curso = em.persistAndFlush(Curso.builder()
                .titulo("Curso Test").descripcion("Desc").codigo("CT-001").build());

        grupo1 = em.persistAndFlush(Grupo.builder().titulo("Grupo A").curso(curso).build());
        grupo2 = em.persistAndFlush(Grupo.builder().titulo("Grupo B").curso(curso).build());
    }

    private Actividad crearActividad(String titulo, Visibilidad vis, LocalDateTime limite, Set<Grupo> grupos) {
        Actividad a = Actividad.builder()
                .titulo(titulo).descripcion("Desc")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaCreacion(LocalDateTime.now())
                .fechaLimite(limite)
                .visibilidad(vis)
                .notaMaxima(10.0)
                .curso(curso)
                .grupos(grupos)
                .build();
        return em.persistAndFlush(a);
    }

    @Test
    @DisplayName("findByCursoId devuelve actividades del curso")
    void findByCursoId() {
        crearActividad("Act 1", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7), new HashSet<>());
        crearActividad("Act 2", Visibilidad.OCULTO, LocalDateTime.now().plusDays(14), new HashSet<>());

        List<Actividad> result = actividadRepository.findByCursoId(curso.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByCursoIdAndVisibilidad filtra por visibilidad")
    void findByCursoIdAndVisibilidad() {
        crearActividad("Visible", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7), new HashSet<>());
        crearActividad("Oculta", Visibilidad.OCULTO, LocalDateTime.now().plusDays(14), new HashSet<>());

        List<Actividad> visibles = actividadRepository.findByCursoIdAndVisibilidad(curso.getId(), Visibilidad.VISIBLE);

        assertThat(visibles).hasSize(1);
        assertThat(visibles.get(0).getTitulo()).isEqualTo("Visible");
    }

    @Test
    @DisplayName("findByCursoIdAndFechaLimiteAfter filtra por fecha")
    void findByCursoIdAndFechaLimiteAfter() {
        crearActividad("Pasada", Visibilidad.VISIBLE, LocalDateTime.now().minusDays(1), new HashSet<>());
        crearActividad("Futura", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7), new HashSet<>());

        List<Actividad> result = actividadRepository.findByCursoIdAndFechaLimiteAfter(
                curso.getId(), LocalDateTime.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitulo()).isEqualTo("Futura");
    }

    @Test
    @DisplayName("findByGrupoId devuelve actividades del grupo")
    void findByGrupoId() {
        crearActividad("Solo G1", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7),
                new HashSet<>(Set.of(grupo1)));
        crearActividad("Solo G2", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7),
                new HashSet<>(Set.of(grupo2)));
        crearActividad("Ambos", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7),
                new HashSet<>(Set.of(grupo1, grupo2)));

        List<Actividad> resultG1 = actividadRepository.findByGrupoId(grupo1.getId());
        List<Actividad> resultG2 = actividadRepository.findByGrupoId(grupo2.getId());

        assertThat(resultG1).hasSize(2);
        assertThat(resultG2).hasSize(2);
    }

    @Test
    @DisplayName("findByGrupoIdAndVisibilidad filtra por grupo y visibilidad")
    void findByGrupoIdAndVisibilidad() {
        crearActividad("Visible", Visibilidad.VISIBLE, LocalDateTime.now().plusDays(7),
                new HashSet<>(Set.of(grupo1)));
        crearActividad("Oculta", Visibilidad.OCULTO, LocalDateTime.now().plusDays(7),
                new HashSet<>(Set.of(grupo1)));

        List<Actividad> result = actividadRepository.findByGrupoIdAndVisibilidad(
                grupo1.getId(), Visibilidad.VISIBLE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitulo()).isEqualTo("Visible");
    }
}
