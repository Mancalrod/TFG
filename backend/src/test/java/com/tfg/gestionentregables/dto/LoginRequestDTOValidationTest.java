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

class LoginRequestDTOValidationTest {

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
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .correoElectronico("test@example.com")
                .contrasena("password123")
                .build();
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Correo obligatorio: blank genera violación")
    void correo_blank() {
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .correoElectronico("")
                .contrasena("password123")
                .build();
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Correo inválido genera violación")
    void correo_invalido() {
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .correoElectronico("no-es-correo")
                .contrasena("password123")
                .build();
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("correo"));
    }

    @Test
    @DisplayName("Contraseña obligatoria: blank genera violación")
    void contrasena_blank() {
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .correoElectronico("test@example.com")
                .contrasena("")
                .build();
        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("contraseña"));
    }
}
