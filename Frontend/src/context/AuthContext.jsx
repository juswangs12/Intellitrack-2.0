import React, { createContext, useState, useContext, useEffect } from 'react';

// Create the Auth Context
const AuthContext = createContext(null);

// Auth Provider Component
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [refreshToken, setRefreshToken] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Initialize auth state from localStorage on mount
  useEffect(() => {
    // Don't auto-restore from localStorage - always start fresh at login
    setLoading(false);
  }, []);

  /**
   * Login with email and password
   */
  const login = async (email, password) => {
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
        console.log('Login response received:', data);
        setUser(data.user);
        setToken(data.token);
        setRefreshToken(data.refreshToken);

        localStorage.setItem('user', JSON.stringify(data.user));
        localStorage.setItem('token', data.token);
        localStorage.setItem('refreshToken', data.refreshToken);

        console.log('Auth state updated. User role:', data.user?.role);
        return { success: true, role: data.role };
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
  };

  /**
   * Handle OAuth2 callback (Google login)
   */
  const handleOAuth2Callback = (callbackData) => {
    setUser(callbackData.user);
    setToken(callbackData.token);
    setRefreshToken(callbackData.refreshToken);

    localStorage.setItem('user', JSON.stringify(callbackData.user));
    localStorage.setItem('token', callbackData.token);
    localStorage.setItem('refreshToken', callbackData.refreshToken);
  };

  /**
   * Refresh access token
   */
  const refreshAccessToken = async () => {
    if (!refreshToken) {
      logout();
      return false;
    }

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
        logout();
        return false;
      }
    } catch (err) {
      console.error('Token refresh failed:', err);
      logout();
      return false;
    }
  };

  /**
   * Logout user
   */
  const logout = () => {
    setUser(null);
    setToken(null);
    setRefreshToken(null);
    setError(null);

    localStorage.removeItem('user');
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
  };

  /**
   * Update user profile
   */
  const updateProfile = (updatedUser) => {
    setUser(updatedUser);
    localStorage.setItem('user', JSON.stringify(updatedUser));
  };

  /**
   * Get authorization header
   */
  const getAuthHeader = () => {
    return token ? { Authorization: `Bearer ${token}` } : {};
  };

  /**
   * Check if user is authenticated
   */
  const isAuthenticated = () => {
    return token !== null && user !== null;
  };

  /**
   * Check if user has specific role
   */
  const hasRole = (role) => {
    const hasIt = user && user.role === role;
    if (!hasIt && user) {
      console.debug(`Role check failed: user.role="${user.role}" vs required="${role}"`);
    }
    return hasIt;
  };

  const value = {
    user,
    token,
    refreshToken,
    loading,
    error,
    login,
    logout,
    handleOAuth2Callback,
    refreshAccessToken,
    updateProfile,
    getAuthHeader,
    isAuthenticated,
    hasRole,
  };

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
