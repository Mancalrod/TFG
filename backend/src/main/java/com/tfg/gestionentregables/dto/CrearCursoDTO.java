package com.tfg.gestionentregables.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearCursoDTO {
    
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200)
    private String titulo;
    
    private String descripcion;
    
    private String codigo;
}
