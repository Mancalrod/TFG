import api from './api';
import { OneDriveConnectionDTO } from '../types';

/**
 * Servicio para la integracion con Microsoft OneDrive.
 * Gestiona la conexion/desconexion y estado de OneDrive para cada usuario.
 */
export const oneDriveService = {
  /**
   * Verifica si la integracion con OneDrive esta habilitada en el servidor.
   */
  async isEnabled(): Promise<boolean> {
    const response = await api.get<{ enabled: boolean }>('/api/onedrive/enabled');
    return response.data.enabled;
  },

  /**
   * Obtiene el estado de conexion de OneDrive para un usuario.
   */
  async getConnectionStatus(usuarioId: number): Promise<OneDriveConnectionDTO> {
    const response = await api.get<OneDriveConnectionDTO>(`/api/onedrive/status/${usuarioId}`);
    return response.data;
  },

  /**
   * Obtiene la URL de autorizacion de Microsoft y abre popup para conectar OneDrive.
   * Devuelve una promesa que se resuelve cuando el usuario completa o cancela el flujo.
   */
  async connectOneDrive(usuarioId: number): Promise<boolean> {
    const response = await api.get<{ authUrl: string }>(`/api/onedrive/auth-url/${usuarioId}`);
    const authUrl = response.data.authUrl;

    return new Promise((resolve) => {
      // Abrir popup de autorizacion
      const popup = window.open(authUrl, 'onedrive-auth', 'width=600,height=700,scrollbars=yes');

      // Escuchar mensajes del popup
      const messageHandler = (event: MessageEvent) => {
        if (event.data?.type === 'onedrive-auth') {
          window.removeEventListener('message', messageHandler);
          resolve(event.data.success === true);
        }
      };

      window.addEventListener('message', messageHandler);

      // Verificar si el popup se cerro manualmente
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
};
