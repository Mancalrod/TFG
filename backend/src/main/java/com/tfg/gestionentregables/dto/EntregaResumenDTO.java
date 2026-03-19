package com.tfg.gestionentregables.dto;

import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para el listado de entregas para evaluar (SYSOP-015).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaResumenDTO {
    private Long entregaId;
    private Long cursoId;
    private String cursoTitulo;
    private Long actividadId;
    private String actividadTitulo;
    private Long entregableId;
    private String entregableTitulo;
    private Long estudianteId;
    private String estudianteNombre;
    private String estudianteCorreo;
    private String grupoTitulo;
    private LocalDateTime fechaEntrega;
    private EstadoEntrega estado;
    private Double calificacion;
    private Boolean fueATiempo;
    private Integer version;
}

