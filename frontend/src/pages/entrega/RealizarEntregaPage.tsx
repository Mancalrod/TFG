import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { EntregableDTO, NodoEstructuraZip } from '../../types';
import { entregableService, entregaService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import './RealizarEntregaPage.css';

// Constantes
const TEMP_STORAGE_PREFIX = 'entrega_temp_';

type IconName =
  | 'file'
  | 'file-pdf'
  | 'file-image'
  | 'file-doc'
  | 'file-archive'
  | 'file-text'
  | 'file-sheet'
  | 'file-slides'
  | 'folder'
  | 'download'
  | 'preview'
  | 'remove'
  | 'upload'
  | 'zip'
  | 'success'
  | 'draft'
  | 'save';

const Icon: React.FC<{ name: IconName; className?: string }> = ({ name, className }) => {
  const cls = className ? `re-icon ${className}` : 're-icon';

  switch (name) {
    case 'file-pdf':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M7 3h7l5 5v13H7z" stroke="currentColor" strokeWidth="1.8" /><path d="M14 3v5h5" stroke="currentColor" strokeWidth="1.8" /><path d="M9 16h6M9 12h6" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'file-image':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="4" y="5" width="16" height="14" rx="2" stroke="currentColor" strokeWidth="1.8" /><circle cx="9" cy="10" r="1.3" fill="currentColor" /><path d="m6 17 4.3-4 2.8 2.6 2.9-3.1L18 17" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'file-doc':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M7 3h7l5 5v13H7z" stroke="currentColor" strokeWidth="1.8" /><path d="M14 3v5h5" stroke="currentColor" strokeWidth="1.8" /><path d="M9 16h6M9 12h6M9 9h3" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'file-archive':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="5" y="4" width="14" height="16" rx="2" stroke="currentColor" strokeWidth="1.8" /><path d="M9 7h6M9 10h6M11 13h2v4h-2z" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'file-text':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M7 3h7l5 5v13H7z" stroke="currentColor" strokeWidth="1.8" /><path d="M14 3v5h5" stroke="currentColor" strokeWidth="1.8" /><path d="M9 11h6M9 14h6M9 17h4" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'file-sheet':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M7 3h7l5 5v13H7z" stroke="currentColor" strokeWidth="1.8" /><path d="M14 3v5h5M9 11h8M9 15h8M12 9v10" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'file-slides':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="5" y="4" width="14" height="12" rx="2" stroke="currentColor" strokeWidth="1.8" /><path d="M9 18h6M12 16v2" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'folder':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M3 8a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'download':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 4v10m0 0 4-4m-4 4-4-4M5 19h14" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" /></svg>;
    case 'preview':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" stroke="currentColor" strokeWidth="1.8" /><circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'remove':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M4 7h16M9 7V5h6v2m-7 0 1 12h6l1-12" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" /></svg>;
    case 'upload':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 15V5m0 0 4 4m-4-4-4 4M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" /></svg>;
    case 'zip':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="5" y="4" width="14" height="16" rx="2" stroke="currentColor" strokeWidth="1.8" /><path d="M10 7h4M10 10h4M11 13h2v4h-2z" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'success':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.8" /><path d="m8 12 2.5 2.5L16 9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" /></svg>;
    case 'draft':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 5h14v14H5z" stroke="currentColor" strokeWidth="1.8" /><path d="M8 9h8M8 12h8M8 15h5" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'save':
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 4h12l2 2v14H5z" stroke="currentColor" strokeWidth="1.8" /><path d="M8 4v6h8V4M9 17h6" stroke="currentColor" strokeWidth="1.8" /></svg>;
    case 'file':
    default:
      return <svg className={cls} viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M7 3h7l5 5v13H7z" stroke="currentColor" strokeWidth="1.8" /><path d="M14 3v5h5" stroke="currentColor" strokeWidth="1.8" /></svg>;
  }
};

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
  comentario: string;
  archivos: ArchivoTemporal[];
  ultimaModificacion: string;
}

