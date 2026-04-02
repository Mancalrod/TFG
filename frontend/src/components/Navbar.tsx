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
    setOpenNotificaciones(false);
    if (notificacion.entregaId) {
      navigate(`/entregas/${notificacion.entregaId}`);
      return;
    }
    if (notificacion.entregableId) {
      navigate(`/entregables/${notificacion.entregableId}`);
      return;
    }
    if (notificacion.actividadId) {
      navigate(`/actividades/${notificacion.actividadId}`);
      return;
    }
    if (notificacion.cursoId) {
      navigate(`/cursos/${notificacion.cursoId}`);
    }
  };

  const decodeHtmlEntities = (value: string): string => {
    const textarea = document.createElement('textarea');
    textarea.innerHTML = value;
    return textarea.value;
  };

  const formatNotificacionDate = (value: string): string => {
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return value;
    }

    const now = new Date();
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const startOfDate = new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate());
    const msDay = 24 * 60 * 60 * 1000;
    const dayDiff = Math.floor((startOfToday.getTime() - startOfDate.getTime()) / msDay);
    const timeText = parsed.toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit',
    });

    if (dayDiff === 0) {
      return `Hoy a las ${timeText}`;
    }
    if (dayDiff === 1) {
      return `Ayer a las ${timeText}`;
    }

    return parsed.toLocaleString('es-ES', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
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
              <span className="notification-title">{decodeHtmlEntities(n.titulo)}</span>
              <span className="notification-message">{decodeHtmlEntities(n.mensaje ?? 'Sin detalle')}</span>
              <span className="notification-date">{formatNotificacionDate(n.fechaCreacion)}</span>
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
        <div className="navbar-notifications">
          <button
            type="button"
            className="btn-notifications"
            onClick={usuario ? toggleNotificaciones : undefined}
            aria-label={usuario ? 'Abrir notificaciones' : 'Notificaciones (inicia sesion)'}
            title={usuario ? 'Notificaciones' : 'Inicia sesion para ver notificaciones'}
            disabled={!usuario}
          >
            <span className="navbar-icon" aria-hidden="true">🔔</span>
            {usuario && unreadCount > 0 && <span className="notifications-badge">{unreadCount}</span>}
          </button>
          {usuario && openNotificaciones && (
            <div className="notifications-panel" aria-label="Panel de notificaciones">
              <div className="notifications-header">
                <strong>Notificaciones</strong>
              </div>
              {renderNotificacionesContenido()}
            </div>
          )}
        </div>

        <button
          onClick={toggleTheme}
          className="btn-theme-toggle"
          title={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
          aria-label={isDark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
        >
          <span className="navbar-icon" aria-hidden="true">{isDark ? '🌙' : '☀️'}</span>
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