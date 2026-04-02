package com.tfg.gestionentregables.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para las preferencias de notificación del usuario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaNotificacionDTO {

    @NotNull(message = "El canal de notificación es obligatorio")
    private String canal; // APP, EMAIL, AMBOS
}
