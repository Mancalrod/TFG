import { render, screen, waitFor } from '@testing-library/react';
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
  const renderNavbarAt = (path: string) => {
    render(
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="*" element={<Navbar />} />
        </Routes>
      </MemoryRouter>
    );
  };

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

  it('no muestra rol contextual fuera de rutas de curso', async () => {
    mockUseAuth.mockReturnValue({
      usuario: { id: 8, nombre: 'Usuario' },
      esProfesor: true,
      esEstudiante: true,
      esAdmin: false,
      logout: mockLogout,
    });

    renderNavbarAt('/dashboard');

    await waitFor(() => {
      expect(screen.queryByText('Profesor/Estudiante')).not.toBeInTheDocument();
    });
  });

  it('muestra Estudiante cuando solo pertenece como estudiante al curso', async () => {
    mockUseAuth.mockReturnValue({
      usuario: { id: 9, nombre: 'Alumno' },
      esProfesor: false,
      esEstudiante: true,
      esAdmin: false,
      logout: mockLogout,
    });
    mockListarPorEstudiante.mockResolvedValue([{ id: 22, titulo: 'Curso Y' }]);

    renderNavbarAt('/cursos/22');

    expect(await screen.findByText('Estudiante')).toBeInTheDocument();
  });

  it('muestra Profesor/Estudiante cuando tiene ambos roles en el curso', async () => {
    mockUseAuth.mockReturnValue({
      usuario: { id: 10, nombre: 'Mixto' },
      esProfesor: true,
      esEstudiante: true,
      esAdmin: false,
      logout: mockLogout,
    });
    mockListarPorProfesor.mockResolvedValue([{ id: 5, titulo: 'Curso Mixto' }]);
    mockListarPorEstudiante.mockResolvedValue([{ id: 5, titulo: 'Curso Mixto' }]);

    renderNavbarAt('/cursos/5');

    expect(await screen.findByText('Profesor/Estudiante')).toBeInTheDocument();
  });

  it('en error de API aplica fallback de roles', async () => {
    mockUseAuth.mockReturnValue({
      usuario: { id: 11, nombre: 'Fallback' },
      esProfesor: false,
      esEstudiante: true,
      esAdmin: false,
      logout: mockLogout,
    });
    mockListarPorEstudiante.mockRejectedValue(new Error('boom'));

    renderNavbarAt('/cursos/90');

    expect(await screen.findByText('Estudiante')).toBeInTheDocument();
  });

  it('si es admin en curso muestra Profesor sin consultar listados', async () => {
    mockUseAuth.mockReturnValue({
      usuario: { id: 12, nombre: 'Admin' },
      esProfesor: false,
      esEstudiante: false,
      esAdmin: true,
      logout: mockLogout,
    });

    renderNavbarAt('/cursos/7');

    expect(await screen.findByText('Profesor')).toBeInTheDocument();
    expect(mockListarPorProfesor).not.toHaveBeenCalled();
    expect(mockListarPorEstudiante).not.toHaveBeenCalled();
  });

  it('renderiza toggle con label de modo oscuro cuando isDark=false', () => {
    mockUseTheme.mockReturnValue({ isDark: false, toggleTheme: mockToggleTheme });
    mockUseAuth.mockReturnValue({
      usuario: { id: 13, nombre: 'Tema' },
      esProfesor: false,
      esEstudiante: false,
      esAdmin: false,
      logout: mockLogout,
    });

    renderNavbarAt('/dashboard');

    expect(screen.getByRole('button', { name: 'Cambiar a modo oscuro' })).toBeInTheDocument();
  });
});
