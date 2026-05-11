import React, { createContext, useState, useContext, useEffect } from 'react';
import apiService from '../services/ApiService';

// Create the Auth Context
const AuthContext = createContext(null);

// Auth Provider Component
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [refreshToken, setRefreshToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const value = {
    user,
    token,
    refreshToken,
    loading,
    error,
    login: async (email, password) => {
      setLoading(true);
      setError(null);
  
      try {
        const response = await fetch('http://localhost:8080/api/auth/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ email, password }),
        });
  
        if (response.ok) {
          const data = await response.json();
          setUser(data.user);
          setToken(data.token);
          setRefreshToken(data.refreshToken);
  
          localStorage.setItem('user', JSON.stringify(data.user));
          localStorage.setItem('token', data.token);
          localStorage.setItem('refreshToken', data.refreshToken);
  
          return { success: true, role: data.user.role };
        } else {
          const errorMsg = 'Invalid credentials';
          setError(errorMsg);
          return { success: false, error: errorMsg };
        }
      } catch (err) {
        const errorMsg = 'Network error. Please check your connection.';
        setError(errorMsg);
        return { success: false, error: errorMsg };
      } finally {
        setLoading(false);
      }
    },
    logout: () => {
      setUser(null);
      setToken(null);
      setRefreshToken(null);
      setError(null);
  
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
    },
    handleOAuth2Callback: (callbackData) => {
      const { user, token, refreshToken } = callbackData;
      setUser(user);
      setToken(token);
      setRefreshToken(refreshToken);
  
      localStorage.setItem('user', JSON.stringify(user));
      localStorage.setItem('token', token);
      localStorage.setItem('refreshToken', refreshToken || '');
    },
    refreshAccessToken: async () => {
      if (!refreshToken) return false;
  
      try {
        const response = await fetch(
          `http://localhost:8080/api/auth/refresh-token?refreshToken=${refreshToken}`,
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
          }
        );
  
        if (response.ok) {
          const data = await response.json();
          setToken(data.token);
          localStorage.setItem('token', data.token);
          return true;
        } else {
          return false;
        }
      } catch (err) {
        return false;
      }
    },
    updateProfile: (updatedUser) => {
      setUser(updatedUser);
      localStorage.setItem('user', JSON.stringify(updatedUser));
    },
    getAuthHeader: () => (token ? { Authorization: `Bearer ${token}` } : {}),
    isAuthenticated: () => token !== null && user !== null,
    hasRole: (role) => user && user.role === role,
  };

  // Connect apiService to this context
  useEffect(() => {
    apiService.setAuthContext(value);
  }, [value]);

  // Initialize auth state from localStorage on mount
  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    const savedToken = localStorage.getItem('token');
    const savedRefreshToken = localStorage.getItem('refreshToken');

    if (savedUser && savedToken) {
      try {
        setUser(JSON.parse(savedUser));
        setToken(savedToken);
        setRefreshToken(savedRefreshToken);
      } catch (e) {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
      }
    }
    setLoading(false);
  }, []);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// Custom hook to use Auth Context
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
