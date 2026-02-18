package com.tfg.gestionentregables.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoDTO {
    private Long id;
    private String titulo;
    private String descripcion;
    private String codigo;
    private List<GrupoDTO> grupos;
    private Integer numeroActividades;
    private Integer numeroProfesores;
    private Integer numeroEstudiantes;
}
