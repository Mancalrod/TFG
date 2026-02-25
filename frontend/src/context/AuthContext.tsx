import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { AuthResponseDTO, LoginRequestDTO } from '../types';
import { authService } from '../services';

interface AuthUser {
  id: number;
  nombre: string;
  correoElectronico: string;
  roles: string[];
}

interface AuthContextType {
  usuario: AuthUser | null;
  loading: boolean;
  esProfesor: boolean;
  esEstudiante: boolean;
  esAdmin: boolean;
  login: (credentials: LoginRequestDTO) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
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

const mapResponseToUser = (data: AuthResponseDTO): AuthUser => ({
  id: data.usuarioId,
  nombre: data.nombre,
  correoElectronico: data.correoElectronico,
  roles: data.roles,
});

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [usuario, setUsuario] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const esProfesor = usuario?.roles?.includes('ROLE_PROFESOR') ?? false;
  const esEstudiante = usuario?.roles?.includes('ROLE_ESTUDIANTE') ?? false;
  const esAdmin = usuario?.roles?.includes('ROLE_ADMIN') ?? false;
  const isAuthenticated = !!usuario;

  // Al montar, verificar si hay token guardado y recuperar sesión
  useEffect(() => {
    const initAuth = async () => {
      const token = authService.getAccessToken();
      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const data = await authService.me();
        setUsuario(mapResponseToUser(data));
      } catch {
        // Token inválido o expirado, intentar refresh
        try {
          const refreshData = await authService.refresh();
          setUsuario(mapResponseToUser(refreshData));
        } catch {
          // Refresh también falló, limpiar sesión
          authService.logout();
          setUsuario(null);
        }
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = useCallback(async (credentials: LoginRequestDTO) => {
    setLoading(true);
    try {
      const data = await authService.login(credentials);
      setUsuario(mapResponseToUser(data));
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    authService.logout();
    setUsuario(null);
  }, []);

  return (
    <AuthContext.Provider value={{
      usuario,
      loading,
      esProfesor,
      esEstudiante,
      esAdmin,
      login,
      logout,
      isAuthenticated,
    }}>
      {children}
    </AuthContext.Provider>
  );
};
