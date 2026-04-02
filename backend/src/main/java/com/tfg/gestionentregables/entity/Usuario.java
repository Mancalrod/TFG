package com.tfg.gestionentregables.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad Usuario - Representa los usuarios de la aplicación.
 * Según DAS ENT-001
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    @Column(nullable = false)
    private String nombre;

    @Size(max = 20)
    private String telefono;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    @Column(name = "correo_electronico", nullable = false, unique = true)
    private String correoElectronico;

    @NotBlank(message = "La contraseña es obligatoria")
    @Column(nullable = false)
    private String contrasena;

    @Column(name = "es_admin", nullable = false)
    @Builder.Default
    private Boolean esAdmin = false;

    @Size(max = 500)
    @Column(name = "foto_perfil_url", length = 500)
    private String fotoPerfilUrl;

    // Relación: Un usuario puede ser profesor en varios cursos
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Profesor> cursosComoProfesor = new HashSet<>();

    // Relación: Un usuario puede ser estudiante en varios grupos
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Estudiante> gruposComoEstudiante = new HashSet<>();
}
