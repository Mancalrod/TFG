import { Routes, Route, Navigate } from 'react-router-dom'
import './App.css'

// Context
import { AuthProvider, useAuth } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'

// Components
import Navbar from './components/Navbar'

// Pages
import HomePage from './pages/home/HomePage'
import LoginPage from './pages/login/LoginPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import CursosPage from './pages/curso/CursosPage'
import CursoDetallePage from './pages/curso/CursoDetallePage'
import ActividadPage from './pages/actividad/ActividadPage'
import EditarActividadPage from './pages/actividad/EditarActividadPage'
import EntregablePage from './pages/entregable/EntregablePage'
import EditarEntregablePage from './pages/entregable/EditarEntregablePage'
import ActividadesPorCursoPage from './pages/actividades-por-curso/ActividadesPorCursoPage'
import NotFoundPage from './pages/NotFoundPage'

// Componente para proteger rutas que requieren autenticación
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh', color: 'var(--text-secondary)' }}>
        Cargando...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

function App() {
  return (
    <ThemeProvider>
    <AuthProvider>
      <div className="app">
        <Navbar />
        <main className="main-content">
          <Routes>
            {/* Rutas públicas */}
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            
            {/* Rutas protegidas */}
            <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
            <Route path="/cursos" element={<ProtectedRoute><CursosPage /></ProtectedRoute>} />
            <Route path="/cursos/:id" element={<ProtectedRoute><CursoDetallePage /></ProtectedRoute>} />
            <Route path="/actividades-por-curso" element={<ProtectedRoute><ActividadesPorCursoPage /></ProtectedRoute>} />
            <Route path="/actividades/:id" element={<ProtectedRoute><ActividadPage /></ProtectedRoute>} />
            <Route path="/actividades/:id/editar" element={<ProtectedRoute><EditarActividadPage /></ProtectedRoute>} />
            <Route path="/entregables/:id" element={<ProtectedRoute><EntregablePage /></ProtectedRoute>} />
            <Route path="/entregables/:id/editar" element={<ProtectedRoute><EditarEntregablePage /></ProtectedRoute>} />
            
            {/* 404 */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </main>
      </div>
    </AuthProvider>
    </ThemeProvider>
  )
}

export default App
