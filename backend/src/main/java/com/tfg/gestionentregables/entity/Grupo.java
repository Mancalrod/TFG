package com.tfg.gestionentregables.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad Grupo - Grupo de prácticas dentro de un curso.
 * Según DAS ENT-005
 */
@Entity
@Table(name = "grupos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título del grupo es obligatorio")
    @Size(max = 100)
    @Column(nullable = false)
    private String titulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    // Relación: Un grupo tiene varios estudiantes
    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Estudiante> estudiantes = new HashSet<>();

    // Relación: Un grupo tiene acceso a varias actividades
    @ManyToMany(mappedBy = "grupos")
    @Builder.Default
    private Set<Actividad> actividades = new HashSet<>();
}
