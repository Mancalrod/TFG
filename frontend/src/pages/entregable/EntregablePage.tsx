import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { EntregableDTO, EntregaDTO, EntregaResumenDTO, EntregaEstadisticasDTO } from '../../types';
import { entregableService, entregaService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import './EntregablePage.css';

const EntregablePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [entregable, setEntregable] = useState<EntregableDTO | null>(null);
  const [entregas, setEntregas] = useState<EntregaResumenDTO[]>([]);
  const [misEntregas, setMisEntregas] = useState<EntregaDTO[]>([]);
  const [estadisticas, setEstadisticas] = useState<EntregaEstadisticasDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const { esProfesor, usuario } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (id) {
      cargarEntregable(parseInt(id));
    }
  }, [id, esProfesor]);

  const cargarEntregable = async (entregableId: number) => {
    setLoading(true);
    try {
      const entregableData = await entregableService.obtener(entregableId);
      setEntregable(entregableData);

      if (esProfesor) {
        const [entregasData, stats] = await Promise.all([
          entregaService.listarParaEvaluar(entregableId),
          entregaService.obtenerEstadisticas(entregableId)
        ]);
        setEntregas(entregasData);
        setEstadisticas(stats);
      } else if (usuario?.id) {
        // Cargar entregas del estudiante
        const misEntregasData = await entregaService.listarHistorial(entregableId, usuario.id);
        setMisEntregas(misEntregasData);
      }
    } catch (err) {
      console.error('Error al cargar entregable:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCloseModal = () => {
    setShowUploadModal(false);
    // Recargar datos después de cerrar el modal
    if (id) {
      cargarEntregable(parseInt(id));
    }
  };

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

  const formatFileSize = (bytes: number | undefined) => {
    if (!bytes) return '-';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Cargando entregable...</p>
      </div>
    );
  }

  if (!entregable) {
    return (
      <div className="error-container">
        <p>Entregable no encontrado</p>
        <button onClick={() => navigate(-1)} className="btn-secondary">
          Volver
        </button>
      </div>
    );
  }

  return (
    <div className="entregable-page">
      <div className="entregable-header">
        <button onClick={() => navigate(-1)} className="btn-back">
          &larr; Volver
        </button>
        
        <div className="entregable-info">
          <h1>{entregable.titulo}</h1>
          <div className="entregable-meta">
            <span className="actividad-name">{entregable.actividadTitulo}</span>
            <span className={`estado-badge ${entregable.enPlazo ? 'en-plazo' : 'vencido'}`}>
              {entregable.enPlazo ? 'Abierto' : 'Cerrado'}
            </span>
            {entregable.permiteReenvio && (
              <span className="reenvio-badge">Permite reenvío</span>
            )}
          </div>
        </div>

        {esProfesor && (
          <button 
            className="btn-secondary"
            onClick={() => navigate(`/entregables/${id}/editar`)}
          >
            Editar
          </button>
        )}

        {!esProfesor && entregable.enPlazo && (
          <button 
            className="btn-primary"
            onClick={() => setShowUploadModal(true)}
          >
            Realizar Entrega
          </button>
        )}
      </div>

      <div className="entregable-content">
        <div className="info-panel">
          <h2>Información</h2>
          <div className="info-grid">
            <div className="info-item">
              <label>Descripción</label>
              <p>{entregable.descripcion || 'Sin descripción'}</p>
            </div>
            <div className="info-item">
              <label>Fecha inicio</label>
              <p>{formatDate(entregable.fechaInicio)}</p>
            </div>
            <div className="info-item">
              <label>Fecha límite</label>
              <p className={!entregable.enPlazo ? 'text-danger' : ''}>
                {formatDate(entregable.fechaLimite)}
              </p>
            </div>
            <div className="info-item">
              <label>Nota máxima</label>
              <p>{entregable.notaMaxima ?? 'No especificada'}</p>
            </div>
            <div className="info-item">
              <label>Tipo archivo esperado</label>
              <p>{entregable.tipoArchivoEsperado || 'Cualquiera'}</p>
            </div>
            <div className="info-item">
              <label>Tamaño máximo</label>
              <p>{formatFileSize(entregable.tamanoMaximoBytes)}</p>
            </div>
          </div>
        </div>

        {esProfesor && estadisticas && (
          <div className="stats-panel">
            <h2>Estadísticas</h2>
            <div className="stats-grid">
              <div className="stat-card">
                <span className="stat-value">{estadisticas.totalEntregas}</span>
                <span className="stat-label">Total Entregas</span>
              </div>
              <div className="stat-card success">
                <span className="stat-value">{estadisticas.entregasATiempo}</span>
                <span className="stat-label">A Tiempo</span>
              </div>
              <div className="stat-card warning">
                <span className="stat-value">{estadisticas.entregasTardias}</span>
                <span className="stat-label">Tardías</span>
              </div>
              <div className="stat-card info">
                <span className="stat-value">{estadisticas.entregasCalificadas}</span>
                <span className="stat-label">Calificadas</span>
              </div>
              <div className="stat-card">
                <span className="stat-value">{estadisticas.entregasPendientes}</span>
                <span className="stat-label">Pendientes</span>
              </div>
              <div className="stat-card primary">
                <span className="stat-value">
                  {estadisticas.promedioCalificacion?.toFixed(2) ?? '-'}
                </span>
                <span className="stat-label">Promedio</span>
              </div>
            </div>
          </div>
        )}

        {esProfesor && (
          <div className="entregas-panel">
            <h2>Entregas Recibidas ({entregas.length})</h2>
            {entregas.length === 0 ? (
              <p className="no-entregas">No se han recibido entregas aún</p>
            ) : (
              <table className="entregas-table">
                <thead>
                  <tr>
                    <th>Estudiante</th>
                    <th>Grupo</th>
                    <th>Fecha</th>
                    <th>Estado</th>
                    <th>Calificación</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {entregas.map(entrega => (
                    <tr key={entrega.entregaId}>
                      <td>
                        <div className="estudiante-info">
                          <span className="nombre">{entrega.estudianteNombre}</span>
                          <span className="correo">{entrega.estudianteCorreo}</span>
                        </div>
                      </td>
                      <td>{entrega.grupoTitulo}</td>
                      <td>
                        <span className={entrega.fueATiempo ? 'text-success' : 'text-danger'}>
                          {formatDate(entrega.fechaEntrega)}
                        </span>
                      </td>
                      <td>
                        <span className={`badge ${entrega.estado.toLowerCase()}`}>
                          {entrega.estado}
                        </span>
                      </td>
                      <td>{entrega.calificacion ?? '-'}</td>
                      <td>
                        <button 
                          className="btn-sm"
                          onClick={() => navigate(`/entregas/${entrega.entregaId}`)}
                        >
                          Ver
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {/* Sección para estudiantes: mis entregas */}
        {!esProfesor && misEntregas.length > 0 && (
          <div className="mis-entregas-panel">
            <h2>Mis Entregas ({misEntregas.length})</h2>
            <ul className="mis-entregas-list">
              {misEntregas.map(entrega => (
                <li key={entrega.id} className="mi-entrega-item">
                  <span className="version">v{entrega.version}</span>
                  <span className="fecha">{formatDate(entrega.fechaEntrega)}</span>
                  <span className={`estado ${entrega.estado.toLowerCase()}`}>{entrega.estado}</span>
                  {entrega.calificacion !== undefined && (
                    <span className="calificacion">{entrega.calificacion} pts</span>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Modal de entrega */}
        {showUploadModal && (
          <div className="modal-overlay" onClick={handleCloseModal}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
              <h2>Realizar Entrega</h2>
              <p>Aquí iría el formulario de subida de archivos.</p>
              <button className="btn-secondary" onClick={handleCloseModal}>
                Cerrar
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default EntregablePage;
