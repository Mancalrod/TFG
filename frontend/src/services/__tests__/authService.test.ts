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

  it('register guarda tokens y devuelve payload', async () => {
    mockPost.mockResolvedValue({
      data: {
        accessToken: 'reg-access',
        refreshToken: 'reg-refresh',
        usuarioId: 2,
        nombre: 'Alumno Demo',
        correoElectronico: 'alumno@test.com',
        roles: ['ROLE_ESTUDIANTE'],
      },
    });

    const result = await authService.register({
      nombre: 'Alumno Demo',
      correoElectronico: 'alumno@test.com',
      contrasena: '1234',
      esAdmin: false,
    });

    expect(mockPost).toHaveBeenCalledWith('/api/auth/register', {
      nombre: 'Alumno Demo',
      correoElectronico: 'alumno@test.com',
      contrasena: '1234',
      esAdmin: false,
    });
    expect(result.accessToken).toBe('reg-access');
    expect(localStorage.getItem('accessToken')).toBe('reg-access');
    expect(localStorage.getItem('refreshToken')).toBe('reg-refresh');
  });

  it('refresh usa refresh token almacenado y actualiza tokens', async () => {
    localStorage.setItem('refreshToken', 'refresh-previo');
    mockPost.mockResolvedValue({
      data: {
        accessToken: 'nuevo-access',
        refreshToken: 'nuevo-refresh',
        usuarioId: 1,
        nombre: 'Profesor Demo',
        correoElectronico: 'profe@test.com',
        roles: ['ROLE_PROFESOR'],
      },
    });

    const refreshed = await authService.refresh();

    expect(mockPost).toHaveBeenCalledWith('/api/auth/refresh', { refreshToken: 'refresh-previo' });
    expect(refreshed.accessToken).toBe('nuevo-access');
    expect(localStorage.getItem('accessToken')).toBe('nuevo-access');
    expect(localStorage.getItem('refreshToken')).toBe('nuevo-refresh');
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

  it('forgotPassword llama al endpoint de recuperacion', async () => {
    mockPost.mockResolvedValue({ data: {} });

    await authService.forgotPassword({ correoElectronico: 'demo@test.com' });

    expect(mockPost).toHaveBeenCalledWith('/api/auth/forgot-password', {
      correoElectronico: 'demo@test.com',
    });
  });

  it('resetPassword llama al endpoint de reseteo', async () => {
    mockPost.mockResolvedValue({ data: {} });

    await authService.resetPassword({
      token: 'reset-token',
      contrasenaNueva: 'NuevaPass1!',
    });

    expect(mockPost).toHaveBeenCalledWith('/api/auth/reset-password', {
      token: 'reset-token',
      contrasenaNueva: 'NuevaPass1!',
    });
  });

  it('logout elimina tokens', () => {
    localStorage.setItem('accessToken', 'a');
    localStorage.setItem('refreshToken', 'b');

    authService.logout();

    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  it('saveTokens no sobreescribe cuando faltan tokens en payload', () => {
    localStorage.setItem('accessToken', 'a-previo');
    localStorage.setItem('refreshToken', 'r-previo');

    authService.saveTokens({
      accessToken: '',
      refreshToken: '',
      tokenType: 'Bearer',
      usuarioId: 1,
      nombre: 'Sin token',
      correoElectronico: 'st@test.com',
      roles: ['ROLE_ESTUDIANTE'],
    });

    expect(localStorage.getItem('accessToken')).toBe('a-previo');
    expect(localStorage.getItem('refreshToken')).toBe('r-previo');
  });

  it('isAuthenticated depende de access token', () => {
    expect(authService.isAuthenticated()).toBe(false);
    localStorage.setItem('accessToken', 'token');
    expect(authService.isAuthenticated()).toBe(true);
  });
});

