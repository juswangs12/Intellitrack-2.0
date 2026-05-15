import React, { useState } from "react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import "../styles/Login.css";

const API_BASE = process.env.REACT_APP_API_BASE_URL || "http://localhost:8080";

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const navigate = useNavigate();

  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (newPassword.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    if (newPassword !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    if (!token) {
      setError("Invalid or missing reset token. Please request a new link.");
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/auth/reset-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, newPassword }),
      });
      if (response.ok) {
        navigate("/login", {
          state: { message: "Password reset successfully. Please log in." },
        });
      } else {
        setError(
          "The reset link is invalid or has expired. Please request a new one.",
        );
      }
    } catch {
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card" style={{ maxWidth: 420 }}>
        <h2 className="login-title">Reset Password</h2>
        {error && (
          <p style={{ color: "#dc2626", marginBottom: "1rem" }}>{error}</p>
        )}
        <form onSubmit={handleSubmit}>
          <div className="form-group" style={{ marginBottom: "1.25rem" }}>
            <label className="form-label" htmlFor="newPassword">
              New Password
            </label>
            <input
              id="newPassword"
              type="password"
              className="form-input"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              autoFocus
              placeholder="At least 8 characters"
              minLength={8}
            />
          </div>
          <div className="form-group" style={{ marginBottom: "1.5rem" }}>
            <label className="form-label" htmlFor="confirm">
              Confirm New Password
            </label>
            <input
              id="confirm"
              type="password"
              className="form-input"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              required
              placeholder="Repeat new password"
            />
          </div>
          <button
            type="submit"
            className="btn-primary"
            style={{ width: "100%" }}
            disabled={loading || !token}
          >
            {loading ? "Resetting…" : "Reset Password"}
          </button>
        </form>
        {!token && (
          <p
            style={{
              marginTop: "1rem",
              textAlign: "center",
              fontSize: "0.875rem",
            }}
          >
            <Link to="/forgot-password">Request a new reset link</Link>
          </p>
        )}
      </div>
    </div>
  );
}
