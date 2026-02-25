import React from 'react';
import { useNavigate } from 'react-router-dom';
import './HomePage.css';

const HomePage: React.FC = () => {
  const navigate = useNavigate();

  const handleLoginClick = () => {
    navigate('/login');
  };

  return (
    <div className="home-wrapper">
      <main className="hero-section">
        <div className="hero-content">
          {/* Pequeña etiqueta superior para darle un toque profesional */}
          <span className="badge">Universidad de Sevilla</span>
          
          <h1 className="hero-title">
            Sistema de Gestión de <br />
            <span className="text-highlight">Entregables</span>
          </h1>
          
          <p className="hero-subtitle">
            Plataforma para la gestión integral de entregables académicos.<br />
            Trabajo de Fin de Grado.
          </p>

          <div className="action-container">
            <button onClick={handleLoginClick} className="btn-hero-primary">
              Iniciar Sesión
              {/* Icono de flecha SVG directamente integrado */}
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default HomePage;