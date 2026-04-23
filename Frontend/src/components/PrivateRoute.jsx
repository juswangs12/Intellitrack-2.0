import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * PrivateRoute Component - Protects routes that require authentication
 * Redirects to login if user is not authenticated
 */
const PrivateRoute = ({ children, requiredRole = null }) => {
  const { isAuthenticated, hasRole, user, loading } = useAuth();

  // Show loading state while auth is being initialized
  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner">Loading...</div>
      </div>
    );
  }

  // Check if user is authenticated
  if (!isAuthenticated()) {
    console.warn('PrivateRoute: User not authenticated');
    return <Navigate to="/" replace />;
  }

  // Check if user has required role (if specified)
  if (requiredRole && !hasRole(requiredRole)) {
    console.warn(`PrivateRoute: Role mismatch. User role: "${user?.role}", Required: "${requiredRole}"`);
    console.warn('User object:', user);
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
};

export default PrivateRoute;
