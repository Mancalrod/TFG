package com.tfg.gestionentregables.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Servicio de envío de correos adaptativo.
 * - En entornos local/development: usa SMTP (JavaMailSender).
 * - En producción: usa API HTTP transaccional para evitar bloqueos de puertos SMTP.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Value("${NODE_ENV:development}")
    private String nodeEnv;

    @Value("${email.provider:${EMAIL_PROVIDER:auto}}")
    private String emailProvider;

    @Value("${email.from-email:${EMAIL_FROM_EMAIL:}}")
    private String emailFromEmail;

    @Value("${email.from-name:${EMAIL_FROM_NAME:Polan}}")
    private String emailFromName;

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${brevo.from-email:${BREVO_FROM_EMAIL:}}")
    private String brevoFromEmail;

    @Value("${sendgrid.api-key:${SENDGRID_API_KEY:}}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email:${SENDGRID_FROM_EMAIL:}}")
    private String sendGridFromEmail;

    @Value("${resend.api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${resend.from-email:${RESEND_FROM_EMAIL:}}")
    private String resendFromEmail;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    /**
     * Envía un correo electrónico de forma adaptativa según el entorno.
     */
    public void enviarCorreo(String destinatario, String asunto, String contenido) {
        if (isProduction()) {
            enviarViaApiHttp(destinatario, asunto, contenido);
        } else {
            enviarViaSmtp(destinatario, asunto, contenido);
        }
    }

    /**
     * Envía correo usando SMTP (JavaMailSender) para entornos locales/desarrollo.
     */
    private void enviarViaSmtp(String destinatario, String asunto, String contenido) {
        try {
            String remitente = obtenerRemitente("smtp");
            if (remitente.isBlank()) {
                log.warn("Remitente de correo no configurado, no se puede enviar correo a: {}", destinatario);
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remitente);
            message.setTo(destinatario);
            message.setSubject(asunto);
            message.setText(contenido);
            mailSender.send(message);
            log.info("Correo enviado vía SMTP a: {}", destinatario);
        } catch (Exception e) {
            log.error("Error al enviar correo vía SMTP a {}: {}", destinatario, e.getMessage());
        }
    }

    /**
     * Envía correo usando un proveedor HTTP configurado para producción.
     */
    private void enviarViaApiHttp(String destinatario, String asunto, String contenido) {
        String provider = obtenerProveedorEmail();

        if ("brevo".equals(provider)) {
            enviarViaBrevo(destinatario, asunto, contenido);
            return;
        }
        if ("sendgrid".equals(provider)) {
            enviarViaSendGrid(destinatario, asunto, contenido);
            return;
        }
        if ("resend".equals(provider)) {
            enviarViaResend(destinatario, asunto, contenido);
            return;
        }
        if ("auto".equals(provider)) {
            if (tieneTexto(brevoApiKey)) {
                enviarViaBrevo(destinatario, asunto, contenido);
                return;
            }
            if (tieneTexto(resendApiKey)) {
                enviarViaResend(destinatario, asunto, contenido);
                return;
            }
            if (tieneTexto(sendGridApiKey)) {
                enviarViaSendGrid(destinatario, asunto, contenido);
                return;
            }
            log.warn("No hay proveedor HTTP de correo configurado (BREVO_API_KEY/RESEND_API_KEY/SENDGRID_API_KEY), no se puede enviar correo a: {}", destinatario);
            return;
        }

        log.warn("Proveedor de correo no soportado: {}. Valores validos: brevo, resend, sendgrid, auto", emailProvider);
    }

    /**
     * Envía correo usando la API HTTP de Brevo para producción.
     */
    private void enviarViaBrevo(String destinatario, String asunto, String contenido) {
        if (!tieneTexto(brevoApiKey)) {
            log.warn("BREVO_API_KEY no configurada, no se puede enviar correo a: {}", destinatario);
            return;
        }

        String remitente = obtenerRemitente("brevo");
        if (remitente.isBlank()) {
            log.warn("EMAIL_FROM_EMAIL/BREVO_FROM_EMAIL no configurado, no se puede enviar correo a: {}", destinatario);
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            String nombreRemitente = tieneTexto(emailFromName) ? emailFromName.trim() : "Polan";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                "sender", Map.of(
                    "name", nombreRemitente,
                    "email", remitente
                ),
                "to", List.of(Map.of("email", destinatario)),
                "subject", asunto,
                "textContent", contenido
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Correo enviado vía Brevo a: {}", destinatario);
            } else {
                log.error("Error Brevo ({}): {}", response.getStatusCode(), response.getBody());
            }
        } catch (HttpStatusCodeException e) {
            log.error("Error Brevo HTTP ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error al enviar correo vía Brevo a {}: {}", destinatario, e.getMessage());
        }
    }

    /**
     * Envía correo usando la API HTTP de SendGrid para producción.
     */
    private void enviarViaSendGrid(String destinatario, String asunto, String contenido) {
        if (!tieneTexto(sendGridApiKey)) {
            log.warn("SENDGRID_API_KEY no configurada, no se puede enviar correo a: {}", destinatario);
            return;
        }

        String remitente = obtenerRemitente("sendgrid");
        if (remitente.isBlank()) {
            log.warn("EMAIL_FROM_EMAIL/SENDGRID_FROM_EMAIL no configurado, no se puede enviar correo a: {}", destinatario);
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(sendGridApiKey);

            // Construir el payload de SendGrid v3
            Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of(
                    "to", List.of(Map.of("email", destinatario))
                )),
                "from", Map.of("email", remitente),
                "subject", asunto,
                "content", List.of(Map.of(
                    "type", "text/plain",
                    "value", contenido
                ))
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(SENDGRID_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Correo enviado vía SendGrid a: {}", destinatario);
            } else {
                log.error("Error SendGrid ({}): {}", response.getStatusCode(), response.getBody());
            }
        } catch (HttpStatusCodeException e) {
            log.error("Error SendGrid HTTP ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error al enviar correo vía SendGrid a {}: {}", destinatario, e.getMessage());
        }
    }

    /**
     * Envía correo usando la API HTTP de Resend para producción.
     */
    private void enviarViaResend(String destinatario, String asunto, String contenido) {
        if (!tieneTexto(resendApiKey)) {
            log.warn("RESEND_API_KEY no configurada, no se puede enviar correo a: {}", destinatario);
            return;
        }

        String remitente = obtenerRemitente("resend");
        if (remitente.isBlank()) {
            log.warn("EMAIL_FROM_EMAIL/RESEND_FROM_EMAIL no configurado, no se puede enviar correo a: {}", destinatario);
            return;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            // Construir el payload de Resend
            Map<String, Object> body = Map.of(
                "from", remitente,
                "to", List.of(destinatario),
                "subject", asunto,
                "html", contenido
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(RESEND_API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Correo enviado vía Resend a: {}", destinatario);
            } else {
                log.error("Error Resend ({}): {}", response.getStatusCode(), response.getBody());
            }
        } catch (HttpStatusCodeException e) {
            log.error("Error Resend HTTP ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error al enviar correo vía Resend a {}: {}", destinatario, e.getMessage());
        }
    }

    /**
     * Determina si el entorno actual es producción.
     */
    boolean isProduction() {
        return "production".equalsIgnoreCase(nodeEnv)
                || "prod".equalsIgnoreCase(activeProfile)
                || "production".equalsIgnoreCase(activeProfile);
    }

    private String obtenerProveedorEmail() {
        return tieneTexto(emailProvider) ? emailProvider.trim().toLowerCase() : "auto";
    }

    private String obtenerRemitente(String provider) {
        if (tieneTexto(emailFromEmail)) {
            return emailFromEmail.trim();
        }
        if ("brevo".equals(provider) && tieneTexto(brevoFromEmail)) {
            return brevoFromEmail.trim();
        }
        if ("sendgrid".equals(provider) && tieneTexto(sendGridFromEmail)) {
            return sendGridFromEmail.trim();
        }
        if ("resend".equals(provider) && tieneTexto(resendFromEmail)) {
            return resendFromEmail.trim();
        }
        if (tieneTexto(smtpUsername)) {
            return smtpUsername.trim();
        }
        if (tieneTexto(brevoFromEmail)) {
            return brevoFromEmail.trim();
        }
        if (tieneTexto(resendFromEmail)) {
            return resendFromEmail.trim();
        }
        if (tieneTexto(sendGridFromEmail)) {
            return sendGridFromEmail.trim();
        }
        return "";
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
