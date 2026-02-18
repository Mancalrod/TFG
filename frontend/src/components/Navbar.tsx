import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

const Navbar: React.FC = () => {
  const { usuario, esProfesor, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <Link to="/">Sistema de Gestión de Entregables</Link>
      </div>
      
      <div className="navbar-menu">
        {usuario ? (
          <>
            <Link to="/dashboard" className="navbar-item">Dashboard</Link>
            <Link to="/cursos" className="navbar-item">Cursos</Link>
            {esProfesor && (
              <Link to="/evaluaciones" className="navbar-item">Evaluaciones</Link>
            )}
            <div className="navbar-user">
              <span className="user-name">{usuario.nombre}</span>
              <span className="user-role">
                {esProfesor ? 'Profesor' : 'Estudiante'}
              </span>
              <button onClick={handleLogout} className="btn-logout">
                Cerrar Sesión
              </button>
            </div>
          </>
        ) : (
          <Link to="/login" className="navbar-item btn-login">
            Iniciar Sesión
          </Link>
        )}
      </div>
    </nav>
  );
};

export default Navbar;
