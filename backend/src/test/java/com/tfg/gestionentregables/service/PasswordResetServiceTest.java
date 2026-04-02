package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.entity.PasswordResetToken;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.PasswordResetTokenRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(10L)
                .nombre("Ana")
                .correoElectronico("ana@ull.edu.es")
                .contrasena("hash-actual")
                .build();

        ReflectionTestUtils.setField(passwordResetService, "resetTokenMinutes", 45L);
        ReflectionTestUtils.setField(passwordResetService, "frontendBaseUrl", "https://frontend.test///");
    }

    @Test
    @DisplayName("No hace nada si el correo no existe")
    void solicitarRecuperacion_usuarioNoExiste() {
        when(usuarioRepository.findByCorreoElectronico("noexiste@ull.edu.es")).thenReturn(Optional.empty());

        passwordResetService.solicitarRecuperacion("noexiste@ull.edu.es");

        verify(passwordResetTokenRepository, never()).deleteByUsuarioId(anyLong());
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Genera token, guarda y envia email con URL normalizada")
    void solicitarRecuperacion_ok() {
        when(usuarioRepository.findByCorreoElectronico(usuario.getCorreoElectronico())).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacion(usuario.getCorreoElectronico());

        verify(passwordResetTokenRepository).deleteByUsuarioId(usuario.getId());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken tokenGuardado = tokenCaptor.getValue();
        assertThat(tokenGuardado.getUsuario()).isEqualTo(usuario);
        assertThat(tokenGuardado.getUsado()).isFalse();
        assertThat(tokenGuardado.getTokenHash()).isNotBlank();
        assertThat(tokenGuardado.getFechaExpiracion())
                .isAfter(LocalDateTime.now().plusMinutes(43))
                .isBefore(LocalDateTime.now().plusMinutes(47));

        verify(emailService).enviarCorreo(
                eq("ana@ull.edu.es"),
                eq("[TFG Entregables] Recuperar contraseña"),
                contains("https://frontend.test/reset-password?token=")
        );
    }

    @Test
    @DisplayName("Si frontendBaseUrl solo tiene barras usa fallback localhost")
    void solicitarRecuperacion_baseUrlSoloBarras_usaFallback() {
        ReflectionTestUtils.setField(passwordResetService, "frontendBaseUrl", "///");
        when(usuarioRepository.findByCorreoElectronico(usuario.getCorreoElectronico())).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacion(usuario.getCorreoElectronico());

        verify(emailService).enviarCorreo(anyString(), anyString(), contains("http://localhost:3000/reset-password?token="));
    }

    @Test
    @DisplayName("Si frontendBaseUrl es null usa fallback localhost")
    void solicitarRecuperacion_baseUrlNull_usaFallback() {
        ReflectionTestUtils.setField(passwordResetService, "frontendBaseUrl", null);
        when(usuarioRepository.findByCorreoElectronico(usuario.getCorreoElectronico())).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacion(usuario.getCorreoElectronico());

        verify(emailService).enviarCorreo(anyString(), anyString(), contains("http://localhost:3000/reset-password?token="));
    }

    @Test
    @DisplayName("Si frontendBaseUrl es vacia usa fallback localhost")
    void solicitarRecuperacion_baseUrlVacia_usaFallback() {
        ReflectionTestUtils.setField(passwordResetService, "frontendBaseUrl", "   ");
        when(usuarioRepository.findByCorreoElectronico(usuario.getCorreoElectronico())).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacion(usuario.getCorreoElectronico());

        verify(emailService).enviarCorreo(anyString(), anyString(), contains("http://localhost:3000/reset-password?token="));
    }

    @Test
    @DisplayName("Lanza error si token no existe o ya fue usado")
    void resetearContrasena_tokenInvalido() {
        when(passwordResetTokenRepository.findByTokenHashAndUsadoFalse(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetearContrasena("token", "Nueva123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no es válido o ya fue usado");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Lanza error si token esta expirado")
    void resetearContrasena_tokenExpirado() {
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(usuario)
                .tokenHash("hash")
                .fechaExpiracion(LocalDateTime.now().minusMinutes(1))
                .usado(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsadoFalse(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetearContrasena("token", "Nueva123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ha expirado");

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("Lanza error si la nueva contraseña coincide con la actual")
    void resetearContrasena_mismaContrasena() {
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(usuario)
                .tokenHash("hash")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(10))
                .usado(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsadoFalse(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("Nueva123!", "hash-actual")).thenReturn(true);

        assertThatThrownBy(() -> passwordResetService.resetearContrasena("token", "Nueva123!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser igual a la actual");

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("Resetea contraseña y marca token como usado")
    void resetearContrasena_ok() {
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(usuario)
                .tokenHash("hash")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(10))
                .usado(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsadoFalse(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("Nueva123!", "hash-actual")).thenReturn(false);
        when(passwordEncoder.encode("Nueva123!")).thenReturn("hash-nuevo");

        passwordResetService.resetearContrasena("token", "Nueva123!");

        assertThat(usuario.getContrasena()).isEqualTo("hash-nuevo");
        assertThat(token.getUsado()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    @DisplayName("Lanza IllegalStateException si falla el hash del token")
    void solicitarRecuperacion_errorHashToken() {
        when(usuarioRepository.findByCorreoElectronico(usuario.getCorreoElectronico())).thenReturn(Optional.of(usuario));

        try (MockedStatic<MessageDigest> digestMock = mockStatic(MessageDigest.class)) {
            digestMock.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new RuntimeException("algoritmo no disponible"));

            assertThatThrownBy(() -> passwordResetService.solicitarRecuperacion(usuario.getCorreoElectronico()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No se pudo procesar el token de recuperación");
        }

        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
    }
}
