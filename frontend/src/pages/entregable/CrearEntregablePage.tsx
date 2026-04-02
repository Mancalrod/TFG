import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CrearEntregableDTO, Visibilidad, TipoMaterial, ActividadDTO, NodoEstructuraZip, ModoOneDrive } from '../../types';
import { entregableService, actividadService, oneDriveService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import EstructuraZipBuilder from '../../components/EstructuraZipBuilder';
import { OneDriveFolderBrowser } from '../../components/OneDriveFolderBrowser';
import './CrearEntregablePage.css';

const labelTipoMaterial = (tipo: TipoMaterial): string => {
  if (tipo === TipoMaterial.SOLO_TEXTO) {
    return 'SOLO_TEXTO (sin archivos)';
  }
  return tipo;
};

const CrearEntregablePage: React.FC = () => {
  const { actividadId } = useParams<{ actividadId: string }>();
  const navigate = useNavigate();
  const { esProfesor, esAdmin, usuario } = useAuth();

  const [actividad, setActividad] = useState<ActividadDTO | null>(null);
  const [loadingActividad, setLoadingActividad] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorGuardar, setErrorGuardar] = useState<string | null>(null);

  // OneDrive
  const [oneDriveEnabled, setOneDriveEnabled] = useState(false);
  const [oneDriveConectado, setOneDriveConectado] = useState(false);

  const [formData, setFormData] = useState<CrearEntregableDTO>({
    titulo: '',
    descripcion: '',
    fechaInicio: '',
    fechaLimite: '',
    notaMaxima: undefined,
    tipoArchivoEsperado: undefined,
    tamanoMaximoBytes: undefined,
    visibilidad: Visibilidad.OCULTO,
    permiteReenvio: true,
  });

  const [tamanoMB, setTamanoMB] = useState<string>('');
  const [estructuraZipNodos, setEstructuraZipNodos] = useState<NodoEstructuraZip[]>([]);
  const [validacionZipEstricta, setValidacionZipEstricta] = useState(false);
  const [nombreZipEsperado, setNombreZipEsperado] = useState('');

  const cargarActividad = useCallback(async (id: number) => {
    setLoadingActividad(true);
    setError(null);
    try {
      const data = await actividadService.obtener(id);
      setActividad(data);

      // Comprobar estado de OneDrive
      try {
        const enabled = await oneDriveService.isEnabled();
        setOneDriveEnabled(enabled);
        if (enabled && usuario) {
          const status = await oneDriveService.getConnectionStatus(usuario.id);
          setOneDriveConectado(status.conectado);
        }
      } catch {
        setOneDriveEnabled(false);
      }
    } catch (err) {
      console.error('Error al cargar actividad:', err);
      setError('No se pudo cargar la actividad');
    } finally {
      setLoadingActividad(false);
    }
  }, [usuario]);

  useEffect(() => {
    if (actividadId) {
      cargarActividad(parseInt(actividadId));
    }
  }, [actividadId, cargarActividad]);

  useEffect(() => {
    if (!loadingActividad && (!esProfesor || esAdmin)) {
      navigate(-1);
    }
  }, [loadingActividad, esProfesor, esAdmin, navigate]);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;

    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked;
      setFormData(prev => ({ ...prev, [name]: checked }));
      return;
    }

    if (name === 'notaMaxima') {
      setFormData(prev => ({
        ...prev,
        notaMaxima: value ? parseFloat(value) : undefined,
      }));
      return;
    }

    if (name === 'tamanoMB') {
      setTamanoMB(value);
      const mb = parseFloat(value);
      setFormData(prev => ({
        ...prev,
        tamanoMaximoBytes: !isNaN(mb) && mb > 0 ? Math.round(mb * 1024 * 1024) : undefined,
      }));
      return;
    }

    if (name === 'tipoArchivoEsperado') {
      const nuevoTipo = value ? (value as unknown as TipoMaterial) : undefined;
      setFormData(prev => ({
        ...prev,
        tipoArchivoEsperado: nuevoTipo,
        tamanoMaximoBytes: nuevoTipo === TipoMaterial.SOLO_TEXTO ? undefined : prev.tamanoMaximoBytes,
      }));
      if (nuevoTipo !== TipoMaterial.ZIP) {
        setEstructuraZipNodos([]);
        setValidacionZipEstricta(false);
        setNombreZipEsperado('');
      }
      if (nuevoTipo === TipoMaterial.SOLO_TEXTO) {
        setTamanoMB('');
      }
      return;
    }

    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSelectOneDriveFolder = (path: string) => {
    setFormData(prev => ({ ...prev, carpetaOneDrive: path }));
  };

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!actividadId) return;

    // Validar carpeta OneDrive si es obligatoria
    if (actividad?.modoOneDrive === ModoOneDrive.ENTREGABLES &&
        actividad.subirAOneDrive &&
        (!formData.carpetaOneDrive || formData.carpetaOneDrive.trim() === '')) {
      setErrorGuardar('La carpeta de OneDrive es obligatoria cuando el modo de almacenamiento de la actividad es por entregables');
      return;
    }

    setGuardando(true);
    setErrorGuardar(null);

    try {
      const datosEnvio: CrearEntregableDTO = { ...formData };

      if (formData.tipoArchivoEsperado === TipoMaterial.SOLO_TEXTO) {
        datosEnvio.tamanoMaximoBytes = undefined;
      }

      // Si el tipo esperado es ZIP y hay estructura definida, incluirla
      if (formData.tipoArchivoEsperado === TipoMaterial.ZIP && estructuraZipNodos.length > 0) {
        datosEnvio.estructuraZip = JSON.stringify(estructuraZipNodos);
        datosEnvio.validacionZipEstricta = validacionZipEstricta;
        datosEnvio.nombreZipEsperado = nombreZipEsperado.trim() || undefined;
      } else {
        datosEnvio.estructuraZip = undefined;
        datosEnvio.validacionZipEstricta = undefined;
        datosEnvio.nombreZipEsperado = undefined;
      }

      const nuevoEntregable = await entregableService.crear(parseInt(actividadId), datosEnvio);
      navigate(`/entregables/${nuevoEntregable.id}`);
    } catch (err: unknown) {
      console.error('Error al crear:', err);
      const axiosErr = err as { response?: { data?: { message?: string; errors?: Record<string, string> } } };
      const msg =
        axiosErr?.response?.data?.message ||
          axiosErr?.response?.data?.errors
          ? Object.values(axiosErr.response?.data?.errors || {}).join(', ')
          : 'Error al crear el entregable';
      setErrorGuardar(typeof msg === 'string' ? msg : 'Error al crear el entregable');
    } finally {
      setGuardando(false);
    }
  };

  if (loadingActividad) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Cargando actividad...</p>
      </div>
    );
  }

  if (error || !actividad) {
    return (
      <div className="error-container">
        <p className="error-message">{error || 'Actividad no encontrada'}</p>
        <button onClick={() => navigate(-1)} className="btn-secondary">
          Volver
        </button>
      </div>
    );
  }

  return (
    <div className="crear-entregable-page">
      <div className="ce-header">
        <button onClick={() => navigate(`/actividades/${actividadId}`)} className="btn-back">
          &larr; Volver a la actividad
        </button>
        <div className="ce-header-info">
          <h1>Nuevo Entregable</h1>
          <span className="ce-subtitle">{actividad.titulo}</span>
        </div>
      </div>

      <form onSubmit={handleCrear} className="ce-form">
        {errorGuardar && (
          <div className="ce-error-banner">
            {errorGuardar}
          </div>
        )}

        {/* Título */}
        <div className="ce-field">
          <label htmlFor="titulo">Título *</label>
          <input
            id="titulo"
            name="titulo"
            type="text"
            value={formData.titulo}
            onChange={handleChange}
            required
            maxLength={200}
            placeholder="Título del entregable"
          />
        </div>

        {/* Descripción */}
        <div className="ce-field">
          <label htmlFor="descripcion">Descripción</label>
          <textarea
            id="descripcion"
            name="descripcion"
            value={formData.descripcion || ''}
            onChange={handleChange}
            rows={4}
            placeholder="Descripción del entregable (opcional)"
          />
        </div>

        {/* Fechas */}
        <div className="ce-row">
          <div className="ce-field">
            <label htmlFor="fechaInicio">Fecha de inicio</label>
            <input
              id="fechaInicio"
              name="fechaInicio"
              type="datetime-local"
              value={formData.fechaInicio || ''}
              onChange={handleChange}
            />
          </div>
          <div className="ce-field">
            <label htmlFor="fechaLimite">Fecha límite *</label>
            <input
              id="fechaLimite"
              name="fechaLimite"
              type="datetime-local"
              value={formData.fechaLimite || ''}
              onChange={handleChange}
              required
            />
          </div>
        </div>

        {/* Nota máxima y Visibilidad */}
        <div className="ce-row">
          <div className="ce-field">
            <label htmlFor="notaMaxima">Nota máxima</label>
            <input
              id="notaMaxima"
              name="notaMaxima"
              type="number"
              step="0.1"
              min="0"
              value={formData.notaMaxima ?? ''}
              onChange={handleChange}
              placeholder="Ej: 10"
            />
          </div>
          <div className="ce-field">
            <label htmlFor="visibilidad">Visibilidad</label>
            <select
              id="visibilidad"
              name="visibilidad"
              value={formData.visibilidad}
              onChange={handleChange}
            >
              <option value={Visibilidad.VISIBLE}>Visible</option>
              <option value={Visibilidad.OCULTO}>Oculto</option>
            </select>
          </div>
        </div>

        {/* Tipo de archivo y Tamaño máximo */}
        <div className="ce-row">
          <div className="ce-field">
            <label htmlFor="tipoArchivoEsperado">Tipo de archivo esperado</label>
            <select
              id="tipoArchivoEsperado"
              name="tipoArchivoEsperado"
              value={(formData.tipoArchivoEsperado as string) || ''}
              onChange={handleChange}
            >
              <option value="">Cualquiera</option>
              {Object.values(TipoMaterial).map(tipo => (
                <option key={tipo} value={tipo}>
                  {labelTipoMaterial(tipo)}
                </option>
              ))}
            </select>
            {formData.tipoArchivoEsperado === TipoMaterial.SOLO_TEXTO && (
              <p className="ce-help-text">
                El alumno solo podrá enviar texto en el comentario, sin archivos adjuntos.
              </p>
            )}
          </div>
          {formData.tipoArchivoEsperado !== TipoMaterial.SOLO_TEXTO && (
            <div className="ce-field">
              <label htmlFor="tamanoMB">Tamaño máximo (MB)</label>
              <input
                id="tamanoMB"
                name="tamanoMB"
                type="number"
                step="0.1"
                min="0"
                value={tamanoMB}
                onChange={handleChange}
                placeholder="Ej: 10"
              />
            </div>
          )}
        </div>

        {/* Estructura del ZIP (solo cuando tipo = ZIP) */}
        {formData.tipoArchivoEsperado === TipoMaterial.ZIP && (
          <div className="ce-field">
            <EstructuraZipBuilder
              nodos={estructuraZipNodos}
              onChange={setEstructuraZipNodos}
              estricta={validacionZipEstricta}
              onEstrictaChange={setValidacionZipEstricta}
              nombreZipEsperado={nombreZipEsperado}
              onNombreZipChange={setNombreZipEsperado}
            />
          </div>
        )}

        {/* Carpeta OneDrive (solo si el modo es ENTREGABLES) */}
        {oneDriveEnabled && actividad?.modoOneDrive === ModoOneDrive.ENTREGABLES && (
          <div className="ce-field">
            <label htmlFor="carpetaOneDrive">
              Carpeta de destino en OneDrive {actividad.subirAOneDrive && '*'}
            </label>
            {oneDriveConectado ? (
              <>
                {usuario && (
                  <OneDriveFolderBrowser
                    usuarioId={usuario.id}
                    selectedPath={formData.carpetaOneDrive || ''}
                    onSelectFolder={handleSelectOneDriveFolder}
                  />
                )}
                <p className="ce-help-text">
                  {formData.carpetaOneDrive
                    ? `Las entregas se guardarán en "${formData.carpetaOneDrive}/[Nombre_Alumno]/"`
                    : "Selecciona una carpeta donde se guardarán las entregas de los estudiantes"}
                </p>
                {!formData.carpetaOneDrive && actividad.subirAOneDrive && (
                  <p className="ce-help-text" style={{color: '#ef4444', marginTop: '5px'}}>
                    La carpeta es obligatoria cuando el modo de almacenamiento de la actividad es por entregables
                  </p>
                )}
              </>
            ) : (
              <p className="ce-help-text">
                Debes conectar tu OneDrive para seleccionar una carpeta de destino.
              </p>
            )}
          </div>
        )}

        {/* Permite reenvío */}
        <div className="ce-field ce-checkbox-field">
          <label className="ce-checkbox-label">
            <input
              type="checkbox"
              name="permiteReenvio"
              checked={formData.permiteReenvio ?? true}
              onChange={handleChange}
            />
            <span>Permite reenvío</span>
          </label>
          <p className="ce-help-text">
            Si está activado, los estudiantes pueden enviar nuevas versiones de su entrega.
          </p>
        </div>

        {/* Acciones del formulario */}
        <div className="ce-actions">
          <div className="ce-actions-right">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => navigate(`/actividades/${actividadId}`)}
              disabled={guardando}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={guardando}
            >
              {guardando ? 'Creando...' : 'Crear entregable'}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default CrearEntregablePage;


