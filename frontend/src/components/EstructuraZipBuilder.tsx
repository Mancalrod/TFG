import React, { useState, useRef } from 'react';
import { NodoEstructuraZip } from '../types';
import { parsearEstructuraZip, ModoImportacion } from '../utils/zipStructureParser';
import './EstructuraZipBuilder.css';

interface EstructuraZipBuilderProps {
  nodos: NodoEstructuraZip[];
  onChange: (nodos: NodoEstructuraZip[]) => void;
  estricta: boolean;
  onEstrictaChange: (estricta: boolean) => void;
  nombreZipEsperado: string;
  onNombreZipChange: (nombre: string) => void;
}

const generarId = () => `${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;

const EXTENSIONES_COMUNES = [
  'java', 'py', 'js', 'ts', 'jsx', 'tsx', 'c', 'cpp', 'h', 'cs',
  'html', 'css', 'scss', 'json', 'xml', 'yaml', 'yml',
  'txt', 'md', 'pdf', 'doc', 'docx', 'xls', 'xlsx',
  'png', 'jpg', 'jpeg', 'gif', 'svg',
  'zip', 'rar', 'tar', 'gz',
  'sql', 'sh', 'bat', 'cmd', 'properties', 'config', 'env',
  'gradle', 'maven', 'pom',
];

const EstructuraZipBuilder: React.FC<EstructuraZipBuilderProps> = ({
  nodos,
  onChange,
  estricta,
  onEstrictaChange,
  nombreZipEsperado,
  onNombreZipChange,
}) => {
  const [modoImportacion, setModoImportacion] = useState<ModoImportacion>('nombres_extensiones');
  const [importando, setImportando] = useState(false);
  const [errorImportacion, setErrorImportacion] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImportarZip = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = e.target.files?.[0];
    if (!archivo) return;

    if (!archivo.name.toLowerCase().endsWith('.zip')) {
      setErrorImportacion('El archivo debe ser un .zip');
      return;
    }

    setImportando(true);
    setErrorImportacion(null);
    try {
      const nodosImportados = await parsearEstructuraZip(archivo, modoImportacion);
      onChange(nodosImportados);
    } catch {
      setErrorImportacion('No se pudo leer el archivo ZIP. Asegúrate de que es un ZIP válido.');
    } finally {
      setImportando(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  return (
    <div className="ezb-container">
      <div className="ezb-header">
        <h3>📦 Estructura esperada del ZIP</h3>
        <p className="ezb-help">
          Define la estructura de archivos y carpetas que debe contener el ZIP.
          Usa <strong>*</strong> como nombre para aceptar cualquier nombre.
        </p>
      </div>

      {/* ── Importar desde ZIP ── */}
      <div className="ezb-import-section">
        <div className="ezb-import-header">
          <span className="ezb-import-title">📥 Importar estructura desde ZIP</span>
          <small className="ezb-import-help">
            Sube un ZIP de ejemplo y se extraerá su estructura automáticamente.
          </small>
        </div>

        <div className="ezb-import-options">
          <label className="ezb-import-option">
            <input
              type="radio"
              name="modoImportacion"
              checked={modoImportacion === 'nombres_extensiones'}
              onChange={() => setModoImportacion('nombres_extensiones')}
            />
            <span className="ezb-import-option-label">
              <strong>Nombres y extensiones</strong>
              <small>Mantiene los nombres de archivos y sus extensiones</small>
            </span>
          </label>
          <label className="ezb-import-option">
            <input
              type="radio"
              name="modoImportacion"
              checked={modoImportacion === 'solo_nombres'}
              onChange={() => setModoImportacion('solo_nombres')}
            />
            <span className="ezb-import-option-label">
              <strong>Solo nombres</strong>
              <small>Mantiene los nombres pero ignora las extensiones</small>
            </span>
          </label>
          <label className="ezb-import-option">
            <input
              type="radio"
              name="modoImportacion"
              checked={modoImportacion === 'solo_estructura'}
              onChange={() => setModoImportacion('solo_estructura')}
            />
            <span className="ezb-import-option-label">
              <strong>Solo estructura</strong>
              <small>Usa comodín (*) para nombres, solo conserva extensiones</small>
            </span>
          </label>
        </div>

        <div className="ezb-import-actions">
          <input
            ref={fileInputRef}
            type="file"
            accept=".zip"
            onChange={handleImportarZip}
            className="ezb-import-file-input"
            id="ezb-import-file"
            data-testid="ezb-import-file"
          />
          <label htmlFor="ezb-import-file" className="ezb-btn-import">
            {importando ? 'Importando...' : '📂 Seleccionar ZIP'}
          </label>
          {nodos.length > 0 && (
            <small className="ezb-import-warning">
              ⚠️ Importar reemplazará la estructura actual
            </small>
          )}
        </div>

        {errorImportacion && (
          <div className="ezb-import-error" role="alert">
            ❌ {errorImportacion}
          </div>
        )}
      </div>

      <div className="ezb-nombre-zip">
        <label htmlFor="ezb-nombre-zip-input" className="ezb-nombre-zip-label">
          Nombre del archivo ZIP esperado
        </label>
        <div className="ezb-nombre-zip-row">
          <input
            id="ezb-nombre-zip-input"
            type="text"
            className="ezb-nombre-zip-input"
            value={nombreZipEsperado}
            onChange={e => onNombreZipChange(e.target.value)}
            placeholder="Ej: proyecto_final  (deja vacío o usa * para cualquier nombre)"
          />
          <span className="ezb-nombre-zip-ext">.zip</span>
        </div>
        <small className="ezb-nombre-zip-help">
          Si está vacío o es <strong>*</strong>, se acepta cualquier nombre de archivo ZIP.
        </small>
      </div>

      <div className="ezb-mode-selector">
        <label className="ezb-mode-option">
          <input
            type="radio"
            name="modoValidacion"
            checked={!estricta}
            onChange={() => onEstrictaChange(false)}
          />
          <span className="ezb-mode-label">
            <strong>Mínimo requerido</strong>
            <small>El ZIP debe contener al menos estos archivos (puede tener más)</small>
          </span>
        </label>
        <label className="ezb-mode-option">
          <input
            type="radio"
            name="modoValidacion"
            checked={estricta}
            onChange={() => onEstrictaChange(true)}
          />
          <span className="ezb-mode-label">
            <strong>Estructura exacta</strong>
            <small>El ZIP debe contener exactamente estos archivos, ni más ni menos</small>
          </span>
        </label>
      </div>

      <div className="ezb-explorer">
        <div className="ezb-explorer-header">
          <span className="ezb-explorer-title">📁 / (raíz del ZIP)</span>
          <div className="ezb-explorer-actions">
            <button
              type="button"
              className="ezb-btn-add"
              onClick={() => onChange([...nodos, crearNodoArchivo()])}
              title="Añadir archivo"
            >
              + 📄 Archivo
            </button>
            <button
              type="button"
              className="ezb-btn-add"
              onClick={() => onChange([...nodos, crearNodoCarpeta()])}
              title="Añadir carpeta"
            >
              + 📁 Carpeta
            </button>
          </div>
        </div>

        <div className="ezb-tree">
          {nodos.length === 0 ? (
            <div className="ezb-empty">
              <p>No hay estructura definida. Añade archivos o carpetas.</p>
            </div>
          ) : (
            <NodoList
              nodos={nodos}
              onChange={onChange}
              nivel={0}
            />
          )}
        </div>
      </div>
    </div>
  );
};

// ── Componente recursivo para renderizar nodos ──
interface NodoListProps {
  nodos: NodoEstructuraZip[];
  onChange: (nodos: NodoEstructuraZip[]) => void;
  nivel: number;
}

const NodoList: React.FC<NodoListProps> = ({ nodos, onChange, nivel }) => {
  const actualizarNodo = (id: string, updates: Partial<NodoEstructuraZip>) => {
    onChange(nodos.map(n => (n.id === id ? { ...n, ...updates } : n)));
  };

  const eliminarNodo = (id: string) => {
    onChange(nodos.filter(n => n.id !== id));
  };

  const actualizarHijos = (id: string, nuevosHijos: NodoEstructuraZip[]) => {
    onChange(nodos.map(n => (n.id === id ? { ...n, hijos: nuevosHijos } : n)));
  };

  return (
    <div className="ezb-nodo-list">
      {nodos.map(nodo => (
        <NodoItem
          key={nodo.id}
          nodo={nodo}
          nivel={nivel}
          onUpdate={(updates) => actualizarNodo(nodo.id, updates)}
          onDelete={() => eliminarNodo(nodo.id)}
          onUpdateHijos={(hijos) => actualizarHijos(nodo.id, hijos)}
        />
      ))}
    </div>
  );
};

// ── Componente individual para cada nodo ──
interface NodoItemProps {
  nodo: NodoEstructuraZip;
  nivel: number;
  onUpdate: (updates: Partial<NodoEstructuraZip>) => void;
  onDelete: () => void;
  onUpdateHijos: (hijos: NodoEstructuraZip[]) => void;
}

const NodoItem: React.FC<NodoItemProps> = ({
  nodo,
  nivel,
  onUpdate,
  onDelete,
  onUpdateHijos,
}) => {
  const [expandido, setExpandido] = useState(true);
  const [editando, setEditando] = useState(false);
  const [nombreTemp, setNombreTemp] = useState(nodo.nombre);
  const [extensionesTemp, setExtensionesTemp] = useState(
    nodo.extensiones?.join(', ') || ''
  );
  const [extensionInput, setExtensionInput] = useState('');
  const [mostrarSugerencias, setMostrarSugerencias] = useState(false);

  const esCarpeta = nodo.tipo === 'CARPETA';
  const esNombreCualquiera = nodo.nombre === '*';

  const iniciarEdicion = () => {
    setNombreTemp(nodo.nombre);
    setExtensionesTemp(nodo.extensiones?.join(', ') || '');
    setEditando(true);
  };

  const guardarEdicion = () => {
    const nuevoNombre = nombreTemp.trim() || '*';
    if (esCarpeta) {
      onUpdate({ nombre: nuevoNombre });
    } else {
      const extensiones = extensionesTemp
        .split(',')
        .map((e: string) => e.trim().toLowerCase().replace(/^\./, ''))
        .filter((e: string) => e.length > 0);
      onUpdate({ nombre: nuevoNombre, extensiones });
    }
    setEditando(false);
  };

  const cancelarEdicion = () => {
    setNombreTemp(nodo.nombre);
    setExtensionesTemp(nodo.extensiones?.join(', ') || '');
    setEditando(false);
  };

  const agregarExtension = (ext: string) => {
    const limpia = ext.trim().toLowerCase().replace(/^\./, '');
    if (!limpia) return;
    const actuales = extensionesTemp
      .split(',')
      .map((e: string) => e.trim())
      .filter((e: string) => e.length > 0);
    if (!actuales.includes(limpia)) {
      const nuevas = [...actuales, limpia].join(', ');
      setExtensionesTemp(nuevas);
    }
    setExtensionInput('');
    setMostrarSugerencias(false);
  };

  const quitarExtension = (ext: string) => {
    const actuales = extensionesTemp
      .split(',')
      .map((e: string) => e.trim())
      .filter((e: string) => e.length > 0 && e !== ext);
    setExtensionesTemp(actuales.join(', '));
  };

  const extensionesArray = extensionesTemp
    .split(',')
    .map((e: string) => e.trim())
    .filter((e: string) => e.length > 0);

  const sugerenciasFiltradas = extensionInput.trim()
    ? EXTENSIONES_COMUNES.filter(
        e => e.includes(extensionInput.toLowerCase()) && !extensionesArray.includes(e)
      ).slice(0, 8)
    : EXTENSIONES_COMUNES.filter(e => !extensionesArray.includes(e)).slice(0, 8);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      if (editando) guardarEdicion();
    }
    if (e.key === 'Escape') {
      cancelarEdicion();
    }
  };

  const handleExtensionKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      if (extensionInput.trim()) {
        agregarExtension(extensionInput);
      }
    }
  };

  const renderNombreDisplay = () => {
    if (esCarpeta) {
      return (
        <span className="ezb-nodo-nombre">
          {esNombreCualquiera ? (
            <em className="ezb-cualquiera">* (cualquier nombre)</em>
          ) : (
            nodo.nombre
          )}
          /
        </span>
      );
    }

    const extensiones = nodo.extensiones || [];
    let extDisplay: string;
    if (extensiones.length === 0) extDisplay = '.*';
    else if (extensiones.length === 1) extDisplay = `.${extensiones[0]}`;
    else extDisplay = `.{${extensiones.join(', ')}}`;

    return (
      <span className="ezb-nodo-nombre">
        {esNombreCualquiera ? (
          <em className="ezb-cualquiera">*</em>
        ) : (
          nodo.nombre
        )}
        <span className="ezb-extension">{extDisplay}</span>
      </span>
    );
  };

  let iconoNodo = '📄';
  if (esCarpeta) {
    iconoNodo = expandido ? '📂' : '📁';
  }

  return (
    <div className="ezb-nodo" style={{ marginLeft: nivel * 20 }}>
      <div className={`ezb-nodo-row ${editando ? 'ezb-nodo-editing' : ''}`}>
        {/* Icono y toggle */}
        <div className="ezb-nodo-left">
          {esCarpeta ? (
            <button
              type="button"
              className="ezb-toggle"
              onClick={() => setExpandido(!expandido)}
            >
              {expandido ? '▾' : '▸'}
            </button>
          ) : (
            <span className="ezb-toggle-spacer" />
          )}
          <span className="ezb-nodo-icono">{iconoNodo}</span>

          {/* Nombre */}
          {editando ? (
            <div className="ezb-edit-fields" onClick={e => e.stopPropagation()}>
              <div className="ezb-edit-nombre-row">
                <label className="ezb-edit-label">Nombre:</label>
                <input
                  type="text"
                  className="ezb-edit-input"
                  value={nombreTemp}
                  onChange={e => setNombreTemp(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="* para cualquiera"
                  autoFocus
                />
                <label className="ezb-edit-cualquiera">
                  <input
                    type="checkbox"
                    checked={nombreTemp === '*'}
                    onChange={e => setNombreTemp(e.target.checked ? '*' : '')}
                  />
                  Cualquiera
                </label>
              </div>

              {!esCarpeta && (
                <div className="ezb-edit-ext-row">
                  <label className="ezb-edit-label">Extensiones:</label>
                  <div className="ezb-ext-editor">
                    <div className="ezb-ext-tags">
                      {extensionesArray.map(ext => (
                        <span key={ext} className="ezb-ext-tag">
                          .{ext}
                          <button
                            type="button"
                            className="ezb-ext-tag-remove"
                            onClick={() => quitarExtension(ext)}
                          >
                            ×
                          </button>
                        </span>
                      ))}
                      {extensionesArray.length === 0 && (
                        <span className="ezb-ext-any-badge">cualquier extensión</span>
                      )}
                    </div>
                    <div className="ezb-ext-input-wrap">
                      <input
                        type="text"
                        className="ezb-ext-input"
                        value={extensionInput}
                        onChange={e => {
                          setExtensionInput(e.target.value);
                          setMostrarSugerencias(true);
                        }}
                        onFocus={() => setMostrarSugerencias(true)}
                        onBlur={() => setTimeout(() => setMostrarSugerencias(false), 200)}
                        onKeyDown={handleExtensionKeyDown}
                        placeholder="Añadir extensión..."
                      />
                      {mostrarSugerencias && sugerenciasFiltradas.length > 0 && (
                        <div className="ezb-ext-suggestions">
                          {sugerenciasFiltradas.map(ext => (
                            <button
                              key={ext}
                              type="button"
                              className="ezb-ext-suggestion"
                              onMouseDown={e => {
                                e.preventDefault();
                                agregarExtension(ext);
                              }}
                            >
                              .{ext}
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}

              <div className="ezb-edit-actions">
                <button type="button" className="ezb-btn-save" onClick={guardarEdicion}>
                  Guardar
                </button>
                <button type="button" className="ezb-btn-cancel" onClick={cancelarEdicion}>
                  Cancelar
                </button>
              </div>
            </div>
          ) : (
            <button
              type="button"
              className="ezb-nodo-name-btn"
              onClick={iniciarEdicion}
              title="Clic para editar"
            >
              {renderNombreDisplay()}
            </button>
          )}
        </div>

        {/* Acciones */}
        {!editando && (
          <div className="ezb-nodo-actions">
            <button
              type="button"
              className="ezb-btn-edit"
              onClick={iniciarEdicion}
              title="Editar"
            >
              ✏️
            </button>
            {esCarpeta && (
              <>
                <button
                  type="button"
                  className="ezb-btn-add-child"
                  onClick={() =>
                    onUpdateHijos([...(nodo.hijos || []), crearNodoArchivo()])
                  }
                  title="Añadir archivo dentro"
                >
                  + 📄
                </button>
                <button
                  type="button"
                  className="ezb-btn-add-child"
                  onClick={() =>
                    onUpdateHijos([...(nodo.hijos || []), crearNodoCarpeta()])
                  }
                  title="Añadir carpeta dentro"
                >
                  + 📁
                </button>
              </>
            )}
            <button
              type="button"
              className="ezb-btn-delete"
              onClick={onDelete}
              title="Eliminar"
            >
              🗑️
            </button>
          </div>
        )}
      </div>

      {/* Hijos de carpeta */}
      {esCarpeta && expandido && (
        <div className="ezb-children">
          {(!nodo.hijos || nodo.hijos.length === 0) ? (
            <div className="ezb-empty-folder" style={{ marginLeft: (nivel + 1) * 20 }}>
              <em>Carpeta vacía</em>
            </div>
          ) : (
            <NodoList
              nodos={nodo.hijos || []}
              onChange={(nuevosHijos) => onUpdateHijos(nuevosHijos)}
              nivel={nivel + 1}
            />
          )}
        </div>
      )}
    </div>
  );
};

// ── Helpers ──
function crearNodoArchivo(): NodoEstructuraZip {
  return {
    id: generarId(),
    nombre: '',
    tipo: 'ARCHIVO',
    extensiones: [],
  };
}

function crearNodoCarpeta(): NodoEstructuraZip {
  return {
    id: generarId(),
    nombre: '',
    tipo: 'CARPETA',
    extensiones: [],
    hijos: [],
  };
}

export default EstructuraZipBuilder;
