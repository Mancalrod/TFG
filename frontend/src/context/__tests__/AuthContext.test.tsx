import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { AuthProvider, useAuth } from '../AuthContext';

const { mockGetAccessToken, mockMe, mockRefresh, mockLogin, mockLogout } = vi.hoisted(() => ({
  mockGetAccessToken: vi.fn(),
  mockMe: vi.fn(),
  mockRefresh: vi.fn(),
  mockLogin: vi.fn(),
  mockLogout: vi.fn(),
}));

vi.mock('../../services', () => ({
  authService: {
    getAccessToken: mockGetAccessToken,
    me: mockMe,
    refresh: mockRefresh,
    login: mockLogin,
    logout: mockLogout,
  },
}));

const authResponse = {
  accessToken: 'a',
  refreshToken: 'r',
  usuarioId: 9,
  nombre: 'Ana',
  correoElectronico: 'ana@test.com',
  roles: ['ROLE_PROFESOR'],
};

const Consumer = () => {
  const { usuario, loading, isAuthenticated, esProfesor, login, logout, actualizarFotoPerfil } = useAuth();

  return (
    <>
      <span>{loading ? 'loading' : 'ready'}</span>
      <span>{isAuthenticated ? 'auth-yes' : 'auth-no'}</span>
      <span>{esProfesor ? 'prof-yes' : 'prof-no'}</span>
      <span>{usuario?.nombre ?? 'sin-usuario'}</span>
      <button
        onClick={() => login({ correoElectronico: 'ana@test.com', contrasena: '1234' })}
      >
        login
      </button>
      <button onClick={logout}>logout</button>
      <button onClick={() => actualizarFotoPerfil('https://img.test/nueva-foto.png')}>actualizar-foto</button>
    </>
  );
};

const ConsumerSoloHook = () => {
  useAuth();
  return null;
};

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetAccessToken.mockReturnValue(null);
  });

  it('queda no autenticado si no hay token', async () => {
    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>
    );

    await waitFor(() => expect(screen.getByText('ready')).toBeInTheDocument());
    expect(screen.getByText('auth-no')).toBeInTheDocument();
    expect(screen.getByText('sin-usuario')).toBeInTheDocument();
  });

  it('lanza error si useAuth se usa fuera de AuthProvider', () => {
    expect(() => render(<ConsumerSoloHook />)).toThrow('useAuth debe usarse dentro de AuthProvider');
  });

  it('carga usuario con me cuando hay token valido', async () => {
    mockGetAccessToken.mockReturnValue('token');
    mockMe.mockResolvedValue(authResponse);

    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>
    );

    await waitFor(() => expect(screen.getByText('Ana')).toBeInTheDocument());
    expect(screen.getByText('auth-yes')).toBeInTheDocument();
    expect(screen.getByText('prof-yes')).toBeInTheDocument();
  });

  it('si me falla intenta refresh y recupera sesion', async () => {
    mockGetAccessToken.mockReturnValue('token');
    mockMe.mockRejectedValue(new Error('expired'));
    mockRefresh.mockResolvedValue(authResponse);

    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>
    );

    await waitFor(() => expect(screen.getByText('Ana')).toBeInTheDocument());
    expect(mockRefresh).toHaveBeenCalledTimes(1);
  });

  it('si me y refresh fallan hace logout', async () => {
    mockGetAccessToken.mockReturnValue('token');
    mockMe.mockRejectedValue(new Error('expired'));
    mockRefresh.mockRejectedValue(new Error('refresh-fail'));

    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>
    );

    await waitFor(() => expect(screen.getByText('ready')).toBeInTheDocument());
    expect(mockLogout).toHaveBeenCalledTimes(1);
    expect(screen.getByText('auth-no')).toBeInTheDocument();
  });

  it('login actualiza el usuario y logout limpia estado', async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValue(authResponse);

    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>
    );

    await user.click(screen.getByRole('button', { name: 'login' }));
    await waitFor(() => expect(screen.getByText('Ana')).toBeInTheDocument());

    await user.click(screen.getByRole('button', { name: 'logout' }));

    expect(mockLogout).toHaveBeenCalledTimes(1);
    expect(screen.getByText('sin-usuario')).toBeInTheDocument();
  });

  it('actualiza foto de perfil cuando hay usuario autenticado', async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValue(authResponse);

    render(
      <AuthProvider>
        <Consumer />
      </AuthProvider>
    );

    await user.click(screen.getByRole('button', { name: 'login' }));
    await waitFor(() => expect(screen.getByText('Ana')).toBeInTheDocument());

    await user.click(screen.getByRole('button', { name: 'actualizar-foto' }));

    expect(screen.getByText('Ana')).toBeInTheDocument();
  });
});
