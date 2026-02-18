import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    // TODO: Implementar autenticación real con OAuth2/Google
    if (email && password) {
      // Simulación de login exitoso
      navigate('/dashboard')
    } else {
      setError('Por favor, completa todos los campos')
    }
  }

  const handleGoogleLogin = () => {
    // TODO: Implementar OAuth2 con Google
    window.location.href = '/api/oauth2/authorization/google'
  }

  return (
    <div className="page">
      <div className="card" style={{ width: '100%', maxWidth: '400px' }}>
        <h2 style={{ marginBottom: '1.5rem' }}>Iniciar Sesión</h2>
        
        {error && (
          <p style={{ color: '#ff6b6b', marginBottom: '1rem' }}>{error}</p>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1rem' }}>
            <input
              type="email"
              placeholder="Correo electrónico"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              style={{
                width: '100%',
                padding: '0.75rem',
                borderRadius: '8px',
                border: '1px solid #333',
                background: 'transparent',
                color: 'inherit',
              }}
            />
          </div>
          <div style={{ marginBottom: '1.5rem' }}>
            <input
              type="password"
              placeholder="Contraseña"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{
                width: '100%',
                padding: '0.75rem',
                borderRadius: '8px',
                border: '1px solid #333',
                background: 'transparent',
                color: 'inherit',
              }}
            />
          </div>
          <button type="submit" className="btn btn-primary" style={{ width: '100%' }}>
            Entrar
          </button>
        </form>

        <div style={{ margin: '1.5rem 0', textAlign: 'center', color: '#666' }}>
          — o —
        </div>

        <button
          onClick={handleGoogleLogin}
          className="btn btn-secondary"
          style={{ width: '100%' }}
        >
          🔐 Continuar con Google
        </button>
      </div>
    </div>
  )
}

export default LoginPage
