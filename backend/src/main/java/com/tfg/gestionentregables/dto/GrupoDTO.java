package com.tfg.gestionentregables.dto;

import lombok.*;

import java.util.List;

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
    private List<Long> cursoIds;
    private List<String> cursoTitulos;
    private Integer numeroEstudiantes;
}
