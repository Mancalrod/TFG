import api from './api';
import { NotificacionDTO, PreferenciaNotificacionDTO } from '../types';

const BASE_URL = '/api/notificaciones';

export const notificacionService = {
  listar: async (): Promise<NotificacionDTO[]> => {
    const response = await api.get<NotificacionDTO[]>(BASE_URL);
    return response.data;
  },

  marcarComoLeida: async (id: number): Promise<void> => {
    await api.put(`${BASE_URL}/${id}/leida`);
  },

  contarNoLeidas: async (): Promise<number> => {
    const response = await api.get<{ count: number }>(`${BASE_URL}/no-leidas/count`);
    return response.data.count;
  },

  obtenerPreferencias: async (): Promise<PreferenciaNotificacionDTO> => {
    const response = await api.get<PreferenciaNotificacionDTO>(`${BASE_URL}/preferencias`);
    return response.data;
  },

  actualizarPreferencias: async (
    payload: PreferenciaNotificacionDTO,
  ): Promise<PreferenciaNotificacionDTO> => {
    const response = await api.put<PreferenciaNotificacionDTO>(`${BASE_URL}/preferencias`, payload);
    return response.data;
  },
};
