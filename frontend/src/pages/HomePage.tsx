import { Link } from 'react-router-dom'

function HomePage() {
  return (
    <div className="page">
      <h1>Sistema de Gestión de Entregables</h1>
      <p>
        Plataforma para la gestión integral de entregables académicos.
        <br />
        Trabajo de Fin de Grado - Universidad de Sevilla
      </p>
      <div style={{ display: 'flex', gap: '1rem' }}>
        <Link to="/login" className="btn btn-primary">
          Iniciar Sesión
        </Link>
        <Link to="/dashboard" className="btn btn-secondary">
          Demo Dashboard
        </Link>
      </div>
    </div>
  )
}

export default HomePage
