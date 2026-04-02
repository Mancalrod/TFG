import React, { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authService } from '../../services';
import './LoginPage.css';

const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = useMemo(() => searchParams.get('token') ?? '', [searchParams]);

  const [nuevaContrasena, setNuevaContrasena] = useState('');
  const [confirmacionContrasena, setConfirmacionContrasena] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!token) {
      setError('El enlace no es valido o no incluye token.');
      return;
    }

    if (nuevaContrasena.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres.');
      return;
    }

    if (nuevaContrasena !== confirmacionContrasena) {
      setError('Las contraseñas no coinciden.');
      return;
    }

    setLoading(true);
    try {
      await authService.resetPassword({ token, contrasenaNueva: nuevaContrasena });
      setSuccess('Tu contraseña se ha actualizado correctamente. Ya puedes iniciar sesión.');
      setNuevaContrasena('');
      setConfirmacionContrasena('');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr?.response?.data?.message || 'No se pudo restablecer la contraseña.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div className="login-header">
          <h2>Restablecer contraseña</h2>
          <p>Introduce tu nueva contraseña.</p>
        </div>

        {error && <div className="error-message">{error}</div>}
        {success && <div className="success-message">{success}</div>}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="input-group">
            <label htmlFor="nueva-contrasena">Nueva contraseña</label>
            <input
              id="nueva-contrasena"
              type="password"
              placeholder="******"
              value={nuevaContrasena}
              onChange={(e) => setNuevaContrasena(e.target.value)}
            />
          </div>

          <div className="input-group">
            <label htmlFor="confirmar-contrasena">Confirmar contraseña</label>
            <input
              id="confirmar-contrasena"
              type="password"
              placeholder="******"
              value={confirmacionContrasena}
              onChange={(e) => setConfirmacionContrasena(e.target.value)}
            />
          </div>

          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Actualizando...' : 'Actualizar contraseña'}
          </button>
        </form>

        <div className="forgot-password" style={{ marginTop: '12px' }}>
          <Link to="/login">Volver a iniciar sesión</Link>
        </div>
      </div>
    </div>
  );
};

export default ResetPasswordPage;
