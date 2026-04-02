package com.tfg.gestionentregables.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
            IllegalArgumentException ex = new IllegalArgumentException("Parametro invalido");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleIllegalArgument(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Parametro invalido");
        }
    }

    @Nested
    @DisplayName("handleIllegalState")
    class HandleIllegalState {

        @Test
        @DisplayName("Devuelve 409 con mensaje")
        void illegalState() {
            IllegalStateException ex = new IllegalStateException("Estado invalido");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleIllegalState(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(409);
            assertThat(response.getBody().getMessage()).isEqualTo("Estado invalido");
        }
    }

    @Nested
    @DisplayName("handleValidationExceptions")
    class HandleValidation {

        @Test
        @DisplayName("Devuelve 400 con mapa de errores de validacion")
        void validationErrors() throws NoSuchMethodException {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "objectName");
            bindingResult.addError(new FieldError("objectName", "titulo", "El titulo es obligatorio"));
            bindingResult.addError(new FieldError("objectName", "email", "El email no es valido"));

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
            assertThat(response.getBody().getMessage()).contains("Error de valid");
            assertThat(response.getBody().getErrors()).containsEntry("titulo", "El titulo es obligatorio");
            assertThat(response.getBody().getErrors()).containsEntry("email", "El email no es valido");
        }
    }

    void dummyMethod(String param) {
        // metodo auxiliar para MethodParameter
    }

    @Nested
    @DisplayName("handleNoResource")
    class HandleNoResource {

        @Test
        @DisplayName("Devuelve 404 cuando no existe recurso")
        void noResource() {
            NoResourceFoundException ex = new NoResourceFoundException(
                    HttpMethod.GET,
                    "/ruta/inexistente",
                    "No existe"
            );

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleNoResource(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).contains("/ruta/inexistente");
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleMaxUploadSize")
    class HandleMaxUploadSize {

        @Test
        @DisplayName("Devuelve 413 con mensaje claro")
        void maxUpload() {
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024L);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleMaxUploadSize(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(413);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(413);
            assertThat(response.getBody().getMessage()).contains("tam");
            assertThat(response.getBody().getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("handleMultipart")
    class HandleMultipart {

        @Test
        @DisplayName("Devuelve 400 cuando hay error multipart")
        void multipart() {
            MultipartException ex = new MultipartException("archivo corrupto");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleMultipart(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).contains("procesar");
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("Devuelve 500 con mensaje generico")
        void genericException() {
            Exception ex = new RuntimeException("Algo salio mal");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleGenericException(ex);

            assertThat(response.getStatusCode().value()).isEqualTo(500);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(500);
            assertThat(response.getBody().getMessage()).isEqualTo("Error interno del servidor");
        }
    }

    @Nested
    @DisplayName("Response DTOs")
    class ResponseDtos {

        @Test
        @DisplayName("ErrorResponse cubre equals/hashCode/toString y setters")
        void errorResponsePojo() {
            LocalDateTime now = LocalDateTime.now();
            GlobalExceptionHandler.ErrorResponse r1 =
                    new GlobalExceptionHandler.ErrorResponse(400, "msg", now);
            GlobalExceptionHandler.ErrorResponse r2 =
                    new GlobalExceptionHandler.ErrorResponse(400, "msg", now);

            assertThat(r1).isEqualTo(r2).hasSameHashCodeAs(r2);
            assertThat(r1.toString()).contains("status=400");

            r1.setStatus(409);
            r1.setMessage("otro");
            r1.setTimestamp(now.plusSeconds(1));

            assertThat(r1.getStatus()).isEqualTo(409);
            assertThat(r1.getMessage()).isEqualTo("otro");
            assertThat(r1.getTimestamp()).isEqualTo(now.plusSeconds(1));
        }

        @Test
        @DisplayName("ValidationErrorResponse cubre equals/hashCode/toString y setters")
        void validationErrorResponsePojo() {
            LocalDateTime now = LocalDateTime.now();
            Map<String, String> errors = Map.of("campo", "obligatorio");

            GlobalExceptionHandler.ValidationErrorResponse r1 =
                    new GlobalExceptionHandler.ValidationErrorResponse(400, "validacion", now, errors);
            GlobalExceptionHandler.ValidationErrorResponse r2 =
                    new GlobalExceptionHandler.ValidationErrorResponse(400, "validacion", now, errors);

            assertThat(r1).isEqualTo(r2).hasSameHashCodeAs(r2);
            assertThat(r1.toString()).contains("validacion");

            r1.setStatus(422);
            r1.setMessage("otra");
            r1.setTimestamp(now.plusSeconds(2));
            r1.setErrors(Map.of("otro", "error"));

            assertThat(r1.getStatus()).isEqualTo(422);
            assertThat(r1.getMessage()).isEqualTo("otra");
            assertThat(r1.getTimestamp()).isEqualTo(now.plusSeconds(2));
            assertThat(r1.getErrors()).containsEntry("otro", "error");
        }
    }
}
