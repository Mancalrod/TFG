package com.tfg.gestionentregables.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        userDetails = new User("juan@test.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilter {

        @Test
        @DisplayName("Continúa sin autenticación si no hay header Authorization")
        void sinHeader() throws ServletException, IOException {
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Continúa sin autenticación si header no empieza con Bearer")
        void headerSinBearer() throws ServletException, IOException {
            request.addHeader("Authorization", "Basic abc123");

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Establece autenticación con token válido")
        void tokenValido() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer valid.jwt.token");

            when(jwtTokenProvider.extractUsername("valid.jwt.token")).thenReturn("juan@test.com");
            when(userDetailsService.loadUserByUsername("juan@test.com")).thenReturn(userDetails);
            when(jwtTokenProvider.isTokenValid("valid.jwt.token", userDetails)).thenReturn(true);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                    .isEqualTo("juan@test.com");
        }

        @Test
        @DisplayName("No establece autenticación con token inválido")
        void tokenInvalido() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer invalid.jwt.token");

            when(jwtTokenProvider.extractUsername("invalid.jwt.token")).thenReturn("juan@test.com");
            when(userDetailsService.loadUserByUsername("juan@test.com")).thenReturn(userDetails);
            when(jwtTokenProvider.isTokenValid("invalid.jwt.token", userDetails)).thenReturn(false);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("No establece autenticación si extractUsername devuelve null")
        void usernameNull() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer some.jwt.token");

            when(jwtTokenProvider.extractUsername("some.jwt.token")).thenReturn(null);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(userDetailsService, never()).loadUserByUsername(any());
        }

        @Test
        @DisplayName("No sobrescribe autenticación existente")
        void autenticacionExistente() throws ServletException, IOException {
            // Establecer autenticación previa
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(existingAuth);

            request.addHeader("Authorization", "Bearer valid.jwt.token");
            when(jwtTokenProvider.extractUsername("valid.jwt.token")).thenReturn("juan@test.com");

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            // No debería cargar userDetails porque ya hay autenticación
            verify(userDetailsService, never()).loadUserByUsername(any());
        }

        @Test
        @DisplayName("Maneja excepción en extracción de token sin romper la cadena")
        void excepcionEnToken() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer corrupt.token");

            when(jwtTokenProvider.extractUsername("corrupt.token"))
                    .thenThrow(new RuntimeException("Token corrupto"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // La cadena de filtros debe continuar aunque haya error
            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Establece authorities correctamente del UserDetails")
        void authoritiesCorrectas() throws ServletException, IOException {
            UserDetails multiRoleUser = new User("prof@test.com", "pass",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("ROLE_PROFESOR")));

            request.addHeader("Authorization", "Bearer valid.jwt.token");
            when(jwtTokenProvider.extractUsername("valid.jwt.token")).thenReturn("prof@test.com");
            when(userDetailsService.loadUserByUsername("prof@test.com")).thenReturn(multiRoleUser);
            when(jwtTokenProvider.isTokenValid("valid.jwt.token", multiRoleUser)).thenReturn(true);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                    .hasSize(2);
        }
    }
}
