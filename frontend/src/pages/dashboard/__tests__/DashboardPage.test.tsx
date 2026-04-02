import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DashboardPage from '../DashboardPage';

const mockUseAuth = vi.fn();
const mockListarPorEstudiante = vi.fn();
const mockListarPendientesEstudiante = vi.fn();
const mockGetConnectionStatus = vi.fn();

vi.mock('../../../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../../../services', () => ({
  cursoService: {
    listarTodos: vi.fn(),
    listarPorProfesor: vi.fn(),
    listarPorEstudiante: (...args: unknown[]) => mockListarPorEstudiante(...args),
  },
  entregaService: {
    listarPendientesEstudiante: (...args: unknown[]) => mockListarPendientesEstudiante(...args),
  },
  oneDriveService: {
    getConnectionStatus: (...args: unknown[]) => mockGetConnectionStatus(...args),
  },
}));

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseAuth.mockReturnValue({
      usuario: { id: 5, nombre: 'Ana' },
      esEstudiante: true,
      esAdmin: false,
    });
    mockListarPorEstudiante.mockResolvedValue([]);
    mockGetConnectionStatus.mockResolvedValue({ conectado: false });
  });

  it('renderiza widget de pendientes ordenado para estudiante', async () => {
    mockListarPendientesEstudiante.mockResolvedValue([
      {
        entregableId: 9,
        entregableTitulo: 'Práctica final',
        cursoTitulo: 'Seguridad',
        actividadTitulo: 'Criptografía',
        fechaLimite: new Date(Date.now() + 2 * 3_600_000).toISOString(),
        tiempoRestante: '2h',
      },
    ]);

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Entregas pendientes')).toBeInTheDocument();
    });
    expect(screen.getByText('Práctica final')).toBeInTheDocument();
    expect(screen.getByText('2h')).toBeInTheDocument();
  });
});
