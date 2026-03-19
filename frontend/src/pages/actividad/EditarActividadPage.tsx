import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ActividadDTO, CrearActividadDTO, TipoActividad, Visibilidad, GrupoDTO, ModoOneDrive } from '../../types';
import { actividadService, cursoService } from '../../services';
import { oneDriveService } from '../../services/oneDriveService';
import { useAuth } from '../../context/AuthContext';
import { OneDriveFolderBrowser } from '../../components/OneDriveFolderBrowser';
import './EditarActividadPage.css';

const EditarActividadPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { esProfesor, usuario } = useAuth();

  const [actividad, setActividad] = useState<ActividadDTO | null>(null);
  const [grupos, setGrupos] = useState<GrupoDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [eliminando, setEliminando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorGuardar, setErrorGuardar] = useState<string | null>(null);
  const [mostrarConfirmarEliminar, setMostrarConfirmarEliminar] = useState(false);

  // Variables para detectar cambio de carpeta OneDrive
  const [carpetaOriginal, setCarpetaOriginal] = useState<string | undefined>(undefined);
  const [mostrarConfirmarCambioCarpeta, setMostrarConfirmarCambioCarpeta] = useState(false);
  const [confirmacionDoble, setConfirmacionDoble] = useState(false);

  const [todosLosGrupos, setTodosLosGrupos] = useState(true);

  // OneDrive
  const [oneDriveEnabled, setOneDriveEnabled] = useState(false);
  const [oneDriveConectado, setOneDriveConectado] = useState(false);
  const [conectandoOneDrive, setConectandoOneDrive] = useState(false);

  const [formData, setFormData] = useState<CrearActividadDTO>({
    titulo: '',
    descripcion: '',
    tipoActividad: TipoActividad.EVALUABLE,
    fechaInicio: '',
    fechaLimite: '',
    visibilidad: Visibilidad.OCULTO,
    notaMaxima: undefined,
    grupoIds: [],
  });

  const cargarActividad = useCallback(async (actividadId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await actividadService.obtener(actividadId);
      setActividad(data);

      // Cargar grupos del curso
      const gruposData = await cursoService.listarGrupos(data.cursoId);
      setGrupos(gruposData);

      const fechaInicioLocal = data.fechaInicio ? toDatetimeLocal(data.fechaInicio) : '';
      const fechaLimiteLocal = data.fechaLimite ? toDatetimeLocal(data.fechaLimite) : '';

      // Determinar si tiene todos los grupos o específicos
      const tieneGruposEspecificos = data.grupoIds && data.grupoIds.length > 0 && data.grupoIds.length < gruposData.length;
      setTodosLosGrupos(!tieneGruposEspecificos);

      // Guardar carpeta original para detectar cambios
      setCarpetaOriginal(data.carpetaOneDrive);

      setFormData({
        titulo: data.titulo,
        descripcion: data.descripcion || '',
        tipoActividad: data.tipoActividad,
        fechaInicio: fechaInicioLocal,
        fechaLimite: fechaLimiteLocal,
        visibilidad: data.visibilidad,
        notaMaxima: data.notaMaxima ?? undefined,
        grupoIds: data.grupoIds || [],
        subirAOneDrive: data.subirAOneDrive || false,
        oneDriveUsuarioId: data.oneDriveUsuarioId,
        carpetaOneDrive: data.carpetaOneDrive,
        modoOneDrive: data.modoOneDrive || ModoOneDrive.ACTIVIDAD,
      });

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
      setLoading(false);
    }
  }, [usuario]);

  useEffect(() => {
    if (id) {
      cargarActividad(parseInt(id));
    }
  }, [id, cargarActividad]);

  useEffect(() => {
    if (!loading && !esProfesor) {
      navigate(-1);
    }
  }, [loading, esProfesor, navigate]);

  const toDatetimeLocal = (iso: string): string => {
    const date = new Date(iso);
    const offset = date.getTimezoneOffset();
    const local = new Date(date.getTime() - offset * 60000);
    return local.toISOString().slice(0, 16);
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;

    if (name === 'notaMaxima') {
      setFormData(prev => ({
        ...prev,
        notaMaxima: value ? parseFloat(value) : undefined,
      }));
      return;
    }

    if (name === 'tipoActividad') {
      setFormData(prev => ({
        ...prev,
        tipoActividad: value as TipoActividad,
      }));
      return;
    }

    if (name === 'visibilidad') {
      setFormData(prev => ({
        ...prev,
        visibilidad: value as Visibilidad,
      }));
      return;
    }

    if (type === 'checkbox') {
      return; // Handled by toggleGrupo
    }

    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSelectOneDriveFolder = (path: string) => {
    setFormData(prev => ({ ...prev, carpetaOneDrive: path }));
  };

  const toggleGrupo = (grupoId: number) => {
    setFormData(prev => {
      const current = prev.grupoIds || [];
      const next = current.includes(grupoId)
        ? current.filter(id => id !== grupoId)
        : [...current, grupoId];
      return { ...prev, grupoIds: next };
    });
  };

  const guardarActividad = async () => {
    if (!id) return;

    // Detectar si cambió la carpeta OneDrive
    const cambioCarpeta = formData.subirAOneDrive &&
                          carpetaOriginal &&
                          formData.carpetaOneDrive !== carpetaOriginal &&
                          formData.modoOneDrive === ModoOneDrive.ACTIVIDAD;

    if (cambioCarpeta && !mostrarConfirmarCambioCarpeta) {
      setMostrarConfirmarCambioCarpeta(true);
      return;
    }

    setGuardando(true);
    setErrorGuardar(null);

    try {
      const dataToSend: CrearActividadDTO = {
        ...formData,
        grupoIds: todosLosGrupos ? [] : formData.grupoIds,
        subirAOneDrive: formData.subirAOneDrive || false,
        oneDriveUsuarioId: formData.subirAOneDrive ? (formData.oneDriveUsuarioId || usuario?.id) : undefined,
        carpetaOneDrive: formData.subirAOneDrive ? formData.carpetaOneDrive : undefined,
        modoOneDrive: formData.subirAOneDrive ? formData.modoOneDrive : undefined,
      };
      await actividadService.actualizar(parseInt(id), dataToSend);
      navigate(`/actividades/${id}`);
    } catch (err: unknown) {
      console.error('Error al guardar:', err);
      const axiosErr = err as { response?: { data?: { message?: string; errors?: Record<string, string> } } };
      const msg =
        axiosErr?.response?.data?.message ||
        (axiosErr?.response?.data?.errors
          ? Object.values(axiosErr.response.data.errors).join(', ')
          : 'Error al guardar los cambios');
      setErrorGuardar(typeof msg === 'string' ? msg : 'Error al guardar los cambios');
    } finally {
      setGuardando(false);
    }
  };

  const handleGuardar = async (e: React.FormEvent) => {
    e.preventDefault();
    await guardarActividad();
  };

  const confirmarCambioCarpeta = () => {
    if (confirmacionDoble) {
      setMostrarConfirmarCambioCarpeta(false);
      setConfirmacionDoble(false);
      // Proceder con el guardado
      void guardarActividad();
    } else {
      setConfirmacionDoble(true);
    }
  };

  const cancelarCambioCarpeta = () => {
    setMostrarConfirmarCambioCarpeta(false);
    setConfirmacionDoble(false);
    // Restaurar carpeta original
    setFormData(prev => ({ ...prev, carpetaOneDrive: carpetaOriginal }));
  };

  const handleEliminar = async () => {
    if (!id) return;

    setEliminando(true);
    try {
      await actividadService.eliminar(parseInt(id));
      if (actividad) {
        navigate(`/cursos/${actividad.cursoId}`);
      } else {
        navigate('/dashboard');
      }
    } catch (err: unknown) {
      console.error('Error al eliminar:', err);
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setErrorGuardar(
        axiosErr?.response?.data?.message || 'Error al eliminar la actividad'
      );
      setMostrarConfirmarEliminar(false);
    } finally {
      setEliminando(false);
    }
  };

  if (loading) {
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
    <div className="editar-actividad-page">
      <div className="ea-header">
        <button onClick={() => navigate(`/actividades/${id}`)} className="btn-back">
          &larr; Volver al detalle
        </button>
        <div className="ea-header-info">
          <h1>Editar Actividad</h1>
          <span className="ea-subtitle">{actividad.cursoTitulo}</span>
        </div>
      </div>

      <form onSubmit={handleGuardar} className="ea-form">
        {errorGuardar && (
          <div className="ea-error-banner">
            {errorGuardar}
          </div>
        )}

        {/* Título */}
        <div className="ea-field">
          <label htmlFor="titulo">Título *</label>
          <input
            id="titulo"
            name="titulo"
            type="text"
            value={formData.titulo}
            onChange={handleChange}
            required
            maxLength={200}
            placeholder="Título de la actividad"
          />
        </div>

        {/* Descripción */}
        <div className="ea-field">
          <label htmlFor="descripcion">Descripción</label>
          <textarea
            id="descripcion"
            name="descripcion"
            value={formData.descripcion || ''}
            onChange={handleChange}
            rows={4}
            placeholder="Descripción de la actividad (opcional)"
          />
        </div>

        {/* Tipo de actividad y Visibilidad */}
        <div className="ea-row">
          <div className="ea-field">
            <label htmlFor="tipoActividad">Tipo de actividad *</label>
            <select
              id="tipoActividad"
              name="tipoActividad"
              value={formData.tipoActividad}
              onChange={handleChange}
              required
            >
              <option value={TipoActividad.EVALUABLE}>Evaluable</option>
              <option value={TipoActividad.NO_EVALUABLE}>No evaluable</option>
            </select>
          </div>
          <div className="ea-field">
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

        {/* Fechas */}
        <div className="ea-row">
          <div className="ea-field">
            <label htmlFor="fechaInicio">Fecha de inicio</label>
            <input
              id="fechaInicio"
              name="fechaInicio"
              type="datetime-local"
              value={formData.fechaInicio || ''}
              onChange={handleChange}
            />
          </div>
          <div className="ea-field">
            <label htmlFor="fechaLimite">Fecha límite</label>
            <input
              id="fechaLimite"
              name="fechaLimite"
              type="datetime-local"
              value={formData.fechaLimite || ''}
              onChange={handleChange}
            />
          </div>
        </div>

        {/* Nota máxima */}
        <div className="ea-row">
          <div className="ea-field">
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
          <div className="ea-field" /> {/* Spacer */}
        </div>

        {/* Grupos */}
        {grupos.length > 0 && (
          <div className="ea-field">
            <label>Grupos asignados</label>
            <div className="ea-grupo-toggle">
              <button
                type="button"
                className={`ea-grupo-toggle-btn ${todosLosGrupos ? 'active' : ''}`}
                onClick={() => setTodosLosGrupos(true)}
              >
                Todos los grupos
              </button>
              <button
                type="button"
                className={`ea-grupo-toggle-btn ${!todosLosGrupos ? 'active' : ''}`}
                onClick={() => setTodosLosGrupos(false)}
              >
                Grupos específicos
              </button>
            </div>

            {!todosLosGrupos && (
              <div className="ea-grupo-checkboxes">
                {grupos.map(grupo => (
                  <label key={grupo.id} className="ea-grupo-checkbox">
                    <input
                      type="checkbox"
                      checked={(formData.grupoIds || []).includes(grupo.id)}
                      onChange={() => toggleGrupo(grupo.id)}
                    />
                    <span className="ea-grupo-checkbox-label">{grupo.titulo}</span>
                    <span className="ea-grupo-checkbox-count">
                      {grupo.numeroEstudiantes} estudiante{grupo.numeroEstudiantes !== 1 ? 's' : ''}
                    </span>
                  </label>
                ))}
              </div>
            )}
          </div>
        )}

        {/* OneDrive */}
        {oneDriveEnabled && (
          <div className="ea-field">
            <label>Subir entregas a OneDrive</label>
            {oneDriveConectado ? (
              <div className="ea-onedrive-options">
                <label className="ea-onedrive-toggle">
                  <input
                    type="checkbox"
                    checked={formData.subirAOneDrive || false}
                    onChange={e => {
                      const checked = e.target.checked;
                      setFormData(prev => ({
                        ...prev,
                        subirAOneDrive: checked,
                        carpetaOneDrive: checked ? prev.carpetaOneDrive : '',
                        modoOneDrive: checked ? (prev.modoOneDrive || ModoOneDrive.ACTIVIDAD) : undefined,
                      }));
                    }}
                  />
                  <span>Las entregas de los estudiantes se subirán automáticamente a tu OneDrive</span>
                </label>

                {formData.subirAOneDrive && (
                  <>
                    <div className="ea-field" style={{ marginTop: '15px' }}>
                      <label style={{ marginBottom: '8px', display: 'block' }}>Modo de organización</label>
                      <select
                        name="modoOneDrive"
                        value={formData.modoOneDrive || ModoOneDrive.ACTIVIDAD}
                        onChange={e => setFormData(prev => ({
                          ...prev,
                          modoOneDrive: e.target.value as ModoOneDrive,
                          carpetaOneDrive: e.target.value === ModoOneDrive.ACTIVIDAD ? prev.carpetaOneDrive : ''
                        }))}
                        style={{ width: '100%', padding: '8px', border: '1px solid #cbd5e1', borderRadius: '6px' }}
                      >
                        <option value={ModoOneDrive.ACTIVIDAD}>Por actividad (una carpeta para toda la actividad)</option>
                        <option value={ModoOneDrive.ENTREGABLES}>Por entregables (una carpeta por cada entregable)</option>
                      </select>
                      <p style={{fontSize: '0.85rem', color: '#64748b', marginTop: '8px'}}>
                        {formData.modoOneDrive === ModoOneDrive.ACTIVIDAD
                          ? "Todas las entregas de todos los entregables se guardarán en una misma carpeta que elijas aquí."
                          : "Cada entregable tendrá su propia carpeta que elegirás al crear/editar el entregable."}
                      </p>
                    </div>

                    {formData.modoOneDrive === ModoOneDrive.ACTIVIDAD && (
                      <div className="ea-field ea-onedrive-folder-select" style={{ marginTop: '15px' }}>
                        <label style={{ marginBottom: '8px', display: 'block' }}>Carpeta de destino en OneDrive</label>
                        {usuario && (
                          <OneDriveFolderBrowser
                            usuarioId={usuario.id}
                            selectedPath={formData.carpetaOneDrive || ''}
                            onSelectFolder={handleSelectOneDriveFolder}
                          />
                        )}
                        <p style={{fontSize: '0.85rem', color: '#64748b', marginTop: '10px'}}>
                          {formData.carpetaOneDrive
                            ? `Las entregas se guardarán en "${formData.carpetaOneDrive}/[Nombre_Entregable]/[Nombre_Alumno]/"`
                            : "Las entregas se guardarán en formato Curso/Actividad/Entregable/[Nombre_Alumno]/"}
                        </p>
                      </div>
                    )}
                  </>
                )}
              </div>
            ) : (
              <div className="ea-onedrive-connect">
                <p className="ea-onedrive-msg">Conecta tu OneDrive para poder recibir las entregas directamente en tu nube.</p>
                <button
                  type="button"
                  className="ea-btn-onedrive"
                  disabled={conectandoOneDrive}
                  onClick={async () => {
                    if (!usuario) return;
                    setConectandoOneDrive(true);
                    try {
                      const ok = await oneDriveService.connectOneDrive(usuario.id);
                      if (ok) setOneDriveConectado(true);
                    } finally {
                      setConectandoOneDrive(false);
                    }
                  }}
                >
                  {conectandoOneDrive ? 'Conectando...' : 'Conectar OneDrive'}
                </button>
              </div>
            )}
          </div>
        )}

        {/* Acciones */}
        <div className="ea-actions">
          <div className="ea-actions-left">
            <button
              type="button"
              className="ea-btn-eliminar"
              onClick={() => setMostrarConfirmarEliminar(true)}
              disabled={guardando}
            >
              Eliminar actividad
            </button>
          </div>
          <div className="ea-actions-right">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => navigate(`/actividades/${id}`)}
              disabled={guardando}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={guardando}
            >
              {guardando ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </div>
        </div>
      </form>

      {/* Modal confirmar eliminación */}
      {mostrarConfirmarEliminar && (
        <div className="modal-overlay" onClick={() => setMostrarConfirmarEliminar(false)}>
          <div className="ea-modal" onClick={e => e.stopPropagation()}>
            <h2>¿Eliminar actividad?</h2>
            <p>
              Se eliminará <strong>"{actividad.titulo}"</strong> y todos sus entregables y entregas asociados.
              Esta acción no se puede deshacer.
            </p>
            {actividad.numeroEntregables > 0 && (
              <p className="ea-modal-warning">
                ⚠ Esta actividad tiene {actividad.numeroEntregables} entregable{actividad.numeroEntregables !== 1 ? 's' : ''} asociado{actividad.numeroEntregables !== 1 ? 's' : ''}.
              </p>
            )}
            <div className="ea-modal-actions">
              <button
                className="btn-secondary"
                onClick={() => setMostrarConfirmarEliminar(false)}
                disabled={eliminando}
              >
                Cancelar
              </button>
              <button
                className="ea-btn-confirmar-eliminar"
                onClick={handleEliminar}
                disabled={eliminando}
              >
                {eliminando ? 'Eliminando...' : 'Sí, eliminar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal confirmar cambio de carpeta OneDrive */}
      {mostrarConfirmarCambioCarpeta && (
        <div className="modal-overlay" onClick={cancelarCambioCarpeta}>
          <div className="ea-modal" onClick={e => e.stopPropagation()}>
            <h2>⚠ Cambio de carpeta de OneDrive</h2>
            <p>
              Estás a punto de cambiar la carpeta de destino de <strong>"{carpetaOriginal}"</strong> a <strong>"{formData.carpetaOneDrive}"</strong>.
            </p>
            <div className="ea-modal-warning" style={{backgroundColor: '#fef3c7', border: '1px solid #fbbf24', padding: '12px', borderRadius: '6px', marginTop: '12px'}}>
              <p><strong>Importante:</strong></p>
              <ul style={{marginLeft: '20px', marginTop: '8px'}}>
                <li>Los archivos de entregas ya existentes NO se moverán automáticamente</li>
                <li>Solo las NUEVAS entregas se guardarán en la nueva carpeta</li>
                <li>Tendrás que mover manualmente los archivos antiguos si lo deseas</li>
              </ul>
            </div>
            {!confirmacionDoble ? (
              <>
                <p style={{marginTop: '15px', fontWeight: 500}}>¿Estás seguro de que deseas continuar?</p>
                <div className="ea-modal-actions" style={{marginTop: '15px'}}>
                  <button
                    className="btn-secondary"
                    onClick={cancelarCambioCarpeta}
                  >
                    Cancelar
                  </button>
                  <button
                    className="btn-primary"
                    onClick={confirmarCambioCarpeta}
                    style={{backgroundColor: '#f59e0b', borderColor: '#f59e0b'}}
                  >
                    Sí, continuar
                  </button>
                </div>
              </>
            ) : (
              <>
                <p style={{marginTop: '15px', fontWeight: 600, color: '#dc2626'}}>
                  Segunda confirmación requerida: ¿Realmente deseas cambiar la carpeta sabiendo que no se moverán los archivos existentes?
                </p>
                <div className="ea-modal-actions" style={{marginTop: '15px'}}>
                  <button
                    className="btn-secondary"
                    onClick={cancelarCambioCarpeta}
                  >
                    Cancelar
                  </button>
                  <button
                    className="btn-primary"
                    onClick={confirmarCambioCarpeta}
                    style={{backgroundColor: '#dc2626', borderColor: '#dc2626'}}
                  >
                    Sí, confirmo el cambio
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default EditarActividadPage;
