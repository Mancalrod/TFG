package com.tfg.gestionentregables.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Servicio de envío de correos adaptativo.
 * - En entornos local/development: usa SMTP (JavaMailSender).
 * - En producción: usa la API HTTP de SendGrid para evitar bloqueos de puertos SMTP.
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

    @Value("${SENDGRID_API_KEY:}")
    private String sendGridApiKey;

    @Value("${SENDGRID_FROM_EMAIL:noreply@tfg-entregables.com}")
    private String fromEmail;

    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

    /**
     * Envía un correo electrónico de forma adaptativa según el entorno.
     */
    public void enviarCorreo(String destinatario, String asunto, String contenido) {
        if (isProduction()) {
            enviarViaSendGrid(destinatario, asunto, contenido);
        } else {
            enviarViaSmtp(destinatario, asunto, contenido);
        }
    }

    /**
     * Envía correo usando SMTP (JavaMailSender) para entornos locales/desarrollo.
     */
    private void enviarViaSmtp(String destinatario, String asunto, String contenido) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
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
     * Envía correo usando la API HTTP de SendGrid para producción.
     */
    private void enviarViaSendGrid(String destinatario, String asunto, String contenido) {
        if (sendGridApiKey.isBlank()) {
            log.warn("SENDGRID_API_KEY no configurada, no se puede enviar correo a: {}", destinatario);
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
                "from", Map.of("email", fromEmail),
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
        } catch (Exception e) {
            log.error("Error al enviar correo vía SendGrid a {}: {}", destinatario, e.getMessage());
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
}
