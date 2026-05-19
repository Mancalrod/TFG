import React, { useState, useEffect } from 'react';
import { MaterialDTO, TipoMaterial } from '../types';
import { entregaService } from '../services/entregaService';
import './FilePreviewModal.css';

interface FilePreviewModalProps {
  material: MaterialDTO;
  onClose: () => void;
}

interface ZipEntry {
  nombre: string;
  tamano: number;
  esCarpeta: boolean;
}

/** Determina el MIME type correcto para crear el blob */
const resolverMimeType = (nombre: string): string => {
  const lower = nombre.toLowerCase();
  if (lower.endsWith('.pdf')) return 'application/pdf';
  if (lower.endsWith('.png')) return 'image/png';
  if (lower.endsWith('.jpg') || lower.endsWith('.jpeg')) return 'image/jpeg';
  if (lower.endsWith('.gif')) return 'image/gif';
  if (lower.endsWith('.svg')) return 'image/svg+xml';
  return 'application/octet-stream';
};

const FilePreviewModal: React.FC<FilePreviewModalProps> = ({ material, onClose }) => {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [textContent, setTextContent] = useState<string | null>(null);
  const [zipEntries, setZipEntries] = useState<ZipEntry[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const esZip = /\.(zip|rar|7z)$/i.test(material.nombre) || material.tipoMaterial === TipoMaterial.ZIP;
  const esTexto = material.tipoMaterial === TipoMaterial.TXT
    || /\.(txt|md|json|xml|html?|css|js|ts|java|py|c|cpp|h|log|csv)$/i.test(material.nombre);
  const esPdf = material.tipoMaterial === TipoMaterial.PDF || material.nombre.toLowerCase().endsWith('.pdf');
  const esImagen = material.tipoMaterial === TipoMaterial.IMAGEN || /\.(png|jpe?g|gif|svg)$/i.test(material.nombre);

  const esPrevisualizable = esPdf || esImagen || esTexto || esZip;

  useEffect(() => {
    if (!esPrevisualizable) {
      setLoading(false);
      return;
    }

    const cargar = async () => {
      setLoading(true);
      setError(null);
      try {
        if (esZip) {
          // ZIP: load content listing from backend
          const entries = await entregaService.listarContenidoZip(material.id);
          setZipEntries(entries);
        } else if (esTexto) {
          const blob = await entregaService.previsualizarArchivo(material.id);
          const text = await blob.text();
          setTextContent(text);
        } else {
          // PDF / Image: create blob with explicit MIME type
          const blob = await entregaService.previsualizarArchivo(material.id);
          const typedBlob = new Blob([blob], { type: resolverMimeType(material.nombre) });
          const url = URL.createObjectURL(typedBlob);
          setBlobUrl(url);
        }
      } catch {
        setError('No se pudo cargar la previsualización');
      } finally {
        setLoading(false);
      }
    };

    cargar();

    return () => {
      if (blobUrl) URL.revokeObjectURL(blobUrl);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [material.id]);

  const handleDescargar = async () => {
    try {
      const blob = await entregaService.descargarArchivo(material.id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = material.nombre;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      alert('Error al descargar el archivo');
    }
  };

  const formatFileSize = (bytes: number) => {
    if (!bytes || bytes <= 0) return '-';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getZipEntryIcon = (entry: ZipEntry) => {
    if (entry.esCarpeta) return '📁';
    const lower = entry.nombre.toLowerCase();
    if (lower.endsWith('.pdf')) return '📕';
    if (/\.(png|jpe?g|gif|svg)$/.test(lower)) return '🖼️';
    if (/\.(doc|docx)$/.test(lower)) return '📘';
    if (/\.(xls|xlsx)$/.test(lower)) return '📊';
    if (/\.(js|ts|java|py|c|cpp|h|html|css|json|xml)$/.test(lower)) return '💻';
    if (/\.(txt|md|log|csv)$/.test(lower)) return '📝';
    return '📄';
  };

  const renderContenido = () => {
    if (loading) {
      return (
        <div className="fpm-loading">
          <div className="fpm-spinner" />
          <p>Cargando previsualización...</p>
        </div>
      );
    }

    if (error) {
      return (
        <div className="fpm-error">
          <p>{error}</p>
          <button className="fpm-btn-descargar" onClick={handleDescargar}>
            Descargar en su lugar
          </button>
        </div>
      );
    }

    if (!esPrevisualizable) {
      return (
        <div className="fpm-no-preview">
          <div className="fpm-file-icon-big">📄</div>
          <p>Este tipo de archivo no se puede previsualizar.</p>
          <button className="fpm-btn-descargar" onClick={handleDescargar}>
            Descargar archivo
          </button>
        </div>
      );
    }

    // ZIP content listing
    if (zipEntries !== null) {
      const carpetas = zipEntries.filter(e => e.esCarpeta);
      const archivos = zipEntries.filter(e => !e.esCarpeta);
      const totalArchivos = archivos.length;
      const totalCarpetas = carpetas.length;

      return (
        <div className="fpm-zip-viewer">
          <div className="fpm-zip-summary">
            <span>📦 {totalArchivos} archivo{totalArchivos !== 1 ? 's' : ''}</span>
            {totalCarpetas > 0 && <span>📁 {totalCarpetas} carpeta{totalCarpetas !== 1 ? 's' : ''}</span>}
          </div>
          <div className="fpm-zip-list">
            {zipEntries.map((entry, i) => (
              <div key={i} className={`fpm-zip-entry ${entry.esCarpeta ? 'fpm-zip-folder' : ''}`}
                   style={{ paddingLeft: `${(entry.nombre.split('/').length - 1) * 16 + 12}px` }}>
                <span className="fpm-zip-entry-icon">{getZipEntryIcon(entry)}</span>
                <span className="fpm-zip-entry-name">
                  {entry.esCarpeta
                    ? entry.nombre
                    : entry.nombre.split('/').pop()}
                </span>
                {!entry.esCarpeta && entry.tamano > 0 && (
                  <span className="fpm-zip-entry-size">{formatFileSize(entry.tamano)}</span>
                )}
              </div>
            ))}
          </div>
        </div>
      );
    }

    // Text content
    if (textContent !== null) {
      return (
        <pre className="fpm-text-content">{textContent}</pre>
      );
    }

    // PDF
    if (blobUrl && esPdf) {
      return (
        <iframe
          src={blobUrl + '#toolbar=1&navpanes=0'}
          className="fpm-pdf-viewer"
          title={material.nombre}
        />
      );
    }

    // Image
    if (blobUrl && esImagen) {
      return (
        <div className="fpm-image-container">
          <img src={blobUrl} alt={material.nombre} className="fpm-image" />
        </div>
      );
    }

    return (
      <div className="fpm-no-preview">
        <p>No se pudo previsualizar este archivo.</p>
        <button className="fpm-btn-descargar" onClick={handleDescargar}>
          Descargar archivo
        </button>
      </div>
    );
  };

  return (
    <div className="fpm-overlay" onClick={onClose}>
      <div className="fpm-modal" onClick={e => e.stopPropagation()}>
        <div className="fpm-header">
          <div className="fpm-header-info">
            <h2 className="fpm-title">{material.nombre}</h2>
            <span className="fpm-meta">{formatFileSize(material.tamanoBytes)}</span>
          </div>
          <div className="fpm-header-actions">
            <button className="fpm-btn-download" onClick={handleDescargar} title="Descargar">
              <span className="fpm-btn-emoji">📥</span> Descargar
            </button>
            <button className="fpm-btn-close" onClick={onClose} title="Cerrar">
              <span className="fpm-btn-emoji">❌</span> Cerrar
            </button>
          </div>
        </div>
        <div className="fpm-content">
          {renderContenido()}
        </div>
      </div>
    </div>
  );
};

export default FilePreviewModal;
