package com.tfg.gestionentregables.dto;

import com.tfg.gestionentregables.entity.enums.TipoMaterial;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearEntregableDTO {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200)
    private String titulo;

    private String descripcion;

    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha límite es obligatoria")
    private LocalDateTime fechaLimite;

    private Double notaMaxima;

    private TipoMaterial tipoArchivoEsperado;

    private Long tamanoMaximoBytes;

    @Builder.Default
    private Visibilidad visibilidad = Visibilidad.OCULTO;

    @Builder.Default
    private Boolean permiteReenvio = true;

    private String estructuraZip;

    @Builder.Default
    private Boolean validacionZipEstricta = false;

    private String nombreZipEsperado;
}
