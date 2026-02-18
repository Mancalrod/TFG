package com.tfg.gestionentregables.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrupoDTO {
    private Long id;
    private String titulo;
    private Long cursoId;
    private String cursoTitulo;
    private Integer numeroEstudiantes;
}
