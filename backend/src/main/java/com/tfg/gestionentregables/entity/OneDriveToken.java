package com.tfg.gestionentregables.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad OneDriveToken - Almacena los tokens de OAuth2 de Microsoft para
 * cada usuario que conecte su cuenta de OneDrive.
 */
@Entity
@Table(name = "onedrive_tokens", uniqueConstraints = {
    @UniqueConstraint(columnNames = "usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OneDriveToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @NotBlank
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @NotBlank
    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    @NotNull
    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "microsoft_user_id")
    private String microsoftUserId;

    @Column(name = "microsoft_email")
    private String microsoftEmail;

    @Column(name = "fecha_conexion")
    @Builder.Default
    private LocalDateTime fechaConexion = LocalDateTime.now();

    @Column(name = "fecha_ultimo_uso")
    private LocalDateTime fechaUltimoUso;

    /**
     * Verifica si el access token ha expirado.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiraEn);
    }
}
