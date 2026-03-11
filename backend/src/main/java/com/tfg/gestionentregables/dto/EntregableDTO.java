package com.tfg.gestionentregables.dto;

import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregableDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaLimite;
    private Double notaMaxima;
    private TipoMaterial tipoArchivoEsperado;
    private Long tamanoMaximoBytes;
    private Visibilidad visibilidad;
    private Boolean permiteReenvio;
    private String estructuraZip;
    private Boolean validacionZipEstricta;
    private String nombreZipEsperado;
    private Long actividadId;
    private String actividadTitulo;
    private Long numeroEntregas;
    private Boolean enPlazo;
}
