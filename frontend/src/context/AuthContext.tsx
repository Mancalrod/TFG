import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { UsuarioDTO } from '../types';
import { usuarioService } from '../services';

interface AuthContextType {
  usuario: UsuarioDTO | null;
  loading: boolean;
  esProfesor: boolean;
  esEstudiante: boolean;
  login: (correo: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe usarse dentro de AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [usuario, setUsuario] = useState<UsuarioDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [esProfesor, setEsProfesor] = useState(false);
  const [esEstudiante, setEsEstudiante] = useState(false);

  useEffect(() => {
    // Verificar si hay un usuario guardado en localStorage
    const savedUser = localStorage.getItem('usuario');
    if (savedUser) {
      const user = JSON.parse(savedUser) as UsuarioDTO;
      setUsuario(user);
      checkRoles(user.id);
    }
    setLoading(false);
  }, []);

  const checkRoles = async (userId: number) => {
    try {
      const [profesor, estudiante] = await Promise.all([
        usuarioService.esProfesor(userId),
        usuarioService.esEstudiante(userId)
      ]);
      setEsProfesor(profesor);
      setEsEstudiante(estudiante);
    } catch (error) {
      console.error('Error al verificar roles:', error);
    }
  };

  const login = async (correo: string) => {
    setLoading(true);
    try {
      const user = await usuarioService.obtenerPorCorreo(correo);
      setUsuario(user);
      localStorage.setItem('usuario', JSON.stringify(user));
      await checkRoles(user.id);
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    setUsuario(null);
    setEsProfesor(false);
    setEsEstudiante(false);
    localStorage.removeItem('usuario');
  };

  return (
    <AuthContext.Provider value={{ usuario, loading, esProfesor, esEstudiante, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
