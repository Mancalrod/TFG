import api from './api';

export interface OneDriveStatus {
  connected: boolean;
  microsoftEmail: string;
}

/**
 * Servicio para gestionar la conexión OAuth2 con Microsoft OneDrive.
 */
export const oneDriveService = {
  /**
   * Obtiene la URL de autorización de Microsoft para conectar OneDrive.
   * El frontend abrirá esta URL en una ventana/pestaña nueva.
   */
  async getAuthorizationUrl(usuarioId: number): Promise<string> {
    const response = await api.get<{ authUrl: string }>(
      '/api/oauth/microsoft/authorize',
      { params: { usuarioId } }
    );
    return response.data.authUrl;
  },

  /**
   * Consulta el estado de conexión de OneDrive para un usuario.
   */
  async getStatus(usuarioId: number): Promise<OneDriveStatus> {
    const response = await api.get<OneDriveStatus>(
      '/api/oauth/microsoft/status',
      { params: { usuarioId } }
    );
    return response.data;
  },

  /**
   * Desconecta la cuenta de Microsoft del usuario.
   */
  async disconnect(usuarioId: number): Promise<void> {
    await api.delete('/api/oauth/microsoft/disconnect', {
      params: { usuarioId },
    });
  },
};
