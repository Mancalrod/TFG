import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './LoginPage.css';

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login, isAuthenticated } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const getApiErrorMessage = (err: unknown, fallback: string): string => {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    return axiosErr?.response?.data?.message || fallback;
  };

  // Si ya está autenticado, redirigir al dashboard
  React.useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard');
    }
  }, [isAuthenticated, navigate]);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');

    if (!email || !password) {
      setError('Por favor, completa todos los campos');
      return;
    }

    setIsLoading(true);
    try {
      await login({
        correoElectronico: email,
        contrasena: password,
      });
      navigate('/dashboard');
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosErr = err as { response?: { status?: number; data?: { message?: string } } };
        if (axiosErr.response?.status === 401) {
          setError('Correo electrónico o contraseña incorrectos');
        } else {
          setError(getApiErrorMessage(err, 'Error al iniciar sesión. Inténtalo de nuevo.'));
        }
      } else {
        setError('Error de conexión con el servidor');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div className="login-header">
          <h2>Iniciar Sesión</h2>
          <p>Bienvenido de nuevo, por favor ingresa tus datos.</p>
        </div>

        {/* Mensaje de error dinámico */}
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="input-group">
            <label htmlFor="email">Correo electrónico</label>
            <input
              type="email"
              id="email"
              placeholder="tu@correo.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="input-group">
            <label htmlFor="password">Contraseña</label>
            <input
              type="password"
              id="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div className="forgot-password">
            <Link to="/forgot-password">¿Olvidaste tu contraseña?</Link>
          </div>

          <button type="submit" className="btn-primary" disabled={isLoading}>
            {isLoading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;