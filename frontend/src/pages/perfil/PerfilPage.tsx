import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { notificacionService, usuarioService } from '../../services';
import { PreferenciaNotificacionDTO } from '../../types';
import './PerfilPage.css';

const MAX_FILE_BYTES = 2 * 1024 * 1024;
const MIME_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);
const OUTPUT_SIZE = 512;

type ApiErrorLike = {
  response?: {
    data?: {
      message?: string;
    };
  };
};

type ImageSize = {
  width: number;
  height: number;
};

type CropState = {
  x: number;
  y: number;
  zoom: number;
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

const getCropMetrics = (imageSize: ImageSize, zoom: number) => {
  const size = Math.min(imageSize.width, imageSize.height) / zoom;
  return {
    size,
    maxX: Math.max(0, imageSize.width - size),
    maxY: Math.max(0, imageSize.height - size),
  };
};

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max);

const buildPhotoFilename = (name: string) => {
  const baseName = name.replace(/\.[^.]+$/, '') || 'foto-perfil';
  return `${baseName}.jpg`;
};

const createCroppedFile = (
  imageUrl: string,
  imageSize: ImageSize,
  crop: CropState,
  originalName: string,
): Promise<File> => new Promise((resolve, reject) => {
  const image = new Image();
  image.onload = () => {
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');
    if (!context) {
      reject(new Error('No se pudo preparar la imagen.'));
      return;
    }

    const cropMetrics = getCropMetrics(imageSize, crop.zoom);
    const sourceX = clamp(crop.x, 0, cropMetrics.maxX);
    const sourceY = clamp(crop.y, 0, cropMetrics.maxY);

    canvas.width = OUTPUT_SIZE;
    canvas.height = OUTPUT_SIZE;
    context.drawImage(
      image,
      sourceX,
      sourceY,
      cropMetrics.size,
      cropMetrics.size,
      0,
      0,
      OUTPUT_SIZE,
      OUTPUT_SIZE,
    );

    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('No se pudo recortar la imagen.'));
        return;
      }
      resolve(new File([blob], buildPhotoFilename(originalName), { type: 'image/jpeg' }));
    }, 'image/jpeg', 0.9);
  };
  image.onerror = () => reject(new Error('No se pudo cargar la imagen.'));
  image.src = imageUrl;
});

