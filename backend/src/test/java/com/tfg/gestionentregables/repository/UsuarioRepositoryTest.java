package com.tfg.gestionentregables.repository;

import com.tfg.gestionentregables.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = em.persistAndFlush(Usuario.builder()
                .nombre("Juan García")
                .correoElectronico("juan@test.com")
                .contrasena("pass123")
                .esAdmin(false)
                .build());
    }

    @Test
    @DisplayName("findByCorreoElectronico encuentra usuario existente")
    void findByCorreo_existe() {
        Optional<Usuario> result = usuarioRepository.findByCorreoElectronico("juan@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getNombre()).isEqualTo("Juan García");
    }

    @Test
    @DisplayName("findByCorreoElectronico devuelve vacío si no existe")
    void findByCorreo_noExiste() {
        Optional<Usuario> result = usuarioRepository.findByCorreoElectronico("noexiste@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByCorreoElectronico devuelve true si existe")
    void existsByCorreo_true() {
        assertThat(usuarioRepository.existsByCorreoElectronico("juan@test.com")).isTrue();
    }

    @Test
    @DisplayName("existsByCorreoElectronico devuelve false si no existe")
    void existsByCorreo_false() {
        assertThat(usuarioRepository.existsByCorreoElectronico("noexiste@test.com")).isFalse();
    }
}
