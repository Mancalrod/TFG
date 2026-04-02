import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { notificacionService, usuarioService } from '../../services';
import { PreferenciaNotificacionDTO } from '../../types';
import './PerfilPage.css';

const MAX_FILE_BYTES = 2 * 1024 * 1024;
const MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);

type ApiErrorLike = {
  response?: {
    data?: {
      message?: string;
    };
  };
};

const getErrorMessage = (error: unknown, fallback: string): string => {
  const maybeError = error as ApiErrorLike;
  return maybeError.response?.data?.message ?? fallback;
};

const EyeOpenIcon: React.FC = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
    <path d="M12 5C7 5 2.73 8.11 1 12c1.73 3.89 6 7 11 7s9.27-3.11 11-7c-1.73-3.89-6-7-11-7zm0 11a4 4 0 1 1 0-8 4 4 0 0 1 0 8z" />
    <circle cx="12" cy="12" r="2" />
  </svg>
);

const EyeClosedIcon: React.FC = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
    <path d="M2 5.27 3.28 4 20 20.72 18.73 22l-3.05-3.05A11.79 11.79 0 0 1 12 19c-5 0-9.27-3.11-11-7a11.85 11.85 0 0 1 3.44-4.57L2 5.27z" />
    <path d="M8.53 10.8A3.5 3.5 0 0 0 13.2 15.47" />
    <path d="M9.88 5.08A12.21 12.21 0 0 1 12 5c5 0 9.27 3.11 11 7a11.86 11.86 0 0 1-3.29 4.4" />
  </svg>
);

