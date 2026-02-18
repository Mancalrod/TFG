package com.tfg.gestionentregables.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearUsuarioDTO {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;
    
    @Size(max = 20)
    private String telefono;
    
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    private String correoElectronico;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String contrasena;
    
    private Boolean esAdmin = false;
}
