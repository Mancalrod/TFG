package com.tfg.gestionentregables.dto;

import com.tfg.gestionentregables.entity.enums.EstadoEntrega;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaDTO {
    private Long id;
    private String nombre;
    private Integer version;
    private LocalDateTime fechaEntrega;
    private EstadoEntrega estado;
    private Double calificacion;
    private LocalDateTime fechaCalificacion;
    private Boolean esVersionActiva;
    private Long entregableId;
    private String entregableTitulo;
    private Long estudianteId;
    private String estudianteNombre;
    private Boolean fueATiempo;
    private List<MaterialDTO> archivos;
    private List<FeedbackDTO> feedbacks;
}
