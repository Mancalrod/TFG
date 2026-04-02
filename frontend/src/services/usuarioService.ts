import api from './api';
import { CambiarContrasenaDTO, CrearUsuarioDTO, UsuarioDTO } from '../types';

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
  },

  eliminarRolProfesor: async (id: number): Promise<void> => {
    await api.delete(`${BASE_URL}/${id}/profesor`);
  },

  eliminarRolEstudiante: async (id: number): Promise<void> => {
    await api.delete(`${BASE_URL}/${id}/estudiante`);
  },

  eliminarEstudianteDeGrupo: async (usuarioId: number, grupoId: number): Promise<void> => {
    await api.delete(`${BASE_URL}/${usuarioId}/estudiante/${grupoId}`);
  },

  listarEstudiantesDeGrupo: async (grupoId: number): Promise<UsuarioDTO[]> => {
    const response = await api.get<UsuarioDTO[]>(`${BASE_URL}/grupo/${grupoId}`);
    return response.data;
  },

  obtenerProfesorId: async (usuarioId: number): Promise<number> => {
    const response = await api.get<number>(`${BASE_URL}/${usuarioId}/profesor-id`);
    return response.data;
  },

  cambiarContrasena: async (usuarioId: number, payload: CambiarContrasenaDTO): Promise<void> => {
    await api.put(`${BASE_URL}/${usuarioId}/contrasena`, payload);
  },

  subirFotoPerfil: async (usuarioId: number, archivo: File): Promise<UsuarioDTO> => {
    const formData = new FormData();
    formData.append('archivo', archivo);
    const response = await api.post<UsuarioDTO>(`${BASE_URL}/${usuarioId}/foto-perfil`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  }
};
