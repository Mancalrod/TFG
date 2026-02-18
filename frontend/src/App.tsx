import { Routes, Route } from 'react-router-dom'
import './App.css'

// Context
import { AuthProvider } from './context/AuthContext'

// Components
import Navbar from './components/Navbar'

// Pages
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import CursosPage from './pages/CursosPage'
import ActividadPage from './pages/ActividadPage'
import EntregablePage from './pages/EntregablePage'
import NotFoundPage from './pages/NotFoundPage'

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
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/cursos" element={<CursosPage />} />
            <Route path="/actividades/:id" element={<ActividadPage />} />
            <Route path="/entregables/:id" element={<EntregablePage />} />
            
            {/* 404 */}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </main>
      </div>
    </AuthProvider>
  )
}

export default App
