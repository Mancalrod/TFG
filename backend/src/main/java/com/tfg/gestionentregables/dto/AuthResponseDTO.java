package com.tfg.gestionentregables.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long usuarioId;
    private String nombre;
    private String correoElectronico;
    private List<String> roles;
    private String fotoPerfilUrl;
}
