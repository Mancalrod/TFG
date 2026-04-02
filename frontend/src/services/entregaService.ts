import api from './api';
import { 
  EntregaDTO, 
  EntregaPendienteDTO,
  EntregaResumenDTO, 
  CalificacionDTO, 
  EntregaEstadisticasDTO 
} from '../types';

const BASE_URL = '/api/entregas';

export const entregaService = {
  obtener: async (id: number): Promise<EntregaDTO> => {
    const response = await api.get<EntregaDTO>(`${BASE_URL}/${id}`);
    return response.data;
  },

  realizar: async (
    entregableId: number,
    estudianteId: number,
    comentario?: string,
    archivos?: File[]
  ): Promise<EntregaDTO> => {
    const formData = new FormData();
    const comentarioNormalizado = comentario?.trim();

    if (comentarioNormalizado) {
      formData.append('comentario', comentarioNormalizado);
    }

    if (archivos && archivos.length > 0) {
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
        }
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

  calificar: async (id: number, profesorId: number, calificacion: CalificacionDTO): Promise<EntregaDTO> => {
    const response = await api.post<EntregaDTO>(
      `${BASE_URL}/${id}/calificar`,
      calificacion,
      { params: { profesorId } }
    );
    return response.data;
  },

  descargarArchivo: async (materialId: number): Promise<Blob> => {
    const response = await api.get(`${BASE_URL}/archivo/${materialId}`, {
      responseType: 'blob'
    });
    return response.data;
  },

  previsualizarArchivo: async (materialId: number): Promise<Blob> => {
    const response = await api.get(`${BASE_URL}/archivo/${materialId}/preview`, {
      responseType: 'blob'
    });
    return response.data;
  },

  descargarTodo: async (entregableId: number): Promise<{ blob: Blob; filename: string }> => {
    const response = await api.get(`${BASE_URL}/entregable/${entregableId}/descargar-todo`, {
      responseType: 'blob'
    });
    const disposition = response.headers['content-disposition'] || '';
    const match = disposition.match(/filename="?([^"]+)"?/);
    const filename = match ? match[1] : `entregas_${entregableId}.zip`;
    return { blob: response.data, filename };
  },

  descargarTodoActividad: async (actividadId: number): Promise<{ blob: Blob; filename: string }> => {
    const response = await api.get(`${BASE_URL}/actividad/${actividadId}/descargar-todo`, {
      responseType: 'blob'
    });
    const disposition = response.headers['content-disposition'] || '';
    const match = disposition.match(/filename="?([^"]+)"?/);
    const filename = match ? match[1] : `actividad_${actividadId}.zip`;
    return { blob: response.data, filename };
  },

  listarContenidoZip: async (materialId: number): Promise<{ nombre: string; tamano: number; esCarpeta: boolean }[]> => {
    const response = await api.get(`${BASE_URL}/archivo/${materialId}/zip-contenido`);
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

  listarPendientesEstudiante: async (usuarioId: number): Promise<EntregaPendienteDTO[]> => {
    const response = await api.get<EntregaPendienteDTO[]>(
      `${BASE_URL}/estudiante/${usuarioId}/pendientes`
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
