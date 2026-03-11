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

class CrearFeedbackDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("DTO válido no tiene violaciones")
    void dto_valido() {
        CrearFeedbackDTO dto = CrearFeedbackDTO.builder().comentario("Buen trabajo").build();
        Set<ConstraintViolation<CrearFeedbackDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Comentario obligatorio: blank genera violación")
    void comentario_blank() {
        CrearFeedbackDTO dto = CrearFeedbackDTO.builder().comentario("").build();
        Set<ConstraintViolation<CrearFeedbackDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("comentario"));
    }

    @Test
    @DisplayName("Comentario obligatorio: null genera violación")
    void comentario_null() {
        CrearFeedbackDTO dto = CrearFeedbackDTO.builder().comentario(null).build();
        Set<ConstraintViolation<CrearFeedbackDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }
}
