package com.tfg.gestionentregables.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad Curso - Representa una asignatura.
 * Según DAS ENT-004
 */
@Entity
@Table(name = "cursos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200)
    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(unique = true)
    private String codigo;

    // Relación: Un curso tiene varios profesores
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Profesor> profesores = new HashSet<>();

    // Relación: Un curso tiene varios grupos
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Grupo> grupos = new HashSet<>();

    // Relación: Un curso tiene varias actividades
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Actividad> actividades = new HashSet<>();

    // Métodos helper para gestionar profesores
    public void addProfesor(Profesor profesor) {
        profesores.add(profesor);
        profesor.setCurso(this);
    }

    public void removeProfesor(Profesor profesor) {
        profesores.remove(profesor);
        profesor.setCurso(null);
    }

    // Métodos helper para gestionar grupos
    public void addGrupo(Grupo grupo) {
        grupos.add(grupo);
        grupo.setCurso(this);
    }

    public void removeGrupo(Grupo grupo) {
        grupos.remove(grupo);
        grupo.setCurso(null);
    }
}
