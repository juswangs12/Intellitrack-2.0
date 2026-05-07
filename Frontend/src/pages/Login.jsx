import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { GraduationCap } from "lucide-react";
import { useAuth } from "../context/AuthContext";

// Google "G" logo SVG (inline, no external dependency)
const GoogleIcon = () => (
  <svg width="20" height="20" viewBox="0 0 48 48" aria-hidden="true">
    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
  </svg>
);

const OAUTH2_URL = "http://localhost:8080/oauth2/authorization/google";

const navigateByRole = (role, nav) => {
  if (role === "student") nav("/student/home");
  else if (role === "adviser") nav("/adviser/home");
  else if (role === "coordinator") nav("/coordinator/home");
  else if (role === "administrator") nav("/admin/home");
  else nav("/");
};

const Login = () => {
  const [activeTab, setActiveTab] = useState("student");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const { login, handleOAuth2Callback, isAuthenticated, user } = useAuth();
  const navigate = useNavigate();

  // On mount: handle OAuth2 redirect params OR auto-redirect if already logged in
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");
    const userParam = params.get("user");
    const refreshToken = params.get("refreshToken");
    const oauthError = params.get("error");

    if (oauthError) {
      const msg = params.get("errorMsg") || oauthError;
      setError("Google sign-in failed: " + decodeURIComponent(msg));
      window.history.replaceState({}, "", "/");
      return;
    }

    if (token && userParam) {
      try {
        const parsedUser = JSON.parse(decodeURIComponent(userParam));
        handleOAuth2Callback({ token, refreshToken: refreshToken || null, user: parsedUser });
        window.history.replaceState({}, "", "/");
        navigateByRole(parsedUser.role, navigate);
      } catch {
        setError("Google sign-in failed: could not parse server response.");
        window.history.replaceState({}, "", "/");
      }
      return;
    }

    if (isAuthenticated()) {
      navigateByRole(user?.role, navigate);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleGoogleLogin = () => {
    window.location.href = OAUTH2_URL;
  };

  const handleStaffLogin = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    const result = await login(email, password);
    setLoading(false);
    if (result.success) {
      navigateByRole(result.role, navigate);
    } else {
      setError(result.error || "Invalid credentials. Please try again.");
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <div className="login-logo">
            <GraduationCap size={40} color="white" />
          </div>
          <h1 className="login-title">IntelliTrack</h1>
          <p className="login-subtitle">Capstone Management System</p>
        </div>

        {/* Role selector tabs */}
        <div className="login-tabs">
          <button
            type="button"
            className={"login-tab" + (activeTab === "student" ? " active" : "")}
            onClick={() => { setActiveTab("student"); setError(""); }}
          >
            Student
          </button>
          <button
            type="button"
            className={"login-tab" + (activeTab === "staff" ? " active" : "")}
            onClick={() => { setActiveTab("staff"); setError(""); }}
          >
            Staff / Adviser
          </button>
        </div>

        {error && (
          <div className="login-error" style={{ marginBottom: "1rem" }}>{error}</div>
        )}

        {/* Student: Google OAuth */}
        {activeTab === "student" && (
          <div className="login-form">
            <p style={{ textAlign: "center", color: "#6b7280", fontSize: "0.875rem", margin: 0 }}>
              Students sign in with their university Google account.
            </p>
            <button type="button" className="google-button" onClick={handleGoogleLogin}>
              <GoogleIcon />
              Continue with Google
            </button>
          </div>
        )}

        {/* Staff / Adviser / Coordinator / Admin: email + password */}
        {activeTab === "staff" && (
          <form className="login-form" onSubmit={handleStaffLogin}>
            <div className="form-group">
              <label className="form-label" htmlFor="email">Email Address</label>
              <input
                id="email"
                type="email"
                className="form-input"
                placeholder="you@university.edu"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="username"
              />
            </div>
            <div className="form-group">
              <label className="form-label" htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                className="form-input"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
            </div>
            <button type="submit" className="login-button" disabled={loading}>
              {loading ? "Signing in..." : "Sign In"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};

export default Login;