const PerfilPage: React.FC = () => {
  const { usuario, actualizarFotoPerfil } = useAuth();
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const cropCanvasRef = useRef<HTMLCanvasElement | null>(null);
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [mostrarActual, setMostrarActual] = useState(false);
  const [mostrarNueva, setMostrarNueva] = useState(false);
  const [mostrarConfirmacion, setMostrarConfirmacion] = useState(false);
  const [canal, setCanal] = useState<PreferenciaNotificacionDTO['canal']>('APP');
  const [selectedImageUrl, setSelectedImageUrl] = useState<string | null>(null);
  const [selectedImageName, setSelectedImageName] = useState('foto-perfil.jpg');
  const [imageSize, setImageSize] = useState<ImageSize | null>(null);
  const [crop, setCrop] = useState<CropState>({ x: 0, y: 0, zoom: 1 });
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

  useEffect(() => {
    if (!selectedImageUrl) return undefined;

    const image = new Image();
    image.onload = () => {
      const size = { width: image.naturalWidth, height: image.naturalHeight };
      const cropSize = Math.min(size.width, size.height);
      setImageSize(size);
      setCrop({
        x: (size.width - cropSize) / 2,
        y: (size.height - cropSize) / 2,
        zoom: 1,
      });
    };
    image.onerror = () => {
      setMsg({ type: 'error', text: 'No se pudo cargar la imagen seleccionada.' });
      setSelectedImageUrl(null);
    };
    image.src = selectedImageUrl;

    return () => {
      URL.revokeObjectURL(selectedImageUrl);
    };
  }, [selectedImageUrl]);

  useEffect(() => {
    if (!imageSize) return;

    const cropMetrics = getCropMetrics(imageSize, crop.zoom);
    setCrop(prev => ({
      ...prev,
      x: clamp(prev.x, 0, cropMetrics.maxX),
      y: clamp(prev.y, 0, cropMetrics.maxY),
    }));
  }, [crop.zoom, imageSize]);

  useEffect(() => {
    if (!selectedImageUrl || !imageSize || !cropCanvasRef.current) return;

    const canvas = cropCanvasRef.current;
    const context = canvas.getContext('2d');
    if (!context) return;

    const image = new Image();
    image.onload = () => {
      const cropMetrics = getCropMetrics(imageSize, crop.zoom);
      const sourceX = clamp(crop.x, 0, cropMetrics.maxX);
      const sourceY = clamp(crop.y, 0, cropMetrics.maxY);

      context.clearRect(0, 0, canvas.width, canvas.height);
      context.drawImage(
        image,
        sourceX,
        sourceY,
        cropMetrics.size,
        cropMetrics.size,
        0,
        0,
        canvas.width,
        canvas.height,
      );
    };
    image.src = selectedImageUrl;
  }, [crop, imageSize, selectedImageUrl]);

  const cropMetrics = useMemo(
    () => imageSize ? getCropMetrics(imageSize, crop.zoom) : null,
    [crop.zoom, imageSize],
  );

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

  const fotoPerfilUrl = usuario.fotoPerfilUrl;
  const inicialUsuario = usuario.nombre.charAt(0).toUpperCase();

  const resetPhotoInput = () => {
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const closeCropper = () => {
    setSelectedImageUrl(null);
    setImageSize(null);
    setSelectedImageName('foto-perfil.jpg');
    setCrop({ x: 0, y: 0, zoom: 1 });
    resetPhotoInput();
  };

  const handlePhotoSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!MIME_TYPES.has(file.type) || file.size > MAX_FILE_BYTES) {
      setMsg({ type: 'error', text: 'Formato no permitido o imagen mayor a 2MB.' });
      resetPhotoInput();
      return;
    }

    setMsg(null);
    setImageSize(null);
    setSelectedImageName(file.name);
    setSelectedImageUrl(URL.createObjectURL(file));
  };

  const handleSavePhoto = async () => {
    if (!selectedImageUrl || !imageSize) {
      setMsg({ type: 'error', text: 'Selecciona una imagen antes de guardarla.' });
      return;
    }

    setLoadingPhoto(true);
    setMsg(null);
    try {
      const croppedFile = await createCroppedFile(selectedImageUrl, imageSize, crop, selectedImageName);
      const updated = await usuarioService.subirFotoPerfil(usuario.id, croppedFile);
      if (updated.fotoPerfilUrl) {
        actualizarFotoPerfil(updated.fotoPerfilUrl);
      }
      closeCropper();
      setMsg({ type: 'success', text: 'Foto de perfil actualizada.' });
    } catch (error: unknown) {
      setMsg({ type: 'error', text: getErrorMessage(error, 'No se pudo subir la foto.') });
    } finally {
      setLoadingPhoto(false);
    }
  };

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
    <div className="perfil-page perfil-photo-page">
      <section className="perfil-photo-panel">
        <div className="perfil-photo-heading">
          <p className="perfil-kicker">Mi perfil</p>
          <h1>{usuario.nombre}</h1>
        </div>

        <div className="perfil-avatar-xl" aria-label="Foto de perfil">
          {fotoPerfilUrl ? (
            <img src={fotoPerfilUrl} alt="Foto de perfil" />
          ) : (
            <span>{inicialUsuario}</span>
          )}
        </div>

        <input
          ref={fileInputRef}
          id="foto-perfil-input"
          className="perfil-file-input"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          onChange={handlePhotoSelected}
          aria-label="Seleccionar imagen"
        />
        <button
          type="button"
          className="perfil-primary-button"
          onClick={() => fileInputRef.current?.click()}
          disabled={loadingPhoto}
        >
          Cambiar foto
        </button>
      </section>

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
          <button type="submit" className="perfil-primary-button" disabled={loadingPassword || !!passwordError}>
            {loadingPassword ? 'Guardando...' : 'Guardar contraseña'}
          </button>
        </form>
      </section>

      <section className="perfil-card">
        <h2>Preferencias</h2>
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
        <button type="button" className="perfil-primary-button" onClick={handleSaveCanal} disabled={loadingPref}>
          {loadingPref ? 'Guardando...' : 'Guardar preferencias'}
        </button>
      </section>

      {selectedImageUrl && (
        <div className="perfil-crop-overlay" onClick={closeCropper}>
          <div
            className="perfil-crop-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="perfil-crop-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="perfil-crop-header">
              <h2 id="perfil-crop-title">Ajustar foto</h2>
              <button
                type="button"
                className="perfil-icon-button"
                onClick={closeCropper}
                aria-label="Cerrar"
                disabled={loadingPhoto}
              >
                X
              </button>
            </div>

            <canvas
              ref={cropCanvasRef}
              className="perfil-crop-canvas"
              width={OUTPUT_SIZE}
              height={OUTPUT_SIZE}
            />

            <div className="perfil-crop-controls">
              <label>
                <span>Zoom</span>
                <input
                  type="range"
                  min="1"
                  max="3"
                  step="0.01"
                  value={crop.zoom}
                  onChange={(e) => setCrop(prev => ({ ...prev, zoom: Number(e.target.value) }))}
                  disabled={!imageSize || loadingPhoto}
                />
              </label>
              <label>
                <span>Horizontal</span>
                <input
                  type="range"
                  min="0"
                  max={cropMetrics?.maxX ?? 0}
                  step="1"
                  value={crop.x}
                  onChange={(e) => setCrop(prev => ({ ...prev, x: Number(e.target.value) }))}
                  disabled={!imageSize || !cropMetrics?.maxX || loadingPhoto}
                />
              </label>
              <label>
                <span>Vertical</span>
                <input
                  type="range"
                  min="0"
                  max={cropMetrics?.maxY ?? 0}
                  step="1"
                  value={crop.y}
                  onChange={(e) => setCrop(prev => ({ ...prev, y: Number(e.target.value) }))}
                  disabled={!imageSize || !cropMetrics?.maxY || loadingPhoto}
                />
              </label>
            </div>

            <div className="perfil-crop-actions">
              <button
                type="button"
                className="perfil-secondary-button"
                onClick={closeCropper}
                disabled={loadingPhoto}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="perfil-primary-button"
                onClick={handleSavePhoto}
                disabled={loadingPhoto || !imageSize}
              >
                {loadingPhoto ? 'Guardando...' : 'Guardar foto'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PerfilPage;
