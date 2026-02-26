import api from './api';
import { ActividadDTO, CrearActividadDTO, Visibilidad } from '../types';

const BASE_URL = '/api/actividades';

export const actividadService = {
  obtener: async (id: number): Promise<ActividadDTO> => {
    const response = await api.get<ActividadDTO>(`${BASE_URL}/${id}`);
    return response.data;
  },

  obtenerConEntregables: async (id: number): Promise<ActividadDTO> => {
    const response = await api.get<ActividadDTO>(`${BASE_URL}/${id}/detalle`);
    return response.data;
  },

  crear: async (cursoId: number, actividad: CrearActividadDTO): Promise<ActividadDTO> => {
    const response = await api.post<ActividadDTO>(`${BASE_URL}/curso/${cursoId}`, actividad);
    return response.data;
  },

  listarPorCurso: async (cursoId: number): Promise<ActividadDTO[]> => {
    const response = await api.get<ActividadDTO[]>(`${BASE_URL}/curso/${cursoId}`);
    return response.data;
  },

  listarPorGrupo: async (grupoId: number): Promise<ActividadDTO[]> => {
    const response = await api.get<ActividadDTO[]>(`${BASE_URL}/grupo/${grupoId}`);
    return response.data;
  },

  actualizar: async (id: number, actividad: CrearActividadDTO): Promise<ActividadDTO> => {
    const response = await api.put<ActividadDTO>(`${BASE_URL}/${id}`, actividad);
    return response.data;
  },

  cambiarVisibilidad: async (id: number, visibilidad: Visibilidad): Promise<ActividadDTO> => {
    const response = await api.patch<ActividadDTO>(`${BASE_URL}/${id}/visibilidad`, null, {
      params: { visibilidad }
    });
    return response.data;
  },

  eliminar: async (id: number): Promise<void> => {
    await api.delete(`${BASE_URL}/${id}`);
  },

  listarEnPlazo: async (cursoId: number): Promise<ActividadDTO[]> => {
    const response = await api.get<ActividadDTO[]>(`${BASE_URL}/curso/${cursoId}/en-plazo`);
    return response.data;
  },

  listarProximas: async (cursoId: number, dias: number = 7): Promise<ActividadDTO[]> => {
    const response = await api.get<ActividadDTO[]>(`${BASE_URL}/curso/${cursoId}/proximas`, {
      params: { dias }
    });
    return response.data;
  }
};