const PerfilPage: React.FC = () => {
  const { usuario, actualizarFotoPerfil } = useAuth();
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [mostrarActual, setMostrarActual] = useState(false);
  const [mostrarNueva, setMostrarNueva] = useState(false);
  const [mostrarConfirmacion, setMostrarConfirmacion] = useState(false);
  const [canal, setCanal] = useState<PreferenciaNotificacionDTO['canal']>('APP');
  const [archivo, setArchivo] = useState<File | null>(null);
  const [loadingPassword, setLoadingPassword] = useState(false);
  const [loadingPhoto, setLoadingPhoto] = useState(false);
  const [loadingPref, setLoadingPref] = useState(false);
  const [msg, setMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    const cargarPreferencias = async () => {
      try {
        const data = await notificacionService.obtenerPreferencias();
        setCanal(data.canal);
      } catch {
        setCanal('APP');
      }
    };
    if (usuario) {
      cargarPreferencias();
    }
  }, [usuario]);

  const passwordError = useMemo(() => {
    if (!nueva) return '';
    if (nueva.length < 8) return 'La nueva contraseña debe tener al menos 8 caracteres.';
    if (!/[A-Z]/.test(nueva) || !/[a-z]/.test(nueva) || !/\d/.test(nueva) || !/[@$!%*?&_.#-]/.test(nueva)) {
      return 'Debe incluir mayúscula, minúscula, número y carácter especial.';
    }
    if (nueva !== confirmacion) return 'La confirmación de contraseña no coincide.';
    return '';
  }, [nueva, confirmacion]);

  if (!usuario) {
    return null;
  }

  const handlePasswordSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (passwordError) {
      setMsg({ type: 'error', text: passwordError });
      return;
    }

    setLoadingPassword(true);
    setMsg(null);
    try {
      await usuarioService.cambiarContrasena(usuario.id, {
        contrasenaActual: actual,
        contrasenaNueva: nueva,
      });
      setActual('');
      setNueva('');
      setConfirmacion('');
      setMsg({ type: 'success', text: 'Contraseña actualizada correctamente.' });
    } catch (error: unknown) {
      setMsg({ type: 'error', text: getErrorMessage(error, 'No se pudo cambiar la contraseña.') });
    } finally {
      setLoadingPassword(false);
    }
  };

  const handlePhotoSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!archivo) {
      setMsg({ type: 'error', text: 'Selecciona una imagen antes de subirla.' });
      return;
    }
    if (!MIME_TYPES.has(archivo.type) || archivo.size > MAX_FILE_BYTES) {
      setMsg({ type: 'error', text: 'Formato no permitido o imagen mayor a 2MB.' });
      return;
    }

    setLoadingPhoto(true);
    setMsg(null);
    try {
      const updated = await usuarioService.subirFotoPerfil(usuario.id, archivo);
      if (updated.fotoPerfilUrl) {
        actualizarFotoPerfil(updated.fotoPerfilUrl);
      }
      setArchivo(null);
      setMsg({ type: 'success', text: 'Foto de perfil actualizada.' });
    } catch (error: unknown) {
      setMsg({ type: 'error', text: getErrorMessage(error, 'No se pudo subir la foto.') });
    } finally {
      setLoadingPhoto(false);
    }
  };

  const handleSaveCanal = async () => {
    setLoadingPref(true);
    setMsg(null);
    try {
      await notificacionService.actualizarPreferencias({ canal });
      setMsg({ type: 'success', text: 'Preferencias de notificación guardadas.' });
    } catch {
      setMsg({ type: 'error', text: 'No se pudieron guardar las preferencias.' });
    } finally {
      setLoadingPref(false);
    }
  };

  return (
    <div className="perfil-page">
      <h1>Mi perfil</h1>
      {msg && <div className={`perfil-msg ${msg.type}`}>{msg.text}</div>}

      <section className="perfil-card">
        <h2>Cambiar contraseña</h2>
        <form onSubmit={handlePasswordSubmit}>
          <label htmlFor="contrasena-actual">
            <span>Contraseña actual</span>
          </label>
          <div className="password-input-wrapper">
            <input
              id="contrasena-actual"
              type={mostrarActual ? 'text' : 'password'}
              value={actual}
              onChange={(e) => setActual(e.target.value)}
              required
            />
            <button
              type="button"
              className="password-eye-btn"
              aria-label={mostrarActual ? 'Ocultar contraseña actual' : 'Mostrar contraseña actual'}
              onClick={() => setMostrarActual((prev) => !prev)}
            >
              {mostrarActual ? <EyeClosedIcon /> : <EyeOpenIcon />}
            </button>
          </div>
          <label htmlFor="contrasena-nueva">
            <span>Nueva contraseña</span>
          </label>
          <div className="password-input-wrapper">
            <input
              id="contrasena-nueva"
              type={mostrarNueva ? 'text' : 'password'}
              value={nueva}
              onChange={(e) => setNueva(e.target.value)}
              required
            />
            <button
              type="button"
              className="password-eye-btn"
              aria-label={mostrarNueva ? 'Ocultar nueva contraseña' : 'Mostrar nueva contraseña'}
              onClick={() => setMostrarNueva((prev) => !prev)}
            >
              {mostrarNueva ? <EyeClosedIcon /> : <EyeOpenIcon />}
            </button>
          </div>
          <label htmlFor="contrasena-confirmacion">
            <span>Confirmar nueva contraseña</span>
          </label>
          <div className="password-input-wrapper">
            <input
              id="contrasena-confirmacion"
              type={mostrarConfirmacion ? 'text' : 'password'}
              value={confirmacion}
              onChange={(e) => setConfirmacion(e.target.value)}
              required
            />
            <button
              type="button"
              className="password-eye-btn"
              aria-label={mostrarConfirmacion ? 'Ocultar confirmación de contraseña' : 'Mostrar confirmación de contraseña'}
              onClick={() => setMostrarConfirmacion((prev) => !prev)}
            >
              {mostrarConfirmacion ? <EyeClosedIcon /> : <EyeOpenIcon />}
            </button>
          </div>
          {passwordError && <p className="perfil-error">{passwordError}</p>}
          <button type="submit" disabled={loadingPassword || !!passwordError}>
            {loadingPassword ? 'Guardando...' : 'Guardar contraseña'}
          </button>
        </form>
      </section>

      <section className="perfil-card">
        <h2>Foto de perfil</h2>
        <form onSubmit={handlePhotoSubmit}>
          <label>
            <span>Seleccionar imagen</span>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif"
              onChange={(e) => setArchivo(e.target.files?.[0] ?? null)}
            />
          </label>
          <button type="submit" disabled={loadingPhoto || !archivo}>
            {loadingPhoto ? 'Subiendo...' : 'Subir foto'}
          </button>
        </form>
      </section>

      <section className="perfil-card">
        <h2>Notificaciones</h2>
        <p>Selecciona cómo quieres recibir avisos de nuevos entregables y recordatorios de cierre.</p>
        <div className="perfil-radio-group">
          <label>
            <input
              type="radio"
              checked={canal === 'APP'}
              onChange={() => setCanal('APP')}
            />
            <span>Solo en la aplicación</span>
          </label>
          <label>
            <input
              type="radio"
              checked={canal === 'EMAIL'}
              onChange={() => setCanal('EMAIL')}
            />
            <span>Solo por correo</span>
          </label>
          <label>
            <input
              type="radio"
              checked={canal === 'AMBOS'}
              onChange={() => setCanal('AMBOS')}
            />
            <span>Aplicación y correo</span>
          </label>
        </div>
        <button type="button" onClick={handleSaveCanal} disabled={loadingPref}>
          {loadingPref ? 'Guardando...' : 'Guardar preferencias'}
        </button>
      </section>
    </div>
  );
};

export default PerfilPage;
