import React, { createContext, useContext, useState, useEffect } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('claimwise_user');
    return savedUser ? JSON.parse(savedUser) : null;
  });

  const [token, setToken] = useState(() => {
    return localStorage.getItem('claimwise_token') || null;
  });

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const initAuth = async () => {
      if (token) {
        try {
          const response = await authService.getCurrentUser();
          if (response.data) {
            setUser(response.data);
            localStorage.setItem('claimwise_user', JSON.stringify(response.data));
          }
        } catch (error) {
          console.warn('Session expired or invalid token:', error);
          logout();
        }
      }
      setLoading(false);
    };

    initAuth();
  }, [token]);

  const login = async (email, password) => {
    const response = await authService.login(email, password);
    const authData = response.data;
    setToken(authData.token);
    setUser({
      id: authData.userId,
      fullName: authData.fullName,
      email: authData.email,
      role: authData.role,
    });
    localStorage.setItem('claimwise_token', authData.token);
    localStorage.setItem('claimwise_user', JSON.stringify({
      id: authData.userId,
      fullName: authData.fullName,
      email: authData.email,
      role: authData.role,
    }));
    return authData;
  };

  const register = async (fullName, email, password, role) => {
    const response = await authService.register(fullName, email, password, role);
    const authData = response.data;
    setToken(authData.token);
    setUser({
      id: authData.userId,
      fullName: authData.fullName,
      email: authData.email,
      role: authData.role,
    });
    localStorage.setItem('claimwise_token', authData.token);
    localStorage.setItem('claimwise_user', JSON.stringify({
      id: authData.userId,
      fullName: authData.fullName,
      email: authData.email,
      role: authData.role,
    }));
    return authData;
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('claimwise_user');
    localStorage.removeItem('claimwise_token');
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
