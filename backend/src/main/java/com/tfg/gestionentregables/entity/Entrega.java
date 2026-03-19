package com.tfg.gestionentregables.entity;

import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad Entrega - La entrega que realiza un alumno a un entregable.
 * Según DAS ENT-009
 */
@Entity
@Table(name = "entregas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @NotNull(message = "La fecha de entrega es obligatoria")
    @Column(name = "fecha_entrega", nullable = false)
    @Builder.Default
    private LocalDateTime fechaEntrega = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoEntrega estado = EstadoEntrega.ENTREGADO;

    @Column(name = "calificacion")
    private Double calificacion;

    @Column(name = "fecha_calificacion")
    private LocalDateTime fechaCalificacion;

    @Column(name = "es_version_activa")
    @Builder.Default
    private Boolean esVersionActiva = true;

    /**
     * Comentario/observaciones del alumno al realizar la entrega.
     * Permite entregas de solo texto o agregar notas a entregas con archivos.
     */
    @Column(name = "comentario_alumno", columnDefinition = "TEXT")
    private String comentarioAlumno;

    // Relación: Una entrega pertenece a un entregable
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregable_id", nullable = false)
    private Entregable entregable;

    // Relación: Una entrega es realizada por un estudiante
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    // Relación: Una entrega tiene varios archivos (materiales)
    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Material> archivos = new HashSet<>();

    // Relación: Una entrega puede tener feedback del profesor
    @OneToMany(mappedBy = "entrega", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Feedback> feedbacks = new HashSet<>();

    /**
     * Verifica si la entrega fue a tiempo.
     */
    public boolean fueATiempo() {
        return fechaEntrega.isBefore(entregable.getFechaLimite()) || 
               fechaEntrega.isEqual(entregable.getFechaLimite());
    }

    /**
     * Marca la entrega como calificada.
     */
    public void calificar(Double nota) {
        this.calificacion = nota;
        this.fechaCalificacion = LocalDateTime.now();
        this.estado = EstadoEntrega.CALIFICADO;
    }

    /**
     * Publica la calificación para que el alumno la vea.
     */
    public void publicarNota() {
        this.estado = EstadoEntrega.PUBLICADO;
    }
}
