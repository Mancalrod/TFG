package com.tfg.gestionentregables.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioDTO {
    private Long id;
    private String nombre;
    private String telefono;
    private String correoElectronico;
    private Boolean esAdmin;
    private String fotoPerfilUrl;
}
