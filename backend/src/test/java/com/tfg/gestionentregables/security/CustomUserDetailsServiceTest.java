package com.tfg.gestionentregables.security;

import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.EstudianteRepository;
import com.tfg.gestionentregables.repository.ProfesorRepository;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProfesorRepository profesorRepository;
    @Mock private EstudianteRepository estudianteRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L).nombre("Juan García")
                .correoElectronico("juan@test.com")
                .contrasena("$2a$10$encodedPassword")
                .esAdmin(false).build();
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUser {

        @Test
        @DisplayName("Carga usuario básico con ROLE_USER")
        void carga_usuarioBasico() {
            when(usuarioRepository.findByCorreoElectronico("juan@test.com"))
                    .thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(false);
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(false);

            UserDetails result = userDetailsService.loadUserByUsername("juan@test.com");

            assertThat(result.getUsername()).isEqualTo("juan@test.com");
            assertThat(result.getPassword()).isEqualTo("$2a$10$encodedPassword");
            assertThat(result.getAuthorities()).hasSize(1);
            assertThat(result.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))).isTrue();
        }

        @Test
        @DisplayName("Carga usuario admin con ROLE_USER y ROLE_ADMIN")
        void carga_admin() {
            usuario.setEsAdmin(true);
            when(usuarioRepository.findByCorreoElectronico("juan@test.com"))
                    .thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(false);
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(false);

            UserDetails result = userDetailsService.loadUserByUsername("juan@test.com");

            assertThat(result.getAuthorities()).hasSize(2);
            assertThat(result.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))).isTrue();
        }

        @Test
        @DisplayName("Carga profesor con ROLE_USER y ROLE_PROFESOR")
        void carga_profesor() {
            when(usuarioRepository.findByCorreoElectronico("juan@test.com"))
                    .thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(true);
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(false);

            UserDetails result = userDetailsService.loadUserByUsername("juan@test.com");

            assertThat(result.getAuthorities()).hasSize(2);
            assertThat(result.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PROFESOR"))).isTrue();
        }

        @Test
        @DisplayName("Carga estudiante con ROLE_USER y ROLE_ESTUDIANTE")
        void carga_estudiante() {
            when(usuarioRepository.findByCorreoElectronico("juan@test.com"))
                    .thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(false);
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(true);

            UserDetails result = userDetailsService.loadUserByUsername("juan@test.com");

            assertThat(result.getAuthorities()).hasSize(2);
            assertThat(result.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ESTUDIANTE"))).isTrue();
        }

        @Test
        @DisplayName("Carga usuario con todos los roles")
        void carga_todosRoles() {
            usuario.setEsAdmin(true);
            when(usuarioRepository.findByCorreoElectronico("juan@test.com"))
                    .thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(true);
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(true);

            UserDetails result = userDetailsService.loadUserByUsername("juan@test.com");

            assertThat(result.getAuthorities()).hasSize(4);
            assertThat(result.getAuthorities().stream()
                    .map(a -> a.getAuthority()))
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_PROFESOR", "ROLE_ESTUDIANTE");
        }

        @Test
        @DisplayName("Lanza UsernameNotFoundException si usuario no existe")
        void carga_noExiste() {
            when(usuarioRepository.findByCorreoElectronico("noexiste@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("noexiste@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("noexiste@test.com");
        }

        @Test
        @DisplayName("Usa correo como username")
        void carga_correoComoUsername() {
            when(usuarioRepository.findByCorreoElectronico("juan@test.com"))
                    .thenReturn(Optional.of(usuario));
            when(profesorRepository.existsByUsuarioId(1L)).thenReturn(false);
            when(estudianteRepository.existsByUsuarioId(1L)).thenReturn(false);

            UserDetails result = userDetailsService.loadUserByUsername("juan@test.com");

            assertThat(result.getUsername()).isEqualTo("juan@test.com");
            verify(usuarioRepository).findByCorreoElectronico("juan@test.com");
        }
    }
}
