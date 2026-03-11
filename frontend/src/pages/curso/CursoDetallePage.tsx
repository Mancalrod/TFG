import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { cursoService } from '../../services/cursoService';
import { actividadService } from '../../services/actividadService';
import { oneDriveService } from '../../services/oneDriveService';
import { CursoDTO, ActividadDTO, CrearActividadDTO, TipoActividad, Visibilidad } from '../../types';
import './CursoDetallePage.css';

const CursoDetallePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { esProfesor, esAdmin, usuario } = useAuth();

  const [curso, setCurso] = useState<CursoDTO | null>(null);
  const [actividades, setActividades] = useState<ActividadDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal crear actividad
  const [mostrarModal, setMostrarModal] = useState(false);
  const [creando, setCreando] = useState(false);
  const [errorCrear, setErrorCrear] = useState<string | null>(null);

  // Modal confirmar eliminación
  const [actividadAEliminar, setActividadAEliminar] = useState<ActividadDTO | null>(null);
  const [eliminando, setEliminando] = useState(false);

  // Vista previa de estudiante
  const [modoPreview, setModoPreview] = useState(false);
  const [grupoPreviewId, setGrupoPreviewId] = useState<number | null>(null);
  const [mostrarSelectorGrupo, setMostrarSelectorGrupo] = useState(false);

  // OneDrive
  const [oneDriveEnabled, setOneDriveEnabled] = useState(false);
  const [oneDriveConectado, setOneDriveConectado] = useState(false);
  const [cargandoOneDrive, setCargandoOneDrive] = useState(false);
  const [conectandoOneDrive, setConectandoOneDrive] = useState(false);

  const [formData, setFormData] = useState<CrearActividadDTO>({
    titulo: '',
    descripcion: '',
    tipoActividad: TipoActividad.EVALUABLE,
    fechaInicio: '',
    fechaLimite: '',
    visibilidad: Visibilidad.VISIBLE,
    notaMaxima: 10,
    grupoIds: [],
  });
  const [todosLosGrupos, setTodosLosGrupos] = useState(true);

  const cursoId = Number(id);

  // Cargar datos del curso y sus actividades
  useEffect(() => {
    if (!id) return;

    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [cursoData, actividadesData] = await Promise.all([
          cursoService.obtener(cursoId),
          actividadService.listarPorCurso(cursoId),
        ]);
        setCurso(cursoData);
        setActividades(actividadesData);
      } catch {
        setError('Error al cargar la información del curso.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id, cursoId]);

  // Comprobar estado de OneDrive al abrir modal de crear actividad
  useEffect(() => {
    if (!mostrarModal || !usuario) return;
    const checkOneDrive = async () => {
      setCargandoOneDrive(true);
      try {
        const enabled = await oneDriveService.isEnabled();
        setOneDriveEnabled(enabled);
        if (enabled) {
          const status = await oneDriveService.getConnectionStatus(usuario.id);
          setOneDriveConectado(status.conectado);
        }
      } catch {
        setOneDriveEnabled(false);
      } finally {
        setCargandoOneDrive(false);
      }
    };
    checkOneDrive();
  }, [mostrarModal, usuario]);

  // Crear actividad
  const handleCrearActividad = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.titulo.trim()) return;

    setCreando(true);
    setErrorCrear(null);
    try {
      const nueva = await actividadService.crear(cursoId, {
        ...formData,
        fechaInicio: formData.fechaInicio || undefined,
        fechaLimite: formData.fechaLimite || undefined,
        grupoIds: todosLosGrupos ? [] : formData.grupoIds,
        subirAOneDrive: formData.subirAOneDrive || false,
        oneDriveUsuarioId: formData.subirAOneDrive ? usuario?.id : undefined,
      });
      setActividades(prev => [nueva, ...prev]);
      setMostrarModal(false);
      resetForm();
    } catch {
      setErrorCrear('Error al crear la actividad. Inténtalo de nuevo.');
    } finally {
      setCreando(false);
    }
  };

  const resetForm = () => {
    setFormData({
      titulo: '',
      descripcion: '',
      tipoActividad: TipoActividad.EVALUABLE,
      fechaInicio: '',
      fechaLimite: '',
      visibilidad: Visibilidad.VISIBLE,
      notaMaxima: 10,
      grupoIds: [],
      subirAOneDrive: false,
    });
    setTodosLosGrupos(true);
    setErrorCrear(null);
  };

  const handleInputChange = (field: keyof CrearActividadDTO, value: string | number) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  // Eliminar actividad
  const handleConfirmarEliminar = async () => {
    if (!actividadAEliminar) return;
    setEliminando(true);
    try {
      await actividadService.eliminar(actividadAEliminar.id);
      setActividades(prev => prev.filter(a => a.id !== actividadAEliminar.id));
      setActividadAEliminar(null);
    } catch {
      alert('Error al eliminar la actividad.');
    } finally {
      setEliminando(false);
    }
  };

  if (loading) {
    return (
      <div className="cd-page">
        <div className="cd-loading">
          <div className="cd-spinner" />
          <p>Cargando curso...</p>
        </div>
      </div>
    );
  }

  if (error || !curso) {
    return (
      <div className="cd-page">
        <div className="cd-error">
          <p>{error || 'Curso no encontrado.'}</p>
          <button onClick={() => navigate('/dashboard')} className="cd-btn-back">
            ← Volver
          </button>
        </div>
      </div>
    );
  }

  const puedeCrear = (esProfesor || esAdmin) && !modoPreview;

  // En modo preview: solo actividades visibles y del grupo seleccionado
  const actividadesMostradas = modoPreview
    ? actividades.filter(act => {
      if (act.visibilidad !== 'VISIBLE') return false;
      if (act.grupoIds.length === 0) return true; // actividad para todos los grupos
      return grupoPreviewId !== null && act.grupoIds.includes(grupoPreviewId);
    })
    : actividades;

  const grupoPreview = curso?.grupos.find(g => g.id === grupoPreviewId);

  const iniciarPreview = (grupoId: number) => {
    setGrupoPreviewId(grupoId);
    setModoPreview(true);
    setMostrarSelectorGrupo(false);
  };

  const salirPreview = () => {
    setModoPreview(false);
    setGrupoPreviewId(null);
  };

  return (
    <div className={`cd-page ${modoPreview ? 'cd-preview-mode' : ''}`}>
      {/* Banner de vista previa */}
      {modoPreview && grupoPreview && (
        <div className="cd-preview-banner">
          <div className="cd-preview-banner-content">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <span>Vista previa de estudiante — <strong>{grupoPreview.titulo}</strong></span>
          </div>
          <div className="cd-preview-banner-actions">
            <button className="cd-preview-cambiar" onClick={() => setMostrarSelectorGrupo(true)}>
              Cambiar grupo
            </button>
            <button className="cd-preview-salir" onClick={salirPreview}>
              Salir de la vista previa
            </button>
          </div>
        </div>
      )}

      {/* Cabecera del curso */}
      <div className="cd-header">
        <button className="cd-back-link" onClick={() => modoPreview ? salirPreview() : navigate('/dashboard')}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          Volver
        </button>

        <div className="cd-header-content">
          <div className="cd-header-info">
            <span className="cd-codigo">{curso.codigo}</span>
            <h1>{curso.titulo}</h1>
            {curso.descripcion && <p className="cd-descripcion">{curso.descripcion}</p>}
          </div>
          <div className="cd-header-stats">
            <div className="cd-stat">
              <span className="cd-stat-value">{actividadesMostradas.length}</span>
              <span className="cd-stat-label">Actividades</span>
            </div>
            <div className="cd-stat">
              <span className="cd-stat-value">{curso.numeroEstudiantes}</span>
              <span className="cd-stat-label">Estudiantes</span>
            </div>
            <div className="cd-stat">
              <span className="cd-stat-value">{curso.grupos.length}</span>
              <span className="cd-stat-label">Grupos</span>
            </div>
            {!modoPreview && (esProfesor || esAdmin) && curso.grupos.length > 0 && (
              <button
                className="cd-btn-preview"
                onClick={() => setMostrarSelectorGrupo(true)}
                title="Ver como estudiante"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                Vista estudiante
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Sección de actividades */}
      <div className="cd-section">
        <div className="cd-section-header">
          <h2>Actividades del curso</h2>
          {puedeCrear && (
            <button className="cd-btn-crear" onClick={() => setMostrarModal(true)}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              Crear Actividad
            </button>
          )}
        </div>

        {actividadesMostradas.length === 0 ? (
          <div className="cd-empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" y1="13" x2="8" y2="13" />
              <line x1="16" y1="17" x2="8" y2="17" />
            </svg>
            <h3>No hay actividades</h3>
            <p>
              {modoPreview
                ? 'No hay actividades visibles para este grupo.'
                : puedeCrear
                  ? 'Crea la primera actividad para este curso.'
                  : 'El profesor aún no ha creado actividades para este curso.'}
            </p>
            {puedeCrear && (
              <button className="cd-btn-crear" onClick={() => setMostrarModal(true)}>
                Crear primera actividad
              </button>
            )}
          </div>
        ) : (
          <div className="cd-actividades-list">
            {actividadesMostradas.map(act => (
              <div key={act.id} className="cd-actividad-card">
                <div className="cd-actividad-left">
                  <div className="cd-actividad-badges">
                    <span className={`cd-tipo ${act.tipoActividad.toLowerCase()}`}>
                      {act.tipoActividad === 'EVALUABLE' ? 'Evaluable' : 'No evaluable'}
                    </span>
                    <span className={`cd-visibilidad ${act.visibilidad.toLowerCase()}`}>
                      {act.visibilidad === 'VISIBLE' ? '👁 Visible' : '🔒 Oculta'}
                    </span>
                    {act.fechaLimite && (() => {
                      const noIniciada = act.fechaInicio && new Date(act.fechaInicio) > new Date();
                      if (noIniciada) {
                        return (
                          <span className="cd-plazo no-iniciada">No iniciada</span>
                        );
                      }
                      return (
                        <span className={`cd-plazo ${act.enPlazo ? 'en-plazo' : 'vencida'}`}>
                          {act.enPlazo ? 'En plazo' : 'Vencida'}
                        </span>
                      );
                    })()}
                  </div>
                  <Link to={`/actividades/${act.id}`} className="cd-actividad-titulo">
                    {act.titulo}
                  </Link>
                  {act.descripcion && (
                    <p className="cd-actividad-desc">{act.descripcion}</p>
                  )}
                  <div className="cd-actividad-meta">
                    {act.fechaInicio && (
                      <span>
                        Inicio: {new Date(act.fechaInicio).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' })}
                      </span>
                    )}
                    {act.fechaLimite && (
                      <span>
                        Límite: {new Date(act.fechaLimite).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' })}
                      </span>
                    )}
                    <span>{act.numeroEntregables} entregable{act.numeroEntregables !== 1 ? 's' : ''}</span>
                    {act.notaMaxima && <span>Nota máx: {act.notaMaxima}</span>}
                  </div>
                </div>
                <div className="cd-actividad-actions">
                  <Link to={`/actividades/${act.id}`} className="cd-btn-ver">
                    Ver detalle →
                  </Link>
                  {puedeCrear && (
                    <button
                      className="cd-btn-eliminar-link"
                      onClick={() => setActividadAEliminar(act)}
                    >
                      Eliminar
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal selector de grupo para vista previa */}
      {mostrarSelectorGrupo && curso && (
        <div className="cd-modal-overlay" onClick={() => setMostrarSelectorGrupo(false)}>
          <div className="cd-modal cd-modal-sm" onClick={e => e.stopPropagation()}>
            <div className="cd-modal-header">
              <h2>Seleccionar grupo</h2>
              <button className="cd-modal-close" onClick={() => setMostrarSelectorGrupo(false)}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <div className="cd-modal-body">
              <p>Selecciona el grupo del que quieres ver la vista de estudiante:</p>
            </div>
            <div className="cd-grupo-list">
              {curso.grupos.map(grupo => (
                <button
                  key={grupo.id}
                  className={`cd-grupo-item ${grupoPreviewId === grupo.id ? 'active' : ''}`}
                  onClick={() => iniciarPreview(grupo.id)}
                >
                  <div className="cd-grupo-item-info">
                    <span className="cd-grupo-item-titulo">{grupo.titulo}</span>
                    <span className="cd-grupo-item-count">{grupo.numeroEstudiantes} estudiante{grupo.numeroEstudiantes !== 1 ? 's' : ''}</span>
                  </div>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="9 18 15 12 9 6" />
                  </svg>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Modal confirmar eliminación */}
      {actividadAEliminar && (
        <div className="cd-modal-overlay" onClick={() => !eliminando && setActividadAEliminar(null)}>
          <div className="cd-modal cd-modal-sm" onClick={e => e.stopPropagation()}>
            <div className="cd-modal-header">
              <h2>Eliminar actividad</h2>
              <button className="cd-modal-close" onClick={() => !eliminando && setActividadAEliminar(null)}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <div className="cd-modal-body">
              <p>¿Estás seguro de que quieres eliminar la actividad <strong>{actividadAEliminar.titulo}</strong>?</p>
              <p className="cd-modal-warning">Esta acción no se puede deshacer.</p>
            </div>
            <div className="cd-modal-actions cd-modal-actions-delete">
              <button
                type="button"
                className="cd-btn-cancelar"
                onClick={() => setActividadAEliminar(null)}
                disabled={eliminando}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="cd-btn-confirmar-eliminar"
                onClick={handleConfirmarEliminar}
                disabled={eliminando}
              >
                {eliminando ? 'Eliminando...' : 'Sí, eliminar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal crear actividad */}
      {mostrarModal && (
        <div className="cd-modal-overlay" onClick={() => { setMostrarModal(false); resetForm(); }}>
          <div className="cd-modal" onClick={e => e.stopPropagation()}>
            <div className="cd-modal-header">
              <h2>Crear nueva actividad</h2>
              <button className="cd-modal-close" onClick={() => { setMostrarModal(false); resetForm(); }}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>

            <form onSubmit={handleCrearActividad} className="cd-modal-form">
              {errorCrear && (
                <div className="cd-form-error">{errorCrear}</div>
              )}

              <div className="cd-form-group">
                <label>Título *</label>
                <input
                  type="text"
                  value={formData.titulo}
                  onChange={e => handleInputChange('titulo', e.target.value)}
                  placeholder="Ej: Práctica 1 - Introducción"
                  required
                />
              </div>

              <div className="cd-form-group">
                <label>Descripción</label>
                <textarea
                  value={formData.descripcion || ''}
                  onChange={e => handleInputChange('descripcion', e.target.value)}
                  placeholder="Descripción de la actividad..."
                  rows={3}
                />
              </div>

              <div className="cd-form-row">
                <div className="cd-form-group">
                  <label>Tipo de actividad</label>
                  <select
                    value={formData.tipoActividad}
                    onChange={e => handleInputChange('tipoActividad', e.target.value)}
                  >
                    <option value={TipoActividad.EVALUABLE}>Evaluable</option>
                    <option value={TipoActividad.NO_EVALUABLE}>No evaluable</option>
                  </select>
                </div>

                <div className="cd-form-group">
                  <label>Visibilidad</label>
                  <select
                    value={formData.visibilidad || Visibilidad.VISIBLE}
                    onChange={e => handleInputChange('visibilidad', e.target.value)}
                  >
                    <option value={Visibilidad.VISIBLE}>Visible</option>
                    <option value={Visibilidad.OCULTO}>Oculta</option>
                  </select>
                </div>
              </div>

              <div className="cd-form-row">
                <div className="cd-form-group">
                  <label>Fecha de inicio</label>
                  <input
                    type="datetime-local"
                    value={formData.fechaInicio || ''}
                    onChange={e => handleInputChange('fechaInicio', e.target.value)}
                  />
                </div>

                <div className="cd-form-group">
                  <label>Fecha límite</label>
                  <input
                    type="datetime-local"
                    value={formData.fechaLimite || ''}
                    onChange={e => handleInputChange('fechaLimite', e.target.value)}
                  />
                </div>
              </div>

              <div className="cd-form-group">
                <label>Nota máxima</label>
                <input
                  type="number"
                  min="0"
                  step="0.5"
                  value={formData.notaMaxima || ''}
                  onChange={e => handleInputChange('notaMaxima', parseFloat(e.target.value) || 0)}
                  placeholder="10"
                />
              </div>

              {/* Selector de grupos */}
              {curso.grupos.length > 0 && (
                <div className="cd-form-group">
                  <label>Asignar a grupos</label>
                  <div className="cd-grupo-toggle">
                    <button
                      type="button"
                      className={`cd-grupo-toggle-btn ${todosLosGrupos ? 'active' : ''}`}
                      onClick={() => { setTodosLosGrupos(true); setFormData(prev => ({ ...prev, grupoIds: [] })); }}
                    >
                      Todos los grupos
                    </button>
                    <button
                      type="button"
                      className={`cd-grupo-toggle-btn ${!todosLosGrupos ? 'active' : ''}`}
                      onClick={() => setTodosLosGrupos(false)}
                    >
                      Grupos específicos
                    </button>
                  </div>
                  {!todosLosGrupos && (
                    <div className="cd-grupo-checkboxes">
                      {curso.grupos.map(grupo => (
                        <label key={grupo.id} className="cd-grupo-checkbox">
                          <input
                            type="checkbox"
                            checked={formData.grupoIds?.includes(grupo.id) || false}
                            onChange={e => {
                              const checked = e.target.checked;
                              setFormData(prev => ({
                                ...prev,
                                grupoIds: checked
                                  ? [...(prev.grupoIds || []), grupo.id]
                                  : (prev.grupoIds || []).filter(id => id !== grupo.id),
                              }));
                            }}
                          />
                          <span className="cd-grupo-checkbox-label">
                            {grupo.titulo}
                            <span className="cd-grupo-checkbox-count">{grupo.numeroEstudiantes} est.</span>
                          </span>
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* OneDrive */}
              {(esProfesor || esAdmin) && oneDriveEnabled && !cargandoOneDrive && (
                <div className="cd-form-group">
                  <label>Subir entregas a OneDrive</label>
                  {oneDriveConectado ? (
                    <label className="cd-onedrive-toggle">
                      <input
                        type="checkbox"
                        checked={formData.subirAOneDrive || false}
                        onChange={e => setFormData(prev => ({ ...prev, subirAOneDrive: e.target.checked }))}
                      />
                      <span>Las entregas de los alumnos se subirán automáticamente a tu OneDrive</span>
                    </label>
                  ) : (
                    <div className="cd-onedrive-connect">
                      <p className="cd-onedrive-msg">Conecta tu OneDrive para poder recibir las entregas directamente en tu nube.</p>
                      <button
                        type="button"
                        className="cd-btn-onedrive"
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

              <div className="cd-modal-actions">
                <button type="button" className="cd-btn-cancelar" onClick={() => { setMostrarModal(false); resetForm(); }}>
                  Cancelar
                </button>
                <button type="submit" className="cd-btn-guardar" disabled={creando || !formData.titulo.trim()}>
                  {creando ? 'Creando...' : 'Crear Actividad'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default CursoDetallePage;
