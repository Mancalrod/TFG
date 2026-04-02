import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ActividadesPorCursoPage from '../ActividadesPorCursoPage';

const mockUseAuth = vi.fn();
const mockListarTodos = vi.fn();
const mockListarPorProfesor = vi.fn();
const mockListarPorEstudiante = vi.fn();
const mockListarActividadesPorCurso = vi.fn();

vi.mock('../../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../../../services/cursoService', () => ({
  cursoService: {
    listarTodos: (...args: unknown[]) => mockListarTodos(...args),
    listarPorProfesor: (...args: unknown[]) => mockListarPorProfesor(...args),
    listarPorEstudiante: (...args: unknown[]) => mockListarPorEstudiante(...args),
  },
}));

vi.mock('../../../services/actividadService', () => ({
  actividadService: {
    listarPorCurso: (...args: unknown[]) => mockListarActividadesPorCurso(...args),
  },
}));

describe('ActividadesPorCursoPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    mockUseAuth.mockReturnValue({
      usuario: { id: 1, nombre: 'Admin Demo', correoElectronico: 'admin@test.com', roles: ['ROLE_ADMIN'] },
      esProfesor: false,
      esAdmin: true,
    });

    mockListarTodos.mockResolvedValue([
      {
        id: 10,
        titulo: 'Programacion I',
        descripcion: 'Curso base',
        codigo: 'PROG1',
        grupos: [],
        numeroActividades: 1,
        numeroProfesores: 1,
        numeroEstudiantes: 30,
      },
    ]);

    mockListarPorProfesor.mockResolvedValue([]);
    mockListarPorEstudiante.mockResolvedValue([]);

    mockListarActividadesPorCurso.mockResolvedValue([
      {
        id: 101,
        titulo: 'Actividad 1',
        descripcion: 'Descripcion actividad',
        tipoActividad: 'EVALUABLE',
        fechaCreacion: '2026-03-25T10:00:00Z',
        visibilidad: 'VISIBLE',
        cursoId: 10,
        cursoTitulo: 'Programacion I',
        grupoIds: [],
        numeroEntregables: 2,
        enPlazo: true,
      },
    ]);
  });

  it('si el usuario es admin carga cursos con listarTodos', async () => {
    render(
      <MemoryRouter>
        <ActividadesPorCursoPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Actividades por Curso')).toBeInTheDocument();
    expect(screen.getByText('Consulta las actividades de todos los cursos')).toBeInTheDocument();

    await waitFor(() => {
      expect(mockListarTodos).toHaveBeenCalledTimes(1);
      expect(mockListarTodos).toHaveBeenCalledWith();
    });

    expect(mockListarPorProfesor).not.toHaveBeenCalled();
    expect(mockListarPorEstudiante).not.toHaveBeenCalled();
    expect(screen.getByText('Programacion I')).toBeInTheDocument();
  });

  it('al expandir un curso carga y muestra actividades', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <ActividadesPorCursoPage />
      </MemoryRouter>
    );

    const botonCurso = await screen.findByRole('button', { name: /Programacion I/i });
    await user.click(botonCurso);

    await waitFor(() => {
      expect(mockListarActividadesPorCurso).toHaveBeenCalledWith(10);
    });

    expect(await screen.findByText('Actividad 1')).toBeInTheDocument();
    expect(screen.getByText('2 entregables')).toBeInTheDocument();
  });
});
