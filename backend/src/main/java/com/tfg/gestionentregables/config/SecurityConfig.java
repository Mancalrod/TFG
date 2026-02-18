package com.tfg.gestionentregables.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad temporal para desarrollo.
 * TODO: Configurar OAuth2 con Google antes de producción.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().permitAll() // TODO: Cambiar a authenticated() cuando se configure OAuth2
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable())); // Para H2 Console
        
        return http.build();
    }
}
