package com.tfg.gestionentregables.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfg.gestionentregables.dto.*;
import com.tfg.gestionentregables.entity.Usuario;
import com.tfg.gestionentregables.repository.UsuarioRepository;
import com.tfg.gestionentregables.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private UsuarioRepository usuarioRepository;
    @MockitoBean private PasswordEncoder passwordEncoder;

    private ObjectMapper objectMapper;
    private Usuario usuario;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        usuario = Usuario.builder().id(1L).nombre("Juan")
                .correoElectronico("juan@test.com")
                .contrasena("encoded").esAdmin(false).build();

        userDetails = User.builder()
                .username("juan@test.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("200 - Login exitoso")
        void login_ok() throws Exception {
            LoginRequestDTO loginReq = LoginRequestDTO.builder()
                    .correoElectronico("juan@test.com").contrasena("pass").build();

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");
            when(usuarioRepository.findByCorreoElectronico("juan@test.com"))
                    .thenReturn(Optional.of(usuario));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.nombre").value("Juan"));
        }

        @Test
        @DisplayName("401 - Credenciales incorrectas")
        void login_credencialesInvalidas() throws Exception {
            LoginRequestDTO loginReq = LoginRequestDTO.builder()
                    .correoElectronico("juan@test.com").contrasena("wrong").build();

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("201 - Registro exitoso")
        void register_ok() throws Exception {
            CrearUsuarioDTO dto = CrearUsuarioDTO.builder()
                    .nombre("Juan").correoElectronico("juan@test.com")
                    .contrasena("pass123").build();

            when(usuarioRepository.existsByCorreoElectronico("juan@test.com")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
            when(userDetailsService.loadUserByUsername("juan@test.com")).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh-token");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.nombre").value("Juan"));
        }

        @Test
        @DisplayName("409 - Correo ya registrado")
        void register_conflicto() throws Exception {
            CrearUsuarioDTO dto = CrearUsuarioDTO.builder()
                    .nombre("Juan").correoElectronico("juan@test.com")
                    .contrasena("pass123").build();

            when(usuarioRepository.existsByCorreoElectronico("juan@test.com")).thenReturn(true);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshToken {

        @Test
        @DisplayName("200 - Refresh exitoso")
        void refresh_ok() throws Exception {
            RefreshTokenRequestDTO req = RefreshTokenRequestDTO.builder()
                    .refreshToken("old-refresh").build();

            when(jwtTokenProvider.extractUsername("old-refresh")).thenReturn("juan@test.com");
            when(userDetailsService.loadUserByUsername("juan@test.com")).thenReturn(userDetails);
            when(jwtTokenProvider.isTokenValid("old-refresh", userDetails)).thenReturn(true);
            when(jwtTokenProvider.generateToken(any(UserDetails.class))).thenReturn("new-access");
            when(jwtTokenProvider.generateRefreshToken(any(UserDetails.class))).thenReturn("new-refresh");
            when(usuarioRepository.findByCorreoElectronico("juan@test.com"))
                    .thenReturn(Optional.of(usuario));

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access"));
        }

        @Test
        @DisplayName("401 - Token inválido")
        void refresh_tokenInvalido() throws Exception {
            RefreshTokenRequestDTO req = RefreshTokenRequestDTO.builder()
                    .refreshToken("invalid").build();

            when(jwtTokenProvider.extractUsername("invalid")).thenReturn("juan@test.com");
            when(userDetailsService.loadUserByUsername("juan@test.com")).thenReturn(userDetails);
            when(jwtTokenProvider.isTokenValid("invalid", userDetails)).thenReturn(false);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUser {

        @Test
        @DisplayName("401 - Sin autenticación")
        void me_sinAuth() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
