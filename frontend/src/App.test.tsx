import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import App from './App';

const mockUseAuth = vi.fn();

vi.mock('./context/AuthContext', () => ({
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  useAuth: () => mockUseAuth(),
}));

vi.mock('./context/ThemeContext', () => ({
  ThemeProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock('./components/Navbar', () => ({
  default: () => <div>Navbar</div>,
}));

vi.mock('./pages/home/HomePage', () => ({ default: () => <div>HomePage</div> }));
vi.mock('./pages/login/LoginPage', () => ({ default: () => <div>LoginPage</div> }));
vi.mock('./pages/dashboard/DashboardPage', () => ({ default: () => <div>DashboardPage</div> }));
vi.mock('./pages/curso/CursosPage', () => ({ default: () => <div>CursosPage</div> }));
vi.mock('./pages/curso/CursoDetallePage', () => ({ default: () => <div>CursoDetallePage</div> }));
vi.mock('./pages/actividad/ActividadPage', () => ({ default: () => <div>ActividadPage</div> }));
vi.mock('./pages/actividad/EditarActividadPage', () => ({ default: () => <div>EditarActividadPage</div> }));
vi.mock('./pages/entregable/EntregablePage', () => ({ default: () => <div>EntregablePage</div> }));
vi.mock('./pages/entregable/EditarEntregablePage', () => ({ default: () => <div>EditarEntregablePage</div> }));
vi.mock('./pages/entregable/CrearEntregablePage', () => ({ default: () => <div>CrearEntregablePage</div> }));
vi.mock('./pages/entrega/RealizarEntregaPage', () => ({ default: () => <div>RealizarEntregaPage</div> }));
vi.mock('./pages/entrega/EntregaDetallePage', () => ({ default: () => <div>EntregaDetallePage</div> }));
vi.mock('./pages/actividades-por-curso/ActividadesPorCursoPage', () => ({ default: () => <div>ActividadesPorCursoPage</div> }));
vi.mock('./pages/admin/AdminPage', () => ({ default: () => <div>AdminPage</div> }));
vi.mock('./pages/evaluaciones/EvaluacionesPage', () => ({ default: () => <div>EvaluacionesPage</div> }));
vi.mock('./pages/NotFoundPage', () => ({ default: () => <div>NotFoundPage</div> }));

const renderAt = (path: string) => {
  render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>
  );
};

describe('App routes', () => {
  it('muestra loading mientras se resuelve auth en ruta protegida', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false, loading: true });

    renderAt('/dashboard');

    expect(screen.getByText('Cargando...')).toBeInTheDocument();
  });

  it('redirige a login si entra a ruta protegida sin auth', async () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false, loading: false });

    renderAt('/dashboard');

    await waitFor(() => {
      expect(screen.getByText('LoginPage')).toBeInTheDocument();
    });
  });

  it('redirige home a dashboard si el usuario ya esta autenticado', async () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true, loading: false });

    renderAt('/');

    await waitFor(() => {
      expect(screen.getByText('DashboardPage')).toBeInTheDocument();
    });
  });

  it('renderiza 404 para rutas no existentes', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false, loading: false });

    renderAt('/ruta-inexistente');

    expect(screen.getByText('NotFoundPage')).toBeInTheDocument();
  });
});
