import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { EntregableDTO, EntregaDTO, EntregaResumenDTO, EntregaEstadisticasDTO, NodoEstructuraZip } from '../../types';
import { entregableService, entregaService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import './EntregablePage.css';

const descargarTodo = async (entregableId: number) => {
  try {
    const { blob, filename } = await entregaService.descargarTodo(entregableId);
    const url = globalThis.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    globalThis.URL.revokeObjectURL(url);
  } catch (err) {
    console.error('Error al descargar todo:', err);
    alert('Error al descargar las entregas');
  }
};

const EntregablePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [entregable, setEntregable] = useState<EntregableDTO | null>(null);
  const [entregas, setEntregas] = useState<EntregaResumenDTO[]>([]);
  const [misEntregas, setMisEntregas] = useState<EntregaDTO[]>([]);
  const [estadisticas, setEstadisticas] = useState<EntregaEstadisticasDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [actualizandoVisibilidadNotas, setActualizandoVisibilidadNotas] = useState(false);
  const { esProfesor, esAdmin, usuario } = useAuth();
  const navigate = useNavigate();
  const puedeEditarEntregable = esProfesor && !esAdmin;

  const cargarEntregable = useCallback(async (entregableId: number) => {
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
  }, [esProfesor, usuario?.id]);

  useEffect(() => {
    if (id) {
      cargarEntregable(Number.parseInt(id, 10));
    }
  }, [id, cargarEntregable]);

  const toggleNotasVisibles = async () => {
    if (!entregable || actualizandoVisibilidadNotas) return;
    setActualizandoVisibilidadNotas(true);
    try {
      const actualizado = await entregableService.cambiarVisibilidadNotas(
        entregable.id,
        !entregable.notasVisiblesEstudiante,
      );
      setEntregable(actualizado);
      if (esProfesor) {
        const entregasData = await entregaService.listarParaEvaluar(actualizado.id);
        setEntregas(entregasData);
      } else if (usuario?.id) {
        const misEntregasData = await entregaService.listarHistorial(actualizado.id, usuario.id);
        setMisEntregas(misEntregasData);
      }
    } catch (err) {
      console.error('Error al cambiar visibilidad de notas:', err);
      alert('No se pudo actualizar la visibilidad de las notas.');
    } finally {
      setActualizandoVisibilidadNotas(false);
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

  const formatearNota = (entrega: EntregaDTO | EntregaResumenDTO) => {
    if (entrega.calificacion === undefined || entrega.calificacion === null) {
      return 'Sin evaluar';
    }
    const notaMaximaEntrega = 'notaMaximaEntregable' in entrega
      ? entrega.notaMaximaEntregable
      : undefined;
    const notaMaxima = notaMaximaEntrega ?? entregable?.notaMaxima;
    if (notaMaxima === undefined || notaMaxima === null) {
      return `${entrega.calificacion}`;
    }
    return `${entrega.calificacion}/${notaMaxima}`;
  };

  const puedeVerNotaAlumno = (entrega: EntregaDTO) => {
    if (esProfesor) return true;
    return entrega.estado === 'PUBLICADO' || Boolean(entrega.notasVisiblesEstudiante) || Boolean(entregable?.notasVisiblesEstudiante);
  };

  const formatFileSize = (bytes: number | undefined) => {
    if (!bytes) return '-';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const formatTipoEsperado = (tipoArchivoEsperado?: string) => {
    if (!tipoArchivoEsperado) return 'Cualquiera';
    if (tipoArchivoEsperado === 'SOLO_TEXTO') return 'SOLO TEXTO (sin archivos)';
    if (tipoArchivoEsperado === 'ENLACE') return 'ENLACE (en comentario, sin archivos)';
    return tipoArchivoEsperado;
  };

  const renderEstructuraZip = (nodos: NodoEstructuraZip[], nivel: number): React.ReactNode => (
    <div className="ep-zip-nodo-list">
      {nodos.map(nodo => {
        const esCarpeta = nodo.tipo === 'CARPETA';
        const esWild = nodo.nombre === '*';
        const extensiones = nodo.extensiones || [];
        let extDisplay = '';
        if (!esCarpeta) {
          if (extensiones.length === 0) extDisplay = '.*';
          else if (extensiones.length === 1) extDisplay = `.${extensiones[0]}`;
          else extDisplay = `.{${extensiones.join(', ')}}`;
        }
        return (
          <div key={nodo.id} className="ep-zip-nodo" style={{ paddingLeft: nivel * 18 }}>
            <span>{esCarpeta ? '📁' : '📄'}</span>
            <span className="ep-zip-nodo-name">
              {esWild ? <em>*</em> : nodo.nombre}
              {!esCarpeta && <span className="ep-zip-nodo-ext">{extDisplay}</span>}
              {esCarpeta && '/'}
            </span>
            {esCarpeta && nodo.hijos && nodo.hijos.length > 0 && renderEstructuraZip(nodo.hijos, nivel + 1)}
          </div>
        );
      })}
    </div>
  );

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

  let textoBotonVisibilidadNotas = 'Mostrar notas al alumnado';
  if (actualizandoVisibilidadNotas) {
    textoBotonVisibilidadNotas = 'Actualizando...';
  } else if (entregable.notasVisiblesEstudiante) {
    textoBotonVisibilidadNotas = 'Ocultar notas al alumnado';
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
            {esProfesor && (
              <span className="reenvio-badge">
                {entregable.notasVisiblesEstudiante ? 'Notas visibles al alumnado' : 'Notas ocultas al alumnado'}
              </span>
            )}
          </div>
        </div>

        {puedeEditarEntregable && (
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
            onClick={() => navigate(`/entregables/${id}/entregar`)}
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
              <p>{formatTipoEsperado(entregable.tipoArchivoEsperado)}</p>
            </div>
            <div className="info-item">
              <label>Tamaño máximo</label>
              <p>{formatFileSize(entregable.tamanoMaximoBytes)}</p>
            </div>
          </div>

          {/* Estructura ZIP esperada */}
          {(entregable.estructuraZip || entregable.nombreZipEsperado) && (() => {
            let nodos: NodoEstructuraZip[] = [];
            if (entregable.estructuraZip) {
              try { nodos = JSON.parse(entregable.estructuraZip); } catch { nodos = []; }
            }
            if (nodos.length === 0 && !entregable.nombreZipEsperado) return null;
            return (
              <div className="ep-zip-structure">
                <h3>📦 Estructura esperada del ZIP</h3>
                <span className={`ep-zip-mode ${entregable.validacionZipEstricta ? 'estricta' : 'minima'}`}>
                  {entregable.validacionZipEstricta ? 'Estructura exacta' : 'Mínimo requerido'}
                </span>
                {entregable.nombreZipEsperado && entregable.nombreZipEsperado !== '*' && (
                  <div className="ep-zip-nombre">
                    <span className="ep-zip-nombre-label">Nombre esperado:</span>
                    <code className="ep-zip-nombre-value">{entregable.nombreZipEsperado}.zip</code>
                  </div>
                )}
                {nodos.length > 0 && (
                  <div className="ep-zip-tree">
                    {renderEstructuraZip(nodos, 0)}
                  </div>
                )}
              </div>
            );
          })()}
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
            <div className="entregas-panel-header">
              <h2>Entregas Recibidas ({entregas.length})</h2>
              <button
                className="btn-secondary"
                onClick={toggleNotasVisibles}
                disabled={actualizandoVisibilidadNotas}
              >
                {textoBotonVisibilidadNotas}
              </button>
              {entregas.length > 0 && (
                <button
                  className="btn-secondary btn-descargar-todo"
                  onClick={() => descargarTodo(Number.parseInt(id!, 10))}
                >
                  ⬇ Descargar Todo
                </button>
              )}
            </div>
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
                      <td>{formatearNota(entrega)}</td>
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
                <li key={entrega.id}>
                  <button
                    type="button"
                    className="mi-entrega-item mi-entrega-clickable"
                    onClick={() => navigate(`/entregas/${entrega.id}`)}
                  >
                    <div className="mi-entrega-info">
                      <span className="version">v{entrega.version}</span>
                      <span className="fecha">{formatDate(entrega.fechaEntrega)}</span>
                      <span className={`estado ${entrega.estado.toLowerCase()}`}>{entrega.estado}</span>
                      {puedeVerNotaAlumno(entrega) && entrega.calificacion !== undefined && entrega.calificacion !== null ? (
                        <span className="calificacion">{formatearNota(entrega)}</span>
                      ) : (
                        <span className="calificacion">Sin evaluar</span>
                      )}
                    </div>
                    <span className="mi-entrega-arrow">→</span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

      </div>
    </div>
  );
};

export default EntregablePage;
