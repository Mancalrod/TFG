package com.tfg.gestionentregables.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardarGrupoDTO {

    @NotBlank(message = "El título del grupo es obligatorio")
    private String titulo;

    @NotEmpty(message = "Debes seleccionar al menos un curso")
    private List<Long> cursoIds;
}
