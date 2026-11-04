import React, { useState, useEffect } from "react";
import { useNavigate, Link, useLocation } from "react-router-dom";
import BrandLogo from "../components/BrandLogo";
import { useAuth } from "../context/AuthContext";

// Google "G" logo SVG (inline, no external dependency)
const GoogleIcon = () => (
  <svg width="20" height="20" viewBox="0 0 48 48" aria-hidden="true">
    <path
      fill="#EA4335"
      d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
    />
    <path
      fill="#4285F4"
      d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
    />
    <path
      fill="#FBBC05"
      d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"
    />
    <path
      fill="#34A853"
      d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
    />
  </svg>
);

const OAUTH2_URL = `${process.env.REACT_APP_API_BASE_URL || "http://localhost:8080"}/oauth2/authorization/google`;

const navigateByRole = (role, nav) => {
  if (role === "student") nav("/student/home");
  else if (role === "adviser") nav("/adviser/home");
  else if (role === "coordinator") nav("/coordinator/home");
  else if (role === "administrator") nav("/admin/home");
  else nav("/");
};

const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const { login, handleOAuth2Callback, isAuthenticated, user, loading:authLoading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const audience = location.pathname === '/staff/login' ? 'staff' : 'student';
  const successMessage = location.state?.message || "";

  // On mount: handle OAuth2 redirect params OR auto-redirect if already logged in
  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.includes("token=") ? window.location.hash.substring(1) : window.location.search);
    const token = params.get("token");
    const userParam = params.get("user");
    const refreshToken = params.get("refreshToken");
    const oauthError = params.get("error");

    if (oauthError) {
      const msg = params.get("errorMsg") || oauthError;
      setError("Google sign-in failed: " + msg);
      window.history.replaceState({}, "", "/");
      return;
    }

    if (token && userParam) {
      window.history.replaceState({}, "", "/");
      handleOAuth2Callback({token,refreshToken:refreshToken||null})
        .then(verifiedUser=>navigateByRole(verifiedUser.role,navigate))
        .catch(e=>setError(e.message||"Google sign-in could not be verified."));
      return;
    }

    if (isAuthenticated()) {
      navigateByRole(user?.role, navigate);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(()=>{if(!authLoading&&isAuthenticated())navigateByRole(user?.role,navigate);},[authLoading,user,navigate]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGoogleLogin = () => {
    window.location.href = `${OAUTH2_URL}?audience=${audience}`;
  };

  const handleEmailLogin = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    const result = await login(email, password, audience);
    setLoading(false);
    if (result.success) {
      navigateByRole(result.role, navigate);
    } else {
      setError(result.error || "Invalid credentials. Please try again.");
    }
  };

  return (
    <div className="login-container" role="main">
      <div className="login-card" aria-label="IntelliTrack Login">
        <div className="login-header">
          <BrandLogo login />
          <h1 className="login-title">IntelliTrack</h1>
          <p className="login-subtitle">Capstone Submission Tracking System</p>
        </div>

        <p className="login-forgot">
          {audience === 'staff' ? 'Staff sign-in for administrators, coordinators, and advisers.' : 'Student sign-in for your capstone requirements and submissions.'}
        </p>
        <nav className="login-portals" aria-label="Sign-in portal">
          <Link to="/student/login" aria-current={audience==='student'?'page':undefined} onClick={()=>{setError('');setPassword('');}}>Student sign-in</Link>
          <Link to="/staff/login" aria-current={audience==='staff'?'page':undefined} onClick={()=>{setError('');setPassword('');}}>Staff sign-in</Link>
        </nav>

        {successMessage && (
          <div
            className="login-success"
            role="alert"
            aria-live="polite"
          >
            {successMessage}
          </div>
        )}

        {error && (
          <div className="login-error" role="alert" aria-live="assertive">
            {error}
          </div>
        )}

        <form className="login-form" onSubmit={handleEmailLogin}>
          {audience==='staff'&&<>
          <div className="form-group">
            <label className="form-label" htmlFor="email">
              Email Address
            </label>
            <input
              id="email"
              type="email"
              className="form-input"
              placeholder="you@university.edu"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="username"
              aria-label="Email address"
            />
          </div>
          <div className="form-group">
            <label className="form-label" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              type="password"
              className="form-input"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              aria-label="Password"
            />
          </div>
          <button type="submit" className="login-button" disabled={loading} aria-label="Sign in">
            {loading ? "Signing in..." : "Sign In"}
          </button>

          {/* Separator */}
          <div className="login-separator" aria-hidden="true">
            <span className="login-separator-line" />
            <span className="login-separator-text">OR</span>
            <span className="login-separator-line" />
          </div>
          </>}

          {/* Google Sign In Button */}
          <button
            type="button"
            className="google-button"
            onClick={handleGoogleLogin}
            aria-label="Continue with Google"
          >
            <GoogleIcon />
            Continue with Google
          </button>

          {audience==='staff'&&<p className="login-forgot">
            <Link to="/forgot-password">Forgot password?</Link>
          </p>}
          <p className="login-forgot">
            {audience === 'staff' ? 'Your administrator must register your exact Google email with a staff role before your first sign-in.' : 'Use the Google email on your classlist so your enrollments can be linked to your account.'}
          </p>
        </form>
      </div>
    </div>
  );
};

export default Login;
