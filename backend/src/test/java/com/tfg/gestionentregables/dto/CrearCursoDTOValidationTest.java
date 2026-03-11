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

class CrearCursoDTOValidationTest {

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
        CrearCursoDTO dto = CrearCursoDTO.builder()
                .titulo("Ingeniería del Software")
                .descripcion("Desc")
                .codigo("IS-001")
                .build();
        Set<ConstraintViolation<CrearCursoDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Título obligatorio: blank genera violación")
    void titulo_blank() {
        CrearCursoDTO dto = CrearCursoDTO.builder().titulo("").build();
        Set<ConstraintViolation<CrearCursoDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("título"));
    }

    @Test
    @DisplayName("Título obligatorio: null genera violación")
    void titulo_null() {
        CrearCursoDTO dto = CrearCursoDTO.builder().titulo(null).build();
        Set<ConstraintViolation<CrearCursoDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Título demasiado largo genera violación")
    void titulo_muyLargo() {
        CrearCursoDTO dto = CrearCursoDTO.builder().titulo("A".repeat(201)).build();
        Set<ConstraintViolation<CrearCursoDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Descripción null es válida (opcional)")
    void descripcion_null() {
        CrearCursoDTO dto = CrearCursoDTO.builder().titulo("Curso").descripcion(null).build();
        Set<ConstraintViolation<CrearCursoDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }
}
