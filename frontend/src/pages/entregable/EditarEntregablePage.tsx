import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { EntregableDTO, CrearEntregableDTO, Visibilidad, TipoMaterial } from '../../types';
import { entregableService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import './EditarEntregablePage.css';

const EditarEntregablePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { esProfesor } = useAuth();

  const [entregable, setEntregable] = useState<EntregableDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [eliminando, setEliminando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorGuardar, setErrorGuardar] = useState<string | null>(null);
  const [mostrarConfirmarEliminar, setMostrarConfirmarEliminar] = useState(false);

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

  // Para el selector de tamaño en MB
  const [tamanoMB, setTamanoMB] = useState<string>('');

  const cargarEntregable = useCallback(async (entregableId: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await entregableService.obtener(entregableId);
      setEntregable(data);

      // Convertir fechas ISO al formato datetime-local
      const fechaInicioLocal = data.fechaInicio
        ? toDatetimeLocal(data.fechaInicio)
        : '';
      const fechaLimiteLocal = data.fechaLimite
        ? toDatetimeLocal(data.fechaLimite)
        : '';

      setFormData({
        titulo: data.titulo,
        descripcion: data.descripcion || '',
        fechaInicio: fechaInicioLocal,
        fechaLimite: fechaLimiteLocal,
        notaMaxima: data.notaMaxima ?? undefined,
        tipoArchivoEsperado: data.tipoArchivoEsperado as unknown as TipoMaterial | undefined,
        tamanoMaximoBytes: data.tamanoMaximoBytes ?? undefined,
        visibilidad: data.visibilidad,
        permiteReenvio: data.permiteReenvio,
      });

      if (data.tamanoMaximoBytes) {
        setTamanoMB((data.tamanoMaximoBytes / (1024 * 1024)).toFixed(1));
      }
    } catch (err) {
      console.error('Error al cargar entregable:', err);
      setError('No se pudo cargar el entregable');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (id) {
      cargarEntregable(parseInt(id));
    }
  }, [id, cargarEntregable]);

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
      setFormData(prev => ({
        ...prev,
        tipoArchivoEsperado: value ? (value as unknown as TipoMaterial) : undefined,
      }));
      return;
    }

    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleGuardar = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;

    setGuardando(true);
    setErrorGuardar(null);

    try {
      await entregableService.actualizar(parseInt(id), formData);
      navigate(`/entregables/${id}`);
    } catch (err: unknown) {
      console.error('Error al guardar:', err);
      const axiosErr = err as { response?: { data?: { message?: string; errors?: Record<string, string> } } };
      const msg =
        axiosErr?.response?.data?.message ||
          axiosErr?.response?.data?.errors
          ? Object.values(axiosErr.response?.data?.errors || {}).join(', ')
          : 'Error al guardar los cambios';
      setErrorGuardar(typeof msg === 'string' ? msg : 'Error al guardar los cambios');
    } finally {
      setGuardando(false);
    }
  };

  const handleEliminar = async () => {
    if (!id) return;

    setEliminando(true);
    try {
      await entregableService.eliminar(parseInt(id));
      // Navegar a la actividad del entregable
      if (entregable) {
        navigate(`/actividades/${entregable.actividadId}`);
      } else {
        navigate(-1);
      }
    } catch (err: unknown) {
      console.error('Error al eliminar:', err);
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setErrorGuardar(
        axiosErr?.response?.data?.message || 'Error al eliminar el entregable'
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

  return (
    <div className="editar-entregable-page">
      <div className="ee-header">
        <button onClick={() => navigate(`/entregables/${id}`)} className="btn-back">
          &larr; Volver al detalle
        </button>
        <div className="ee-header-info">
          <h1>Editar Entregable</h1>
          <span className="ee-subtitle">{entregable.actividadTitulo}</span>
        </div>
      </div>

      <form onSubmit={handleGuardar} className="ee-form">
        {errorGuardar && (
          <div className="ee-error-banner">
            {errorGuardar}
          </div>
        )}

        {/* Título */}
        <div className="ee-field">
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
        <div className="ee-field">
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
        <div className="ee-row">
          <div className="ee-field">
            <label htmlFor="fechaInicio">Fecha de inicio</label>
            <input
              id="fechaInicio"
              name="fechaInicio"
              type="datetime-local"
              value={formData.fechaInicio || ''}
              onChange={handleChange}
            />
          </div>
          <div className="ee-field">
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
        <div className="ee-row">
          <div className="ee-field">
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
          <div className="ee-field">
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
        <div className="ee-row">
          <div className="ee-field">
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
                  {tipo}
                </option>
              ))}
            </select>
          </div>
          <div className="ee-field">
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
        </div>

        {/* Permite reenvío */}
        <div className="ee-field ee-checkbox-field">
          <label className="ee-checkbox-label">
            <input
              type="checkbox"
              name="permiteReenvio"
              checked={formData.permiteReenvio ?? true}
              onChange={handleChange}
            />
            <span>Permite reenvío</span>
          </label>
          <p className="ee-help-text">
            Si está activado, los estudiantes pueden enviar nuevas versiones de su entrega.
          </p>
        </div>

        {/* Acciones del formulario */}
        <div className="ee-actions">
          <div className="ee-actions-left">
            <button
              type="button"
              className="ee-btn-eliminar"
              onClick={() => setMostrarConfirmarEliminar(true)}
              disabled={guardando}
            >
              Eliminar entregable
            </button>
          </div>
          <div className="ee-actions-right">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => navigate(`/entregables/${id}`)}
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
          <div className="ee-modal" onClick={e => e.stopPropagation()}>
            <h2>¿Eliminar entregable?</h2>
            <p>
              Se eliminará <strong>"{entregable.titulo}"</strong> y todas sus entregas asociadas.
              Esta acción no se puede deshacer.
            </p>
            {entregable.numeroEntregas > 0 && (
              <p className="ee-modal-warning">
                ⚠ Este entregable tiene {entregable.numeroEntregas} entrega{entregable.numeroEntregas !== 1 ? 's' : ''} asociada{entregable.numeroEntregas !== 1 ? 's' : ''}.
              </p>
            )}
            <div className="ee-modal-actions">
              <button
                className="btn-secondary"
                onClick={() => setMostrarConfirmarEliminar(false)}
                disabled={eliminando}
              >
                Cancelar
              </button>
              <button
                className="ee-btn-confirmar-eliminar"
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

export default EditarEntregablePage;
