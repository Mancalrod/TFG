import React, { useEffect, useState } from 'react';
import { Link, NavLink, useMatch, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { cursoService } from '../services/cursoService';
import { notificacionService } from '../services/notificacionService';
import { NotificacionDTO } from '../types';
import './Navbar.css';

type RolEnCurso = 'PROFESOR' | 'ESTUDIANTE' | 'AMBOS' | null;

const Navbar: React.FC = () => {
  const { usuario, esProfesor, esEstudiante, esAdmin, logout } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const courseMatch = useMatch('/cursos/:id');
  const cursoIdActual = courseMatch?.params?.id ? Number(courseMatch.params.id) : null;
  const [rolEnCursoNavbar, setRolEnCursoNavbar] = useState<RolEnCurso>(null);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificaciones, setNotificaciones] = useState<NotificacionDTO[]>([]);
  const [openNotificaciones, setOpenNotificaciones] = useState(false);
  const [loadingNotificaciones, setLoadingNotificaciones] = useState(false);

  const navItemClassName = ({ isActive }: { isActive: boolean }) =>
    isActive ? 'navbar-item active' : 'navbar-item';

  const resolverRol = (profesorEnCurso: boolean, estudianteEnCurso: boolean): RolEnCurso => {
    if (profesorEnCurso && estudianteEnCurso) return 'AMBOS';
    if (profesorEnCurso) return 'PROFESOR';
    if (estudianteEnCurso) return 'ESTUDIANTE';
    return null;
  };

  const obtenerEtiquetaRol = (rol: Exclude<RolEnCurso, null>) => {
    if (rol === 'AMBOS') return 'Profesor/Estudiante';
    if (rol === 'PROFESOR') return 'Profesor';
    return 'Estudiante';
  };

  useEffect(() => {
    if (!usuario || !cursoIdActual) {
      setRolEnCursoNavbar(null);
      return;
    }

    let cancelled = false;

    const resolverRolEnCurso = async () => {
      if (esAdmin) {
        if (!cancelled) setRolEnCursoNavbar('PROFESOR');
        return;
      }

      try {
        let profesorEnCurso = false;
        let estudianteEnCurso = false;

        if (esProfesor) {
          const cursosProfesor = await cursoService.listarPorProfesor(usuario.id);
          profesorEnCurso = cursosProfesor.some(c => c.id === cursoIdActual);
        }

        if (esEstudiante) {
          const cursosEstudiante = await cursoService.listarPorEstudiante(usuario.id);
          estudianteEnCurso = cursosEstudiante.some(c => c.id === cursoIdActual);
        }

        if (cancelled) return;

        setRolEnCursoNavbar(resolverRol(profesorEnCurso, estudianteEnCurso));
      } catch {
        if (cancelled) return;

        setRolEnCursoNavbar(resolverRol(esProfesor, esEstudiante));
      }
    };

    resolverRolEnCurso();

    return () => {
      cancelled = true;
    };
  }, [usuario, cursoIdActual, esProfesor, esEstudiante, esAdmin]);

  useEffect(() => {
    if (!usuario) {
      setUnreadCount(0);
      setNotificaciones([]);
      setOpenNotificaciones(false);
      return;
    }

    let cancelled = false;
    const cargarCount = async () => {
      try {
        const count = await notificacionService.contarNoLeidas();
        if (!cancelled) setUnreadCount(count);
      } catch {
        if (!cancelled) setUnreadCount(0);
      }
    };

    cargarCount();
    return () => {
      cancelled = true;
    };
  }, [usuario]);

  const toggleNotificaciones = async () => {
    const siguienteEstado = !openNotificaciones;
    setOpenNotificaciones(siguienteEstado);
    if (!siguienteEstado || loadingNotificaciones) return;

    setLoadingNotificaciones(true);
    try {
      const data = await notificacionService.listar();
      setNotificaciones(data);
    } catch {
      setNotificaciones([]);
    } finally {
      setLoadingNotificaciones(false);
    }
  };

  const handleMarcarLeida = async (notificacion: NotificacionDTO) => {
    if (notificacion.leida) return;
    try {
      await notificacionService.marcarComoLeida(notificacion.id);
      setNotificaciones((prev) =>
        prev.map((item) =>
          item.id === notificacion.id ? { ...item, leida: true } : item,
        ),
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {
      // no-op
    }
  };

  const abrirNotificacion = (notificacion: NotificacionDTO) => {
    handleMarcarLeida(notificacion);
    if (notificacion.cursoId) {
      setOpenNotificaciones(false);
      navigate(`/cursos/${notificacion.cursoId}`);
    }
  };

  const renderNotificacionesContenido = () => {
    if (loadingNotificaciones) {
      return <p className="notifications-empty">Cargando...</p>;
    }
    if (notificaciones.length === 0) {
      return <p className="notifications-empty">No hay notificaciones.</p>;
    }

    return (
      <ul className="notifications-list">
        {notificaciones.slice(0, 10).map((n) => (
          <li key={n.id}>
            <button
              type="button"
              className={`notification-item ${n.leida ? 'read' : 'unread'}`}
              onClick={() => abrirNotificacion(n)}
            >
              <span className="notification-title">{n.titulo}</span>
              <span className="notification-message">{n.mensaje ?? 'Sin detalle'}</span>
            </button>
          </li>
        ))}
      </ul>
    );
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="navbar">
      {/* Lado izquierdo: Logo / Marca */}
      <div className="navbar-brand">
        <Link to="/">Sistema de Gestión de Entregables</Link>
      </div>
      
      {/* Centro: Enlaces de navegación */}
      <div className="navbar-menu">
        {usuario && (
          <>
            <NavLink 
              to="/dashboard" 
              className={navItemClassName}
            >
              Dashboard
            </NavLink>
            <NavLink 
              to="/actividades-por-curso" 
              className={navItemClassName}
            >
              Actividades
            </NavLink>
            <NavLink
              to="/perfil"
              className={navItemClassName}
            >
              Perfil
            </NavLink>
            {esProfesor && (
              <NavLink 
                to="/evaluaciones" 
                className={navItemClassName}
              >
                Evaluaciones
              </NavLink>
            )}
            {esAdmin && (
              <NavLink 
                to="/admin" 
                className={navItemClassName}
              >
                Administración
              </NavLink>
            )}
          </>
        )}
      </div>

      {/* Lado derecho: Theme Toggle, Perfil y Acciones */}
      <div className="navbar-actions">
        {usuario && (
          <div className="navbar-notifications">
            <button
              type="button"
              className="btn-notifications"
              onClick={toggleNotificaciones}
              aria-label="Abrir notificaciones"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              {unreadCount > 0 && <span className="notifications-badge">{unreadCount}</span>}
            </button>
            {openNotificaciones && (
              <div className="notifications-panel" aria-label="Panel de notificaciones">
                <div className="notifications-header">
                  <strong>Notificaciones</strong>
                </div>
                {renderNotificacionesContenido()}
              </div>
            )}
          </div>
        )}

        <button
          onClick={toggleTheme}
          className="btn-theme-toggle"
          title={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
          aria-label={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
        >
          {isDark ? (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="5"/>
              <line x1="12" y1="1" x2="12" y2="3"/>
              <line x1="12" y1="21" x2="12" y2="23"/>
              <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
              <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
              <line x1="1" y1="12" x2="3" y2="12"/>
              <line x1="21" y1="12" x2="23" y2="12"/>
              <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
              <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
            </svg>
          ) : (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          )}
        </button>

        {usuario ? (
          <div className="navbar-user">
            <div className="user-info-nav">
              {usuario.fotoPerfilUrl ? (
                <img
                  src={usuario.fotoPerfilUrl}
                  alt="Foto de perfil"
                  className="navbar-avatar"
                />
              ) : (
                <div className="navbar-avatar navbar-avatar-fallback" aria-label="Avatar por defecto">
                  {usuario.nombre.charAt(0).toUpperCase()}
                </div>
              )}
              <span className="user-name">{usuario.nombre}</span>
              {cursoIdActual && rolEnCursoNavbar && (
                <span className={`user-role-navbar ${rolEnCursoNavbar.toLowerCase()}`}>
                  {obtenerEtiquetaRol(rolEnCursoNavbar)}
                </span>
              )}
            </div>
            <button onClick={handleLogout} className="btn-logout">
              Cerrar Sesión
            </button>
          </div>
        ) : (
          <Link to="/login" className="btn-login">
            Iniciar Sesión
          </Link>
        )}
      </div>
    </nav>
  );
};

export default Navbar;