package com.tfg.gestionentregables.service;

import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityContextUserServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private SecurityContextUserService service;

    @Test
    @DisplayName("getCurrentUserId devuelve null cuando no hay autenticacion")
    void getCurrentUserId_nullAuthentication() {
        assertThat(service.getCurrentUserId(null)).isNull();
    }

    @Test
    @DisplayName("getCurrentUserId devuelve null cuando authentication no es valido")
    void getCurrentUserId_unauthenticated() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThat(service.getCurrentUserId(authentication)).isNull();
    }

    @Test
    @DisplayName("getCurrentUserId lanza error si el usuario no existe")
    void getCurrentUserId_userNotFound() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "ana@test.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")));
        when(usuarioRepository.findByCorreoElectronico("ana@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUserId(authentication))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Usuario autenticado no encontrado");
    }

    @Test
    @DisplayName("getCurrentUserId devuelve el id del usuario autenticado")
    void getCurrentUserId_ok() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "ana@test.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")));
        when(usuarioRepository.findByCorreoElectronico("ana@test.com"))
                .thenReturn(Optional.of(Usuario.builder().id(22L).correoElectronico("ana@test.com").build()));

        assertThat(service.getCurrentUserId(authentication)).isEqualTo(22L);
    }

    @Test
    @DisplayName("hasRole devuelve true cuando encuentra el rol")
    void hasRole_true() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "ana@test.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(service.hasRole(authentication, "ADMIN")).isTrue();
    }

    @Test
    @DisplayName("hasRole devuelve false con auth nulo o sin match")
    void hasRole_false() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "ana@test.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")));

        assertThat(service.hasRole(null, "ADMIN")).isFalse();
        assertThat(service.hasRole(authentication, "ADMIN")).isFalse();
    }
}
