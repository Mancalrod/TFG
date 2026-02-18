import api from './api';
import { FeedbackDTO, CrearFeedbackDTO } from '../types';

const BASE_URL = '/feedback';

export const feedbackService = {
  obtener: async (id: number): Promise<FeedbackDTO> => {
    const response = await api.get<FeedbackDTO>(`${BASE_URL}/${id}`);
    return response.data;
  },

  crear: async (
    entregaId: number, 
    profesorId: number, 
    feedback: CrearFeedbackDTO
  ): Promise<FeedbackDTO> => {
    const response = await api.post<FeedbackDTO>(
      `${BASE_URL}/entrega/${entregaId}/profesor/${profesorId}`,
      feedback
    );
    return response.data;
  },

  listarPorEntrega: async (entregaId: number): Promise<FeedbackDTO[]> => {
    const response = await api.get<FeedbackDTO[]>(`${BASE_URL}/entrega/${entregaId}`);
    return response.data;
  },

  actualizar: async (
    id: number, 
    profesorId: number, 
    feedback: CrearFeedbackDTO
  ): Promise<FeedbackDTO> => {
    const response = await api.put<FeedbackDTO>(
      `${BASE_URL}/${id}/profesor/${profesorId}`,
      feedback
    );
    return response.data;
  },

  eliminar: async (id: number, profesorId: number): Promise<void> => {
    await api.delete(`${BASE_URL}/${id}/profesor/${profesorId}`);
  },

  listarPorProfesor: async (profesorId: number): Promise<FeedbackDTO[]> => {
    const response = await api.get<FeedbackDTO[]>(`${BASE_URL}/profesor/${profesorId}`);
    return response.data;
  },

  contarRecientes: async (estudianteId: number, dias: number = 7): Promise<number> => {
    const response = await api.get<number>(`${BASE_URL}/estudiante/${estudianteId}/recientes`, {
      params: { dias }
    });
    return response.data;
  }
};
