package com.tfg.gestionentregables.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CrearEntregableDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private CrearEntregableDTO dtoValido() {
        return CrearEntregableDTO.builder()
                .titulo("Entregable 1")
                .descripcion("Descripción")
                .fechaLimite(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    @DisplayName("DTO válido no tiene violaciones")
    void dto_valido() {
        Set<ConstraintViolation<CrearEntregableDTO>> violations = validator.validate(dtoValido());
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Título obligatorio: blank genera violación")
    void titulo_blank() {
        CrearEntregableDTO dto = dtoValido();
        dto.setTitulo("");
        Set<ConstraintViolation<CrearEntregableDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("título"));
    }

    @Test
    @DisplayName("Título demasiado largo genera violación")
    void titulo_muyLargo() {
        CrearEntregableDTO dto = dtoValido();
        dto.setTitulo("A".repeat(201));
        Set<ConstraintViolation<CrearEntregableDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Fecha límite obligatoria: null genera violación")
    void fechaLimite_null() {
        CrearEntregableDTO dto = dtoValido();
        dto.setFechaLimite(null);
        Set<ConstraintViolation<CrearEntregableDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("fecha límite"));
    }
}
