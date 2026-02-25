import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CursoDTO, ActividadDTO } from '../../types';
import { cursoService, actividadService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import './CursosPage.css';

const CursosPage: React.FC = () => {
  const [cursos, setCursos] = useState<CursoDTO[]>([]);
  const [actividadesPorCurso, setActividadesPorCurso] = useState<Record<number, ActividadDTO[]>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { usuario, esProfesor } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    cargarCursos();
  }, [usuario, esProfesor]);

  const cargarCursos = async () => {
    if (!usuario) return;
    
    setLoading(true);
    setError(null);
    
    try {
      // Por ahora cargamos todos los cursos - esto se puede filtrar por rol
      const data = await cursoService.listarTodos();
      setCursos(data);
      
      // Cargar actividades para cada curso
      const actividadesMap: Record<number, ActividadDTO[]> = {};
      for (const curso of data) {
        const actividades = await actividadService.listarPorCurso(curso.id);
        actividadesMap[curso.id] = actividades;
      }
      setActividadesPorCurso(actividadesMap);
    } catch (err) {
      setError('Error al cargar los cursos');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleVerActividad = (actividadId: number) => {
    navigate(`/actividades/${actividadId}`);
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Cargando cursos...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error-container">
        <p className="error-message">{error}</p>
        <button onClick={cargarCursos} className="btn-retry">
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className="cursos-page">
      <header className="page-header">
        <h1>Mis Cursos</h1>
        {esProfesor && (
          <button 
            className="btn-primary"
            onClick={() => navigate('/cursos/nuevo')}
          >
            + Nuevo Curso
          </button>
        )}
      </header>

      {cursos.length === 0 ? (
        <div className="empty-state">
          <h2>No hay cursos disponibles</h2>
          <p>
            {esProfesor
              ? 'Crea un nuevo curso para comenzar.'
              : 'Aún no estás inscrito en ningún curso.'}
          </p>
        </div>
      ) : (
        <div className="cursos-grid">
          {cursos.map(curso => (
            <div key={curso.id} className="curso-card">
              <div className="curso-header">
                <h2>{curso.titulo}</h2>
                <span className="curso-codigo">{curso.codigo}</span>
              </div>
              
              {curso.descripcion && (
                <p className="curso-descripcion">{curso.descripcion}</p>
              )}
              
              <div className="curso-stats">
                <div className="stat">
                  <span className="stat-value">{curso.numeroActividades}</span>
                  <span className="stat-label">Actividades</span>
                </div>
                <div className="stat">
                  <span className="stat-value">{curso.numeroEstudiantes}</span>
                  <span className="stat-label">Estudiantes</span>
                </div>
                <div className="stat">
                  <span className="stat-value">{curso.grupos.length}</span>
                  <span className="stat-label">Grupos</span>
                </div>
              </div>

              <div className="curso-actividades">
                <h3>Actividades Recientes</h3>
                {actividadesPorCurso[curso.id]?.slice(0, 3).map(actividad => (
                  <div 
                    key={actividad.id} 
                    className="actividad-item"
                    onClick={() => handleVerActividad(actividad.id)}
                  >
                    <span className="actividad-nombre">{actividad.titulo}</span>
                    <span className={`actividad-estado ${actividad.enPlazo ? 'en-plazo' : 'vencido'}`}>
                      {actividad.enPlazo ? 'En plazo' : 'Vencido'}
                    </span>
                  </div>
                )) || <p className="no-actividades">Sin actividades</p>}
              </div>

              <div className="curso-actions">
                <button 
                  className="btn-secondary"
                  onClick={() => navigate(`/cursos/${curso.id}`)}
                >
                  Ver Detalles
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default CursosPage;
