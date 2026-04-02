package com.tfg.gestionentregables.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para notificaciones del usuario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionDTO {

    private Long id;
    private String tipo;
    private String titulo;
    private String mensaje;
    private Boolean leida;
    private Long cursoId;
    private LocalDateTime fechaCreacion;
}
