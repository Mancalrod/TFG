package com.tfg.gestionentregables.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la integración con Microsoft OneDrive.
 * Los valores se leen desde application.properties con prefijo "onedrive".
 *
 * Para obtener estos valores, registrar una aplicación en:
 * https://portal.azure.com → Azure Active Directory → App registrations
 *
 * Permisos requeridos (delegados):
 * - Files.ReadWrite (leer/escribir archivos del usuario)
 * - User.Read (leer perfil básico)
 * - offline_access (obtener refresh token)
 */
@Configuration
@ConfigurationProperties(prefix = "onedrive")
@Getter
@Setter
public class OneDriveConfig {

    /**
     * Client ID de la aplicación registrada en Azure AD.
     */
    private String clientId;

    /**
     * Client Secret de la aplicación registrada en Azure AD.
     */
    private String clientSecret;

    /**
     * URL de redirección tras la autenticación OAuth2 de Microsoft.
     * Debe coincidir con la configurada en Azure AD App Registration.
     */
    private String redirectUri = "http://localhost:8080/api/onedrive/callback";

    /**
     * Scopes de Microsoft Graph solicitados.
     * files.readwrite = acceso a OneDrive del usuario
     * offline_access = para obtener refresh_token
     * user.read = perfil básico
     */
    private String scopes = "files.readwrite offline_access user.read";

    /**
     * URL base de autorización de Microsoft.
     */
    private String authorizeUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";

    /**
     * URL para intercambio de tokens.
     */
    private String tokenUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/token";

    /**
     * URL base de la API de Microsoft Graph.
     */
    private String graphApiUrl = "https://graph.microsoft.com/v1.0";

    /**
     * Nombre de la carpeta raíz en OneDrive donde se almacenarán los entregables.
     */
    private String rootFolder = "TFG-Entregables";

    /**
     * Si la integración con OneDrive está habilitada.
     */
    private boolean enabled = false;
}
