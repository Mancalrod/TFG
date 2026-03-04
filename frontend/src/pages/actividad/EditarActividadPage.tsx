import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ActividadDTO, CrearActividadDTO, TipoActividad, Visibilidad, GrupoDTO } from '../../types';
import { actividadService, cursoService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import './EditarActividadPage.css';

const EditarActividadPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { esProfesor } = useAuth();

  const [actividad, setActividad] = useState<ActividadDTO | null>(null);
  const [grupos, setGrupos] = useState<GrupoDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [eliminando, setEliminando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorGuardar, setErrorGuardar] = useState<string | null>(null);
  const [mostrarConfirmarEliminar, setMostrarConfirmarEliminar] = useState(false);

  const [todosLosGrupos, setTodosLosGrupos] = useState(true);

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

      setFormData({
        titulo: data.titulo,
        descripcion: data.descripcion || '',
        tipoActividad: data.tipoActividad,
        fechaInicio: fechaInicioLocal,
        fechaLimite: fechaLimiteLocal,
        visibilidad: data.visibilidad,
        notaMaxima: data.notaMaxima ?? undefined,
        grupoIds: data.grupoIds || [],
      });
    } catch (err) {
      console.error('Error al cargar actividad:', err);
      setError('No se pudo cargar la actividad');
    } finally {
      setLoading(false);
    }
  }, []);

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

  const toggleGrupo = (grupoId: number) => {
    setFormData(prev => {
      const current = prev.grupoIds || [];
      const next = current.includes(grupoId)
        ? current.filter(id => id !== grupoId)
        : [...current, grupoId];
      return { ...prev, grupoIds: next };
    });
  };

  const handleGuardar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    setGuardando(true);
    setErrorGuardar(null);

    try {
      const dataToSend: CrearActividadDTO = {
        ...formData,
        grupoIds: todosLosGrupos ? [] : formData.grupoIds,
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
    </div>
  );
};

export default EditarActividadPage;
