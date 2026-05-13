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
    Object.defineProperty(URL, 'createObjectURL', {
      value: vi.fn(() => 'blob:avatar'),
      configurable: true,
    });
    Object.defineProperty(URL, 'revokeObjectURL', {
      value: vi.fn(),
      configurable: true,
    });
    mockUseAuth.mockReturnValue({
      usuario: {
        id: 11,
        nombre: 'Ana',
        fotoPerfilUrl: 'https://img.test/actual.png',
      },
      actualizarFotoPerfil: mockActualizarFotoPerfil,
    });
    mockObtenerPreferencias.mockResolvedValue({ canal: 'APP' });
    mockActualizarPreferencias.mockResolvedValue({ canal: 'APP' });
  });

  it('muestra el nombre y la foto de perfil en grande', () => {
    render(<PerfilPage />);

    expect(screen.getByRole('heading', { name: 'Ana' })).toBeInTheDocument();
    expect(screen.getByAltText('Foto de perfil')).toHaveAttribute('src', 'https://img.test/actual.png');
  });

  it('muestra cambio de contraseña y preferencias debajo del perfil', () => {
    render(<PerfilPage />);

    expect(screen.getByRole('heading', { name: 'Cambiar contraseña' })).toBeInTheDocument();
    expect(screen.getByLabelText('Nueva contraseña')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Preferencias' })).toBeInTheDocument();
    expect(screen.getByText('Solo en la aplicación')).toBeInTheDocument();
  });

  it('abre el ajuste cuadrado al seleccionar una imagen valida', async () => {
    const user = userEvent.setup();
    render(<PerfilPage />);

    const file = new File(['png'], 'avatar.png', { type: 'image/png' });
    await user.upload(screen.getByLabelText('Seleccionar imagen'), file);

    expect(screen.getByRole('dialog', { name: 'Ajustar foto' })).toBeInTheDocument();
    expect(screen.getByLabelText('Zoom')).toBeInTheDocument();
    expect(screen.getByLabelText('Horizontal')).toBeInTheDocument();
    expect(screen.getByLabelText('Vertical')).toBeInTheDocument();
  });

  it('envia cambio de contraseña cuando los datos son validos', async () => {
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

  it('guarda preferencias de notificacion', async () => {
    const user = userEvent.setup();
    render(<PerfilPage />);

    await user.click(screen.getByLabelText('Solo por correo'));
    await user.click(screen.getByRole('button', { name: 'Guardar preferencias' }));

    await waitFor(() => {
      expect(mockActualizarPreferencias).toHaveBeenCalledWith({ canal: 'EMAIL' });
    });
  });
});
