package com.tfg.gestionentregables.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshTokenRequestDTO {
    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
