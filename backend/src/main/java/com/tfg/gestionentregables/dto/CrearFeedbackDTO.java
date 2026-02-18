package com.tfg.gestionentregables.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearFeedbackDTO {
    
    @NotBlank(message = "El comentario es obligatorio")
    private String comentario;
}
