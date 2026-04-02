package com.tfg.gestionentregables.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de Rate Limiting para endpoints sensibles.
 * Usa un token bucket in-memory por IP + endpoint path.
 */
@Component
@Slf4j
public class RateLimitFilter implements Filter {

    @Value("${app.security.rate-limit.credential-change.path-regex:^/api/usuarios/\\d+/contrasena$}")
    private String credentialChangePathRegex;

    @Value("${app.security.rate-limit.login.path:/api/auth/login}")
    private String loginPath;

    @Value("${app.security.rate-limit.credential-change.max-requests:5}")
    private int credentialChangeMaxRequests;

    @Value("${app.security.rate-limit.login.max-requests:10}")
    private int loginMaxRequests;

    private static final long WINDOW_MS = 60_000; // 1 minuto

    private final ConcurrentHashMap<String, long[]> requestCounts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();
        String ip = obtenerIpCliente(request);

        Integer maxRequests = obtenerLimitePorPath(path);

        if (maxRequests != null) {
            String key = ip + ":" + normalizarPath(path);
            long ahora = System.currentTimeMillis();

            long[] bucket = requestCounts.compute(key, (k, v) -> {
                if (v == null || (ahora - v[1]) > WINDOW_MS) {
                    // Nueva ventana
                    return new long[]{1, ahora};
                }
                v[0]++;
                return v;
            });

            if (bucket[0] > maxRequests) {
                log.warn("Rate limit excedido para IP {} en path {}: {}/{} req/min",
                    ip, path, bucket[0], maxRequests);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"status\":429,\"message\":\"Demasiadas peticiones. Inténtalo de nuevo en un minuto.\"}");
                return;
            }
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    /**
     * Obtiene el límite de peticiones correspondiente a una ruta.
     */
    private Integer obtenerLimitePorPath(String path) {
        if (path.matches(credentialChangePathRegex)) {
            return credentialChangeMaxRequests;
        }
        if (path.equals(loginPath)) {
            return loginMaxRequests;
        }
        return null;
    }

    /**
     * Normaliza el path para agrupar (ej. /api/usuarios/123/contrasena a /api/usuarios/{id}/contrasena).
     */
    private String normalizarPath(String path) {
        return path.replaceAll("/\\d+/", "/*/");
    }

    /**
     * Obtiene la IP del cliente considerando posibles proxies.
     */
    private String obtenerIpCliente(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
