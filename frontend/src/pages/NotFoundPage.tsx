import { Link } from 'react-router-dom'

function NotFoundPage() {
  return (
    <div className="page">
      <h1>404</h1>
      <p>La página que buscas no existe.</p>
      <Link to="/" className="btn btn-primary">
        Volver al Inicio
      </Link>
    </div>
  )
}

export default NotFoundPage
