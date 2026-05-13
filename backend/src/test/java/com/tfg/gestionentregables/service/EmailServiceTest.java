package com.tfg.gestionentregables.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Nested
    @DisplayName("isProduction")
    class IsProduction {

        @Test
        @DisplayName("true cuando NODE_ENV es production")
        void nodeEnvProduction() {
            ReflectionTestUtils.setField(emailService, "nodeEnv", "production");
            ReflectionTestUtils.setField(emailService, "activeProfile", "local");

            assertThat(emailService.isProduction()).isTrue();
        }

        @Test
        @DisplayName("true cuando spring profile es prod")
        void profileProd() {
            ReflectionTestUtils.setField(emailService, "nodeEnv", "");
            ReflectionTestUtils.setField(emailService, "activeProfile", "prod");

            assertThat(emailService.isProduction()).isTrue();
        }

        @Test
        @DisplayName("false cuando no es entorno productivo")
        void nonProduction() {
            ReflectionTestUtils.setField(emailService, "nodeEnv", "development");
            ReflectionTestUtils.setField(emailService, "activeProfile", "local");

            assertThat(emailService.isProduction()).isFalse();
        }
    }

    @Test
    @DisplayName("enviarCorreo usa SMTP en no-produccion")
    void enviarCorreo_smtp() {
        ReflectionTestUtils.setField(emailService, "nodeEnv", "development");
        ReflectionTestUtils.setField(emailService, "activeProfile", "local");
        ReflectionTestUtils.setField(emailService, "emailFromEmail", "noreply@test.com");

        emailService.enviarCorreo("destino@test.com", "Asunto", "Mensaje");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreo no explota en error SMTP")
    void enviarCorreo_smtpError() {
        ReflectionTestUtils.setField(emailService, "nodeEnv", "development");
        ReflectionTestUtils.setField(emailService, "activeProfile", "local");
        ReflectionTestUtils.setField(emailService, "emailFromEmail", "noreply@test.com");
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.enviarCorreo("destino@test.com", "Asunto", "Mensaje");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreo en produccion con Brevo vacio no usa SMTP")
    void enviarCorreo_productionWithoutBrevoKey() {
        ReflectionTestUtils.setField(emailService, "nodeEnv", "production");
        ReflectionTestUtils.setField(emailService, "activeProfile", "prod");
        ReflectionTestUtils.setField(emailService, "emailProvider", "brevo");
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "");

        emailService.enviarCorreo("destino@test.com", "Asunto", "Mensaje");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreo en produccion con Resend vacio no usa SMTP")
    void enviarCorreo_productionWithoutResendKey() {
        ReflectionTestUtils.setField(emailService, "nodeEnv", "production");
        ReflectionTestUtils.setField(emailService, "activeProfile", "prod");
        ReflectionTestUtils.setField(emailService, "emailProvider", "resend");
        ReflectionTestUtils.setField(emailService, "resendApiKey", "");

        emailService.enviarCorreo("destino@test.com", "Asunto", "Mensaje");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreo en produccion con Resend sin from-email no envia")
    void enviarCorreo_productionResendNoFromEmail() {
        ReflectionTestUtils.setField(emailService, "nodeEnv", "production");
        ReflectionTestUtils.setField(emailService, "activeProfile", "prod");
        ReflectionTestUtils.setField(emailService, "emailProvider", "resend");
        ReflectionTestUtils.setField(emailService, "resendApiKey", "re_test_key");
        ReflectionTestUtils.setField(emailService, "resendFromEmail", "");
        ReflectionTestUtils.setField(emailService, "emailFromEmail", "");
        ReflectionTestUtils.setField(emailService, "brevoFromEmail", "");
        ReflectionTestUtils.setField(emailService, "sendGridFromEmail", "");
        ReflectionTestUtils.setField(emailService, "smtpUsername", "");

        emailService.enviarCorreo("destino@test.com", "Asunto", "Mensaje");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreo en produccion con SendGrid vacio no usa SMTP")
    void enviarCorreo_productionWithoutSendGridKey() {
        ReflectionTestUtils.setField(emailService, "nodeEnv", "production");
        ReflectionTestUtils.setField(emailService, "activeProfile", "prod");
        ReflectionTestUtils.setField(emailService, "emailProvider", "sendgrid");
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", "");

        emailService.enviarCorreo("destino@test.com", "Asunto", "Mensaje");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreo con proveedor no soportado no envia")
    void enviarCorreo_productionUnsupportedProvider() {
        ReflectionTestUtils.setField(emailService, "nodeEnv", "production");
        ReflectionTestUtils.setField(emailService, "activeProfile", "prod");
        ReflectionTestUtils.setField(emailService, "emailProvider", "mailgun");

        emailService.enviarCorreo("destino@test.com", "Asunto", "Mensaje");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreo auto sin ninguna API key no envia")
    void enviarCorreo_productionAutoNoKeys() {
        ReflectionTestUtils.setField(emailService, "nodeEnv", "production");
        ReflectionTestUtils.setField(emailService, "activeProfile", "prod");
        ReflectionTestUtils.setField(emailService, "emailProvider", "auto");
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "");
        ReflectionTestUtils.setField(emailService, "resendApiKey", "");
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", "");

        emailService.enviarCorreo("destino@test.com", "Asunto", "Mensaje");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarCorreo SMTP sin remitente no envia")
    void enviarCorreo_smtpNoFromEmail() {
        ReflectionTestUtils.setField(emailService, "nodeEnv", "development");
        ReflectionTestUtils.setField(emailService, "activeProfile", "local");
        ReflectionTestUtils.setField(emailService, "emailFromEmail", "");
        ReflectionTestUtils.setField(emailService, "brevoFromEmail", "");
        ReflectionTestUtils.setField(emailService, "resendFromEmail", "");
        ReflectionTestUtils.setField(emailService, "sendGridFromEmail", "");
        ReflectionTestUtils.setField(emailService, "smtpUsername", "");

        emailService.enviarCorreo("destino@test.com", "Asunto", "Mensaje");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
