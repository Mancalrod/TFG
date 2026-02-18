package com.tfg.gestionentregables.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Feedback - Mensajes de retroalimentación del profesor.
 * Según DAS ENT-010
 */
@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El comentario es obligatorio")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String comentario;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // Relación: Un feedback pertenece a una entrega
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_id", nullable = false)
    private Entrega entrega;

    // Relación: Un feedback es escrito por un profesor (usuario)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_id", nullable = false)
    private Usuario profesor;
}
