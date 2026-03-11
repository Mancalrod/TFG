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
class CursoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CursoRepository cursoRepository;

    private Curso curso1;
    private Curso curso2;
    private Usuario profesor;
    private Usuario alumno;

    @BeforeEach
    void setUp() {
        curso1 = em.persistAndFlush(Curso.builder()
                .titulo("Ingeniería del Software").descripcion("Desc").codigo("IS-001").build());
        curso2 = em.persistAndFlush(Curso.builder()
                .titulo("Bases de Datos").descripcion("Desc").codigo("BDA-001").build());

        profesor = em.persistAndFlush(Usuario.builder()
                .nombre("Prof").correoElectronico("prof@test.com")
                .contrasena("pass123").esAdmin(false).build());
        alumno = em.persistAndFlush(Usuario.builder()
                .nombre("Alumno").correoElectronico("alumno@test.com")
                .contrasena("pass123").esAdmin(false).build());

        em.persistAndFlush(Profesor.builder().usuario(profesor).curso(curso1).build());

        Grupo grupo = em.persistAndFlush(Grupo.builder().titulo("Grupo A").curso(curso2).build());
        em.persistAndFlush(Estudiante.builder().usuario(alumno).grupo(grupo).build());
    }

    @Test
    @DisplayName("findByCodigo encuentra curso por código")
    void findByCodigo() {
        Optional<Curso> result = cursoRepository.findByCodigo("IS-001");

        assertThat(result).isPresent();
        assertThat(result.get().getTitulo()).isEqualTo("Ingeniería del Software");
    }

    @Test
    @DisplayName("findByCodigo devuelve vacío para código inexistente")
    void findByCodigo_noExiste() {
        Optional<Curso> result = cursoRepository.findByCodigo("XXX-999");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByCodigo devuelve true para código existente")
    void existsByCodigo_true() {
        assertThat(cursoRepository.existsByCodigo("IS-001")).isTrue();
    }

    @Test
    @DisplayName("existsByCodigo devuelve false para código inexistente")
    void existsByCodigo_false() {
        assertThat(cursoRepository.existsByCodigo("XXX-999")).isFalse();
    }

    @Test
    @DisplayName("findByProfesorUsuarioId devuelve cursos del profesor")
    void findByProfesorUsuarioId() {
        List<Curso> result = cursoRepository.findByProfesorUsuarioId(profesor.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCodigo()).isEqualTo("IS-001");
    }

    @Test
    @DisplayName("findByEstudianteUsuarioId devuelve cursos del estudiante")
    void findByEstudianteUsuarioId() {
        List<Curso> result = cursoRepository.findByEstudianteUsuarioId(alumno.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCodigo()).isEqualTo("BDA-001");
    }

    @Test
    @DisplayName("findByProfesorUsuarioId devuelve vacío si no tiene cursos")
    void findByProfesorUsuarioId_sinCursos() {
        List<Curso> result = cursoRepository.findByProfesorUsuarioId(alumno.getId());

        assertThat(result).isEmpty();
    }
}
