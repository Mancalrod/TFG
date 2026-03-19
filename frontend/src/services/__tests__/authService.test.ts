import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockPost, mockGet } = vi.hoisted(() => ({
  mockPost: vi.fn(),
  mockGet: vi.fn(),
}));

vi.mock('../api', () => ({
  default: {
    post: mockPost,
    get: mockGet,
  },
}));

import { authService } from '../authService';

describe('authService', () => {
  beforeEach(() => {
    mockPost.mockReset();
    mockGet.mockReset();
    localStorage.clear();
  });

  it('login guarda access y refresh token', async () => {
    mockPost.mockResolvedValue({
      data: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        usuarioId: 1,
        nombre: 'Profesor Demo',
        correoElectronico: 'profe@test.com',
        roles: ['ROLE_PROFESOR'],
      },
    });

    const result = await authService.login({
      correoElectronico: 'profe@test.com',
      contrasena: 'secreto',
    });

    expect(mockPost).toHaveBeenCalledWith('/api/auth/login', {
      correoElectronico: 'profe@test.com',
      contrasena: 'secreto',
    });
    expect(result.accessToken).toBe('access-token');
    expect(localStorage.getItem('accessToken')).toBe('access-token');
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token');
  });

  it('refresh lanza error si no hay refresh token', async () => {
    await expect(authService.refresh()).rejects.toThrow('No refresh token available');
  });

  it('me consulta endpoint de perfil', async () => {
    mockGet.mockResolvedValue({
      data: {
        usuarioId: 1,
        nombre: 'Profesor Demo',
        correoElectronico: 'profe@test.com',
        roles: ['ROLE_PROFESOR'],
      },
    });

    const me = await authService.me();
    expect(mockGet).toHaveBeenCalledWith('/api/auth/me');
    expect(me.usuarioId).toBe(1);
  });

  it('logout elimina tokens', () => {
    localStorage.setItem('accessToken', 'a');
    localStorage.setItem('refreshToken', 'b');

    authService.logout();

    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });
});

