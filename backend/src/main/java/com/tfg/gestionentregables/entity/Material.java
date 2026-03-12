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

    /**
     * Ruta local del archivo. Puede ser null si el archivo está en OneDrive.
     */
    @Column(nullable = true)
    private String ruta;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    // === Campos de OneDrive ===

    /**
     * ID del archivo en OneDrive (null si se almacena localmente).
     */
    @Column(name = "onedrive_file_id")
    private String onedriveFileId;

    /**
     * URL web del archivo en OneDrive.
     */
    @Column(name = "onedrive_web_url")
    private String onedriveWebUrl;

    /**
     * ID del usuario cuyo OneDrive almacena el archivo.
     * Necesario para obtener el access token correcto al descargar.
     */
    @Column(name = "onedrive_owner_id")
    private Long onedriveOwnerId;

    // === Campos de Cloudinary ===

    /**
     * Public ID del archivo en Cloudinary (null si no se almacena en Cloudinary).
     */
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    /**
     * URL segura del archivo en Cloudinary.
     */
    @Column(name = "cloudinary_url")
    private String cloudinaryUrl;

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
