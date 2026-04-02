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
const mockNotiContar = vi.fn();
const mockNotiListar = vi.fn();
const mockNotiMarcar = vi.fn();

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

vi.mock('../../services/notificacionService', () => ({
  notificacionService: {
    contarNoLeidas: (...args: unknown[]) => mockNotiContar(...args),
    listar: (...args: unknown[]) => mockNotiListar(...args),
    marcarComoLeida: (...args: unknown[]) => mockNotiMarcar(...args),
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
    mockNotiContar.mockResolvedValue(0);
    mockNotiListar.mockResolvedValue([]);
    mockNotiMarcar.mockResolvedValue(undefined);
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
    expect(screen.getByRole('button', { name: 'Notificaciones (inicia sesion)' })).toBeDisabled();
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

  it('muestra avatar de perfil cuando usuario tiene foto', () => {
    mockUseAuth.mockReturnValue({
      usuario: { id: 14, nombre: 'Avatar', fotoPerfilUrl: 'https://img.test/avatar.png' },
      esProfesor: false,
      esEstudiante: false,
      esAdmin: false,
      logout: mockLogout,
    });

    renderNavbarAt('/dashboard');

    const avatar = screen.getByAltText('Foto de perfil');
    expect(avatar).toBeInTheDocument();
    expect(avatar).toHaveAttribute('src', expect.stringContaining('https://img.test/avatar.png'));
  });

  it('muestra panel de notificaciones con contador y lista', async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      usuario: { id: 15, nombre: 'Noti User' },
      esProfesor: false,
      esEstudiante: true,
      esAdmin: false,
      logout: mockLogout,
    });
    mockNotiContar.mockResolvedValue(2);
    mockNotiListar.mockResolvedValue([
      { id: 101, titulo: 'Nuevo entregable', mensaje: 'Hay una nueva tarea', leida: false, tipo: 'NUEVO_ENTREGABLE', fechaCreacion: '2026-03-26T18:00:00' },
    ]);

    renderNavbarAt('/dashboard');

    expect(await screen.findByText('2')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Abrir notificaciones' }));

    expect(await screen.findByText('Notificaciones')).toBeInTheDocument();
    expect(await screen.findByText('Nuevo entregable')).toBeInTheDocument();
    expect(await screen.findByText('Hay una nueva tarea')).toBeInTheDocument();
  });

  it('muestra estado vacio si falla carga de notificaciones', async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      usuario: { id: 17, nombre: 'Sin Notis' },
      esProfesor: false,
      esEstudiante: true,
      esAdmin: false,
      logout: mockLogout,
    });
    mockNotiContar.mockRejectedValue(new Error('count error'));
    mockNotiListar.mockRejectedValue(new Error('list error'));

    renderNavbarAt('/dashboard');

    await user.click(screen.getByRole('button', { name: 'Abrir notificaciones' }));

    expect(await screen.findByText('No hay notificaciones.')).toBeInTheDocument();
  });

  it('decodifica entidades html en notificaciones y navega por actividadId', async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      usuario: { id: 18, nombre: 'HTML User' },
      esProfesor: false,
      esEstudiante: true,
      esAdmin: false,
      logout: mockLogout,
    });
    mockNotiContar.mockResolvedValue(1);
    mockNotiListar.mockResolvedValue([
      {
        id: 303,
        titulo: 'Nueva &amp; actividad',
        mensaje: 'Detalle &lt;importante&gt;',
        leida: false,
        tipo: 'NUEVA_ACTIVIDAD',
        fechaCreacion: '2026-04-02T12:01:00',
        actividadId: 77,
      },
    ]);

    renderNavbarAt('/dashboard');

    await user.click(screen.getByRole('button', { name: 'Abrir notificaciones' }));
    expect(await screen.findByText('Nueva & actividad')).toBeInTheDocument();
    expect(await screen.findByText('Detalle <importante>')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Nueva & actividad/i }));
    expect(mockNavigate).toHaveBeenCalledWith('/actividades/77');
    expect(mockNotiMarcar).toHaveBeenCalledWith(303);
  });

  it('no intenta marcar como leida cuando ya esta leida', async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      usuario: { id: 19, nombre: 'Read User' },
      esProfesor: false,
      esEstudiante: true,
      esAdmin: false,
      logout: mockLogout,
    });
    mockNotiListar.mockResolvedValue([
      {
        id: 404,
        titulo: 'Leida',
        mensaje: 'ya estaba leida',
        leida: true,
        tipo: 'NUEVO_ENTREGABLE',
        fechaCreacion: '2026-04-02T12:03:00',
        cursoId: 55,
      },
    ]);

    renderNavbarAt('/dashboard');

    await user.click(screen.getByRole('button', { name: 'Abrir notificaciones' }));
    await user.click(await screen.findByRole('button', { name: /Leida/i }));

    expect(mockNotiMarcar).not.toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith('/cursos/55');
  });

  it('navega al recurso especifico de la notificacion cuando existe entregaId', async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({
      usuario: { id: 16, nombre: 'Noti Deep Link' },
      esProfesor: false,
      esEstudiante: true,
      esAdmin: false,
      logout: mockLogout,
    });
    mockNotiContar.mockResolvedValue(1);
    mockNotiListar.mockResolvedValue([
      {
        id: 202,
        titulo: 'Entrega evaluada',
        mensaje: 'Tu entrega ha sido evaluada',
        leida: false,
        tipo: 'ENTREGA_EVALUADA',
        fechaCreacion: '2026-04-02T11:58:00',
        cursoId: 5,
        entregaId: 88,
      },
    ]);

    renderNavbarAt('/dashboard');

    await user.click(screen.getByRole('button', { name: 'Abrir notificaciones' }));
    await user.click(await screen.findByRole('button', { name: /Entrega evaluada/i }));

    expect(mockNavigate).toHaveBeenCalledWith('/entregas/88');
  });
});
