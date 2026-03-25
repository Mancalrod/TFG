import React, { useEffect, useState } from 'react';
import { Link, NavLink, useMatch, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { cursoService } from '../services/cursoService';
import './Navbar.css';

const Navbar: React.FC = () => {
  const { usuario, esProfesor, esEstudiante, esAdmin, logout } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const courseMatch = useMatch('/cursos/:id');
  const cursoIdActual = courseMatch?.params?.id ? Number(courseMatch.params.id) : null;
  const [rolEnCursoNavbar, setRolEnCursoNavbar] = useState<'PROFESOR' | 'ESTUDIANTE' | 'AMBOS' | null>(null);

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

        if (profesorEnCurso && estudianteEnCurso) {
          setRolEnCursoNavbar('AMBOS');
        } else if (profesorEnCurso) {
          setRolEnCursoNavbar('PROFESOR');
        } else if (estudianteEnCurso) {
          setRolEnCursoNavbar('ESTUDIANTE');
        } else {
          setRolEnCursoNavbar(null);
        }
      } catch {
        if (cancelled) return;

        if (esProfesor && esEstudiante) setRolEnCursoNavbar('AMBOS');
        else if (esProfesor) setRolEnCursoNavbar('PROFESOR');
        else if (esEstudiante) setRolEnCursoNavbar('ESTUDIANTE');
        else setRolEnCursoNavbar(null);
      }
    };

    resolverRolEnCurso();

    return () => {
      cancelled = true;
    };
  }, [usuario, cursoIdActual, esProfesor, esEstudiante, esAdmin]);

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
              className={({ isActive }) => isActive ? "navbar-item active" : "navbar-item"}
            >
              Dashboard
            </NavLink>
            <NavLink 
              to="/actividades-por-curso" 
              className={({ isActive }) => isActive ? "navbar-item active" : "navbar-item"}
            >
              Actividades
            </NavLink>
            {esProfesor && (
              <NavLink 
                to="/evaluaciones" 
                className={({ isActive }) => isActive ? "navbar-item active" : "navbar-item"}
              >
                Evaluaciones
              </NavLink>
            )}
            {esAdmin && (
              <NavLink 
                to="/admin" 
                className={({ isActive }) => isActive ? "navbar-item active" : "navbar-item"}
              >
                Administración
              </NavLink>
            )}
          </>
        )}
      </div>

      {/* Lado derecho: Theme Toggle, Perfil y Acciones */}
      <div className="navbar-actions">
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
              <span className="user-name">{usuario.nombre}</span>
              {cursoIdActual && rolEnCursoNavbar && (
                <span className={`user-role-navbar ${rolEnCursoNavbar.toLowerCase()}`}>
                  {rolEnCursoNavbar === 'AMBOS'
                    ? 'Profesor/Estudiante'
                    : rolEnCursoNavbar === 'PROFESOR'
                      ? 'Profesor'
                      : 'Estudiante'}
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