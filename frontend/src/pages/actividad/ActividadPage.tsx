import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ActividadDTO, EntregableDTO, Visibilidad } from '../../types';
import { actividadService, entregableService, entregaService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import './ActividadPage.css';

const ActividadPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [actividad, setActividad] = useState<ActividadDTO | null>(null);
  const [entregables, setEntregables] = useState<EntregableDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [actualizandoVisibilidad, setActualizandoVisibilidad] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { esProfesor, esAdmin } = useAuth();
  const navigate = useNavigate();
  const puedeEditarContenido = esProfesor && !esAdmin;

  const alternarVisibilidadActividad = async () => {
    if (!id || !actividad || !puedeEditarContenido || actualizandoVisibilidad) return;

    const siguienteVisibilidad = actividad.visibilidad === Visibilidad.VISIBLE
      ? Visibilidad.OCULTO
      : Visibilidad.VISIBLE;
    setActualizandoVisibilidad(true);
    try {
      const actualizada = await actividadService.cambiarVisibilidad(actividad.id, siguienteVisibilidad);
      setActividad(actualizada);
    } catch (err) {
      console.error('Error al cambiar visibilidad de actividad:', err);
      setError('No se pudo cambiar la visibilidad de la actividad');
    } finally {
      setActualizandoVisibilidad(false);
    }
  };

  const handleDescargarTodoActividad = async () => {
    if (!id) return;
    try {
      const { blob, filename } = await entregaService.descargarTodoActividad(parseInt(id));
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error al descargar todo:', err);
      alert('Error al descargar las entregas de la actividad');
    }
  };

  const cargarActividad = useCallback(async (actividadId: number) => {
    setLoading(true);
    setError(null);
    
    try {
      const [actividadData, entregablesData] = await Promise.all([
        actividadService.obtenerConEntregables(actividadId),
        entregableService.listarPorActividad(actividadId)
      ]);

      if (!esProfesor && !esAdmin && actividadData.visibilidad !== 'VISIBLE') {
        setError('Esta actividad no está disponible.');
        setActividad(null);
        setEntregables([]);
        return;
      }

      const entregablesFiltrados = (!esProfesor && !esAdmin)
        ? entregablesData.filter(e => e.visibilidad === 'VISIBLE')
        : entregablesData;

      setActividad(actividadData);
      setEntregables(entregablesFiltrados);
    } catch (err) {
      setError('Error al cargar la actividad');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [esProfesor, esAdmin]);

  useEffect(() => {
    if (id) {
      cargarActividad(parseInt(id, 10));
    }
  }, [id, cargarActividad]);

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return 'Sin fecha';
    return new Date(dateStr).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const ahora = new Date();
  const fechaInicioActividad = actividad?.fechaInicio ? new Date(actividad.fechaInicio) : null;
  const actividadNoIniciada = !!fechaInicioActividad && ahora < fechaInicioActividad;
  const estadoActividadTexto = actividadNoIniciada
    ? 'No iniciada'
    : (actividad?.enPlazo ? 'En plazo' : 'Fuera de plazo');
  const estadoActividadClase = actividadNoIniciada
    ? 'no-iniciada'
    : (actividad?.enPlazo ? 'en-plazo' : 'vencido');

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
        <button onClick={() => navigate('/dashboard')} className="btn-secondary">
          Volver al Dashboard
        </button>
      </div>
    );
  }

  return (
    <div className="actividad-page">
      <div className="actividad-header">
        <button onClick={() => navigate(`/cursos/${actividad.cursoId}`)} className="btn-back">
          &larr; Volver
        </button>
        
        <div className="actividad-info">
          <h1>{actividad.titulo}</h1>
          <div className="actividad-meta">
            <span className="curso-link">{actividad.cursoTitulo}</span>
            <span className={`tipo-badge ${actividad.tipoActividad.toLowerCase()}`}>
              {actividad.tipoActividad}
            </span>
            <span className={`estado-badge ${estadoActividadClase}`}>
              {estadoActividadTexto}
            </span>
          </div>
        </div>

        {esProfesor && (
          <div className="actividad-actions">
            {puedeEditarContenido && (
              <button
                className="btn-secondary"
                onClick={alternarVisibilidadActividad}
                disabled={actualizandoVisibilidad}
              >
                {actualizandoVisibilidad
                  ? 'Actualizando...'
                  : (actividad.visibilidad === 'VISIBLE' ? 'Ocultar actividad' : 'Hacer visible actividad')}
              </button>
            )}
            {puedeEditarContenido && (
              <button 
                className="btn-secondary"
                onClick={() => navigate(`/actividades/${id}/editar`)}
              >
                Editar
              </button>
            )}
            {entregables.length > 0 && (
              <button
                className="btn-secondary btn-descargar-actividad"
                onClick={handleDescargarTodoActividad}
              >
                ⬇ Descargar Todo
              </button>
            )}
            {puedeEditarContenido && (
              <button 
                className="btn-primary"
                onClick={() => navigate(`/actividades/${id}/entregables/nuevo`)}
              >
                + Nuevo Entregable
              </button>
            )}
          </div>
        )}
      </div>

      <div className="actividad-content">
        <div className="actividad-details">
          <h2>Detalles</h2>
          <div className="detail-grid">
            <div className="detail-item">
              <label>Descripción</label>
              <p>{actividad.descripcion || 'Sin descripción'}</p>
            </div>
            <div className="detail-item">
              <label>Fecha de inicio</label>
              <p>{formatDate(actividad.fechaInicio)}</p>
            </div>
            <div className="detail-item">
              <label>Fecha límite</label>
              <p>{formatDate(actividad.fechaLimite)}</p>
            </div>
            <div className="detail-item">
              <label>Nota máxima</label>
              <p>{actividad.notaMaxima ?? 'No especificada'}</p>
            </div>
          </div>
        </div>

        <div className="entregables-section">
          <h2>Entregables ({entregables.length})</h2>
          
          {entregables.length === 0 ? (
            <div className="empty-entregables">
              <p>No hay entregables en esta actividad</p>
              {puedeEditarContenido && (
                <button 
                  className="btn-primary"
                  onClick={() => navigate(`/actividades/${id}/entregables/nuevo`)}
                >
                  Crear Primer Entregable
                </button>
              )}
            </div>
          ) : (
            <div className="entregables-list">
              {entregables.map(entregable => (
                <div 
                  key={entregable.id} 
                  className="entregable-card"
                  onClick={() => navigate(`/entregables/${entregable.id}`)}
                >
                  <div className="entregable-header">
                    <h3>{entregable.titulo}</h3>
                    <span className={`estado ${entregable.enPlazo ? 'activo' : 'vencido'}`}>
                      {entregable.enPlazo ? 'Activo' : 'Cerrado'}
                    </span>
                  </div>
                  
                  {entregable.descripcion && (
                    <p className="entregable-desc">{entregable.descripcion}</p>
                  )}
                  
                  <div className="entregable-info">
                    <span>
                      <strong>Fecha límite:</strong> {formatDate(entregable.fechaLimite)}
                    </span>
                    <span>
                      <strong>Nota máxima:</strong> {entregable.notaMaxima ?? '-'}
                    </span>
                    <span>
                      <strong>Entregas:</strong> {entregable.numeroEntregas}
                    </span>
                    {entregable.permiteReenvio && (
                      <span className="tag-reenvio">Permite reenvío</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ActividadPage;
