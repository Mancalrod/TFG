import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { entregaService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import { EntregaResumenDTO } from '../../types';
import './EvaluacionesPage.css';

type OrdenEntrega = 'prioridad' | 'recientes' | 'antiguas' | 'alumno';
type PageSize = 25 | 50 | 100;

type ResumenEntregable = {
  entregableId: number;
  actividadId: number;
  cursoId: number;
  entregableTitulo: string;
  total: number;
  tardias: number;
  masAntigua: string;
};

type SelectorVisual = {
  id: number;
  titulo: string;
  total: number;
  tardias: number;
};

const EvaluacionesPage: React.FC = () => {
  const navigate = useNavigate();
  const { usuario, esProfesor } = useAuth();

  const [entregas, setEntregas] = useState<EntregaResumenDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busqueda, setBusqueda] = useState('');
  const [cursoFiltro, setCursoFiltro] = useState('todos');
  const [actividadFiltro, setActividadFiltro] = useState('todos');
  const [entregableFiltro, setEntregableFiltro] = useState('todos');
  const [soloTardias, setSoloTardias] = useState(false);
  const [orden, setOrden] = useState<OrdenEntrega>('prioridad');
  const [pageSize, setPageSize] = useState<PageSize>(25);
  const [paginaActual, setPaginaActual] = useState(1);
  const [ultimaActualizacion, setUltimaActualizacion] = useState<Date | null>(null);
  const [descargandoZip, setDescargandoZip] = useState(false);

  const cargarPendientes = useCallback(async (silent = false) => {
    if (!usuario?.id || !esProfesor) {
      setLoading(false);
      return;
    }

    if (silent) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    setError(null);
    try {
      const data = await entregaService.listarPendientesCalificar(usuario.id);
      setEntregas(data);
      setUltimaActualizacion(new Date());
    } catch (err) {
      console.error('Error al cargar evaluaciones pendientes:', err);
      setError('No se pudieron cargar las entregas pendientes. Intenta de nuevo.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [usuario?.id, esProfesor]);

  useEffect(() => {
    cargarPendientes();
  }, [cargarPendientes]);

  const compararPorFecha = (a: string, b: string): number => {
    const fechaA = new Date(a).getTime();
    const fechaB = new Date(b).getTime();
    return fechaA - fechaB;
  };

  const cumpleBusqueda = (entrega: EntregaResumenDTO, termino: string) => {
    if (!termino) return true;
    const partes = [
      entrega.estudianteNombre,
      entrega.estudianteCorreo,
      entrega.grupoTitulo,
      entrega.entregableTitulo,
      entrega.actividadTitulo,
      entrega.cursoTitulo
    ].map(valor => (valor || '').toLowerCase());

    return partes.some(texto => texto.includes(termino));
  };

  const agruparOpciones = (
    base: EntregaResumenDTO[],
    getId: (entrega: EntregaResumenDTO) => number,
    getTitulo: (entrega: EntregaResumenDTO) => string
  ): SelectorVisual[] => {
    const map = new Map<number, SelectorVisual>();
    base.forEach(entrega => {
      const id = getId(entrega);
      const actual = map.get(id);
      if (!actual) {
        map.set(id, {
          id,
          titulo: getTitulo(entrega),
          total: 1,
          tardias: entrega.fueATiempo ? 0 : 1
        });
        return;
      }

      actual.total += 1;
      actual.tardias += entrega.fueATiempo ? 0 : 1;
    });

    return Array.from(map.values()).sort((a, b) => a.titulo.localeCompare(b.titulo, 'es'));
  };

  const baseSelectorVisual = useMemo(() => {
    const termino = busqueda.trim().toLowerCase();
    return entregas
      .filter(entrega => cumpleBusqueda(entrega, termino))
      .filter(entrega => !soloTardias || !entrega.fueATiempo);
  }, [entregas, busqueda, soloTardias]);

  const gruposVisuales = useMemo(() => {
    return agruparOpciones(
      baseSelectorVisual,
      entrega => entrega.cursoId,
      entrega => entrega.cursoTitulo
    );
  }, [baseSelectorVisual]);

  const actividadesVisuales = useMemo(() => {
    const base = baseSelectorVisual
      .filter(entrega => cursoFiltro === 'todos' || entrega.cursoId.toString() === cursoFiltro);
    return agruparOpciones(
      base,
      entrega => entrega.actividadId,
      entrega => entrega.actividadTitulo
    );
  }, [baseSelectorVisual, cursoFiltro]);

  const entregablesVisuales = useMemo(() => {
    const base = baseSelectorVisual
      .filter(entrega => cursoFiltro === 'todos' || entrega.cursoId.toString() === cursoFiltro)
      .filter(entrega => actividadFiltro === 'todos' || entrega.actividadId.toString() === actividadFiltro);
    return agruparOpciones(
      base,
      entrega => entrega.entregableId,
      entrega => entrega.entregableTitulo
    );
  }, [baseSelectorVisual, cursoFiltro, actividadFiltro]);

  const entregasFiltradas = useMemo(() => {
    const termino = busqueda.trim().toLowerCase();

    const base = entregas.filter(entrega => {
      const coincideBusqueda = cumpleBusqueda(entrega, termino);
      const coincideCurso = cursoFiltro === 'todos' || entrega.cursoId.toString() === cursoFiltro;
      const coincideActividad = actividadFiltro === 'todos' || entrega.actividadId.toString() === actividadFiltro;
      const coincideEntregable = entregableFiltro === 'todos' || entrega.entregableId.toString() === entregableFiltro;
      const coincideTardia = !soloTardias || !entrega.fueATiempo;

      return coincideBusqueda
        && coincideCurso
        && coincideActividad
        && coincideEntregable
        && coincideTardia;
    });

    const ordenada = [...base];
    ordenada.sort((a, b) => {
      if (orden === 'prioridad') {
        if (a.fueATiempo !== b.fueATiempo) {
          return a.fueATiempo ? 1 : -1;
        }
        return compararPorFecha(a.fechaEntrega, b.fechaEntrega);
      }

      if (orden === 'recientes') {
        return compararPorFecha(b.fechaEntrega, a.fechaEntrega);
      }

      if (orden === 'antiguas') {
        return compararPorFecha(a.fechaEntrega, b.fechaEntrega);
      }

      return a.estudianteNombre.localeCompare(b.estudianteNombre, 'es');
    });

    return ordenada;
  }, [entregas, busqueda, cursoFiltro, actividadFiltro, entregableFiltro, soloTardias, orden]);

  const resumenPorEntregable = useMemo(() => {
    const termino = busqueda.trim().toLowerCase();
    const map = new Map<number, ResumenEntregable>();

    entregas
      .filter(entrega => cumpleBusqueda(entrega, termino))
      .filter(entrega => cursoFiltro === 'todos' || entrega.cursoId.toString() === cursoFiltro)
      .filter(entrega => actividadFiltro === 'todos' || entrega.actividadId.toString() === actividadFiltro)
      .filter(entrega => !soloTardias || !entrega.fueATiempo)
      .forEach(entrega => {
        const actual = map.get(entrega.entregableId);
        if (!actual) {
          map.set(entrega.entregableId, {
            entregableId: entrega.entregableId,
            actividadId: entrega.actividadId,
            cursoId: entrega.cursoId,
            entregableTitulo: entrega.entregableTitulo,
            total: 1,
            tardias: entrega.fueATiempo ? 0 : 1,
            masAntigua: entrega.fechaEntrega
          });
          return;
        }

        actual.total += 1;
        actual.tardias += entrega.fueATiempo ? 0 : 1;
        if (compararPorFecha(entrega.fechaEntrega, actual.masAntigua) < 0) {
          actual.masAntigua = entrega.fechaEntrega;
        }
      });

    return Array.from(map.values()).sort((a, b) => {
      if (b.total !== a.total) return b.total - a.total;
      return compararPorFecha(a.masAntigua, b.masAntigua);
    });
  }, [entregas, busqueda, cursoFiltro, actividadFiltro, soloTardias]);

  const totalPaginas = Math.max(1, Math.ceil(entregasFiltradas.length / pageSize));
  const paginaSegura = Math.min(paginaActual, totalPaginas);
  const inicio = (paginaSegura - 1) * pageSize;
  const entregasPagina = entregasFiltradas.slice(inicio, inicio + pageSize);

  useEffect(() => {
    setPaginaActual(1);
  }, [busqueda, cursoFiltro, actividadFiltro, entregableFiltro, soloTardias, orden, pageSize]);

  useEffect(() => {
    if (paginaActual > totalPaginas) {
      setPaginaActual(totalPaginas);
    }
  }, [paginaActual, totalPaginas]);

  const totalPendientes = entregas.length;
  const totalTardias = entregas.filter(entrega => !entrega.fueATiempo).length;
  const totalATiempo = totalPendientes - totalTardias;
  const totalEntregables = new Set(entregas.map(entrega => entrega.entregableId)).size;

  const urgentes = entregasFiltradas.filter(entrega => !entrega.fueATiempo).slice(0, 4);

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });

  const formatRelative = (dateStr: string) => {
    const now = Date.now();
    const time = new Date(dateStr).getTime();
    const diffMin = Math.max(0, Math.floor((now - time) / 60000));

    if (diffMin < 60) return `hace ${diffMin} min`;
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) return `hace ${diffHours} h`;
    const diffDays = Math.floor(diffHours / 24);
    return `hace ${diffDays} dias`;
  };

  const irAEntrega = (entregaId: number) => {
    navigate(`/entregas/${entregaId}`);
  };

  const descargarZip = (blob: Blob, filename: string) => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  };

  const cursoSeleccionado = cursoFiltro !== 'todos';
  const actividadSeleccionada = actividadFiltro !== 'todos';
  const entregableSeleccionado = entregableFiltro !== 'todos';
  const nombreCursoSeleccionado = cursoSeleccionado
    ? (gruposVisuales.find(item => item.id.toString() === cursoFiltro)?.titulo ?? 'Grupo')
    : '';
  const nombreActividadSeleccionada = actividadSeleccionada
    ? (actividadesVisuales.find(item => item.id.toString() === actividadFiltro)?.titulo ?? 'Actividad')
    : '';
  const nombreEntregableSeleccionado = entregableSeleccionado
    ? (entregablesVisuales.find(item => item.id.toString() === entregableFiltro)?.titulo ?? 'Entregable')
    : '';

  const puedeDescargarTodo = actividadSeleccionada;
  const textoBotonDescarga = entregableSeleccionado ? 'Descargar entregable' : 'Descargar actividad';
  const ayudaDescarga = !cursoSeleccionado
    ? 'Selecciona un grupo para ver sus actividades.'
    : !actividadSeleccionada
      ? `Grupo seleccionado: ${nombreCursoSeleccionado}. Ahora elige una actividad.`
    : entregableSeleccionado
      ? `Listo para descargar: ${nombreEntregableSeleccionado}.`
      : `Listo para descargar toda la actividad: ${nombreActividadSeleccionada}.`;

  const descargarTodo = async () => {
    if (!puedeDescargarTodo || descargandoZip) {
      return;
    }

    setDescargandoZip(true);
    try {
      const payload = entregableSeleccionado
        ? await entregaService.descargarTodo(Number(entregableFiltro))
        : await entregaService.descargarTodoActividad(Number(actividadFiltro));
      descargarZip(payload.blob, payload.filename);
    } catch (err) {
      console.error('Error al descargar entregas:', err);
      alert('No se pudo generar el ZIP de entregas. Intenta de nuevo.');
    } finally {
      setDescargandoZip(false);
    }
  };

  if (!esProfesor) {
    return (
      <div className="evaluaciones-page">
        <div className="ev-empty-state">
          <h2>Seccion solo para profesores</h2>
          <p>Este apartado muestra entregas pendientes de calificar.</p>
          <button className="ev-primary-btn" onClick={() => navigate('/dashboard')}>
            Ir al dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="evaluaciones-page">
      <header className="ev-header">
        <div>
          <h1>Evaluaciones pendientes</h1>
          <p>Vista para clases grandes: filtra por contexto y corrige de forma directa.</p>
        </div>
        <div className="ev-header-actions">
          <button
            className="ev-secondary-btn"
            onClick={() => cargarPendientes(true)}
            disabled={refreshing || loading}
          >
            {refreshing ? 'Actualizando...' : 'Actualizar'}
          </button>
        </div>
      </header>

      <section className="ev-stats-grid">
        <article className="ev-stat-card">
          <span className="ev-stat-value">{totalPendientes}</span>
          <span className="ev-stat-label">Pendientes</span>
        </article>
        <article className="ev-stat-card warning">
          <span className="ev-stat-value">{totalTardias}</span>
          <span className="ev-stat-label">Tardias</span>
        </article>
        <article className="ev-stat-card success">
          <span className="ev-stat-value">{totalATiempo}</span>
          <span className="ev-stat-label">A tiempo</span>
        </article>
        <article className="ev-stat-card info">
          <span className="ev-stat-value">{totalEntregables}</span>
          <span className="ev-stat-label">Entregables activos</span>
        </article>
      </section>

      <section className="ev-controls">
        <div className="ev-search-box">
          <input
            type="text"
            value={busqueda}
            onChange={e => setBusqueda(e.target.value)}
            placeholder="Buscar por alumno o entregable..."
          />
        </div>

        <div className="ev-flow-legend">
          <span className={`ev-flow-step ${cursoSeleccionado ? 'active' : ''}`}>1. Grupo</span>
          <span className={`ev-flow-step ${actividadSeleccionada ? 'active' : ''}`}>2. Actividad</span>
          <span className={`ev-flow-step ${entregableSeleccionado ? 'active' : ''}`}>3. Entregable</span>
        </div>

        <div className="ev-flow-grid">
          <article className="ev-flow-panel">
            <header className="ev-flow-panel-header">
              <h3>Grupo / clase</h3>
              <small>{gruposVisuales.length} disponibles</small>
            </header>
            <div className="ev-option-list">
              {gruposVisuales.map(grupo => {
                const selected = cursoFiltro === grupo.id.toString();
                return (
                  <button
                    key={grupo.id}
                    type="button"
                    className={`ev-option-btn ${selected ? 'selected' : ''}`}
                    onClick={() => {
                      if (selected) {
                        setCursoFiltro('todos');
                        setActividadFiltro('todos');
                        setEntregableFiltro('todos');
                        return;
                      }
                      setCursoFiltro(grupo.id.toString());
                      setActividadFiltro('todos');
                      setEntregableFiltro('todos');
                    }}
                  >
                    <span className="ev-option-title">{grupo.titulo}</span>
                    <span className="ev-option-meta">{grupo.total} pendientes</span>
                  </button>
                );
              })}
            </div>
          </article>

          <article className={`ev-flow-panel ${!cursoSeleccionado ? 'locked' : ''}`}>
            <header className="ev-flow-panel-header">
              <h3>Actividad</h3>
              <small>{cursoSeleccionado ? `${actividadesVisuales.length} disponibles` : 'Selecciona grupo'}</small>
            </header>
            {!cursoSeleccionado ? (
              <p className="ev-flow-empty">Elige primero un grupo para ver actividades.</p>
            ) : (
              <div className="ev-option-list">
                {actividadesVisuales.map(actividad => {
                  const selected = actividadFiltro === actividad.id.toString();
                  return (
                    <button
                      key={actividad.id}
                      type="button"
                      className={`ev-option-btn ${selected ? 'selected' : ''}`}
                      onClick={() => {
                        if (selected) {
                          setActividadFiltro('todos');
                          setEntregableFiltro('todos');
                          return;
                        }
                        setActividadFiltro(actividad.id.toString());
                        setEntregableFiltro('todos');
                      }}
                    >
                      <span className="ev-option-title">{actividad.titulo}</span>
                      <span className="ev-option-meta">{actividad.total} pendientes</span>
                    </button>
                  );
                })}
              </div>
            )}
          </article>

          <article className={`ev-flow-panel ${!actividadSeleccionada ? 'locked' : ''}`}>
            <header className="ev-flow-panel-header">
              <h3>Entregable</h3>
              <small>{actividadSeleccionada ? `${entregablesVisuales.length} disponibles` : 'Selecciona actividad'}</small>
            </header>
            {!actividadSeleccionada ? (
              <p className="ev-flow-empty">Elige una actividad para ver entregables.</p>
            ) : (
              <div className="ev-option-list">
                {entregablesVisuales.map(entregable => {
                  const selected = entregableFiltro === entregable.id.toString();
                  return (
                    <button
                      key={entregable.id}
                      type="button"
                      className={`ev-option-btn ${selected ? 'selected' : ''}`}
                      onClick={() => {
                        if (selected) {
                          setEntregableFiltro('todos');
                          return;
                        }
                        setEntregableFiltro(entregable.id.toString());
                      }}
                    >
                      <span className="ev-option-title">{entregable.titulo}</span>
                      <span className="ev-option-meta">{entregable.total} pendientes</span>
                    </button>
                  );
                })}
              </div>
            )}
          </article>
        </div>

        <div className="ev-tools-row">
          <select value={orden} onChange={e => setOrden(e.target.value as OrdenEntrega)}>
            <option value="prioridad">Orden: prioridad</option>
            <option value="recientes">Orden: mas recientes</option>
            <option value="antiguas">Orden: mas antiguas</option>
            <option value="alumno">Orden: nombre alumno</option>
          </select>

          <select value={pageSize} onChange={e => setPageSize(Number(e.target.value) as PageSize)}>
            <option value={25}>25 por pagina</option>
            <option value={50}>50 por pagina</option>
            <option value={100}>100 por pagina</option>
          </select>

          <label className="ev-checkbox">
            <input
              type="checkbox"
              checked={soloTardias}
              onChange={e => setSoloTardias(e.target.checked)}
            />
            Solo tardias
          </label>

          {actividadSeleccionada && (
            <button
              className="ev-primary-btn ev-download-inline-btn"
              onClick={descargarTodo}
              disabled={!puedeDescargarTodo || descargandoZip || loading}
              title="Descarga todas las entregas del filtro actual en ZIP"
            >
              {descargandoZip ? 'Preparando ZIP...' : textoBotonDescarga}
            </button>
          )}
        </div>

        <p className="ev-download-hint">{ayudaDescarga}</p>
      </section>

      <div className="ev-meta">
        <span>
          Mostrando {entregasFiltradas.length} de {totalPendientes} entregas pendientes.
        </span>
        {ultimaActualizacion && (
          <span>Actualizado: {ultimaActualizacion.toLocaleTimeString('es-ES')}</span>
        )}
      </div>

      {loading ? (
        <div className="ev-loading-state">
          <div className="ev-spinner" />
          <p>Cargando evaluaciones...</p>
        </div>
      ) : error ? (
        <div className="ev-error-state">
          <p>{error}</p>
          <button className="ev-primary-btn" onClick={() => cargarPendientes()}>
            Reintentar
          </button>
        </div>
      ) : (
        <>
          <section className="ev-resumen-entregables">
            <div className="ev-resumen-header">
              <h2>Carga por entregable</h2>
              <span>{actividadSeleccionada ? `${resumenPorEntregable.length} entregables` : 'Selecciona una actividad'}</span>
            </div>
            {!cursoSeleccionado ? (
              <p className="ev-resumen-vacio">Selecciona un grupo para continuar.</p>
            ) : !actividadSeleccionada ? (
              <p className="ev-resumen-vacio">Ahora selecciona una actividad para ver sus entregables.</p>
            ) : resumenPorEntregable.length === 0 ? (
              <p className="ev-resumen-vacio">No hay entregables para los filtros actuales.</p>
            ) : (
              <div className="ev-resumen-grid">
                {resumenPorEntregable.map(item => (
                  <article
                    key={item.entregableId}
                    className={`ev-resumen-card ev-resumen-card-clickable ${entregableFiltro === item.entregableId.toString() ? 'selected' : ''}`}
                    onClick={() => {
                      if (entregableFiltro === item.entregableId.toString()) {
                        setEntregableFiltro('todos');
                        return;
                      }
                      setCursoFiltro(item.cursoId.toString());
                      setActividadFiltro(item.actividadId.toString());
                      setEntregableFiltro(item.entregableId.toString());
                      setPaginaActual(1);
                    }}
                  >
                    <div className="ev-resumen-card-main">
                      <strong>{item.entregableTitulo}</strong>
                    </div>
                    <div className="ev-resumen-card-stats">
                      <span>{item.total} pendientes</span>
                      <span className={item.tardias > 0 ? 'late' : ''}>{item.tardias} tardias</span>
                      <span>Desde {formatDate(item.masAntigua)}</span>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          {urgentes.length > 0 && (
            <section className="ev-urgent-panel">
              <h2>Entregas tardias (prioridad sugerida)</h2>
              <div className="ev-urgent-list">
                {urgentes.map(entrega => (
                  <article key={entrega.entregaId} className="ev-urgent-card">
                    <div className="ev-urgent-card-main">
                      <strong>{entrega.estudianteNombre}</strong>
                      <span>{entrega.entregableTitulo}</span>
                      <small>{formatDate(entrega.fechaEntrega)}</small>
                    </div>
                    <button
                      className="ev-primary-btn"
                      onClick={() => irAEntrega(entrega.entregaId)}
                    >
                      Calificar ahora
                    </button>
                  </article>
                ))}
              </div>
            </section>
          )}

          {entregasFiltradas.length === 0 ? (
            <div className="ev-empty-state">
              <h2>No hay entregas para mostrar</h2>
              <p>Prueba cambiando filtros o espera nuevas entregas.</p>
            </div>
          ) : (
            <section className="ev-table-panel">
              <table className="ev-table">
                <thead>
                  <tr>
                    <th>Entregable</th>
                    <th>Alumno</th>
                    <th>Grupo</th>
                    <th>Entrega</th>
                    <th>Estado</th>
                    <th>Version</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {entregasPagina.map(entrega => (
                    <tr key={entrega.entregaId}>
                      <td>
                        <span>{entrega.entregableTitulo}</span>
                      </td>
                      <td>
                        <div className="ev-student-cell">
                          <span className="name">{entrega.estudianteNombre}</span>
                          <span className="mail">{entrega.estudianteCorreo}</span>
                        </div>
                      </td>
                      <td>{entrega.grupoTitulo}</td>
                      <td>
                        <div className="ev-date-cell">
                          <span>{formatDate(entrega.fechaEntrega)}</span>
                          <small>{formatRelative(entrega.fechaEntrega)}</small>
                        </div>
                      </td>
                      <td>
                        <span className={`ev-status ${entrega.fueATiempo ? 'ok' : 'late'}`}>
                          {entrega.fueATiempo ? 'A tiempo' : 'Tardia'}
                        </span>
                      </td>
                      <td>v{entrega.version}</td>
                      <td>
                        <button
                          className="ev-primary-btn ev-small-btn"
                          onClick={() => irAEntrega(entrega.entregaId)}
                        >
                          Ver y calificar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}

          {entregasFiltradas.length > 0 && (
            <div className="ev-pagination">
              <button
                className="ev-secondary-btn ev-small-btn"
                onClick={() => setPaginaActual(actual => Math.max(1, actual - 1))}
                disabled={paginaSegura === 1}
              >
                Pagina anterior
              </button>
              <span>
                Pagina {paginaSegura} de {totalPaginas}
              </span>
              <button
                className="ev-secondary-btn ev-small-btn"
                onClick={() => setPaginaActual(actual => Math.min(totalPaginas, actual + 1))}
                disabled={paginaSegura >= totalPaginas}
              >
                Pagina siguiente
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default EvaluacionesPage;


