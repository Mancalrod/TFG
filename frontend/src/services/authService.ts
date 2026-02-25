import api from './api';
import { AuthResponseDTO, LoginRequestDTO, CrearUsuarioDTO } from '../types';

const TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export const authService = {
  login: async (credentials: LoginRequestDTO): Promise<AuthResponseDTO> => {
    const response = await api.post<AuthResponseDTO>('/api/auth/login', credentials);
    authService.saveTokens(response.data);
    return response.data;
  },

  register: async (usuario: CrearUsuarioDTO): Promise<AuthResponseDTO> => {
    const response = await api.post<AuthResponseDTO>('/api/auth/register', usuario);
    authService.saveTokens(response.data);
    return response.data;
  },

  refresh: async (): Promise<AuthResponseDTO> => {
    const refreshToken = authService.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }
    const response = await api.post<AuthResponseDTO>('/api/auth/refresh', { refreshToken });
    authService.saveTokens(response.data);
    return response.data;
  },

  me: async (): Promise<AuthResponseDTO> => {
    const response = await api.get<AuthResponseDTO>('/api/auth/me');
    return response.data;
  },

  logout: (): void => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },

  saveTokens: (data: AuthResponseDTO): void => {
    if (data.accessToken) {
      localStorage.setItem(TOKEN_KEY, data.accessToken);
    }
    if (data.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
    }
  },

  getAccessToken: (): string | null => {
    return localStorage.getItem(TOKEN_KEY);
  },

  getRefreshToken: (): string | null => {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  isAuthenticated: (): boolean => {
    return !!localStorage.getItem(TOKEN_KEY);
  },
};
