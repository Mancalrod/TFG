package com.tfg.gestionentregables.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Estudiante - Relación entre Usuario y Grupo.
 * Según DAS ENT-003
 */
@Entity
@Table(name = "estudiantes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"usuario_id", "grupo_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;
}
