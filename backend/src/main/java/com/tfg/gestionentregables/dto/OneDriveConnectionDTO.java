package com.tfg.gestionentregables.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para la información de conexión con OneDrive.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneDriveConnectionDTO {
    private boolean conectado;
    private String microsoftEmail;
    private LocalDateTime fechaConexion;
    private LocalDateTime fechaUltimoUso;
    private boolean integrationEnabled;
}
