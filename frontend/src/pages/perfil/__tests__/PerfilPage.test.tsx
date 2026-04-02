import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PerfilPage from '../PerfilPage';

const mockUseAuth = vi.fn();
const mockCambiarContrasena = vi.fn();
const mockSubirFotoPerfil = vi.fn();
const mockObtenerPreferencias = vi.fn();
const mockActualizarPreferencias = vi.fn();
const mockActualizarFotoPerfil = vi.fn();

vi.mock('../../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../../../services', () => ({
  usuarioService: {
    cambiarContrasena: (...args: unknown[]) => mockCambiarContrasena(...args),
    subirFotoPerfil: (...args: unknown[]) => mockSubirFotoPerfil(...args),
  },
  notificacionService: {
    obtenerPreferencias: (...args: unknown[]) => mockObtenerPreferencias(...args),
    actualizarPreferencias: (...args: unknown[]) => mockActualizarPreferencias(...args),
  },
}));

describe('PerfilPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuth.mockReturnValue({
      usuario: { id: 11, nombre: 'Ana' },
      actualizarFotoPerfil: mockActualizarFotoPerfil,
    });
    mockObtenerPreferencias.mockResolvedValue({ canal: 'APP' });
    mockActualizarPreferencias.mockResolvedValue({ canal: 'APP' });
  });

  it('muestra error cuando la nueva contraseña es débil', async () => {
    const user = userEvent.setup();
    render(<PerfilPage />);

    await user.type(screen.getByLabelText('Nueva contraseña'), 'weak');
    await user.type(screen.getByLabelText('Confirmar nueva contraseña'), 'weak');

    expect(screen.getByText(/al menos 8 caracteres/i)).toBeInTheDocument();
  });

  it('permite mostrar y ocultar cada campo de contraseña de forma independiente', async () => {
    const user = userEvent.setup();
    render(<PerfilPage />);

    const actualInput = screen.getByLabelText('Contraseña actual');
    const nuevaInput = screen.getByLabelText('Nueva contraseña');
    const confirmInput = screen.getByLabelText('Confirmar nueva contraseña');

    expect(actualInput).toHaveAttribute('type', 'password');
    expect(nuevaInput).toHaveAttribute('type', 'password');
    expect(confirmInput).toHaveAttribute('type', 'password');

    await user.click(screen.getByRole('button', { name: 'Mostrar contraseña actual' }));
    expect(actualInput).toHaveAttribute('type', 'text');
    expect(nuevaInput).toHaveAttribute('type', 'password');

    await user.click(screen.getByRole('button', { name: 'Mostrar nueva contraseña' }));
    expect(nuevaInput).toHaveAttribute('type', 'text');

    await user.click(screen.getByRole('button', { name: 'Mostrar confirmación de contraseña' }));
    expect(confirmInput).toHaveAttribute('type', 'text');

    await user.click(screen.getByRole('button', { name: 'Ocultar contraseña actual' }));
    expect(actualInput).toHaveAttribute('type', 'password');
  });

  it('actualiza navbar context al subir foto de perfil', async () => {
    const user = userEvent.setup();
    mockSubirFotoPerfil.mockResolvedValue({ fotoPerfilUrl: 'https://img.test/nueva.png' });

    render(<PerfilPage />);

    const file = new File(['png'], 'avatar.png', { type: 'image/png' });
    await user.upload(screen.getByLabelText('Seleccionar imagen'), file);
    await user.click(screen.getByRole('button', { name: 'Subir foto' }));

    await waitFor(() => {
      expect(mockSubirFotoPerfil).toHaveBeenCalledWith(11, file);
    });
    expect(mockActualizarFotoPerfil).toHaveBeenCalledWith('https://img.test/nueva.png');
  });

  it('envia cambio de contraseña cuando los datos son válidos', async () => {
    const user = userEvent.setup();
    mockCambiarContrasena.mockResolvedValue(undefined);

    render(<PerfilPage />);

    await user.type(screen.getByLabelText('Contraseña actual'), 'Actual123!');
    await user.type(screen.getByLabelText('Nueva contraseña'), 'Nueva123!');
    await user.type(screen.getByLabelText('Confirmar nueva contraseña'), 'Nueva123!');
    await user.click(screen.getByRole('button', { name: 'Guardar contraseña' }));

    await waitFor(() => {
      expect(mockCambiarContrasena).toHaveBeenCalledWith(11, {
        contrasenaActual: 'Actual123!',
        contrasenaNueva: 'Nueva123!',
      });
    });
  });
});
