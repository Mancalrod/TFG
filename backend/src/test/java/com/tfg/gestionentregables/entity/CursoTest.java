package com.tfg.gestionentregables.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CursoTest {

    private Curso curso;

    @BeforeEach
    void setUp() {
        curso = Curso.builder()
                .id(1L)
                .titulo("Ingeniería del Software")
                .codigo("IS-001")
                .build();
    }

    @Test
    @DisplayName("addProfesor añade profesor y establece relación bidireccional")
    void addProfesor() {
        Usuario usuario = Usuario.builder().id(1L).nombre("Juan").build();
        Profesor profesor = Profesor.builder().id(1L).usuario(usuario).build();

        curso.addProfesor(profesor);

        assertThat(curso.getProfesores()).contains(profesor);
        assertThat(profesor.getCurso()).isEqualTo(curso);
    }

    @Test
    @DisplayName("removeProfesor elimina profesor y rompe relación bidireccional")
    void removeProfesor() {
        Usuario usuario = Usuario.builder().id(1L).nombre("Juan").build();
        Profesor profesor = Profesor.builder().id(1L).usuario(usuario).curso(curso).build();
        curso.getProfesores().add(profesor);

        curso.removeProfesor(profesor);

        assertThat(curso.getProfesores()).doesNotContain(profesor);
        assertThat(profesor.getCurso()).isNull();
    }

    @Test
    @DisplayName("addGrupo añade grupo y establece relación bidireccional")
    void addGrupo() {
        Grupo grupo = Grupo.builder().id(1L).titulo("Grupo A").build();

        curso.addGrupo(grupo);

        assertThat(curso.getGrupos()).contains(grupo);
        assertThat(grupo.getCurso()).isEqualTo(curso);
    }

    @Test
    @DisplayName("removeGrupo elimina grupo y rompe relación bidireccional")
    void removeGrupo() {
        Grupo grupo = Grupo.builder().id(1L).titulo("Grupo A").curso(curso).build();
        curso.getGrupos().add(grupo);

        curso.removeGrupo(grupo);

        assertThat(curso.getGrupos()).doesNotContain(grupo);
        assertThat(grupo.getCurso()).isNull();
    }
}
