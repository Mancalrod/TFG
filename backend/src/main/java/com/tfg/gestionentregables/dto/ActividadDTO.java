package com.tfg.gestionentregables.dto;

import com.tfg.gestionentregables.entity.enums.TipoActividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.entity.ModoOneDrive;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActividadDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private TipoActividad tipoActividad;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaLimite;
    private Visibilidad visibilidad;
    private Double notaMaxima;
    private Long cursoId;
    private String cursoTitulo;
    private List<Long> grupoIds;
    private List<EntregableDTO> entregables;
    private Integer numeroEntregables;
    private Integer numeroEntregas;
    private Boolean enPlazo;
    private Boolean subirAOneDrive;
    private Long oneDriveUsuarioId;
    private String carpetaOneDrive;
    private ModoOneDrive modoOneDrive;
}
