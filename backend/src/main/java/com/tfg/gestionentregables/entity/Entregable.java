package com.tfg.gestionentregables.entity;

import com.tfg.gestionentregables.entity.enums.TipoMaterial;
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
 * Entidad Entregable - Subapartados de las actividades.
 * Según DAS ENT-008
 */
@Entity
@Table(name = "entregables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entregable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200)
    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha límite es obligatoria")
    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    @Column(name = "nota_maxima")
    private Double notaMaxima;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_archivo_esperado")
    private TipoMaterial tipoArchivoEsperado;

    @Column(name = "tamano_maximo_bytes")
    private Long tamanoMaximoBytes;

    @NotNull(message = "La visibilidad es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Visibilidad visibilidad = Visibilidad.OCULTO;

    @Column(name = "permite_reenvio")
    @Builder.Default
    private Boolean permiteReenvio = true;

    /**
     * Estructura esperada del ZIP como JSON.
     * Contiene un array de nodos (archivos/carpetas) con nombres y extensiones
     * permitidas.
     */
    @Column(name = "estructura_zip", columnDefinition = "TEXT")
    private String estructuraZip;

    /**
     * Nombre esperado del archivo ZIP entregado (sin extensión).
     * Si es nulo, vacío o "*", se acepta cualquier nombre.
     */
    @Column(name = "nombre_zip_esperado")
    private String nombreZipEsperado;

    /**
     * Modo de validación del ZIP:
     * true = el ZIP debe contener exactamente los archivos definidos,
     * false = el ZIP debe contener al menos los archivos definidos (puede tener
     * más).
     */
    @Column(name = "validacion_zip_estricta")
    @Builder.Default
    private Boolean validacionZipEstricta = false;

    // Relación: Un entregable pertenece a una actividad
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    // Relación: Un entregable tiene varias entregas de alumnos
    @OneToMany(mappedBy = "entregable", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Entrega> entregas = new HashSet<>();

    // Relación: Un entregable puede tener materiales de apoyo
    @OneToMany(mappedBy = "entregable", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Material> materiales = new HashSet<>();

    /**
     * Verifica si el entregable está en plazo.
     */
    public boolean estaEnPlazo() {
        LocalDateTime ahora = LocalDateTime.now();
        boolean despuesDeInicio = fechaInicio == null || ahora.isAfter(fechaInicio);
        boolean antesDeFinalizacion = ahora.isBefore(fechaLimite);
        return despuesDeInicio && antesDeFinalizacion;
    }

    /**
     * Verifica si el entregable es visible para alumnos.
     */
    public boolean esVisibleParaAlumnos() {
        return visibilidad == Visibilidad.VISIBLE;
    }
}
