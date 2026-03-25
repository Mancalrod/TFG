package com.tfg.gestionentregables.entity;

import com.tfg.gestionentregables.entity.enums.TipoActividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad Actividad - Representa una tarea o práctica.
 * Según DAS ENT-006
 */
@Entity
@Table(name = "actividades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200)
    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @NotNull(message = "El tipo de actividad es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_actividad", nullable = false)
    private TipoActividad tipoActividad;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha límite es obligatoria")
    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    @NotNull(message = "La visibilidad es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibilidad visibilidad = Visibilidad.OCULTO;

    @Column(name = "nota_maxima")
    private Double notaMaxima;

    /**
     * Si true, las entregas de esta actividad se suben al OneDrive del profesor.
     */
    @Column(name = "subir_a_onedrive")
    @Builder.Default
    private Boolean subirAOneDrive = false;

    /**
     * ID del usuario (profesor) cuyo OneDrive se usará para almacenar entregas.
     * Solo se establece cuando subirAOneDrive = true.
     */
    @Column(name = "onedrive_usuario_id")
    private Long oneDriveUsuarioId;

    /**
     * ID o ruta de la carpeta de OneDrive seleccionada para la actividad.
     * Si es nulo, se genera automáticamente.
     */
    @Column(name = "carpeta_onedrive")
    private String carpetaOneDrive;

    /**
     * Define si el destino se gestiona a nivel de Actividad o a nivel de Entregables.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "modo_onedrive")
    @Builder.Default
    private ModoOneDrive modoOneDrive = ModoOneDrive.ACTIVIDAD;

    // Relación: Una actividad pertenece a un curso
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    // Relación: Una actividad puede estar asignada a varios grupos
    @ManyToMany
    @JoinTable(
        name = "actividades_grupos",
        joinColumns = @JoinColumn(name = "actividad_id"),
        inverseJoinColumns = @JoinColumn(name = "grupo_id")
    )
    @Builder.Default
    private Set<Grupo> grupos = new HashSet<>();

    // Relación: Una actividad tiene varios entregables
    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Entregable> entregables = new HashSet<>();

    // Relación: Una actividad puede tener materiales de apoyo
    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Material> materiales = new HashSet<>();

    /**
     * Verifica si la actividad está en plazo.
     */
    public boolean estaEnPlazo() {
        LocalDateTime ahora = LocalDateTime.now();
        boolean despuesDeInicio = fechaInicio == null || ahora.isAfter(fechaInicio);
        boolean antesDeFinalizacion = ahora.isBefore(fechaLimite);
        return despuesDeInicio && antesDeFinalizacion;
    }

    /**
     * Verifica si la actividad es visible para alumnos.
     */
    public boolean esVisibleParaAlumnos() {
        return visibilidad == Visibilidad.VISIBLE;
    }
}
