package com.tfg.gestionentregables.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalificacionDTO {
    
    @NotNull(message = "La nota es obligatoria")
    @Min(value = 0, message = "La nota mínima es 0")
    @Max(value = 10, message = "La nota máxima es 10")
    private Double nota;
    
    private String comentario;
}
