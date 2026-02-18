package com.tfg.gestionentregables.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas de entregas de un entregable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntregaEstadisticasDTO {
    private Long entregableId;
    private Long totalEntregas;
    private Long entregasATiempo;
    private Long entregasTardias;
    private Long entregasCalificadas;
    private Long entregasPendientes;
    private Double promedioCalificacion;
}
