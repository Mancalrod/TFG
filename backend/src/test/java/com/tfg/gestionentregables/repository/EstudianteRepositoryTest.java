package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EstudianteRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private EstudianteRepository estudianteRepository;

    private Usuario usuario1;
    private Usuario usuario2;
    private Curso curso;
    private Grupo grupo1;
    private Grupo grupo2;

    @BeforeEach
    void setUp() {
        curso = em.persistAndFlush(Curso.builder()
                .titulo("Curso Test").descripcion("Desc").codigo("CT-001").build());

        grupo1 = em.persistAndFlush(Grupo.builder().titulo("Grupo A").curso(curso).build());
        grupo2 = em.persistAndFlush(Grupo.builder().titulo("Grupo B").curso(curso).build());

        usuario1 = em.persistAndFlush(Usuario.builder()
                .nombre("Ana").correoElectronico("ana@test.com")
                .contrasena("pass123").esAdmin(false).build());
        usuario2 = em.persistAndFlush(Usuario.builder()
                .nombre("Pedro").correoElectronico("pedro@test.com")
                .contrasena("pass123").esAdmin(false).build());

        em.persistAndFlush(Estudiante.builder().usuario(usuario1).grupo(grupo1).build());
        em.persistAndFlush(Estudiante.builder().usuario(usuario1).grupo(grupo2).build());
        em.persistAndFlush(Estudiante.builder().usuario(usuario2).grupo(grupo1).build());
    }

    @Test
    @DisplayName("findByUsuarioId devuelve todas las inscripciones del usuario")
    void findByUsuarioId() {
        List<Estudiante> result = estudianteRepository.findByUsuarioId(usuario1.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByGrupoId devuelve estudiantes del grupo")
    void findByGrupoId() {
        List<Estudiante> result = estudianteRepository.findByGrupoId(grupo1.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByUsuarioIdAndGrupoId encuentra inscripción específica")
    void findByUsuarioIdAndGrupoId() {
        Optional<Estudiante> result = estudianteRepository.findByUsuarioIdAndGrupoId(
                usuario1.getId(), grupo1.getId());
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("existsByUsuarioId devuelve true para usuario inscrito")
    void existsByUsuarioId_true() {
        assertThat(estudianteRepository.existsByUsuarioId(usuario1.getId())).isTrue();
    }

    @Test
    @DisplayName("existsByUsuarioIdAndGrupoId devuelve false si no inscrito en ese grupo")
    void existsByUsuarioIdAndGrupoId_false() {
        assertThat(estudianteRepository.existsByUsuarioIdAndGrupoId(
                usuario2.getId(), grupo2.getId())).isFalse();
    }

    @Test
    @DisplayName("findFirstByUsuarioIdAndGrupoCursoId encuentra estudiante por usuario y curso")
    void findFirstByUsuarioIdAndGrupoCursoId() {
        Optional<Estudiante> result = estudianteRepository.findFirstByUsuarioIdAndGrupoCursoId(
                usuario1.getId(), curso.getId());
        assertThat(result).isPresent();
    }
}
