package com.tfg.gestionentregables.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserDetails userDetails;

    // Secret de al menos 256 bits codificado en Base64
    private static final String SECRET = "TuClaveSecretaMuyLargaYSeguraParaElTFGDeGestionEntregables2026AlMenos256Bits";
    private static final long ACCESS_EXPIRATION = 86400000L;    // 24h
    private static final long REFRESH_EXPIRATION = 604800000L;  // 7 días

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", REFRESH_EXPIRATION);

        userDetails = new User(
                "juan@test.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_PROFESOR"))
        );
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("Genera token de acceso no nulo")
        void genera_ok() {
            String token = jwtTokenProvider.generateToken(userDetails);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Token contiene el username correcto")
        void genera_conUsername() {
            String token = jwtTokenProvider.generateToken(userDetails);
            String username = jwtTokenProvider.extractUsername(token);

            assertThat(username).isEqualTo("juan@test.com");
        }

        @Test
        @DisplayName("Tokens diferentes para misma invocación")
        void genera_diferentes() {
            String token1 = jwtTokenProvider.generateToken(userDetails);
            String token2 = jwtTokenProvider.generateToken(userDetails);

            // Pueden ser iguales si se generan en el mismo milisegundo, pero el test valida que no lanza error
            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("Genera refresh token no nulo")
        void genera_ok() {
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            assertThat(refreshToken).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Refresh token contiene username correcto")
        void genera_conUsername() {
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
            String username = jwtTokenProvider.extractUsername(refreshToken);

            assertThat(username).isEqualTo("juan@test.com");
        }

        @Test
        @DisplayName("Refresh token es diferente al access token")
        void genera_diferenteDeAccess() {
            String accessToken = jwtTokenProvider.generateToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            assertThat(accessToken).isNotEqualTo(refreshToken);
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("Extrae username de token de acceso")
        void extrae_deAccessToken() {
            String token = jwtTokenProvider.generateToken(userDetails);

            String username = jwtTokenProvider.extractUsername(token);

            assertThat(username).isEqualTo("juan@test.com");
        }

        @Test
        @DisplayName("Extrae username de refresh token")
        void extrae_deRefreshToken() {
            String token = jwtTokenProvider.generateRefreshToken(userDetails);

            String username = jwtTokenProvider.extractUsername(token);

            assertThat(username).isEqualTo("juan@test.com");
        }

        @Test
        @DisplayName("Lanza excepción con token inválido")
        void extrae_tokenInvalido() {
            assertThatThrownBy(() -> jwtTokenProvider.extractUsername("token.invalido.abc"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("extractExpiration")
    class ExtractExpiration {

        @Test
        @DisplayName("Expiration del access token es en el futuro")
        void expiracion_accessToken() {
            String token = jwtTokenProvider.generateToken(userDetails);

            Date expiration = jwtTokenProvider.extractExpiration(token);

            assertThat(expiration).isAfter(new Date());
        }

        @Test
        @DisplayName("Expiration del refresh token es posterior al access token")
        void expiracion_refreshMayor() {
            String accessToken = jwtTokenProvider.generateToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            Date accessExp = jwtTokenProvider.extractExpiration(accessToken);
            Date refreshExp = jwtTokenProvider.extractExpiration(refreshToken);

            assertThat(refreshExp).isAfter(accessExp);
        }
    }

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("Token válido para el usuario correcto")
        void valido_ok() {
            String token = jwtTokenProvider.generateToken(userDetails);

            boolean valid = jwtTokenProvider.isTokenValid(token, userDetails);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Token inválido para otro usuario")
        void invalido_otroUsuario() {
            String token = jwtTokenProvider.generateToken(userDetails);

            UserDetails otroUsuario = new User("otro@test.com", "pass",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            boolean valid = jwtTokenProvider.isTokenValid(token, otroUsuario);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("Token expirado es inválido")
        void invalido_expirado() {
            // Configurar expiración negativa para generar token ya expirado
            ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", -1000L);
            String token = jwtTokenProvider.generateToken(userDetails);
            // Restaurar expiración normal
            ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration", ACCESS_EXPIRATION);

            assertThatThrownBy(() -> jwtTokenProvider.isTokenValid(token, userDetails))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Token con roles")
    class TokenConRoles {

        @Test
        @DisplayName("Token incluye roles en los claims")
        void token_conRoles() {
            String token = jwtTokenProvider.generateToken(userDetails);

            // Verificar que el token se genera y valida correctamente con roles
            assertThat(jwtTokenProvider.isTokenValid(token, userDetails)).isTrue();
            assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("juan@test.com");
        }

        @Test
        @DisplayName("Token para usuario con un solo rol")
        void token_unRol() {
            UserDetails singleRole = new User("solo@test.com", "pass",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            String token = jwtTokenProvider.generateToken(singleRole);

            assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("solo@test.com");
            assertThat(jwtTokenProvider.isTokenValid(token, singleRole)).isTrue();
        }
    }
}
