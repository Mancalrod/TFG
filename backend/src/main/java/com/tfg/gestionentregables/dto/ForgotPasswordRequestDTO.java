package com.tfg.gestionentregables.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequestDTO {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    private String correoElectronico;
}
