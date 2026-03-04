package com.tfg.gestionentregables.dto;

import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialDTO {
    private Long id;
    private String nombre;
    private TipoMaterial tipoMaterial;
    private String ruta;
    private Long tamanoBytes;
    private String oneDriveWebUrl;
    private String oneDriveItemId;
}
