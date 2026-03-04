package com.tfg.gestionentregables.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuración para la integración con OneDrive
 * mediante Microsoft Graph API (client_credentials flow).
 */
@Configuration
@ConfigurationProperties(prefix = "app.onedrive")
@Getter
@Setter
public class OneDriveProperties {

    /** Habilitar/deshabilitar la integración con OneDrive */
    private boolean enabled = false;

    /** Tenant ID de Azure AD */
    private String tenantId;

    /** Client ID del App Registration */
    private String clientId;

    /** Client Secret del App Registration */
    private String clientSecret;

    /** Carpeta raíz en el OneDrive del alumno para entregas */
    private String studentFolder = "Entregas";

    /** Carpeta raíz en el OneDrive del profesor para entregas recibidas */
    private String professorFolder = "Entregas Recibidas";

    /** URI de redirección para el callback OAuth2 de Microsoft */
    private String redirectUri = "http://localhost:8080/api/oauth/microsoft/callback";
}
