package com.tfg.gestionentregables.entity;

import com.tfg.gestionentregables.entity.enums.CanalNotificacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Entidad PreferenciaNotificacion - Preferencias de canal de notificación por usuario.
 */
@Entity
@Table(name = "preferencias_notificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciaNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CanalNotificacion canal = CanalNotificacion.APP;
}
