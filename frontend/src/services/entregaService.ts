import api from './api';
import { 
  EntregaDTO, 
  EntregaResumenDTO, 
  CalificacionDTO, 
  EntregaEstadisticasDTO 
} from '../types';

const BASE_URL = '/entregas';

export const entregaService = {
  obtener: async (id: number): Promise<EntregaDTO> => {
    const response = await api.get<EntregaDTO>(`${BASE_URL}/${id}`);
    return response.data;
  },

  realizar: async (
    entregableId: number, 
    estudianteId: number, 
    nombre: string, 
    archivos?: File[]
  ): Promise<EntregaDTO> => {
    const formData = new FormData();
    formData.append('nombre', nombre);
    
    if (archivos) {
      archivos.forEach(archivo => {
        formData.append('archivos', archivo);
      });
    }

    const response = await api.post<EntregaDTO>(
      `${BASE_URL}/entregable/${entregableId}/estudiante/${estudianteId}`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        params: { nombre }
      }
    );
    return response.data;
  },

  listarParaEvaluar: async (entregableId: number): Promise<EntregaResumenDTO[]> => {
    const response = await api.get<EntregaResumenDTO[]>(`${BASE_URL}/entregable/${entregableId}`);
    return response.data;
  },

  listarHistorial: async (entregableId: number, estudianteId: number): Promise<EntregaDTO[]> => {
    const response = await api.get<EntregaDTO[]>(
      `${BASE_URL}/entregable/${entregableId}/estudiante/${estudianteId}`
    );
    return response.data;
  },

  calificar: async (id: number, calificacion: CalificacionDTO): Promise<EntregaDTO> => {
    const response = await api.post<EntregaDTO>(`${BASE_URL}/${id}/calificar`, calificacion);
    return response.data;
  },

  descargarArchivo: async (materialId: number): Promise<Blob> => {
    const response = await api.get(`${BASE_URL}/archivo/${materialId}`, {
      responseType: 'blob'
    });
    return response.data;
  },

  listarPorEstudiante: async (estudianteId: number): Promise<EntregaDTO[]> => {
    const response = await api.get<EntregaDTO[]>(`${BASE_URL}/estudiante/${estudianteId}`);
    return response.data;
  },

  listarPendientesCalificar: async (profesorId: number): Promise<EntregaResumenDTO[]> => {
    const response = await api.get<EntregaResumenDTO[]>(
      `${BASE_URL}/profesor/${profesorId}/pendientes`
    );
    return response.data;
  },

  obtenerEstadisticas: async (entregableId: number): Promise<EntregaEstadisticasDTO> => {
    const response = await api.get<EntregaEstadisticasDTO>(
      `${BASE_URL}/entregable/${entregableId}/estadisticas`
    );
    return response.data;
  },

  eliminar: async (id: number): Promise<void> => {
    await api.delete(`${BASE_URL}/${id}`);
  }
};
