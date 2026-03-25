import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import Navbar from '../Navbar';

const mockUseAuth = vi.fn();
const mockUseTheme = vi.fn();
const mockNavigate = vi.fn();
const mockLogout = vi.fn();
const mockToggleTheme = vi.fn();
const mockListarPorProfesor = vi.fn();
const mockListarPorEstudiante = vi.fn();

vi.mock('../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../../context/ThemeContext', () => ({
  useTheme: () => mockUseTheme(),
}));

vi.mock('../../services/cursoService', () => ({
  cursoService: {
    listarPorProfesor: (...args: unknown[]) => mockListarPorProfesor(...args),
    listarPorEstudiante: (...args: unknown[]) => mockListarPorEstudiante(...args),
  },
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('Navbar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseTheme.mockReturnValue({ isDark: true, toggleTheme: mockToggleTheme });
    mockListarPorProfesor.mockResolvedValue([]);
    mockListarPorEstudiante.mockResolvedValue([]);
  });

  it('muestra login cuando no hay usuario', () => {
    mockUseAuth.mockReturnValue({
      usuario: null,
      esProfesor: false,
      esAdmin: false,
      logout: mockLogout,
    });

    render(
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    );

    expect(screen.getByRole('link', { name: 'Iniciar Sesión' })).toBeInTheDocument();
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument();
  });

  it('muestra enlaces de profesor y admin para usuario con roles', async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      usuario: { id: 1, nombre: 'Admin Profe' },
      esProfesor: true,
      esAdmin: true,
      logout: mockLogout,
    });

    render(
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    );

    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Evaluaciones' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Administración' })).toBeInTheDocument();
    expect(screen.queryByText('Administrador')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cambiar a modo claro' }));
    expect(mockToggleTheme).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: 'Cerrar Sesión' }));
    expect(mockLogout).toHaveBeenCalledTimes(1);
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('muestra rol contextual en navbar al entrar en un curso', async () => {
    mockUseAuth.mockReturnValue({
      usuario: { id: 7, nombre: 'Docente' },
      esProfesor: true,
      esEstudiante: false,
      esAdmin: false,
      logout: mockLogout,
    });
    mockListarPorProfesor.mockResolvedValue([{ id: 1, titulo: 'Curso X' }]);

    render(
      <MemoryRouter initialEntries={['/cursos/1']}>
        <Routes>
          <Route path="/cursos/:id" element={<Navbar />} />
        </Routes>
      </MemoryRouter>
    );

    expect(await screen.findByText('Profesor')).toBeInTheDocument();
  });
});
