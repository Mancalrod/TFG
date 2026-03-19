import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { EntregaDTO, MaterialDTO, CalificacionDTO } from '../../types';
import { entregaService } from '../../services/entregaService';
import { useAuth } from '../../context/AuthContext';
import FilePreviewModal from '../../components/FilePreviewModal';
import './EntregaDetallePage.css';

const EntregaDetallePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { esProfesor, usuario } = useAuth();

  const [entrega, setEntrega] = useState<EntregaDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [previewMaterial, setPreviewMaterial] = useState<MaterialDTO | null>(null);
  const [descargando, setDescargando] = useState<number | null>(null);
  const [errorDescarga, setErrorDescarga] = useState<string | null>(null);

  // Estados para calificación
  const [mostrarFormCalificar, setMostrarFormCalificar] = useState(false);
  const [nota, setNota] = useState<string>('');
  const [comentarioProfesor, setComentarioProfesor] = useState('');
  const [calificando, setCalificando] = useState(false);
  const [errorCalificacion, setErrorCalificacion] = useState<string | null>(null);

  const cargarEntrega = useCallback(async (entregaId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await entregaService.obtener(entregaId);
      setEntrega(data);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr?.response?.data?.message || 'No se pudo cargar la entrega');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (id) cargarEntrega(parseInt(id));
  }, [id, cargarEntrega]);

  const handleDescargar = async (material: MaterialDTO) => {
    setDescargando(material.id);
    setErrorDescarga(null);
    try {
      const blob = await entregaService.descargarArchivo(material.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = material.nombre;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setErrorDescarga(axiosErr?.response?.data?.message || `Error al descargar "${material.nombre}"`);
    } finally {
      setDescargando(null);
    }
  };

  const handleDescargarTodos = async () => {
    if (!entrega) return;
    for (const archivo of entrega.archivos) {
      await handleDescargar(archivo);
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('es-ES', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getFileIcon = (nombre: string) => {
    const lower = nombre.toLowerCase();
    if (lower.endsWith('.pdf')) return '📕';
    if (/\.(png|jpe?g|gif|svg|webp)$/.test(lower)) return '🖼️';
    if (/\.(zip|rar|7z|tar|gz)$/.test(lower)) return '📦';
    if (/\.(doc|docx)$/.test(lower)) return '📘';
    if (/\.(xls|xlsx)$/.test(lower)) return '📊';
    if (/\.(ppt|pptx)$/.test(lower)) return '📙';
    if (/\.(txt|md|log|csv)$/.test(lower)) return '📝';
    if (/\.(js|ts|java|py|c|cpp|h|html|css|json|xml)$/.test(lower)) return '💻';
    return '📄';
  };

  const esPrevisualizeable = (nombre: string) => {
    return /\.(pdf|png|jpe?g|gif|svg|txt|md|json|xml|html?|css|js|ts|java|py|c|cpp|h|log|csv|zip|rar|7z)$/i.test(nombre);
  };

  // Handler para calificar
  const handleCalificar = async () => {
    if (!entrega || !usuario?.id) return;

    if (!nota || isNaN(parseFloat(nota))) {
      setErrorCalificacion('La nota es obligatoria');
      return;
    }

    const notaNum = parseFloat(nota);
    if (notaNum < 0) {
      setErrorCalificacion('La nota no puede ser negativa');
      return;
    }

    setCalificando(true);
    setErrorCalificacion(null);

    try {
      const calificacionDTO: CalificacionDTO = {
        calificacion: notaNum,
        comentario: comentarioProfesor.trim() || undefined
      };
      await entregaService.calificar(entrega.id, usuario.id, calificacionDTO);
      // Recargar entrega para ver cambios
      await cargarEntrega(entrega.id);
      setMostrarFormCalificar(false);
      setNota('');
      setComentarioProfesor('');
    } catch (err: unknown) {
      console.error('Error al calificar:', err);
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setErrorCalificacion(axiosErr?.response?.data?.message || 'Error al calificar la entrega');
    } finally {
      setCalificando(false);
    }
  };

  if (loading) {
    return (
      <div className="edp-page">
        <div className="edp-loading">
          <div className="edp-spinner" />
          <p>Cargando entrega...</p>
        </div>
      </div>
    );
  }

  if (error || !entrega) {
    return (
      <div className="edp-page">
        <div className="edp-error">
          <p>{error || 'Entrega no encontrada'}</p>
          <button onClick={() => navigate(-1)} className="edp-btn-back">← Volver</button>
        </div>
      </div>
    );
  }

  return (
    <div className="edp-page">
      <div className="edp-header">
        <button onClick={() => navigate(-1)} className="edp-btn-back">← Volver</button>
        <div className="edp-header-info">
          <h1>{entrega.nombre}</h1>
          <div className="edp-header-meta">
            <span className="edp-entregable">{entrega.entregableTitulo}</span>
            <span className={`edp-badge ${entrega.estado.toLowerCase()}`}>{entrega.estado}</span>
            <span className="edp-version">v{entrega.version}</span>
          </div>
        </div>
      </div>

      <div className="edp-content">
        {/* Info panel */}
        <div className="edp-info-panel">
          <div className="edp-info-grid">
            <div className="edp-info-item">
              <label>Estudiante</label>
              <p>{entrega.estudianteNombre}</p>
            </div>
            <div className="edp-info-item">
              <label>Fecha de entrega</label>
              <p className={entrega.fueATiempo ? 'edp-text-ok' : 'edp-text-warn'}>
                {formatDate(entrega.fechaEntrega)}
                {entrega.fueATiempo ? ' ✓ A tiempo' : ' ⚠ Tardía'}
              </p>
            </div>
            {entrega.calificacion !== undefined && entrega.calificacion !== null && (
              <div className="edp-info-item">
                <label>Calificación</label>
                <p className="edp-calificacion">{entrega.calificacion} pts</p>
              </div>
            )}
            {entrega.fechaCalificacion && (
              <div className="edp-info-item">
                <label>Fecha calificación</label>
                <p>{formatDate(entrega.fechaCalificacion)}</p>
              </div>
            )}
          </div>
        </div>

        {/* Files panel */}
        <div className="edp-files-panel">
          <div className="edp-files-header">
            <h2>Archivos ({entrega.archivos.length})</h2>
            {entrega.archivos.length > 1 && (
              <button className="edp-btn-download-all" onClick={handleDescargarTodos}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="7 10 12 15 17 10" />
                  <line x1="12" y1="15" x2="12" y2="3" />
                </svg>
                Descargar todos
              </button>
            )}
          </div>

          {entrega.archivos.length === 0 ? (
            <p className="edp-no-files">No hay archivos adjuntos</p>
          ) : (
            <div className="edp-file-list">
              {entrega.archivos.map(archivo => (
                <div key={archivo.id} className="edp-file-item">
                  <div className="edp-file-icon">{getFileIcon(archivo.nombre)}</div>
                  <div className="edp-file-info">
                    <span className="edp-file-name">{archivo.nombre}</span>
                    <span className="edp-file-size">{formatFileSize(archivo.tamanoBytes)}</span>
                  </div>
                  <div className="edp-file-actions">
                    {esPrevisualizeable(archivo.nombre) && (
                      <button
                        className="edp-btn-preview"
                        onClick={() => setPreviewMaterial(archivo)}
                        title="Previsualizar"
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                          <circle cx="12" cy="12" r="3" />
                        </svg>
                        Ver
                      </button>
                    )}
                    <button
                      className="edp-btn-download"
                      onClick={() => handleDescargar(archivo)}
                      disabled={descargando === archivo.id}
                      title="Descargar"
                    >
                      {descargando === archivo.id ? (
                        <div className="edp-mini-spinner" />
                      ) : (
                        <>
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                            <polyline points="7 10 12 15 17 10" />
                            <line x1="12" y1="15" x2="12" y2="3" />
                          </svg>
                          Descargar
                        </>
                      )}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Comentario del alumno (si existe) */}
        {entrega.comentarioAlumno && (
          <div className="edp-comentario-alumno">
            <h2>Comentario del Alumno</h2>
            <p className="edp-comentario-texto">{entrega.comentarioAlumno}</p>
          </div>
        )}

        {/* Feedback panel */}
        {entrega.feedbacks && entrega.feedbacks.length > 0 && (
          <div className="edp-feedback-panel">
            <h2>Feedback del Profesor</h2>
            <div className="edp-feedback-list">
              {entrega.feedbacks.map(fb => (
                <div key={fb.id} className="edp-feedback-item">
                  <div className="edp-feedback-header">
                    <span className="edp-feedback-autor">{fb.profesorNombre}</span>
                    <span className="edp-feedback-fecha">{formatDate(fb.fechaCreacion)}</span>
                  </div>
                  <p className="edp-feedback-texto">{fb.comentario}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Panel de calificación (para profesores) */}
        {esProfesor && entrega.estado === 'ENTREGADO' && (
          <div className="edp-calificar-panel">
            {!mostrarFormCalificar ? (
              <button
                className="edp-btn-calificar"
                onClick={() => setMostrarFormCalificar(true)}
              >
                Calificar esta entrega
              </button>
            ) : (
              <div className="edp-calificar-form">
                <h3>Calificar Entrega</h3>
                {errorCalificacion && (
                  <div className="edp-calificar-error">{errorCalificacion}</div>
                )}
                <div className="edp-form-group">
                  <label htmlFor="nota">Nota *</label>
                  <input
                    id="nota"
                    type="number"
                    step="0.1"
                    min="0"
                    value={nota}
                    onChange={e => setNota(e.target.value)}
                    disabled={calificando}
                    placeholder="Ej: 8.5"
                  />
                </div>
                <div className="edp-form-group">
                  <label htmlFor="comentarioProfesor">Comentario / Feedback (opcional)</label>
                  <textarea
                    id="comentarioProfesor"
                    value={comentarioProfesor}
                    onChange={e => setComentarioProfesor(e.target.value)}
                    rows={4}
                    maxLength={5000}
                    placeholder="Escribe un comentario para el alumno..."
                    disabled={calificando}
                  />
                  <span className="edp-char-count">{comentarioProfesor.length}/5000</span>
                </div>
                <div className="edp-form-actions">
                  <button
                    className="btn-secondary"
                    onClick={() => {
                      setMostrarFormCalificar(false);
                      setErrorCalificacion(null);
                    }}
                    disabled={calificando}
                  >
                    Cancelar
                  </button>
                  <button
                    className="btn-primary"
                    onClick={handleCalificar}
                    disabled={calificando || !nota}
                  >
                    {calificando ? 'Guardando...' : 'Guardar Calificación'}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Ya calificada - mostrar info */}
        {esProfesor && (entrega.estado === 'CALIFICADO' || entrega.estado === 'PUBLICADO') && (
          <div className="edp-calificada-info">
            ✓ Esta entrega ya ha sido calificada con <strong>{entrega.calificacion} pts</strong>
          </div>
        )}
      </div>

      {previewMaterial && (
        <FilePreviewModal
          material={previewMaterial}
          onClose={() => setPreviewMaterial(null)}
        />
      )}
    </div>
  );
};

export default EntregaDetallePage;
