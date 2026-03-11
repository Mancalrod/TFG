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
class ProfesorRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProfesorRepository profesorRepository;

    private Usuario usuario1;
    private Usuario usuario2;
    private Curso curso1;
    private Curso curso2;

    @BeforeEach
    void setUp() {
        curso1 = em.persistAndFlush(Curso.builder()
                .titulo("Curso 1").descripcion("Desc").codigo("C-001").build());
        curso2 = em.persistAndFlush(Curso.builder()
                .titulo("Curso 2").descripcion("Desc").codigo("C-002").build());

        usuario1 = em.persistAndFlush(Usuario.builder()
                .nombre("Prof 1").correoElectronico("prof1@test.com")
                .contrasena("pass123").esAdmin(false).build());
        usuario2 = em.persistAndFlush(Usuario.builder()
                .nombre("Prof 2").correoElectronico("prof2@test.com")
                .contrasena("pass123").esAdmin(false).build());

        em.persistAndFlush(Profesor.builder().usuario(usuario1).curso(curso1).build());
        em.persistAndFlush(Profesor.builder().usuario(usuario1).curso(curso2).build());
        em.persistAndFlush(Profesor.builder().usuario(usuario2).curso(curso1).build());
    }

    @Test
    @DisplayName("findByUsuarioId devuelve asignaciones del profesor")
    void findByUsuarioId() {
        List<Profesor> result = profesorRepository.findByUsuarioId(usuario1.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByCursoId devuelve profesores del curso")
    void findByCursoId() {
        List<Profesor> result = profesorRepository.findByCursoId(curso1.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByUsuarioIdAndCursoId encuentra asignación específica")
    void findByUsuarioIdAndCursoId() {
        Optional<Profesor> result = profesorRepository.findByUsuarioIdAndCursoId(
                usuario1.getId(), curso1.getId());
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("existsByUsuarioId devuelve true para profesor existente")
    void existsByUsuarioId_true() {
        assertThat(profesorRepository.existsByUsuarioId(usuario1.getId())).isTrue();
    }

    @Test
    @DisplayName("existsByUsuarioIdAndCursoId devuelve false si no asignado a ese curso")
    void existsByUsuarioIdAndCursoId_false() {
        assertThat(profesorRepository.existsByUsuarioIdAndCursoId(
                usuario2.getId(), curso2.getId())).isFalse();
    }
}
