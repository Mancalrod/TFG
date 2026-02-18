package com.tfg.gestionentregables.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackDTO {
    private Long id;
    private String comentario;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private Long entregaId;
    private Long profesorId;
    private String profesorNombre;
}
