import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CrearEntregableDTO, Visibilidad, TipoMaterial, ActividadDTO } from '../../types';
import { entregableService, actividadService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import './CrearEntregablePage.css';

const CrearEntregablePage: React.FC = () => {
  const { actividadId } = useParams<{ actividadId: string }>();
  const navigate = useNavigate();
  const { esProfesor } = useAuth();

  const [actividad, setActividad] = useState<ActividadDTO | null>(null);
  const [loadingActividad, setLoadingActividad] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorGuardar, setErrorGuardar] = useState<string | null>(null);

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

  useEffect(() => {
    if (actividadId) {
      cargarActividad(parseInt(actividadId));
    }
  }, [actividadId]);

  useEffect(() => {
    if (!loadingActividad && !esProfesor) {
      navigate(-1);
    }
  }, [loadingActividad, esProfesor, navigate]);

  const cargarActividad = async (id: number) => {
    setLoadingActividad(true);
    setError(null);
    try {
      const data = await actividadService.obtener(id);
      setActividad(data);
    } catch (err) {
      console.error('Error al cargar actividad:', err);
      setError('No se pudo cargar la actividad');
    } finally {
      setLoadingActividad(false);
    }
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

  const handleCrear = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!actividadId) return;

    setGuardando(true);
    setErrorGuardar(null);

    try {
      const nuevoEntregable = await entregableService.crear(parseInt(actividadId), formData);
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
                  {tipo}
                </option>
              ))}
            </select>
          </div>
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
        </div>

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
