import React from 'react';
import { Link } from 'react-router-dom';
import './NotFoundPage.css';

const NotFoundPage: React.FC = () => {
  return (
    <div className="not-found-container">
      <div className="not-found-content">
        <h1 className="error-code">404</h1>
        <h2 className="error-title">Página no encontrada</h2>
        <p className="error-message">
          Lo sentimos, no hemos podido encontrar la página que buscas. 
          Es posible que haya sido eliminada o que la dirección sea incorrecta.
        </p>
        
        <Link to="/" className="btn-home">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          Volver al Inicio
        </Link>
      </div>
    </div>
  );
};

export default NotFoundPage;