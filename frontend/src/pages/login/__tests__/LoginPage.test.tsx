import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from '../LoginPage';

const mockNavigate = vi.fn();
const mockUseAuth = vi.fn();
const mockLogin = vi.fn();

vi.mock('../../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuth.mockReturnValue({
      login: mockLogin,
      isAuthenticated: false,
    });
  });

  const renderPage = () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );
  };

  it('valida que email y password sean obligatorios', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(screen.getByText('Por favor, completa todos los campos')).toBeInTheDocument();
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('hace login y navega al dashboard', async () => {
    const user = userEvent.setup();
    mockLogin.mockResolvedValue({});
    renderPage();

    await user.type(screen.getByLabelText('Correo electrónico'), 'demo@test.com');
    await user.type(screen.getByLabelText('Contraseña'), '1234');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        correoElectronico: 'demo@test.com',
        contrasena: '1234',
      });
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });

  it('muestra mensaje especifico en 401', async () => {
    const user = userEvent.setup();
    mockLogin.mockRejectedValue({ response: { status: 401, data: {} } });
    renderPage();

    await user.type(screen.getByLabelText('Correo electrónico'), 'demo@test.com');
    await user.type(screen.getByLabelText('Contraseña'), 'bad');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByText('Correo electrónico o contraseña incorrectos')).toBeInTheDocument();
  });

  it('muestra mensaje de backend en errores no-401', async () => {
    const user = userEvent.setup();
    mockLogin.mockRejectedValue({ response: { status: 500, data: { message: 'Backend roto' } } });
    renderPage();

    await user.type(screen.getByLabelText('Correo electrónico'), 'demo@test.com');
    await user.type(screen.getByLabelText('Contraseña'), 'bad');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByText('Backend roto')).toBeInTheDocument();
  });

  it('muestra error de conexion para excepciones sin response', async () => {
    const user = userEvent.setup();
    mockLogin.mockRejectedValue(new Error('network'));
    renderPage();

    await user.type(screen.getByLabelText('Correo electrónico'), 'demo@test.com');
    await user.type(screen.getByLabelText('Contraseña'), 'bad');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByText('Error de conexión con el servidor')).toBeInTheDocument();
  });

  it('redirige automaticamente si ya esta autenticado', async () => {
    mockUseAuth.mockReturnValue({
      login: mockLogin,
      isAuthenticated: true,
    });

    renderPage();

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });
});
