package com.tfg.gestionentregables.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarUsuarioDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 20)
    private String telefono;

    @NotBlank(message = "El correo electronico es obligatorio")
    @Email(message = "El correo electronico debe ser valido")
    private String correoElectronico;

    private String contrasena;

    @Builder.Default
    private Boolean esAdmin = false;
}
