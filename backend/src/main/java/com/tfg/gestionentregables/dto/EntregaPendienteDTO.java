package com.tfg.gestionentregables.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para entregas pendientes del alumno.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaPendienteDTO {

    private Long entregableId;
    private String entregableTitulo;
    private Long actividadId;
    private String actividadTitulo;
    private Long cursoId;
    private String cursoTitulo;
    private LocalDateTime fechaLimite;
    private String tiempoRestante;
}
