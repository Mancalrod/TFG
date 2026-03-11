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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class GrupoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private GrupoRepository grupoRepository;

    private Curso curso1;
    private Curso curso2;

    @BeforeEach
    void setUp() {
        curso1 = em.persistAndFlush(Curso.builder()
                .titulo("Curso 1").descripcion("Desc").codigo("C-001").build());
        curso2 = em.persistAndFlush(Curso.builder()
                .titulo("Curso 2").descripcion("Desc").codigo("C-002").build());

        em.persistAndFlush(Grupo.builder().titulo("G1-A").curso(curso1).build());
        em.persistAndFlush(Grupo.builder().titulo("G1-B").curso(curso1).build());
        em.persistAndFlush(Grupo.builder().titulo("G2-A").curso(curso2).build());
    }

    @Test
    @DisplayName("findByCursoId devuelve grupos del curso")
    void findByCursoId() {
        List<Grupo> result = grupoRepository.findByCursoId(curso1.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findByCursoId devuelve vacío para curso sin grupos")
    void findByCursoId_sinGrupos() {
        Curso cursoVacio = em.persistAndFlush(Curso.builder()
                .titulo("Vacío").descripcion("Desc").codigo("V-001").build());

        List<Grupo> result = grupoRepository.findByCursoId(cursoVacio.getId());

        assertThat(result).isEmpty();
    }
}