const normalizarExtensionesZip = (extensiones: string[] = []): string[] => {
  const limpias = extensiones
    .map(ext => ext.trim().toLowerCase().replace(/^\.+/, ''))
    .filter(ext => ext.length > 0);

  // Compatibilidad: "*" implica cualquier extensión.
  if (limpias.includes('*')) {
    return [];
  }

  return Array.from(new Set(limpias));
};

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
  const [comentario, setComentario] = useState('');
  const [archivos, setArchivos] = useState<ArchivoTemporal[]>([]);
  const [archivoPreview, setArchivoPreview] = useState<ArchivoTemporal | null>(null);

  // Drag & Drop
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Indicador de borrador cargado
  const [borradorCargado, setBorradorCargado] = useState(false);

  const storageKey = id ? `${TEMP_STORAGE_PREFIX}${id}` : '';

  // Cargar entregable
  useEffect(() => {
    if (id) {
      cargarEntregable(parseInt(id));
    }
  }, [id]);

  // Redirigir si no es estudiante
  useEffect(() => {
    if (!loading && !esEstudiante) {
      navigate(-1);
    }
  }, [loading, esEstudiante, navigate]);

  // Cargar borrador desde localStorage
  useEffect(() => {
    if (!storageKey) return;
    try {
      const raw = localStorage.getItem(storageKey);
      if (!raw) return;

      const borrador: BorradorMeta = JSON.parse(raw);
      const comentarioBorrador = typeof borrador.comentario === 'string' ? borrador.comentario : '';
      const archivosBorrador = Array.isArray(borrador.archivos) ? borrador.archivos : [];
      const tieneContenido = comentarioBorrador.trim().length > 0 || archivosBorrador.length > 0;

      if (!tieneContenido) {
        localStorage.removeItem(storageKey);
        setBorradorCargado(false);
        return;
      }

      setComentario(comentarioBorrador);
      setArchivos(archivosBorrador);
      setBorradorCargado(true);
    } catch {
      // localStorage corrupto, ignorar
    }
  }, [storageKey]);

  // Guardar borrador en localStorage cuando cambie
  const guardarBorrador = useCallback(() => {
    if (!storageKey || !id) return;
    try {
      if (comentario.trim().length === 0 && archivos.length === 0) {
        localStorage.removeItem(storageKey);
        return;
      }

      const borrador: BorradorMeta = {
        entregableId: parseInt(id),
        comentario,
        archivos,
        ultimaModificacion: new Date().toISOString(),
      };
      localStorage.setItem(storageKey, JSON.stringify(borrador));
    } catch {
      // localStorage lleno, intentar limpiar
      console.warn('No se pudo guardar el borrador en localStorage');
    }
  }, [storageKey, id, comentario, archivos]);

  useEffect(() => {
    // Auto-guardar cuando cambie comentario o archivos (debounce sencillo)
    if (!storageKey) return;
    const timer = setTimeout(() => {
      guardarBorrador();
    }, 500);
    return () => clearTimeout(timer);
  }, [comentario, archivos, guardarBorrador, storageKey]);

  const limpiarBorrador = () => {
    if (storageKey) {
      localStorage.removeItem(storageKey);
    }
  };

  const requiereZipEstructurado = !!entregable && (
    !!entregable.estructuraZip ||
    (!!entregable.nombreZipEsperado && entregable.nombreZipEsperado.trim() !== '' && entregable.nombreZipEsperado.trim() !== '*')
  );
  const tipoEsperado = (entregable?.tipoArchivoEsperado || '').toUpperCase();
  const esSoloTexto = tipoEsperado === 'SOLO_TEXTO';
  const esEnlace = tipoEsperado === 'ENLACE';
  const noPermiteArchivos = esSoloTexto || esEnlace;
  const permiteSoloComentario = !entregable?.tipoArchivoEsperado
    || tipoEsperado === 'OTRO'
    || esSoloTexto
    || esEnlace;
  const etiquetaTipoEsperado = entregable?.tipoArchivoEsperado === 'SOLO_TEXTO'
    ? 'SOLO TEXTO (sin archivos)'
    : entregable?.tipoArchivoEsperado === 'ENLACE'
      ? 'ENLACE (en comentario, sin archivos)'
      : (entregable?.tipoArchivoEsperado || 'NO ESPECIFICADO');
  const puedeEnviar = noPermiteArchivos
    ? comentario.trim().length > 0 && archivos.length === 0
    : archivos.length > 0 || (comentario.trim().length > 0 && permiteSoloComentario);

  useEffect(() => {
    if (noPermiteArchivos && archivos.length > 0) {
      setArchivos([]);
    }
  }, [noPermiteArchivos, archivos.length]);

  const cargarEntregable = async (entregableId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await entregableService.obtener(entregableId);

      if (data.visibilidad !== 'VISIBLE') {
        setError('Este entregable no está disponible.');
        setEntregable(null);
        return;
      }

      setEntregable(data);
    } catch (err) {
      console.error('Error al cargar entregable:', err);
      setError('No se pudo cargar el entregable');
    } finally {
      setLoading(false);
    }
  };

  // Manejo de archivos
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
    const tipoEsperado = (entregable.tipoArchivoEsperado || '').toUpperCase();

    if (requiereZipEstructurado && !file.name.toLowerCase().endsWith('.zip')) {
      return `Este entregable requiere un único archivo ZIP. "${file.name}" no es un .zip`;
    }

    if (tipoEsperado === 'SOLO_TEXTO') {
      return 'Este entregable es de solo texto: no se permiten archivos adjuntos.';
    }

    // Validar tamaño
    if (entregable.tamanoMaximoBytes && file.size > entregable.tamanoMaximoBytes) {
      const maxMB = (entregable.tamanoMaximoBytes / (1024 * 1024)).toFixed(1);
      return `El archivo "${file.name}" excede el tamaño máximo de ${maxMB} MB`;
    }

    if (!tipoEsperado || tipoEsperado === 'OTRO') {
      return null;
    }

    const ext = file.name.split('.').pop()?.toUpperCase() || '';
    const mime = file.type.toLowerCase();

    const permitidasPorTipo: Record<string, string[]> = {
      PDF: ['PDF'],
      DOCX: ['DOC', 'DOCX'],
      ZIP: ['ZIP'],
      RAR: ['RAR', '7Z'],
      TXT: ['TXT'],
      IMAGEN: ['PNG', 'JPG', 'JPEG', 'GIF', 'WEBP', 'BMP', 'SVG'],
      VIDEO: ['MP4', 'AVI', 'MOV', 'MKV', 'WEBM'],
      ENLACE: []
    };

    if (tipoEsperado === 'IMAGEN' && !mime.startsWith('image/')) {
      return `Se espera una imagen, pero "${file.name}" no parece un archivo de imagen.`;
    }

    if (tipoEsperado === 'VIDEO' && !mime.startsWith('video/')) {
      return `Se espera un video, pero "${file.name}" no parece un archivo de video.`;
    }

    if (tipoEsperado === 'ENLACE') {
      return 'Este entregable espera texto/enlace en el comentario, no archivos adjuntos.';
    }

    const extensiones = permitidasPorTipo[tipoEsperado];
    if (extensiones && extensiones.length > 0 && !extensiones.includes(ext)) {
      return `Se espera un archivo de tipo ${tipoEsperado}, pero se seleccionó ".${ext || 'sin extensión'}"`;
    }

    return null;
  };

  const agregarArchivos = async (files: FileList | File[]) => {
    const archivosEntrantes = Array.from(files);
    if (noPermiteArchivos) {
      setErrorEnvio(
        esSoloTexto
          ? 'Este entregable es de solo texto: escribe el contenido en el comentario.'
          : 'Este entregable es de tipo ENLACE: pega el enlace en el comentario.'
      );
      return;
    }

    if (requiereZipEstructurado && (archivos.length + archivosEntrantes.length > 1)) {
      setErrorEnvio('Este entregable solo permite adjuntar un único archivo ZIP');
      return;
    }

    const nuevosArchivos: ArchivoTemporal[] = [];
    const errores: string[] = [];

    for (const file of archivosEntrantes) {
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

  // Drag & Drop handlers
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

  // Preview
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

  const getIconoArchivo = (archivo: ArchivoTemporal): IconName => {
    const ext = archivo.nombre.split('.').pop()?.toLowerCase() || '';
    const tipo = archivo.tipo.toLowerCase();

    if (tipo === 'application/pdf' || ext === 'pdf') return 'file-pdf';
    if (tipo.startsWith('image/')) return 'file-image';
    if (ext === 'doc' || ext === 'docx' || tipo.includes('word')) return 'file-doc';
    if (ext === 'zip' || ext === 'rar' || ext === '7z') return 'file-archive';
    if (ext === 'txt') return 'file-text';
    if (ext === 'xls' || ext === 'xlsx') return 'file-sheet';
    if (ext === 'ppt' || ext === 'pptx') return 'file-slides';
    return 'file';
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
            <div className="re-preview-doc-icon"><Icon name="file-doc" /></div>
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

  // Enviar entrega
  const handleEnviar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !usuario?.id || !entregable) return;

    // Validar combinaciones permitidas según el tipo de entregable
    const tieneArchivos = archivos.length > 0;
    const tieneComentario = comentario.trim().length > 0;

    if (noPermiteArchivos) {
      if (tieneArchivos) {
        setErrorEnvio(
          esSoloTexto
            ? 'Este entregable es de solo texto: no puedes adjuntar archivos.'
            : 'Este entregable es de tipo ENLACE: no puedes adjuntar archivos.'
        );
        return;
      }
      if (!tieneComentario) {
        setErrorEnvio(
          esSoloTexto
            ? 'Este entregable es de solo texto: debes escribir un comentario.'
            : 'Este entregable es de tipo ENLACE: debes pegar el enlace en el comentario.'
        );
        return;
      }
    } else if (!tieneArchivos && !tieneComentario) {
      setErrorEnvio('Debes adjuntar al menos un archivo o escribir un comentario');
      return;
    } else if (!tieneArchivos && tieneComentario && !permiteSoloComentario) {
      setErrorEnvio('Para este tipo de entregable debes adjuntar al menos un archivo. El comentario no sustituye al archivo.');
      return;
    }

    // Revalidación final previa al envío para cubrir borradores recuperados
    // o cambios de configuración del entregable desde que se adjuntó el archivo.
    if (tieneArchivos) {
      if (requiereZipEstructurado) {
        if (archivos.length !== 1) {
          setErrorEnvio('Este entregable solo permite adjuntar un único archivo ZIP');
          return;
        }
        if (!archivos[0].nombre.toLowerCase().endsWith('.zip')) {
          setErrorEnvio(`Este entregable requiere un único archivo ZIP. "${archivos[0].nombre}" no es un .zip`);
          return;
        }
      }

      if (entregable.tamanoMaximoBytes && entregable.tamanoMaximoBytes > 0) {
        const maxBytes = entregable.tamanoMaximoBytes;
        const archivoExcedido = archivos.find(a => a.tamano > maxBytes);
        if (archivoExcedido) {
          const maxMB = (maxBytes / (1024 * 1024)).toFixed(1);
          setErrorEnvio(`El archivo "${archivoExcedido.nombre}" excede el tamaño máximo de ${maxMB} MB`);
          return;
        }
      }
    }

    setEnviando(true);
    setErrorEnvio(null);

    try {
      // Convertir dataUrls a File objects para el envío
      let fileObjects: File[] | undefined;
      if (tieneArchivos) {
        fileObjects = await Promise.all(
          archivos.map(async (archivo) => {
            const response = await fetch(archivo.dataUrl);
            const blob = await response.blob();
            return new File([blob], archivo.nombre, { type: archivo.tipo });
          })
        );
      }

      await entregaService.realizar(
        parseInt(id),
        usuario.id,
        tieneComentario ? comentario.trim() : undefined,
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

      let mensajeError = 'Error al realizar la entrega';

      // Intentar extraer mensaje detallado del error
      if (axiosErr?.response?.data?.message) {
        mensajeError = axiosErr.response.data.message;
      } else if (axiosErr?.response?.data?.errors) {
        const errores = Object.entries(axiosErr.response.data.errors)
          .map(([campo, mensaje]) => `${campo}: ${mensaje}`)
          .join('\n');
        mensajeError = `Errores de validación:\n${errores}`;
      } else if ((err as Error).message) {
        mensajeError = `Error: ${(err as Error).message}`;
      }

      // Agregar contexto adicional si el error es relacionado con ZIP
      if (entregable?.estructuraZip && mensajeError.toLowerCase().includes('zip')) {
        mensajeError += '\n\nRevisa que tu archivo ZIP cumpla con la estructura esperada mostrada arriba.';
      }

      // Agregar contexto si es problema de tamaño
      if (mensajeError.toLowerCase().includes('tamaño') || mensajeError.toLowerCase().includes('size')) {
        if (entregable?.tamanoMaximoBytes) {
          const maxMB = (entregable.tamanoMaximoBytes / (1024 * 1024)).toFixed(1);
          mensajeError += `\n\nEl tamaño máximo permitido es ${maxMB} MB.`;
        }
      }

      setErrorEnvio(mensajeError);
    } finally {
      setEnviando(false);
    }
  };

  const handleDescartarBorrador = () => {
    setComentario('');
    setArchivos([]);
    setArchivoPreview(null);
    limpiarBorrador();
    setBorradorCargado(false);
  };

  // Utilidades de formato
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

  // Render
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
            <span className="re-info-value">{etiquetaTipoEsperado}</span>
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
              <span className="re-inline-icon-text"><Icon name="zip" />Estructura esperada del ZIP</span>
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
          <span className="re-inline-icon-text"><Icon name="draft" />Se ha recuperado un borrador guardado automáticamente.</span>
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
          <span className="re-inline-icon-text"><Icon name="success" />{successMsg}</span>
        </div>
      )}

      <form onSubmit={handleEnviar} className="re-form">
        {errorEnvio && (
          <div className="re-error-banner">
            {errorEnvio}
          </div>
        )}

        {/* Zona de subida de archivos */}
        {!noPermiteArchivos ? (
          <div className="re-field">
            <label>Archivos</label>
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
                multiple={!requiereZipEstructurado}
                onChange={handleFileSelect}
                className="re-file-input"
                disabled={enviando}
              />
              <div className="re-dropzone-content">
                <span className="re-dropzone-icon"><Icon name="upload" /></span>
                <p className="re-dropzone-text">
                  Arrastra archivos aquí o <span className="re-dropzone-link">haz clic para seleccionar</span>
                </p>
                {entregable.tipoArchivoEsperado && (
                  <p className="re-dropzone-hint">Tipo esperado: {etiquetaTipoEsperado}</p>
                )}
                {entregable.tamanoMaximoBytes && (
                  <p className="re-dropzone-hint">Tamaño máximo: {formatFileSize(entregable.tamanoMaximoBytes)}</p>
                )}
                {requiereZipEstructurado && (
                  <p className="re-dropzone-hint">Este entregable acepta solo 1 archivo ZIP</p>
                )}
              </div>
            </div>
          </div>
        ) : (
          <div className="re-field">
            <label>Archivos</label>
            <p className="re-hint">
              {esSoloTexto
                ? <>Este entregable es de <strong>solo texto</strong>. Escribe tu entrega en el comentario.</>
                : <>Este entregable es de tipo <strong>enlace</strong>. Pega el enlace en el comentario.</>}
            </p>
          </div>
        )}

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
                    <span className="re-file-icon"><Icon name={getIconoArchivo(archivo)} /></span>
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
                        <Icon name="preview" className="re-btn-icon" />
                      </button>
                    )}
                    <a
                      href={archivo.dataUrl}
                      download={archivo.nombre}
                      className="re-btn-download"
                      title="Descargar"
                      onClick={e => e.stopPropagation()}
                    >
                      <Icon name="download" className="re-btn-icon" />
                    </a>
                    <button
                      type="button"
                      className="re-btn-remove"
                      onClick={() => eliminarArchivo(archivo.id)}
                      disabled={enviando}
                      title="Eliminar"
                    >
                      <Icon name="remove" className="re-btn-icon" />
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Comentario/Observaciones */}
        <div className="re-field">
          <label htmlFor="comentario">
            Comentario / Observaciones {noPermiteArchivos ? '(obligatorio)' : '(opcional)'}
          </label>
          <textarea
            id="comentario"
            value={comentario}
            onChange={e => setComentario(e.target.value)}
            maxLength={5000}
            rows={4}
            placeholder="Puedes escribir observaciones, aclaraciones o incluso hacer una entrega de solo texto si no necesitas adjuntar archivos..."
            disabled={enviando}
            className="re-textarea"
          />
          <div className="re-char-count">{comentario.length}/5000 caracteres</div>
        </div>

        <p className="re-hint">
          {esSoloTexto
            ? 'Este entregable solo admite texto en el comentario.'
            : esEnlace
              ? 'Este entregable espera un enlace en el comentario.'
            : permiteSoloComentario
              ? 'Puedes entregar con archivos, con comentario o con ambos.'
              : 'Para este tipo de entregable debes adjuntar al menos un archivo. El comentario es adicional.'}
        </p>

        {/* Acciones */}
        <div className="re-actions">
          <span className="re-autosave-hint">
            <span className="re-inline-icon-text"><Icon name="save" />Los archivos se guardan automáticamente como borrador</span>
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
              disabled={enviando || !puedeEnviar}
            >
              {enviando ? 'Enviando...' : 'Enviar entrega'}
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

// Componente read-only para mostrar la estructura esperada al estudiante
const EstructuraZipReadonly: React.FC<{ nodos: NodoEstructuraZip[]; nivel: number }> = ({ nodos, nivel }) => (
  <div className="re-zip-nodo-list">
    {nodos.map(nodo => {
      const esCarpeta = nodo.tipo === 'CARPETA';
      const esWild = nodo.nombre === '*';
      const extensiones = normalizarExtensionesZip(nodo.extensiones || []);
      let extDisplay = '';
      if (!esCarpeta) {
        if (extensiones.length === 0) extDisplay = '.*';
        else if (extensiones.length === 1) extDisplay = `.${extensiones[0]}`;
        else extDisplay = `.{${extensiones.join(', ')}}`;
      }

      return (
        <div key={nodo.id} className="re-zip-nodo" style={{ paddingLeft: nivel * 18 }}>
          <span className="re-zip-nodo-icon"><Icon name={esCarpeta ? 'folder' : 'file'} /></span>
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


