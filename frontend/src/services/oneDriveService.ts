import api from './api';
<<<<<<< HEAD

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
=======
import { OneDriveConnectionDTO } from '../types';

/**
 * Servicio para la integración con Microsoft OneDrive.
 * Gestiona la conexión/desconexión y estado de OneDrive para cada usuario.
 */
export const oneDriveService = {
  /**
   * Verifica si la integración con OneDrive está habilitada en el servidor.
   */
  async isEnabled(): Promise<boolean> {
    const response = await api.get<{ enabled: boolean }>('/api/onedrive/enabled');
    return response.data.enabled;
  },

  /**
   * Obtiene el estado de conexión de OneDrive para un usuario.
   */
  async getConnectionStatus(usuarioId: number): Promise<OneDriveConnectionDTO> {
    const response = await api.get<OneDriveConnectionDTO>(`/api/onedrive/status/${usuarioId}`);
>>>>>>> 5139ff1424314d1f81bab2ab5f417cd43fbb4b28
    return response.data;
  },

  /**
<<<<<<< HEAD
   * Desconecta la cuenta de Microsoft del usuario.
   */
  async disconnect(usuarioId: number): Promise<void> {
    await api.delete('/api/oauth/microsoft/disconnect', {
      params: { usuarioId },
    });
  },
=======
   * Obtiene la URL de autorización de Microsoft y abre popup para conectar OneDrive.
   * Devuelve una promesa que se resuelve cuando el usuario completa o cancela el flujo.
   */
  async connectOneDrive(usuarioId: number): Promise<boolean> {
    const response = await api.get<{ authUrl: string }>(`/api/onedrive/auth-url/${usuarioId}`);
    const authUrl = response.data.authUrl;

    return new Promise((resolve) => {
      // Abrir popup de autorización
      const popup = window.open(authUrl, 'onedrive-auth', 'width=600,height=700,scrollbars=yes');

      // Escuchar mensajes del popup
      const messageHandler = (event: MessageEvent) => {
        if (event.data?.type === 'onedrive-auth') {
          window.removeEventListener('message', messageHandler);
          resolve(event.data.success === true);
        }
      };

      window.addEventListener('message', messageHandler);

      // Verificar si el popup se cerró manualmente
      const checkClosed = setInterval(() => {
        if (popup?.closed) {
          clearInterval(checkClosed);
          window.removeEventListener('message', messageHandler);
          // Dar un momento para que el mensaje llegue
          setTimeout(() => resolve(false), 500);
        }
      }, 1000);
    });
  },

  /**
   * Desconecta la cuenta de OneDrive de un usuario.
   */
  async disconnectOneDrive(usuarioId: number): Promise<void> {
    await api.post(`/api/onedrive/disconnect/${usuarioId}`);
  },
>>>>>>> 5139ff1424314d1f81bab2ab5f417cd43fbb4b28
};
