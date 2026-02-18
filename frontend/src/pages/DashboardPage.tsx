import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import api from '../services/api'

interface HealthStatus {
  status: string
  application: string
  version: string
}

type UserRole = 'ALUMNO' | 'PROFESOR' | 'ADMINISTRADOR';

function DashboardPage() {
  const [health, setHealth] = useState<HealthStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Rol simulado (TODO: obtener del contexto de autenticación)
  const [userRole] = useState<UserRole>('PROFESOR')
  const userName = 'Usuario Demo'

  useEffect(() => {
    const checkHealth = async () => {
      try {
        const response = await api.get<HealthStatus>('/api/health')
        setHealth(response.data)
      } catch (err) {
        setError('No se pudo conectar con el servidor')
        console.error(err)
      } finally {
        setLoading(false)
      }
    }
    checkHealth()
  }, [])

  return (
    <div>
      {/* Header */}
      <header style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center',
        marginBottom: '2rem',
        paddingBottom: '1rem',
        borderBottom: '1px solid #333'
      }}>
        <h1 style={{ fontSize: '1.5rem' }}>📚 Gestión de Entregables</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <span>{userName} ({userRole})</span>
          <Link to="/" className="btn btn-secondary" style={{ padding: '0.5rem 1rem' }}>
            Cerrar Sesión
          </Link>
        </div>
      </header>

      {/* Estado del servidor */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <h3>Estado del Sistema</h3>
        {loading ? (
          <p>Verificando conexión...</p>
        ) : error ? (
          <p style={{ color: '#ff6b6b' }}>❌ {error}</p>
        ) : health ? (
          <p style={{ color: '#4ade80' }}>
            ✅ {health.application} v{health.version} - {health.status}
          </p>
        ) : null}
      </div>

      {/* Contenido según rol */}
      {userRole === 'PROFESOR' && (
        <div>
          <h2 style={{ marginBottom: '1rem' }}>Panel del Profesor</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem' }}>
            <div className="card">
              <h4>📝 Mis Cursos</h4>
              <p style={{ color: '#888', margin: '0.5rem 0' }}>Gestiona tus asignaturas y grupos</p>
              <button className="btn btn-primary">Ver Cursos</button>
            </div>
            <div className="card">
              <h4>➕ Nueva Actividad</h4>
              <p style={{ color: '#888', margin: '0.5rem 0' }}>Crea una nueva tarea o entregable</p>
              <button className="btn btn-primary">Crear</button>
            </div>
            <div className="card">
              <h4>📊 Evaluaciones</h4>
              <p style={{ color: '#888', margin: '0.5rem 0' }}>Revisa y califica entregas</p>
              <button className="btn btn-primary">Evaluar</button>
            </div>
          </div>
        </div>
      )}

      {userRole === 'ALUMNO' && (
        <div>
          <h2 style={{ marginBottom: '1rem' }}>Panel del Alumno</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem' }}>
            <div className="card">
              <h4>📚 Mis Asignaturas</h4>
              <p style={{ color: '#888', margin: '0.5rem 0' }}>Ve tus cursos matriculados</p>
              <button className="btn btn-primary">Ver</button>
            </div>
            <div className="card">
              <h4>📤 Mis Entregas</h4>
              <p style={{ color: '#888', margin: '0.5rem 0' }}>Estado de tus entregables</p>
              <button className="btn btn-primary">Ver Entregas</button>
            </div>
            <div className="card">
              <h4>📈 Calificaciones</h4>
              <p style={{ color: '#888', margin: '0.5rem 0' }}>Consulta tu feedback</p>
              <button className="btn btn-primary">Ver Notas</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default DashboardPage
