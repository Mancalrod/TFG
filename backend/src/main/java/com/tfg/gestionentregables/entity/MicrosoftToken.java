package com.tfg.gestionentregables.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que almacena los tokens OAuth2 de Microsoft de cada usuario.
 * Permite acceder al OneDrive personal del usuario con permisos delegados.
 */
@Entity
@Table(name = "microsoft_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MicrosoftToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Relación 1:1 con el usuario de la plataforma */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    /** Email de la cuenta Microsoft vinculada */
    @Column(name = "microsoft_email", nullable = false)
    private String microsoftEmail;

    /** Token de acceso (corta duración ~1h) */
    @Column(name = "access_token", nullable = false, length = 4096)
    private String accessToken;

    /** Token de refresco (larga duración) */
    @Column(name = "refresh_token", nullable = false, length = 4096)
    private String refreshToken;

    /** Momento en que expira el access token */
    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    /** Scopes concedidos */
    @Column(name = "scopes", length = 1024)
    private String scopes;

    /** Momento de creación del registro */
    @Column(name = "fecha_conexion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaConexion = LocalDateTime.now();

    /** Última vez que se refrescó el token */
    @Column(name = "ultimo_refresco")
    private LocalDateTime ultimoRefresco;

    /**
     * Comprueba si el access token ha expirado.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiraEn);
    }
}
