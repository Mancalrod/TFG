package com.tfg.gestionentregables.security;

/**
 * Utilidad para sanitizar entradas y prevenir ataques XSS.
 */
public final class InputSanitizer {

    private InputSanitizer() {
        // Clase de utilidad, no instanciar
    }

    /**
     * Sanitiza una cadena de texto eliminando/escapando código HTML/JS peligroso.
     * Previene inyecciones XSS en campos de texto libre.
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        return input
            // Eliminar etiquetas <script>...</script> y su contenido
            .replaceAll("(?i)<script[^>]*>.*?</script>", "")
            // Eliminar cualquier etiqueta HTML
            .replaceAll("<[^>]*>", "")
            // Escapar caracteres especiales HTML
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            // Eliminar event handlers (onclick, onerror, etc.)
            .replaceAll("(?i)on\\w+\\s*=", "")
            // Eliminar javascript: URLs
            .replaceAll("(?i)javascript\\s*:", "")
            // Eliminar data: URLs (potencialmente peligroso)
            .replaceAll("(?i)data\\s*:", "")
            .trim();
    }

    /**
     * Valida que un parámetro numérico sea un Long válido.
     * Previene inyección en parámetros de URL.
     */
    public static Long sanitizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID no puede ser vacío");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID no válido: " + id);
        }
    }

    /**
     * Valida que un email tenga formato válido básico.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
