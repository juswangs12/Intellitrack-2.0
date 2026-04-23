import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import '../styles/Login.css';

const Login = () => {
  const [authMethod, setAuthMethod] = useState(''); // 'student', 'staff', ''
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { login, handleOAuth2Callback, isAuthenticated } = useAuth();
  const [searchParams] = useSearchParams();

  // Check for OAuth2 callback on mount
  useEffect(() => {
    // Handle OAuth2 callback from backend
    const tokenParam = searchParams.get('token');
    const refreshTokenParam = searchParams.get('refreshToken');
    const userParam = searchParams.get('user');
    const roleParam = searchParams.get('role');
    const errorParam = searchParams.get('error');

    if (errorParam) {
      const errorMsg = searchParams.get('errorMsg');
      console.error('OAuth error code:', errorParam, 'details:', errorMsg);
      setError(`OAuth login failed: ${errorParam}${errorMsg ? ' - ' + decodeURIComponent(errorMsg) : ''}`);
      return;
    }

    if (tokenParam && userParam) {
      try {
        const parsedUser = JSON.parse(userParam);
        handleOAuth2Callback({ user: parsedUser, token: tokenParam, refreshToken: refreshTokenParam });
        // Route based on role - map administrator to admin
        const route = roleParam === 'administrator' ? '/admin' : `/${roleParam || 'student'}`;
        navigate(route, { replace: true });
      } catch (err) {
        console.error('OAuth callback parsing error:', err);
        setError('OAuth authentication failed: Invalid response');
      }
    }
  }, []);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleStaffLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const result = await login(formData.email, formData.password);

      if (result.success) {
        // Navigate based on role - map administrator to admin
        const route = result.role === 'administrator' ? '/admin' : `/${result.role}`;
        navigate(route);
      } else {
        setError(result.error || 'Login failed. Please try again.');
      }
    } catch (err) {
      setError('Network error. Please check your connection.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    // Redirect to Google OAuth on the backend
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  const renderAuthSelection = () => (
    <div className="auth-selection">
      <h2>Welcome to IntelliTrack</h2>
      <p>Please select your login method:</p>

      <div className="auth-options">
        <button
          className="auth-option student-option"
          onClick={() => setAuthMethod('student')}
        >
          <div className="option-icon">🎓</div>
          <div className="option-content">
            <h3>Student Login</h3>
            <p>Login with Google Account</p>
          </div>
        </button>

        <button
          className="auth-option staff-option"
          onClick={() => setAuthMethod('staff')}
        >
          <div className="option-icon">👨‍🏫</div>
          <div className="option-content">
            <h3>Staff Login</h3>
            <p>Adviser / Coordinator / Admin</p>
          </div>
        </button>
      </div>
    </div>
  );

  const renderStudentLogin = () => (
    <div className="login-form-container">
      <div className="login-header">
        <h1>🎓 Student Login</h1>
        <p>Sign in with your Google Account</p>
      </div>

      <button
        className="google-login-btn"
        onClick={handleGoogleLogin}
      >
        <img src="https://developers.google.com/identity/images/g-logo.png" alt="Google" />
        Continue with Google
      </button>

      <button
        className="back-btn"
        onClick={() => setAuthMethod('')}
      >
        ← Back to Login Options
      </button>
    </div>
  );

  const renderStaffLogin = () => (
    <div className="login-form-container">
      <div className="login-header">
        <h1>👨‍🏫 Staff Login</h1>
        <p>Enter your credentials</p>
      </div>

      <form onSubmit={handleStaffLogin} className="login-form">
        <div className="form-group">
          <label htmlFor="email">Email Address</label>
          <input
            type="email"
            id="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
            placeholder="Enter your email"
          />
        </div>

        <div className="form-group">
          <label htmlFor="password">Password</label>
          <input
            type="password"
            id="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
            placeholder="Enter your password"
          />
        </div>

        {error && <div className="error-message">{error}</div>}

        <button type="submit" className="login-btn" disabled={loading}>
          {loading ? 'Signing In...' : 'Sign In'}
        </button>
      </form>

      <button
        className="back-btn"
        onClick={() => setAuthMethod('')}
      >
        ← Back to Login Options
      </button>
    </div>
  );

  return (
    <div className="login-container">
      <div className="login-card">
        {!authMethod && renderAuthSelection()}
        {authMethod === 'student' && renderStudentLogin()}
        {authMethod === 'staff' && renderStaffLogin()}
      </div>
    </div>
  );
};

export default Login;