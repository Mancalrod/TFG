import api from './api';
import { CursoDTO, CrearCursoDTO, GrupoDTO } from '../types';

const BASE_URL = '/cursos';

export const cursoService = {
  listarTodos: async (): Promise<CursoDTO[]> => {
    const response = await api.get<CursoDTO[]>(BASE_URL);
    return response.data;
  },

  obtener: async (id: number): Promise<CursoDTO> => {
    const response = await api.get<CursoDTO>(`${BASE_URL}/${id}`);
    return response.data;
  },

  obtenerPorCodigo: async (codigo: string): Promise<CursoDTO> => {
    const response = await api.get<CursoDTO>(`${BASE_URL}/codigo/${codigo}`);
    return response.data;
  },

  crear: async (profesorId: number, curso: CrearCursoDTO): Promise<CursoDTO> => {
    const response = await api.post<CursoDTO>(`${BASE_URL}/profesor/${profesorId}`, curso);
    return response.data;
  },

  listarPorProfesor: async (profesorId: number): Promise<CursoDTO[]> => {
    const response = await api.get<CursoDTO[]>(`${BASE_URL}/profesor/${profesorId}`);
    return response.data;
  },

  listarPorEstudiante: async (estudianteId: number): Promise<CursoDTO[]> => {
    const response = await api.get<CursoDTO[]>(`${BASE_URL}/estudiante/${estudianteId}`);
    return response.data;
  },

  actualizar: async (id: number, curso: CrearCursoDTO): Promise<CursoDTO> => {
    const response = await api.put<CursoDTO>(`${BASE_URL}/${id}`, curso);
    return response.data;
  },

  eliminar: async (id: number): Promise<void> => {
    await api.delete(`${BASE_URL}/${id}`);
  },

  agregarProfesor: async (cursoId: number, profesorId: number): Promise<CursoDTO> => {
    const response = await api.post<CursoDTO>(`${BASE_URL}/${cursoId}/profesores/${profesorId}`);
    return response.data;
  },

  quitarProfesor: async (cursoId: number, profesorId: number): Promise<CursoDTO> => {
    const response = await api.delete<CursoDTO>(`${BASE_URL}/${cursoId}/profesores/${profesorId}`);
    return response.data;
  },

  crearGrupo: async (cursoId: number, titulo: string): Promise<GrupoDTO> => {
    const response = await api.post<GrupoDTO>(`${BASE_URL}/${cursoId}/grupos`, null, {
      params: { titulo }
    });
    return response.data;
  },

  listarGrupos: async (cursoId: number): Promise<GrupoDTO[]> => {
    const response = await api.get<GrupoDTO[]>(`${BASE_URL}/${cursoId}/grupos`);
    return response.data;
  }
};
