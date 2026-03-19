import React, { useState, useEffect } from 'react';
import { oneDriveService } from '../services/oneDriveService';
import { OneDriveFolder } from '../types';

interface Props {
  usuarioId: number;
  selectedPath: string;
  onSelectFolder: (path: string) => void;
}

export const OneDriveFolderBrowser: React.FC<Props> = ({ usuarioId, selectedPath, onSelectFolder }) => {
  const [breadcrumbs, setBreadcrumbs] = useState<{id: string, name: string, path: string}[]>([
    { id: 'root', name: 'Raíz (TFG-Entregables)', path: '' }
  ]);
  const [folders, setFolders] = useState<OneDriveFolder[]>([]);
  const [loading, setLoading] = useState(false);

  const currentFolderId = breadcrumbs[breadcrumbs.length - 1].id;
  const currentPath = breadcrumbs[breadcrumbs.length - 1].path;

  useEffect(() => {
    const fetchFolders = async () => {
      setLoading(true);
      try {
        const result = await oneDriveService.listarCarpetas(usuarioId, currentFolderId === 'root' ? undefined : currentFolderId);
        setFolders(result);
      } catch (error) {
        console.error("Error al cargar carpetas de OneDrive", error);
      } finally {
        setLoading(false);
      }
    };
    fetchFolders();
  }, [usuarioId, currentFolderId]);

  const handleEnterFolder = (folder: OneDriveFolder) => {
    setBreadcrumbs([...breadcrumbs, { id: folder.id, name: folder.name, path: folder.path }]);
  };

  const handleGoToBreadcrumb = (index: number) => {
    setBreadcrumbs(breadcrumbs.slice(0, index + 1));
  };

  return (
    <div className="onedrive-browser" style={{ border: '1px solid #e2e8f0', borderRadius: '6px', overflow: 'hidden', backgroundColor: 'white' }}>
      {/* Cabecera / Breadcrumbs */}
      <div className="ob-header" style={{ padding: '0.75rem', backgroundColor: '#f8fafc', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
        <div className="ob-breadcrumbs" style={{ flex: 1, display: 'flex', gap: '5px', fontSize: '0.9rem', alignItems: 'center' }}>
          {breadcrumbs.map((bc, idx) => (
            <React.Fragment key={bc.id}>
              {idx > 0 && <span style={{ color: '#94a3b8' }}>/</span>}
              <button 
                type="button" 
                onClick={() => handleGoToBreadcrumb(idx)}
                style={{ 
                  background: 'none', 
                  border: 'none', 
                  color: idx === breadcrumbs.length - 1 ? '#334155' : '#3b82f6', 
                  cursor: idx === breadcrumbs.length - 1 ? 'default' : 'pointer', 
                  padding: 0, 
                  fontWeight: idx === breadcrumbs.length - 1 ? '600' : 'normal' 
                }}
              >
                {bc.name}
              </button>
            </React.Fragment>
          ))}
        </div>
        <button 
          type="button" 
          onClick={() => onSelectFolder(currentPath)}
          style={{ 
            padding: '0.4rem 0.8rem', 
            fontSize: '0.85rem', 
            backgroundColor: '#10b981', 
            color: 'white', 
            border: 'none', 
            borderRadius: '4px', 
            cursor: 'pointer',
            fontWeight: '500'
          }}
        >
          ✓ Seleccionar contenedor actual
        </button>
      </div>

      {/* Lista de Carpetas */}
      <div className="ob-content" style={{ maxHeight: '250px', overflowY: 'auto', padding: '0.5rem' }}>
        {loading ? (
          <div style={{ padding: '2rem 1rem', textAlign: 'center', color: '#64748b', fontSize: '0.9rem' }}>
            <span style={{ display: 'inline-block', animation: 'spin 1s linear infinite' }}>⏳</span> Cargando subcarpetas...
          </div>
        ) : folders.length === 0 ? (
          <div style={{ padding: '2rem 1rem', textAlign: 'center', color: '#64748b', fontSize: '0.9rem' }}>
            Esta carpeta está vacía.
          </div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {folders.map(folder => (
              <li 
                key={folder.id} 
                style={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  alignItems: 'center', 
                  padding: '0.6rem 0.8rem', 
                  borderBottom: '1px solid #f1f5f9',
                  transition: 'background-color 0.2s ease'
                }}
                onMouseOver={(e) => e.currentTarget.style.backgroundColor = '#f8fafc'}
                onMouseOut={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
              >
                <div 
                  style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', cursor: 'pointer', flex: 1 }} 
                  onClick={() => handleEnterFolder(folder)}
                >
                  <span style={{ fontSize: '1.3rem' }}>📁</span>
                  <span style={{ color: '#334155', fontWeight: '500' }}>{folder.name}</span>
                </div>
                <button 
                  type="button" 
                  onClick={(e) => { e.stopPropagation(); onSelectFolder(folder.path); }} 
                  style={{ 
                    background: '#f1f5f9', 
                    color: '#475569',
                    border: '1px solid #cbd5e1', 
                    borderRadius: '4px', 
                    padding: '0.3rem 0.6rem', 
                    fontSize: '0.8rem', 
                    cursor: 'pointer',
                    fontWeight: '500'
                  }}
                  onMouseOver={(e) => { e.currentTarget.style.background = '#e2e8f0'; }}
                  onMouseOut={(e) => { e.currentTarget.style.background = '#f1f5f9'; }}
                >
                  Seleccionar
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Footer / Info Selección */}
      <div style={{ 
        padding: '0.6rem 0.8rem', 
        backgroundColor: '#eff6ff', 
        borderTop: '1px solid #bfdbfe', 
        fontSize: '0.85rem', 
        color: '#1e40af',
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem'
      }}>
        <span style={{ fontSize: '1rem' }}>📌</span>
        <div>
          <strong>Ruta seleccionada actualmente:</strong><br/>
          <span style={{ fontFamily: 'monospace', opacity: 0.9 }}>
            {selectedPath === '' ? 'Directorio Principal (Predeterminado)' : selectedPath}
          </span>
        </div>
      </div>
    </div>
  );
};
