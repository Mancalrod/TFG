import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { cursoService, oneDriveService } from '../../services';
import { CursoDTO } from '../../types';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const { usuario, esEstudiante, esAdmin } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [busqueda, setBusqueda] = useState('');
  const [cursos, setCursos] = useState<CursoDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // ── OneDrive ──
  const [oneDriveConnected, setOneDriveConnected] = useState(false);
  const [oneDriveEmail, setOneDriveEmail] = useState('');
  const [oneDriveLoading, setOneDriveLoading] = useState(false);
  const [oneDriveMsg, setOneDriveMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Comprobar parámetros de callback de OneDrive
  useEffect(() => {
    const onedriveResult = searchParams.get('onedrive');
    if (onedriveResult === 'success') {
      setOneDriveMsg({ type: 'success', text: 'OneDrive conectado correctamente.' });
      searchParams.delete('onedrive');
      setSearchParams(searchParams, { replace: true });
    } else if (onedriveResult === 'error') {
      const reason = searchParams.get('reason') || 'desconocido';
      setOneDriveMsg({ type: 'error', text: `Error al conectar OneDrive: ${reason}` });
      searchParams.delete('onedrive');
      searchParams.delete('reason');
      setSearchParams(searchParams, { replace: true });
    }
  }, []);

  // Cargar estado de OneDrive
  useEffect(() => {
    if (usuario) {
      oneDriveService.getConnectionStatus(usuario.id).then((status) => {
        setOneDriveConnected(status.conectado);
        setOneDriveEmail(status.microsoftEmail || '');
      }).catch(() => { /* ignorar si OneDrive no está habilitado */ });
    }
  }, [usuario, oneDriveMsg]);

  useEffect(() => {
    const cargarCursos = async () => {
      if (!usuario) {
        setLoading(false);
        return;
      }
      setLoading(true);
      setError('');
      try {
        let data: CursoDTO[];
        if (esAdmin) {
          data = await cursoService.listarTodos();
        } else if (esEstudiante) {
          data = await cursoService.listarPorEstudiante(usuario.id);
        } else {
          data = await cursoService.listarPorProfesor(usuario.id);
        }
        setCursos(data);
      } catch (err) {
        console.error('Error al cargar cursos:', err);
        setError('No se pudieron cargar los cursos.');
      } finally {
        setLoading(false);
      }
    };
    cargarCursos();
  }, [usuario, esEstudiante, esAdmin]);

  // Filtrado por nombre en el buscador
  const cursosFiltrados = useMemo(() => {
    if (!busqueda.trim()) return cursos;
    const termino = busqueda.toLowerCase();
    return cursos.filter(
      (c) =>
        c.titulo.toLowerCase().includes(termino) ||
        c.codigo.toLowerCase().includes(termino)
    );
  }, [cursos, busqueda]);

  // ── OneDrive handlers ──
  const handleConnectOneDrive = async () => {
    if (!usuario) return;
    setOneDriveLoading(true);
    try {
      const success = await oneDriveService.connectOneDrive(usuario.id);
      if (success) {
        setOneDriveMsg({ type: 'success', text: 'OneDrive conectado correctamente.' });
      } else {
        setOneDriveMsg({ type: 'error', text: 'No se completó la conexión con OneDrive.' });
      }
    } catch (err) {
      console.error('Error al conectar OneDrive:', err);
      setOneDriveMsg({ type: 'error', text: 'No se pudo iniciar la conexión con OneDrive.' });
    } finally {
      setOneDriveLoading(false);
    }
  };

  const handleDisconnectOneDrive = async () => {
    if (!usuario) return;
    setOneDriveLoading(true);
    try {
      await oneDriveService.disconnectOneDrive(usuario.id);
      setOneDriveConnected(false);
      setOneDriveEmail('');
      setOneDriveMsg({ type: 'success', text: 'OneDrive desconectado correctamente.' });
    } catch (err) {
      console.error('Error al desconectar OneDrive:', err);
      setOneDriveMsg({ type: 'error', text: 'No se pudo desconectar OneDrive.' });
    } finally {
      setOneDriveLoading(false);
    }
  };

  return (
    <div className="dashboard-container">
      <main className="dashboard-content">

        {/* Buscador central */}
        <div className="search-container">
          <div className="search-wrapper">
            <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input
              type="text"
              placeholder="Buscar asignatura por nombre o código..."
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              className="search-input"
            />
          </div>
        </div>

        {/* Mensaje de OneDrive */}
        {oneDriveMsg && (
          <div className={`onedrive-alert ${oneDriveMsg.type === 'success' ? 'onedrive-alert-success' : 'onedrive-alert-error'}`}>
            <span>{oneDriveMsg.text}</span>
            <button className="onedrive-alert-close" onClick={() => setOneDriveMsg(null)}>×</button>
          </div>
        )}

        {/* Tarjeta de conexión OneDrive */}
        <div className="onedrive-card">
          <div className="onedrive-card-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" width="28" height="28">
              <path d="M12 2L2 7l10 5 10-5-10-5z" />
              <path d="M2 17l10 5 10-5" />
              <path d="M2 12l10 5 10-5" />
            </svg>
          </div>
          <div className="onedrive-card-info">
            <h3>Microsoft OneDrive</h3>
            {oneDriveConnected ? (
              <p className="onedrive-status connected">
                Conectado como <strong>{oneDriveEmail}</strong>
              </p>
            ) : (
              <p className="onedrive-status disconnected">
                Conecta tu cuenta de Microsoft para subir tus entregas a OneDrive.
              </p>
            )}
          </div>
          <div className="onedrive-card-action">
            {oneDriveConnected ? (
              <button
                className="onedrive-btn onedrive-btn-disconnect"
                onClick={handleDisconnectOneDrive}
                disabled={oneDriveLoading}
              >
                {oneDriveLoading ? 'Desconectando...' : 'Desconectar'}
              </button>
            ) : (
              <button
                className="onedrive-btn onedrive-btn-connect"
                onClick={handleConnectOneDrive}
                disabled={oneDriveLoading}
              >
                {oneDriveLoading ? 'Conectando...' : 'Conectar OneDrive'}
              </button>
            )}
          </div>
        </div>

        {/* Estados de carga y error */}
        {loading && (
          <div className="dashboard-status">
            <p>Cargando asignaturas...</p>
          </div>
        )}

        {error && (
          <div className="dashboard-status dashboard-error">
            <p>{error}</p>
          </div>
        )}

        {/* Cuadrícula de Asignaturas */}
        {!loading && !error && (
          <>
            {cursosFiltrados.length === 0 ? (
              <div className="dashboard-status">
                <p>
                  {busqueda
                    ? `No se encontraron asignaturas para "${busqueda}"`
                    : 'No tienes asignaturas asignadas.'}
                </p>
              </div>
            ) : (
              <div className="courses-grid">
                {cursosFiltrados.map((curso) => (
                  <div
                    key={curso.id}
                    className="course-card"
                    onClick={() => navigate(`/cursos/${curso.id}`)}
                  >
                    <div className="course-image-placeholder">
                      <span>{curso.codigo}</span>
                    </div>
                    <div className="course-info">
                      <h2 className="course-title">{curso.titulo}</h2>
                      {curso.descripcion && (
                        <p className="course-description">{curso.descripcion}</p>
                      )}
                      <div className="course-stats">
                        <span className="stat">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="14" height="14">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                            <circle cx="9" cy="7" r="4"></circle>
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                          </svg>
                          {curso.numeroEstudiantes} estudiantes
                        </span>
                        <span className="stat">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="14" height="14">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                            <polyline points="14 2 14 8 20 8"></polyline>
                          </svg>
                          {curso.numeroActividades} actividades
                        </span>
                        <span className="stat">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="14" height="14">
                            <rect x="3" y="3" width="7" height="7"></rect>
                            <rect x="14" y="3" width="7" height="7"></rect>
                            <rect x="3" y="14" width="7" height="7"></rect>
                            <rect x="14" y="14" width="7" height="7"></rect>
                          </svg>
                          {curso.grupos.length} grupos
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
};

export default Dashboard;