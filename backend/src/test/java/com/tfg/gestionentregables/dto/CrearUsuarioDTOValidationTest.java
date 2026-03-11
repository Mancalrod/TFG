package com.tfg.gestionentregables.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CrearUsuarioDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private CrearUsuarioDTO dtoValido() {
        return CrearUsuarioDTO.builder()
                .nombre("Juan García")
                .telefono("600123456")
                .correoElectronico("juan@test.com")
                .contrasena("password123")
                .esAdmin(false)
                .build();
    }

    @Test
    @DisplayName("DTO válido no tiene violaciones")
    void dto_valido() {
        Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dtoValido());
        assertThat(violations).isEmpty();
    }

    @Nested
    @DisplayName("Validación de nombre")
    class Nombre {

        @Test
        @DisplayName("Nombre obligatorio: blank genera violación")
        void nombre_blank() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setNombre("");
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getMessage().contains("nombre"));
        }

        @Test
        @DisplayName("Nombre obligatorio: null genera violación")
        void nombre_null() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setNombre(null);
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Nombre demasiado largo genera violación")
        void nombre_muyLargo() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setNombre("A".repeat(101));
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Validación de correo")
    class Correo {

        @Test
        @DisplayName("Correo obligatorio: blank genera violación")
        void correo_blank() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setCorreoElectronico("");
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Correo inválido genera violación")
        void correo_invalido() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setCorreoElectronico("no-es-un-correo");
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getMessage().contains("correo"));
        }

        @Test
        @DisplayName("Correo válido no genera violación")
        void correo_valido() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setCorreoElectronico("test@example.com");
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validación de contraseña")
    class Contrasena {

        @Test
        @DisplayName("Contraseña obligatoria: blank genera violación")
        void contrasena_blank() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setContrasena("");
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Contraseña muy corta genera violación")
        void contrasena_corta() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setContrasena("abc");
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).anyMatch(v -> v.getMessage().contains("6 caracteres"));
        }

        @Test
        @DisplayName("Contraseña de 6 caracteres es válida")
        void contrasena_minima() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setContrasena("abcdef");
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validación de teléfono")
    class Telefono {

        @Test
        @DisplayName("Teléfono demasiado largo genera violación")
        void telefono_muyLargo() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setTelefono("1".repeat(21));
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("Teléfono null es válido (opcional)")
        void telefono_null() {
            CrearUsuarioDTO dto = dtoValido();
            dto.setTelefono(null);
            Set<ConstraintViolation<CrearUsuarioDTO>> violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }
}
