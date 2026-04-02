package com.tfg.gestionentregables.entity;

import com.tfg.gestionentregables.entity.enums.TipoNotificacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Notificacion - Notificaciones in-app para usuarios.
 */
@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @NotBlank
    @Size(max = 300)
    @Column(nullable = false, length = 300)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false)
    @Builder.Default
    private Boolean leida = false;

    @Column(name = "curso_id")
    private Long cursoId;

    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(name = "entregable_id")
    private Long entregableId;

    @Column(name = "entrega_id")
    private Long entregaId;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
