package com.tfg.gestionentregables.entity;

import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Entidad Material - Archivos de apoyo o entregas.
 * Según DAS ENT-007
 */
@Entity
@Table(name = "materiales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo de material es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_material", nullable = false)
    private TipoMaterial tipoMaterial;

    @NotBlank(message = "La ruta es obligatoria")
    @Column(nullable = false)
    private String ruta;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    // Relación: Un material puede pertenecer a una actividad (material de apoyo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    // Relación: Un material puede pertenecer a un entregable (material de apoyo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregable_id")
    private Entregable entregable;

    // Relación: Un material puede ser parte de una entrega (archivo subido por alumno)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrega_id")
    private Entrega entrega;
}
