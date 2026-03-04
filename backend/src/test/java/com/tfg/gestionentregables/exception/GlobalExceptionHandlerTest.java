package com.tfg.gestionentregables.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleEntityNotFound")
    class HandleEntityNotFound {

        @Test
        @DisplayName("Devuelve 404 con mensaje")
        void entityNotFound() {
            EntityNotFoundException ex = new EntityNotFoundException("Actividad no encontrada");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleEntityNotFound(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).isEqualTo("Actividad no encontrada");
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleIllegalArgument")
    class HandleIllegalArgument {

        @Test
        @DisplayName("Devuelve 400 con mensaje")
        void illegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException("Parámetro inválido");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Parámetro inválido");
        }
    }

    @Nested
    @DisplayName("handleIllegalState")
    class HandleIllegalState {

        @Test
        @DisplayName("Devuelve 409 con mensaje")
        void illegalState() {
            IllegalStateException ex = new IllegalStateException("Estado inválido");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleIllegalState(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(409);
            assertThat(response.getBody().getMessage()).isEqualTo("Estado inválido");
        }
    }

    @Nested
    @DisplayName("handleValidationExceptions")
    class HandleValidation {

        @Test
        @DisplayName("Devuelve 400 con mapa de errores de validación")
        void validationErrors() throws NoSuchMethodException {
            // Crear un BindingResult con errores de campo
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "objectName");
            bindingResult.addError(new FieldError("objectName", "titulo",
                    "El título es obligatorio"));
            bindingResult.addError(new FieldError("objectName", "email",
                    "El email no es válido"));

            MethodParameter methodParameter = new MethodParameter(
                    GlobalExceptionHandlerTest.class
                            .getDeclaredMethod("dummyMethod", String.class), 0);

            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(methodParameter, bindingResult);

            ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
                    handler.handleValidationExceptions(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Error de validación");
            assertThat(response.getBody().getErrors()).containsEntry("titulo", "El título es obligatorio");
            assertThat(response.getBody().getErrors()).containsEntry("email", "El email no es válido");
        }
    }
    // M\u00e9todo auxiliar para crear MethodParameter en tests de validaci\u00f3n
    @SuppressWarnings("unused")
    void dummyMethod(String param) {}
    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("Devuelve 500 con mensaje genérico")
        void genericException() {
            Exception ex = new RuntimeException("Algo salió mal");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleGenericException(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getMessage()).contains("Algo salió mal");
        }
    }
}
