import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import EvaluacionesPage from '../EvaluacionesPage';

const mockUseAuth = vi.fn();
const mockListarPendientes = vi.fn();
const mockDescargarActividad = vi.fn();

vi.mock('../../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../../../services', () => ({
  entregaService: {
    listarPendientesCalificar: (...args: unknown[]) => mockListarPendientes(...args),
    descargarTodoActividad: (...args: unknown[]) => mockDescargarActividad(...args),
    descargarTodo: vi.fn(),
  },
}));

describe('EvaluacionesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Object.defineProperty(globalThis.URL, 'createObjectURL', {
      writable: true,
      value: vi.fn(() => 'blob:mock-url'),
    });
    Object.defineProperty(globalThis.URL, 'revokeObjectURL', {
      writable: true,
      value: vi.fn(),
    });
    mockUseAuth.mockReturnValue({
      usuario: { id: 1, nombre: 'Profesor Demo', correoElectronico: 'profe@test.com', roles: ['ROLE_PROFESOR'] },
      esProfesor: true,
    });
    mockListarPendientes.mockResolvedValue([
      {
        entregaId: 100,
        cursoId: 10,
        cursoTitulo: 'Programacion I',
        actividadId: 11,
        actividadTitulo: 'Actividad 1',
        entregableId: 12,
        entregableTitulo: 'Practica Arrays',
        estudianteId: 200,
        estudianteNombre: 'Ana Perez',
        estudianteCorreo: 'ana@test.com',
        grupoTitulo: 'Grupo A',
        fechaEntrega: '2026-03-15T10:00:00Z',
        estado: 'ENTREGADO',
        fueATiempo: false,
        version: 2,
      },
    ]);
    mockDescargarActividad.mockResolvedValue({
      blob: new Blob(['zip']),
      filename: 'actividad_11.zip',
    });
  });

  it('carga pendientes y permite descargar por actividad', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter>
        <EvaluacionesPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Evaluaciones pendientes')).toBeInTheDocument();
    expect(await screen.findByText('Programacion I')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Programacion I/i }));
    await user.click(screen.getByRole('button', { name: /Actividad 1/i }));
    await user.click(screen.getByRole('button', { name: /Descargar actividad/i }));

    await waitFor(() => {
      expect(mockDescargarActividad).toHaveBeenCalledWith(11);
    });
  });

  it('muestra mensaje de acceso restringido para usuarios no profesor', async () => {
    mockUseAuth.mockReturnValue({
      usuario: { id: 2, nombre: 'Alumno Demo', correoElectronico: 'alumno@test.com', roles: ['ROLE_ESTUDIANTE'] },
      esProfesor: false,
    });

    render(
      <MemoryRouter>
        <EvaluacionesPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Seccion solo para profesores')).toBeInTheDocument();
  });
});
