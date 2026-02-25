package com.tfg.gestionentregables.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador SPA: redirige todas las rutas que no coincidan con un recurso
 * estático ni con /api/** al index.html, para que React Router gestione la
 * navegación del lado del cliente.
 */
@Controller
public class SpaController {

    /**
     * Captura rutas como /dashboard, /login, /admin/users, etc.
     * NO captura rutas con extensión (.js, .css, .png...) ni /api/**.
     * El regex [^.]* excluye paths que contienen un punto (archivos estáticos).
     */
    @RequestMapping("/{path:[^\\.]*}")
    public String redirectRoot() {
        return "forward:/index.html";
    }

    /**
     * Captura rutas con sub-paths como /admin/users/123.
     */
    @RequestMapping("/{path:[^\\.]*}/{subPath:[^\\.]*}")
    public String redirectSubPath() {
        return "forward:/index.html";
    }

    /**
     * Captura rutas con más niveles de profundidad.
     */
    @RequestMapping("/{path:[^\\.]*}/{subPath:[^\\.]*}/{remaining:[^\\.]*}")
    public String redirectDeepPath() {
        return "forward:/index.html";
    }
}
