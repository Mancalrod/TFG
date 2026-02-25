import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { cursoService } from '../../services/cursoService';
import { actividadService } from '../../services/actividadService';
import { CursoDTO, ActividadDTO } from '../../types';
import './ActividadesPorCursoPage.css';

interface CursoConActividades {
  curso: CursoDTO;
  actividades: ActividadDTO[];
  loading: boolean;
  loaded: boolean;
}

const ActividadesPorCursoPage: React.FC = () => {
  const { usuario, esProfesor, esAdmin } = useAuth();
  const [cursosData, setCursosData] = useState<CursoConActividades[]>([]);
  const [expandedCursos, setExpandedCursos] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busqueda, setBusqueda] = useState('');

  // Cargar cursos al montar
  useEffect(() => {
    if (!usuario) return;

    const fetchCursos = async () => {
      setLoading(true);
      setError(null);
      try {
        let cursos: CursoDTO[];
        if (esProfesor || esAdmin) {
          cursos = await cursoService.listarPorProfesor(usuario.id);
        } else {
          cursos = await cursoService.listarPorEstudiante(usuario.id);
        }
        setCursosData(cursos.map(c => ({ curso: c, actividades: [], loading: false, loaded: false })));
      } catch {
        setError('Error al cargar los cursos.');
      } finally {
        setLoading(false);
      }
    };

    fetchCursos();
  }, [usuario, esProfesor, esAdmin]);

  // Expandir/colapsar curso y cargar actividades si no están cargadas
  const toggleCurso = async (cursoId: number) => {
    const newExpanded = new Set(expandedCursos);

    if (newExpanded.has(cursoId)) {
      newExpanded.delete(cursoId);
      setExpandedCursos(newExpanded);
      return;
    }

    newExpanded.add(cursoId);
    setExpandedCursos(newExpanded);

    const entry = cursosData.find(c => c.curso.id === cursoId);
    if (entry && entry.loaded) return;

    // Marcar como loading
    setCursosData(prev =>
      prev.map(c => c.curso.id === cursoId ? { ...c, loading: true } : c)
    );

    try {
      const actividades = await actividadService.listarPorCurso(cursoId);
      setCursosData(prev =>
        prev.map(c => c.curso.id === cursoId ? { ...c, actividades, loading: false, loaded: true } : c)
      );
    } catch {
      setCursosData(prev =>
        prev.map(c => c.curso.id === cursoId ? { ...c, loading: false, loaded: true } : c)
      );
    }
  };

  // Filtrar cursos y actividades por búsqueda
  const cursosFiltrados = cursosData.filter(({ curso, actividades }) => {
    const term = busqueda.toLowerCase();
    if (!term) return true;
    if (curso.titulo.toLowerCase().includes(term) || curso.codigo.toLowerCase().includes(term)) return true;
    return actividades.some(a => a.titulo.toLowerCase().includes(term));
  });

  if (loading) {
    return (
      <div className="apc-page">
        <div className="apc-loading">
          <div className="apc-spinner" />
          <p>Cargando cursos...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="apc-page">
        <div className="apc-error">
          <p>{error}</p>
          <button onClick={() => window.location.reload()} className="apc-btn-retry">Reintentar</button>
        </div>
      </div>
    );
  }

  return (
    <div className="apc-page">
      {/* Cabecera */}
      <div className="apc-header">
        <div>
          <h1>Actividades por Curso</h1>
          <p className="apc-subtitle">
            {esProfesor || esAdmin
              ? 'Gestiona las actividades de tus cursos'
              : 'Consulta las actividades de tus cursos'}
          </p>
        </div>
      </div>

      {/* Buscador */}
      <div className="apc-search-wrapper">
        <svg className="apc-search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          type="text"
          className="apc-search-input"
          placeholder="Buscar curso o actividad..."
          value={busqueda}
          onChange={e => setBusqueda(e.target.value)}
        />
      </div>

      {/* Lista de cursos */}
      {cursosFiltrados.length === 0 ? (
        <div className="apc-empty">
          {busqueda ? (
            <>
              <h2>Sin resultados</h2>
              <p>No se encontraron cursos ni actividades para "{busqueda}"</p>
            </>
          ) : (
            <>
              <h2>No tienes cursos asignados</h2>
              <p>Los cursos que se te asignen aparecerán aquí con sus actividades.</p>
            </>
          )}
        </div>
      ) : (
        <div className="apc-list">
          {cursosFiltrados.map(({ curso, actividades, loading: loadingActs }) => {
            const isExpanded = expandedCursos.has(curso.id);
            const term = busqueda.toLowerCase();
            const actividadesFiltradas = term
              ? actividades.filter(a => a.titulo.toLowerCase().includes(term) || curso.titulo.toLowerCase().includes(term) || curso.codigo.toLowerCase().includes(term))
              : actividades;

            return (
              <div key={curso.id} className={`apc-curso-card ${isExpanded ? 'expanded' : ''}`}>
                {/* Header del curso */}
                <button className="apc-curso-header" onClick={() => toggleCurso(curso.id)}>
                  <div className="apc-curso-info">
                    <span className="apc-curso-codigo">{curso.codigo}</span>
                    <span className="apc-curso-titulo">{curso.titulo}</span>
                    {curso.descripcion && (
                      <span className="apc-curso-desc">{curso.descripcion}</span>
                    )}
                  </div>
                  <div className="apc-curso-meta">
                    <Link
                      to={`/cursos/${curso.id}`}
                      className="apc-ver-curso-link"
                      onClick={(e) => e.stopPropagation()}
                    >
                      Ver curso →
                    </Link>
                    <span className="apc-curso-stat">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                        <line x1="16" y1="13" x2="8" y2="13"/>
                        <line x1="16" y1="17" x2="8" y2="17"/>
                      </svg>
                      {curso.numeroActividades} actividades
                    </span>
                    <span className="apc-curso-stat">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                        <circle cx="9" cy="7" r="4"/>
                        <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                      </svg>
                      {curso.numeroEstudiantes} estudiantes
                    </span>
                    <svg
                      className={`apc-chevron ${isExpanded ? 'open' : ''}`}
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <polyline points="6 9 12 15 18 9" />
                    </svg>
                  </div>
                </button>

                {/* Panel de actividades expandible */}
                {isExpanded && (
                  <div className="apc-actividades-panel">
                    {loadingActs ? (
                      <div className="apc-actividades-loading">
                        <div className="apc-spinner-sm" />
                        Cargando actividades...
                      </div>
                    ) : actividadesFiltradas.length === 0 ? (
                      <div className="apc-actividades-empty">
                        Este curso no tiene actividades registradas.
                      </div>
                    ) : (
                      <div className="apc-actividades-grid">
                        {actividadesFiltradas.map(act => (
                          <Link
                            key={act.id}
                            to={`/actividades/${act.id}`}
                            className="apc-actividad-card"
                          >
                            <div className="apc-actividad-top">
                              <span className={`apc-tipo-badge ${act.tipoActividad.toLowerCase()}`}>
                                {act.tipoActividad === 'EVALUABLE' ? 'Evaluable' : 'No evaluable'}
                              </span>
                              {act.fechaLimite && (
                                <span className={`apc-fecha ${new Date(act.fechaLimite) < new Date() ? 'vencida' : ''}`}>
                                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <circle cx="12" cy="12" r="10"/>
                                    <polyline points="12 6 12 12 16 14"/>
                                  </svg>
                                  {new Date(act.fechaLimite).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' })}
                                </span>
                              )}
                            </div>
                            <h3 className="apc-actividad-titulo">{act.titulo}</h3>
                            {act.descripcion && (
                              <p className="apc-actividad-desc">{act.descripcion}</p>
                            )}
                            <div className="apc-actividad-footer">
                              <span className="apc-actividad-entregables">
                                {act.numeroEntregables} entregable{act.numeroEntregables !== 1 ? 's' : ''}
                              </span>
                              <span className="apc-actividad-arrow">→</span>
                            </div>
                          </Link>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default ActividadesPorCursoPage;
