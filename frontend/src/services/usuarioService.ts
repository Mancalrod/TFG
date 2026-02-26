import api from './api';
import { UsuarioDTO, CrearUsuarioDTO } from '../types';

const BASE_URL = '/api/usuarios';

export const usuarioService = {
  listar: async (): Promise<UsuarioDTO[]> => {
    const response = await api.get<UsuarioDTO[]>(BASE_URL);
    return response.data;
  },

  obtener: async (id: number): Promise<UsuarioDTO> => {
    const response = await api.get<UsuarioDTO>(`${BASE_URL}/${id}`);
    return response.data;
  },

  obtenerPorCorreo: async (correo: string): Promise<UsuarioDTO> => {
    const response = await api.get<UsuarioDTO>(`${BASE_URL}/correo/${correo}`);
    return response.data;
  },

  crear: async (usuario: CrearUsuarioDTO): Promise<UsuarioDTO> => {
    const response = await api.post<UsuarioDTO>(BASE_URL, usuario);
    return response.data;
  },

  actualizar: async (id: number, usuario: CrearUsuarioDTO): Promise<UsuarioDTO> => {
    const response = await api.put<UsuarioDTO>(`${BASE_URL}/${id}`, usuario);
    return response.data;
  },

  eliminar: async (id: number): Promise<void> => {
    await api.delete(`${BASE_URL}/${id}`);
  },

  registrarComoProfesor: async (id: number): Promise<void> => {
    await api.post(`${BASE_URL}/${id}/profesor`);
  },

  registrarComoEstudiante: async (id: number, grupoId: number): Promise<void> => {
    await api.post(`${BASE_URL}/${id}/estudiante/${grupoId}`);
  },

  esProfesor: async (id: number): Promise<boolean> => {
    const response = await api.get<boolean>(`${BASE_URL}/${id}/es-profesor`);
    return response.data;
  },

  esEstudiante: async (id: number): Promise<boolean> => {
    const response = await api.get<boolean>(`${BASE_URL}/${id}/es-estudiante`);
    return response.data;
  }
};
