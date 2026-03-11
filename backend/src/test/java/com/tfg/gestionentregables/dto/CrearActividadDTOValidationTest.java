package com.tfg.gestionentregables.dto;

import com.tfg.gestionentregables.entity.enums.TipoActividad;
import com.tfg.gestionentregables.entity.enums.Visibilidad;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CrearActividadDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private CrearActividadDTO dtoValido() {
        return CrearActividadDTO.builder()
                .titulo("Práctica 1")
                .descripcion("Descripción de la práctica")
                .tipoActividad(TipoActividad.EVALUABLE)
                .fechaInicio(LocalDateTime.now())
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .visibilidad(Visibilidad.VISIBLE)
                .notaMaxima(10.0)
                .cursoId(1L)
                .grupoIds(List.of(1L))
                .build();
    }

    @Test
    @DisplayName("DTO válido no tiene violaciones")
    void dto_valido() {
        Set<ConstraintViolation<CrearActividadDTO>> violations = validator.validate(dtoValido());
        assertThat(violations).isEmpty();
    }

    @Nested
    @DisplayName("Validación de título")
    class Titulo {

        @Test
        @DisplayName("Título obligatorio: blank genera violación")
        void titulo_blank() {
            CrearActividadDTO dto = dtoValido();
            dto.setTitulo("");
            Set<ConstraintViolation<CrearActividadDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getMessage().contains("título"));
        }

        @Test
        @DisplayName("Título demasiado largo genera violación")
        void titulo_muyLargo() {
            CrearActividadDTO dto = dtoValido();
            dto.setTitulo("A".repeat(201));
            Set<ConstraintViolation<CrearActividadDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Validación de tipoActividad")
    class TipoActividadValidacion {

        @Test
        @DisplayName("Tipo de actividad obligatorio: null genera violación")
        void tipo_null() {
            CrearActividadDTO dto = dtoValido();
            dto.setTipoActividad(null);
            Set<ConstraintViolation<CrearActividadDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getMessage().contains("tipo de actividad"));
        }
    }

    @Nested
    @DisplayName("Validación de fechaLimite")
    class FechaLimite {

        @Test
        @DisplayName("Fecha límite obligatoria: null genera violación")
        void fechaLimite_null() {
            CrearActividadDTO dto = dtoValido();
            dto.setFechaLimite(null);
            Set<ConstraintViolation<CrearActividadDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getMessage().contains("fecha límite"));
        }
    }
}
