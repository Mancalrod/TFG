import api from './api';
import { EntregableDTO, CrearEntregableDTO, Visibilidad } from '../types';

const BASE_URL = '/api/entregables';

export const entregableService = {
  obtener: async (id: number): Promise<EntregableDTO> => {
    const response = await api.get<EntregableDTO>(`${BASE_URL}/${id}`);
    return response.data;
  },

  crear: async (actividadId: number, entregable: CrearEntregableDTO): Promise<EntregableDTO> => {
    const response = await api.post<EntregableDTO>(
      `${BASE_URL}/actividad/${actividadId}`, 
      entregable
    );
    return response.data;
  },

  listarPorActividad: async (actividadId: number): Promise<EntregableDTO[]> => {
    const response = await api.get<EntregableDTO[]>(`${BASE_URL}/actividad/${actividadId}`);
    return response.data;
  },

  listarVisibles: async (actividadId: number): Promise<EntregableDTO[]> => {
    const response = await api.get<EntregableDTO[]>(`${BASE_URL}/actividad/${actividadId}/visibles`);
    return response.data;
  },

  actualizar: async (id: number, entregable: CrearEntregableDTO): Promise<EntregableDTO> => {
    const response = await api.put<EntregableDTO>(`${BASE_URL}/${id}`, entregable);
    return response.data;
  },

  cambiarVisibilidad: async (id: number, visibilidad: Visibilidad): Promise<EntregableDTO> => {
    const response = await api.patch<EntregableDTO>(`${BASE_URL}/${id}/visibilidad`, null, {
      params: { visibilidad }
    });
    return response.data;
  },

  cambiarVisibilidadNotas: async (id: number, visible: boolean): Promise<EntregableDTO> => {
    const response = await api.patch<EntregableDTO>(`${BASE_URL}/${id}/notas-visibles`, null, {
      params: { visible }
    });
    return response.data;
  },

  eliminar: async (id: number): Promise<void> => {
    await api.delete(`${BASE_URL}/${id}`);
  },

  listarEnPlazo: async (actividadId: number): Promise<EntregableDTO[]> => {
    const response = await api.get<EntregableDTO[]>(`${BASE_URL}/actividad/${actividadId}/en-plazo`);
    return response.data;
  },

  listarProximos: async (actividadId: number, dias: number = 7): Promise<EntregableDTO[]> => {
    const response = await api.get<EntregableDTO[]>(`${BASE_URL}/actividad/${actividadId}/proximos`, {
      params: { dias }
    });
    return response.data;
  },

  listarPendientes: async (actividadId: number, estudianteId: number): Promise<EntregableDTO[]> => {
    const response = await api.get<EntregableDTO[]>(
      `${BASE_URL}/actividad/${actividadId}/pendientes/${estudianteId}`
    );
    return response.data;
  }
};
