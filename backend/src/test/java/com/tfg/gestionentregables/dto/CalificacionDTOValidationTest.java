package com.tfg.gestionentregables.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CalificacionDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("DTO válido con nota 8.5 no tiene violaciones")
    void dto_valido() {
        CalificacionDTO dto = CalificacionDTO.builder().nota(8.5).comentario("Bien").build();
        Set<ConstraintViolation<CalificacionDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Nota obligatoria: null genera violación")
    void nota_null() {
        CalificacionDTO dto = CalificacionDTO.builder().nota(null).build();
        Set<ConstraintViolation<CalificacionDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("nota es obligatoria"));
    }

    @Test
    @DisplayName("Nota negativa genera violación")
    void nota_negativa() {
        CalificacionDTO dto = CalificacionDTO.builder().nota(-1.0).build();
        Set<ConstraintViolation<CalificacionDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("mínima es 0"));
    }

    @Test
    @DisplayName("Nota mayor que 10 genera violación")
    void nota_mayorDiez() {
        CalificacionDTO dto = CalificacionDTO.builder().nota(10.5).build();
        Set<ConstraintViolation<CalificacionDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("máxima es 10"));
    }

    @Test
    @DisplayName("Nota 0 es válida")
    void nota_cero() {
        CalificacionDTO dto = CalificacionDTO.builder().nota(0.0).build();
        Set<ConstraintViolation<CalificacionDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Nota 10 es válida")
    void nota_diez() {
        CalificacionDTO dto = CalificacionDTO.builder().nota(10.0).build();
        Set<ConstraintViolation<CalificacionDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Comentario null es válido (opcional)")
    void comentario_null() {
        CalificacionDTO dto = CalificacionDTO.builder().nota(5.0).comentario(null).build();
        Set<ConstraintViolation<CalificacionDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }
}
