package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.entity.PasswordResetToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.PasswordResetTokenRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PasswordResetService {

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-ES");
    private static final DateTimeFormatter FECHA_HUMANA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy 'a las' HH:mm", LOCALE_ES);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl = "http://localhost:3000";

    @Value("${app.auth.reset-token-minutes:30}")
    private long resetTokenMinutes = 30;

    public void solicitarRecuperacion(String correoElectronico) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoElectronico(correoElectronico);
        if (usuarioOpt.isEmpty()) {
            return;
        }

        Usuario usuario = usuarioOpt.get();
        passwordResetTokenRepository.deleteByUsuarioId(usuario.getId());

        String tokenPlano = generarTokenSeguro();
        String tokenHash = hashToken(tokenPlano);
        LocalDateTime fechaExpiracion = LocalDateTime.now().plusMinutes(resetTokenMinutes);

        PasswordResetToken token = PasswordResetToken.builder()
            .usuario(usuario)
            .tokenHash(tokenHash)
            .fechaExpiracion(fechaExpiracion)
            .usado(false)
            .build();
        passwordResetTokenRepository.save(token);

        String urlReset = construirUrlReset(tokenPlano);
        String asunto = "[TFG Entregables] Recuperar contraseña";
        String mensaje = "Hemos recibido una solicitud para restablecer tu contraseña.\n\n"
            + "Restablecer contraseña: " + urlReset + "\n\n"
            + "Este enlace expira el " + fechaExpiracion.format(FECHA_HUMANA) + ".\n"
            + "Si no solicitaste este cambio, puedes ignorar este correo.";

        emailService.enviarCorreo(usuario.getCorreoElectronico(), asunto, mensaje);
        log.info("Enlace de recuperación generado para usuario ID: {}", usuario.getId());
    }

    public void resetearContrasena(String tokenPlano, String contrasenaNueva) {
        String tokenHash = hashToken(tokenPlano);

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashAndUsadoFalse(tokenHash)
            .orElseThrow(() -> new IllegalArgumentException("El enlace de recuperación no es válido o ya fue usado"));

        if (token.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El enlace de recuperación ha expirado");
        }

        Usuario usuario = token.getUsuario();
        if (passwordEncoder.matches(contrasenaNueva, usuario.getContrasena())) {
            throw new IllegalArgumentException("La nueva contraseña no puede ser igual a la actual");
        }

        usuario.setContrasena(passwordEncoder.encode(contrasenaNueva));
        usuarioRepository.save(usuario);

        token.setUsado(true);
        passwordResetTokenRepository.save(token);
        log.info("Contraseña restablecida para usuario ID: {}", usuario.getId());
    }

    private String construirUrlReset(String tokenPlano) {
        String base = frontendBaseUrl != null ? frontendBaseUrl.replaceAll("/+$", "") : "http://localhost:3000";
        return base + "/reset-password?token=" + URLEncoder.encode(tokenPlano, StandardCharsets.UTF_8);
    }

    private String generarTokenSeguro() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String tokenPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo procesar el token de recuperación", e);
        }
    }
}
