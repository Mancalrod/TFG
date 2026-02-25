import { Routes, Route, Navigate } from 'react-router-dom'
import './App.css'

// Context
import { AuthProvider, useAuth } from './context/AuthContext'

// Components
import Navbar from './components/Navbar'

// Pages
import HomePage from './pages/home/HomePage'
import LoginPage from './pages/login/LoginPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import CursosPage from './pages/curso/CursosPage'
import ActividadPage from './pages/actividad/ActividadPage'
import EntregablePage from './pages/entregable/EntregablePage'
import NotFoundPage from './pages/NotFoundPage'

// Componente para proteger rutas que requieren autenticación
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh', color: '#a1a1aa' }}>
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
            <Route path="/actividades/:id" element={<ProtectedRoute><ActividadPage /></ProtectedRoute>} />
            <Route path="/entregables/:id" element={<ProtectedRoute><EntregablePage /></ProtectedRoute>} />
            
            {/* 404 */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </main>
      </div>
    </AuthProvider>
  )
}

export default App
