import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { EntregableDTO, NodoEstructuraZip } from '../../types';
import { entregableService, entregaService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import './RealizarEntregaPage.css';

// ── Constantes ──
const TEMP_STORAGE_PREFIX = 'entrega_temp_';

interface ArchivoTemporal {
  id: string;
  nombre: string;
  tipo: string;
  tamano: number;
  dataUrl: string; // base64 para persistir en localStorage
  fechaAgregado: string;
}

interface BorradorMeta {
  entregableId: number;
  nombre: string;
  archivos: ArchivoTemporal[];
  ultimaModificacion: string;
}

const RealizarEntregaPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { usuario, esEstudiante } = useAuth();

  const [entregable, setEntregable] = useState<EntregableDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorEnvio, setErrorEnvio] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Formulario
  const [nombre, setNombre] = useState('');
  const [archivos, setArchivos] = useState<ArchivoTemporal[]>([]);
  const [archivoPreview, setArchivoPreview] = useState<ArchivoTemporal | null>(null);

  // Drag & Drop
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Indicador de borrador cargado
  const [borradorCargado, setBorradorCargado] = useState(false);

  const storageKey = id ? `${TEMP_STORAGE_PREFIX}${id}` : '';

  // ── Cargar entregable ──
  useEffect(() => {
    if (id) {
      cargarEntregable(parseInt(id));
    }
  }, [id]);

  // ── Redirigir si no es estudiante ──
  useEffect(() => {
    if (!loading && !esEstudiante) {
      navigate(-1);
    }
  }, [loading, esEstudiante, navigate]);

  // ── Cargar borrador desde localStorage ──
  useEffect(() => {
    if (!storageKey) return;
    try {
      const raw = localStorage.getItem(storageKey);
      if (raw) {
        const borrador: BorradorMeta = JSON.parse(raw);
        setNombre(borrador.nombre);
        setArchivos(borrador.archivos);
        setBorradorCargado(true);
      }
    } catch {
      // localStorage corrupto, ignorar
    }
  }, [storageKey]);

  // ── Guardar borrador en localStorage cuando cambie ──
  const guardarBorrador = useCallback(() => {
    if (!storageKey || !id) return;
    const borrador: BorradorMeta = {
      entregableId: parseInt(id),
      nombre,
      archivos,
      ultimaModificacion: new Date().toISOString(),
    };
    try {
      localStorage.setItem(storageKey, JSON.stringify(borrador));
    } catch {
      // localStorage lleno, intentar limpiar
      console.warn('No se pudo guardar el borrador en localStorage');
    }
  }, [storageKey, id, nombre, archivos]);

  useEffect(() => {
    // Auto-guardar cuando cambie nombre o archivos (debounce sencillo)
    if (!storageKey) return;
    const timer = setTimeout(() => {
      guardarBorrador();
    }, 500);
    return () => clearTimeout(timer);
  }, [nombre, archivos, guardarBorrador, storageKey]);

  const limpiarBorrador = () => {
    if (storageKey) {
      localStorage.removeItem(storageKey);
    }
  };

  const cargarEntregable = async (entregableId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await entregableService.obtener(entregableId);
      setEntregable(data);
    } catch (err) {
      console.error('Error al cargar entregable:', err);
      setError('No se pudo cargar el entregable');
    } finally {
      setLoading(false);
    }
  };

  // ── Manejo de archivos ──
  const fileToTemporal = (file: File): Promise<ArchivoTemporal> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        resolve({
          id: `${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
          nombre: file.name,
          tipo: file.type,
          tamano: file.size,
          dataUrl: reader.result as string,
          fechaAgregado: new Date().toISOString(),
        });
      };
      reader.onerror = () => reject(new Error('Error al leer el archivo'));
      reader.readAsDataURL(file);
    });
  };

  const validarArchivo = (file: File): string | null => {
    if (!entregable) return null;

    // Validar tamaño
    if (entregable.tamanoMaximoBytes && file.size > entregable.tamanoMaximoBytes) {
      const maxMB = (entregable.tamanoMaximoBytes / (1024 * 1024)).toFixed(1);
      return `El archivo "${file.name}" excede el tamaño máximo de ${maxMB} MB`;
    }

    // Validar tipo
    if (entregable.tipoArchivoEsperado) {
      const ext = file.name.split('.').pop()?.toUpperCase();
      const tipoEsperado = entregable.tipoArchivoEsperado.toUpperCase();
      if (ext !== tipoEsperado) {
        return `Se espera un archivo de tipo ${tipoEsperado}, pero se seleccionó ".${ext}"`;
      }
    }

    return null;
  };

  const agregarArchivos = async (files: FileList | File[]) => {
    const nuevosArchivos: ArchivoTemporal[] = [];
    const errores: string[] = [];

    for (const file of Array.from(files)) {
      const errorValidacion = validarArchivo(file);
      if (errorValidacion) {
        errores.push(errorValidacion);
        continue;
      }
      try {
        const temp = await fileToTemporal(file);
        nuevosArchivos.push(temp);
      } catch {
        errores.push(`Error al procesar "${file.name}"`);
      }
    }

    if (errores.length > 0) {
      setErrorEnvio(errores.join('\n'));
    }

    if (nuevosArchivos.length > 0) {
      setArchivos(prev => [...prev, ...nuevosArchivos]);
    }
  };

  const eliminarArchivo = (archivoId: string) => {
    setArchivos(prev => prev.filter(a => a.id !== archivoId));
    if (archivoPreview?.id === archivoId) {
      setArchivoPreview(null);
    }
  };

  // ── Drag & Drop handlers ──
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(false);
    if (e.dataTransfer.files.length > 0) {
      agregarArchivos(e.dataTransfer.files);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      agregarArchivos(e.target.files);
      // Resetear el input para poder seleccionar el mismo archivo otra vez
      e.target.value = '';
    }
  };

  // ── Preview ──
  const puedePrevisualizar = (archivo: ArchivoTemporal): boolean => {
    const tipo = archivo.tipo.toLowerCase();
    const ext = archivo.nombre.split('.').pop()?.toLowerCase() || '';
    return (
      tipo === 'application/pdf' ||
      tipo.startsWith('image/') ||
      ext === 'pdf' ||
      ext === 'doc' ||
      ext === 'docx' ||
      tipo === 'application/msword' ||
      tipo === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    );
  };

  const getIconoArchivo = (archivo: ArchivoTemporal): string => {
    const ext = archivo.nombre.split('.').pop()?.toLowerCase() || '';
    const tipo = archivo.tipo.toLowerCase();

    if (tipo === 'application/pdf' || ext === 'pdf') return '📄';
    if (tipo.startsWith('image/')) return '🖼️';
    if (ext === 'doc' || ext === 'docx' || tipo.includes('word')) return '📝';
    if (ext === 'zip' || ext === 'rar' || ext === '7z') return '📦';
    if (ext === 'txt') return '📃';
    if (ext === 'xls' || ext === 'xlsx') return '📊';
    if (ext === 'ppt' || ext === 'pptx') return '📽️';
    return '📎';
  };

  const renderPreview = (archivo: ArchivoTemporal) => {
    const tipo = archivo.tipo.toLowerCase();
    const ext = archivo.nombre.split('.').pop()?.toLowerCase() || '';

    // PDF: embeber directamente
    if (tipo === 'application/pdf' || ext === 'pdf') {
      return (
        <div className="re-preview-container">
          <div className="re-preview-header">
            <h3>{archivo.nombre}</h3>
            <button
              className="re-preview-close"
              onClick={() => setArchivoPreview(null)}
            >
              ✕
            </button>
          </div>
          <iframe
            src={archivo.dataUrl}
            className="re-preview-iframe"
            title={`Vista previa de ${archivo.nombre}`}
          />
        </div>
      );
    }

    // Imágenes
    if (tipo.startsWith('image/')) {
      return (
        <div className="re-preview-container">
          <div className="re-preview-header">
            <h3>{archivo.nombre}</h3>
            <button
              className="re-preview-close"
              onClick={() => setArchivoPreview(null)}
            >
              ✕
            </button>
          </div>
          <div className="re-preview-image-wrap">
            <img
              src={archivo.dataUrl}
              alt={archivo.nombre}
              className="re-preview-image"
            />
          </div>
        </div>
      );
    }

    // DOC/DOCX: Usar Office Online Viewer via Google Docs Viewer (requiere URL pública)
    // Como estamos con dataUrl local, mostramos mensaje descargable
    if (ext === 'doc' || ext === 'docx' || tipo.includes('word')) {
      return (
        <div className="re-preview-container">
          <div className="re-preview-header">
            <h3>{archivo.nombre}</h3>
            <button
              className="re-preview-close"
              onClick={() => setArchivoPreview(null)}
            >
              ✕
            </button>
          </div>
          <div className="re-preview-doc">
            <div className="re-preview-doc-icon">📝</div>
            <p className="re-preview-doc-name">{archivo.nombre}</p>
            <p className="re-preview-doc-info">
              Los archivos Word se pueden previsualizar una vez subidos a OneDrive.
            </p>
            <a
              href={archivo.dataUrl}
              download={archivo.nombre}
              className="btn-primary re-preview-download-btn"
            >
              Descargar para ver
            </a>
          </div>
        </div>
      );
    }

    return null;
  };

  // ── Enviar entrega ──
  const handleEnviar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !usuario?.id) return;

    if (archivos.length === 0) {
      setErrorEnvio('Debes adjuntar al menos un archivo');
      return;
    }

    if (!nombre.trim()) {
      setErrorEnvio('Debes escribir un nombre para la entrega');
      return;
    }

    setEnviando(true);
    setErrorEnvio(null);

    try {
      // Convertir dataUrls a File objects para el envío
      const fileObjects: File[] = await Promise.all(
        archivos.map(async (archivo) => {
          const response = await fetch(archivo.dataUrl);
          const blob = await response.blob();
          return new File([blob], archivo.nombre, { type: archivo.tipo });
        })
      );

      await entregaService.realizar(
        parseInt(id),
        usuario.id,
        nombre.trim(),
        fileObjects
      );

      // Limpiar borrador después de envío exitoso
      limpiarBorrador();
      setSuccessMsg('¡Entrega realizada con éxito!');

      // Redirigir tras 1.5 segundos
      setTimeout(() => {
        navigate(`/entregables/${id}`);
      }, 1500);
    } catch (err: unknown) {
      console.error('Error al enviar:', err);
      const axiosErr = err as { response?: { data?: { message?: string; errors?: Record<string, string> } } };
      const msg =
        axiosErr?.response?.data?.message ||
        (axiosErr?.response?.data?.errors
          ? Object.values(axiosErr.response.data.errors).join(', ')
          : 'Error al realizar la entrega');
      setErrorEnvio(typeof msg === 'string' ? msg : 'Error al realizar la entrega');
    } finally {
      setEnviando(false);
    }
  };

  const handleDescartarBorrador = () => {
    setNombre('');
    setArchivos([]);
    setArchivoPreview(null);
    limpiarBorrador();
    setBorradorCargado(false);
  };

  // ── Utilidades de formato ──
  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return 'Sin fecha';
    return new Date(dateStr).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // ── Render ──
  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Cargando entregable...</p>
      </div>
    );
  }

  if (error || !entregable) {
    return (
      <div className="error-container">
        <p className="error-message">{error || 'Entregable no encontrado'}</p>
        <button onClick={() => navigate(-1)} className="btn-secondary">
          Volver
        </button>
      </div>
    );
  }

  if (!entregable.enPlazo) {
    return (
      <div className="error-container">
        <p className="error-message">El plazo de entrega ha finalizado</p>
        <button onClick={() => navigate(`/entregables/${id}`)} className="btn-secondary">
          Volver al entregable
        </button>
      </div>
    );
  }

  return (
    <div className="realizar-entrega-page">
      {/* Header */}
      <div className="re-header">
        <button onClick={() => navigate(`/entregables/${id}`)} className="btn-back">
          &larr; Volver al entregable
        </button>
        <div className="re-header-info">
          <h1>Realizar Entrega</h1>
          <span className="re-subtitle">{entregable.titulo}</span>
          <span className="re-actividad">{entregable.actividadTitulo}</span>
        </div>
      </div>

      {/* Info del entregable */}
      <div className="re-info-bar">
        <div className="re-info-item">
          <span className="re-info-label">Fecha límite</span>
          <span className="re-info-value">{formatDate(entregable.fechaLimite)}</span>
        </div>
        {entregable.notaMaxima && (
          <div className="re-info-item">
            <span className="re-info-label">Nota máxima</span>
            <span className="re-info-value">{entregable.notaMaxima}</span>
          </div>
        )}
        {entregable.tipoArchivoEsperado && (
          <div className="re-info-item">
            <span className="re-info-label">Tipo esperado</span>
            <span className="re-info-value">{entregable.tipoArchivoEsperado}</span>
          </div>
        )}
        {entregable.tamanoMaximoBytes && (
          <div className="re-info-item">
            <span className="re-info-label">Tamaño máx.</span>
            <span className="re-info-value">{formatFileSize(entregable.tamanoMaximoBytes)}</span>
          </div>
        )}
        {entregable.permiteReenvio && (
          <div className="re-info-item">
            <span className="re-info-tag reenvio">Permite reenvío</span>
          </div>
        )}
      </div>

      {/* Estructura esperada del ZIP */}
      {(entregable.estructuraZip || entregable.nombreZipEsperado) && (() => {
        let nodos: NodoEstructuraZip[] = [];
        if (entregable.estructuraZip) {
          try { nodos = JSON.parse(entregable.estructuraZip); } catch { nodos = []; }
        }
        if (nodos.length === 0 && !entregable.nombreZipEsperado) return null;
        return (
          <div className="re-zip-structure">
            <div className="re-zip-structure-header">
              <span>📦 Estructura esperada del ZIP</span>
              <span className={`re-zip-mode-badge ${entregable.validacionZipEstricta ? 'estricta' : 'minima'}`}>
                {entregable.validacionZipEstricta ? 'Estructura exacta' : 'Mínimo requerido'}
              </span>
            </div>
            {entregable.nombreZipEsperado && entregable.nombreZipEsperado !== '*' && (
              <div className="re-zip-nombre">
                <span className="re-zip-nombre-label">Nombre esperado:</span>
                <code className="re-zip-nombre-value">{entregable.nombreZipEsperado}.zip</code>
              </div>
            )}
            {nodos.length > 0 && (
              <div className="re-zip-tree">
                <EstructuraZipReadonly nodos={nodos} nivel={0} />
              </div>
            )}
          </div>
        );
      })()}

      {/* Aviso de borrador recuperado */}
      {borradorCargado && (
        <div className="re-borrador-banner">
          <span>📋 Se ha recuperado un borrador guardado automáticamente.</span>
          <button
            className="re-borrador-descartar"
            onClick={handleDescartarBorrador}
          >
            Descartar borrador
          </button>
        </div>
      )}

      {/* Mensaje de éxito */}
      {successMsg && (
        <div className="re-success-banner">
          ✅ {successMsg}
        </div>
      )}

      <form onSubmit={handleEnviar} className="re-form">
        {errorEnvio && (
          <div className="re-error-banner">
            {errorEnvio}
          </div>
        )}

        {/* Nombre de la entrega */}
        <div className="re-field">
          <label htmlFor="nombre">Nombre de la entrega *</label>
          <input
            id="nombre"
            type="text"
            value={nombre}
            onChange={e => setNombre(e.target.value)}
            required
            maxLength={200}
            placeholder="Ej: Entrega final - Práctica 2"
            disabled={enviando}
          />
        </div>

        {/* Zona de subida de archivos */}
        <div className="re-field">
          <label>Archivos *</label>
          <div
            className={`re-dropzone ${dragOver ? 'drag-over' : ''} ${archivos.length > 0 ? 'has-files' : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              onChange={handleFileSelect}
              className="re-file-input"
              disabled={enviando}
            />
            <div className="re-dropzone-content">
              <span className="re-dropzone-icon">📁</span>
              <p className="re-dropzone-text">
                Arrastra archivos aquí o <span className="re-dropzone-link">haz clic para seleccionar</span>
              </p>
              {entregable.tipoArchivoEsperado && (
                <p className="re-dropzone-hint">Tipo esperado: {entregable.tipoArchivoEsperado}</p>
              )}
              {entregable.tamanoMaximoBytes && (
                <p className="re-dropzone-hint">Tamaño máximo: {formatFileSize(entregable.tamanoMaximoBytes)}</p>
              )}
            </div>
          </div>
        </div>

        {/* Lista de archivos */}
        {archivos.length > 0 && (
          <div className="re-files-list">
            <div className="re-files-header">
              <h3>Archivos adjuntos ({archivos.length})</h3>
            </div>
            <ul className="re-files">
              {archivos.map(archivo => (
                <li key={archivo.id} className="re-file-item">
                  <div className="re-file-info">
                    <span className="re-file-icon">{getIconoArchivo(archivo)}</span>
                    <div className="re-file-details">
                      <span className="re-file-name">{archivo.nombre}</span>
                      <span className="re-file-size">{formatFileSize(archivo.tamano)}</span>
                    </div>
                  </div>
                  <div className="re-file-actions">
                    {puedePrevisualizar(archivo) && (
                      <button
                        type="button"
                        className="re-btn-preview"
                        onClick={() => setArchivoPreview(archivo)}
                        title="Previsualizar"
                      >
                        👁️
                      </button>
                    )}
                    <a
                      href={archivo.dataUrl}
                      download={archivo.nombre}
                      className="re-btn-download"
                      title="Descargar"
                      onClick={e => e.stopPropagation()}
                    >
                      ⬇️
                    </a>
                    <button
                      type="button"
                      className="re-btn-remove"
                      onClick={() => eliminarArchivo(archivo.id)}
                      disabled={enviando}
                      title="Eliminar"
                    >
                      🗑️
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Acciones */}
        <div className="re-actions">
          <span className="re-autosave-hint">
            💾 Los archivos se guardan automáticamente como borrador
          </span>
          <div className="re-actions-right">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => navigate(`/entregables/${id}`)}
              disabled={enviando}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={enviando || archivos.length === 0}
            >
              {enviando ? 'Subiendo a OneDrive...' : 'Enviar entrega'}
            </button>
          </div>
        </div>
      </form>

      {/* Modal de previsualización */}
      {archivoPreview && (
        <div className="re-preview-overlay" onClick={() => setArchivoPreview(null)}>
          <div className="re-preview-modal" onClick={e => e.stopPropagation()}>
            {renderPreview(archivoPreview)}
          </div>
        </div>
      )}
    </div>
  );
};

// ── Componente read-only para mostrar la estructura esperada al estudiante ──
const EstructuraZipReadonly: React.FC<{ nodos: NodoEstructuraZip[]; nivel: number }> = ({ nodos, nivel }) => (
  <div className="re-zip-nodo-list">
    {nodos.map(nodo => {
      const esCarpeta = nodo.tipo === 'CARPETA';
      const esWild = nodo.nombre === '*';
      const extensiones = nodo.extensiones || [];
      let extDisplay = '';
      if (!esCarpeta) {
        if (extensiones.length === 0) extDisplay = '.*';
        else if (extensiones.length === 1) extDisplay = `.${extensiones[0]}`;
        else extDisplay = `.{${extensiones.join(', ')}}`;
      }

      return (
        <div key={nodo.id} className="re-zip-nodo" style={{ paddingLeft: nivel * 18 }}>
          <span className="re-zip-nodo-icon">{esCarpeta ? '📁' : '📄'}</span>
          <span className="re-zip-nodo-name">
            {esWild ? <em>*</em> : nodo.nombre}
            {!esCarpeta && <span className="re-zip-nodo-ext">{extDisplay}</span>}
            {esCarpeta && '/'}
          </span>
          {esCarpeta && nodo.hijos && nodo.hijos.length > 0 && (
            <EstructuraZipReadonly nodos={nodo.hijos} nivel={nivel + 1} />
          )}
        </div>
      );
    })}
  </div>
);

export default RealizarEntregaPage;
