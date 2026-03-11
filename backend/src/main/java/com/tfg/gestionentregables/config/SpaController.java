package com.tfg.gestionentregables.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador SPA: redirige todas las rutas que no coincidan con un recurso
 * estático ni con /api/** al index.html, para que React Router gestione la
 * navegación del lado del cliente.
 */
@Controller
public class SpaController {

    private static final String FORWARD_INDEX = "forward:/index.html";

    @RequestMapping("/{path:^(?!api|h2-console)[^\\.]*$}")
    public String redirectRoot(@PathVariable String path) {
        return FORWARD_INDEX;
    }

    @RequestMapping("/{path:^(?!api|h2-console)[^\\.]*$}/{subPath:[^\\.]*}")
    public String redirectSubPath(@PathVariable String path, @PathVariable String subPath) {
        return FORWARD_INDEX;
    }

    @RequestMapping("/{path:^(?!api|h2-console)[^\\.]*$}/{subPath:[^\\.]*}/{remaining:[^\\.]*}")
    public String redirectDeepPath(@PathVariable String path, @PathVariable String subPath,
                                   @PathVariable String remaining) {
        return FORWARD_INDEX;
    }
}
