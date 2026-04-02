import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { authService } from '../../services';
import './LoginPage.css';

const ForgotPasswordPage: React.FC = () => {
  const [correo, setCorreo] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!correo.trim()) {
      setError('El correo electrónico es obligatorio');
      return;
    }

    setLoading(true);
    try {
      await authService.forgotPassword({ correoElectronico: correo.trim() });
      setSuccess('Si el correo existe, te hemos enviado un enlace para recuperar tu contraseña.');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr?.response?.data?.message || 'No se pudo solicitar la recuperación de contraseña.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div className="login-header">
          <h2>Recuperar contraseña</h2>
          <p>Te enviaremos un enlace para restablecerla.</p>
        </div>

        {error && <div className="error-message">{error}</div>}
        {success && <div className="success-message">{success}</div>}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="input-group">
            <label htmlFor="correo-recuperacion">Correo electrónico</label>
            <input
              id="correo-recuperacion"
              type="email"
              placeholder="tu@correo.com"
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
            />
          </div>

          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Enviando...' : 'Enviar enlace'}
          </button>
        </form>

        <div className="forgot-password" style={{ marginTop: '12px' }}>
          <Link to="/login">Volver a iniciar sesión</Link>
        </div>
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
