import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Login from './pages/Login';
import StudentDashboard from './pages/StudentDashboard';
import AdviserDashboard from './pages/AdviserDashboard';
import CoordinatorDashboard from './pages/CoordinatorDashboard';
import AdminDashboard from './pages/AdminDashboard';
import UserProfile from './components/UserProfile';
import PrivateRoute from './components/PrivateRoute';
import './styles/App.css';

const UnauthorizedPage = () => (
  <div style={{ padding: '40px', textAlign: 'center' }}>
    <h1>Access Denied</h1>
    <p>You do not have permission to access this page.</p>
    <a href="/">Return to Login</a>
  </div>
);

function App() {
  return (
    <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AuthProvider>
        <div className="app">
          <Routes>
            <Route path="/" element={<Login />} />
            
            {/* Protected Routes */}
            <Route
              path="/student"
              element={
                <PrivateRoute requiredRole="student">
                  <StudentDashboard />
                </PrivateRoute>
              }
            />
            <Route
              path="/adviser"
              element={
                <PrivateRoute requiredRole="adviser">
                  <AdviserDashboard />
                </PrivateRoute>
              }
            />
            <Route
              path="/coordinator"
              element={
                <PrivateRoute requiredRole="coordinator">
                  <CoordinatorDashboard />
                </PrivateRoute>
              }
            />
            <Route
              path="/admin"
              element={
                <PrivateRoute requiredRole="administrator">
                  <AdminDashboard />
                </PrivateRoute>
              }
            />
            
            {/* Profile Routes */}
            <Route
              path="/profile/:id"
              element={
                <PrivateRoute>
                  <UserProfile />
                </PrivateRoute>
              }
            />
            
            {/* Unauthorized Page */}
            <Route path="/unauthorized" element={<UnauthorizedPage />} />
          </Routes>
        </div>
      </AuthProvider>
    </Router>
  );
}

export default App;