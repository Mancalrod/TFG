package com.tfg.gestionentregables.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Profesor - Relación entre Usuario y Curso.
 * Según DAS ENT-002
 */
@Entity
@Table(name = "profesores", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "curso_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profesor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;
}
