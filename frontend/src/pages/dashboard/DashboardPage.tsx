import React, { useState } from 'react';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const [busqueda, setBusqueda] = useState('');

  return (
    <div className="dashboard-layout">
      {/* --- BARRA SUPERIOR --- */}
      <header className="top-navbar">
        <div className="nav-brand">
          <div className="nav-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
            </svg>
          </div>
          <span className="nav-title">Sistema de Gestión de Entregables</span>
        </div>
        
        {/* Enlaces centrales de navegación */}
        <nav className="nav-links">
          <a href="#dashboard" className="nav-link">Dashboard</a>
          <a href="#cursos" className="nav-link active">Cursos</a>
          <a href="#evaluaciones" className="nav-link">Evaluaciones</a>
        </nav>

        <div className="nav-profile">
          <div className="profile-text">
            <span className="profile-name">Juan García Pérez</span>
            <span className="profile-role">Profesor</span>
          </div>
          <div className="profile-avatar">JG</div>
        </div>
      </header>

      {/* --- CONTENIDO PRINCIPAL --- */}
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
              placeholder="Buscar asignatura..." 
              value={busqueda}
              onChange={(e) => setBusqueda(e.target.value)}
              className="search-input"
            />
          </div>
        </div>
        
        {/* Cuadrícula de Asignaturas (2 columnas) */}
        <div className="courses-grid">
          
          {/* Tarjeta 1 */}
          <div className="course-card">
            <div className="course-image-placeholder">
              <span>Imagen de la<br/>asignatura</span>
            </div>
            <div className="course-info">
              <h2 className="course-title">Sistema operativo</h2>
            </div>
          </div>

          {/* Tarjeta 2 */}
          <div className="course-card">
            <div className="course-image-placeholder">
              <span>Imagen de la<br/>asignatura</span>
            </div>
            <div className="course-info">
              <h2 className="course-title">Redes de computadores</h2>
            </div>
          </div>

        </div>
      </main>
    </div>
  );
};

export default Dashboard;