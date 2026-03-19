package com.tfg.gestionentregables.dto;

import com.tfg.gestionentregables.entity.enums.TipoActividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import com.tfg.gestionentregables.entity.ModoOneDrive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearActividadDTO {
    
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200)
    private String titulo;
    
    private String descripcion;
    
    @NotNull(message = "El tipo de actividad es obligatorio")
    private TipoActividad tipoActividad;
    
    private LocalDateTime fechaInicio;
    
    @NotNull(message = "La fecha límite es obligatoria")
    private LocalDateTime fechaLimite;
    
    private Visibilidad visibilidad = Visibilidad.OCULTO;
    
    private Double notaMaxima;
    
    private Long cursoId;
    
    private List<Long> grupoIds;

    private Boolean subirAOneDrive;

    private Long oneDriveUsuarioId;

    private String carpetaOneDrive;

    private ModoOneDrive modoOneDrive = ModoOneDrive.ACTIVIDAD;
}
