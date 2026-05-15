import React, { useState } from "react";
import { Link } from "react-router-dom";
import "../styles/Login.css";

const API_BASE = process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await fetch(`${API_BASE}/api/auth/forgot-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });
      // Always show success to avoid leaking whether the email is registered
      setSubmitted(true);
    } catch {
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="login-container">
        <div className="login-card" style={{ maxWidth: 420 }}>
          <h2 className="login-title">Check your email</h2>
          <p style={{ marginBottom: "1.5rem", color: "#4b5563" }}>
            If an account with that email exists, we have sent a password reset
            link. The link expires in 1 hour.
          </p>
          <Link
            to="/login"
            className="btn-primary"
            style={{ display: "block", textAlign: "center" }}
          >
            Back to Login
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="login-container">
      <div className="login-card" style={{ maxWidth: 420 }}>
        <h2 className="login-title">Forgot Password</h2>
        <p style={{ marginBottom: "1.5rem", color: "#4b5563" }}>
          Enter your institutional email address and we will send you a link to
          reset your password.
        </p>
        {error && (
          <p style={{ color: "#dc2626", marginBottom: "1rem" }}>{error}</p>
        )}
        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: "1.25rem" }}>
            <label className="form-label" htmlFor="email">
              Email Address
            </label>
            <input
              id="email"
              type="email"
              className="form-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoFocus
              placeholder="you@institution.edu"
            />
          </div>
          <button
            type="submit"
            className="btn-primary"
            style={{ width: "100%" }}
            disabled={loading}
          >
            {loading ? "Sending…" : "Send Reset Link"}
          </button>
        </form>
        <p
          style={{
            marginTop: "1rem",
            textAlign: "center",
            fontSize: "0.875rem",
          }}
        >
          <Link to="/login">Back to Login</Link>
        </p>
      </div>
    </div>
  );
}
