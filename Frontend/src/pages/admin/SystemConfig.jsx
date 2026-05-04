import React from "react";
import { Settings, Database, Shield, Bell, Save } from "lucide-react";

const SystemConfig = () => {
  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">System Configuration</h1>
        <p className="page-description">
          Configure system-wide settings and preferences.
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "1.5rem",
        }}
      >
        <div className="card">
          <div className="card-header">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Database
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">Database Settings</h2>
            </div>
          </div>
          <div className="form-group" style={{ marginBottom: "1rem" }}>
            <label className="form-label">Database Type</label>
            <input className="form-input" value="H2 In-Memory" disabled />
          </div>
          <div className="form-group" style={{ marginBottom: "1rem" }}>
            <label className="form-label">JDBC URL</label>
            <input
              className="form-input"
              value="jdbc:h2:mem:intellitrack"
              disabled
            />
          </div>
          <div className="form-group">
            <label className="form-label">Console URL</label>
            <a
              href="http://localhost:8080/h2-console"
              target="_blank"
              rel="noreferrer"
              style={{
                display: "block",
                padding: "0.75rem",
                border: "1px solid #d1d5db",
                borderRadius: "0.5rem",
                color: "var(--maroon)",
                textDecoration: "none",
                fontSize: "0.875rem",
              }}
            >
              http://localhost:8080/h2-console ↗
            </a>
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Shield
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">Security Settings</h2>
            </div>
          </div>
          <div className="form-group" style={{ marginBottom: "1rem" }}>
            <label className="form-label">JWT Token Expiry</label>
            <select className="form-select">
              <option>24 hours</option>
              <option>48 hours</option>
              <option>7 days</option>
            </select>
          </div>
          <div className="form-group" style={{ marginBottom: "1rem" }}>
            <label className="form-label">Session Timeout</label>
            <select className="form-select">
              <option>30 minutes</option>
              <option>1 hour</option>
              <option>2 hours</option>
            </select>
          </div>
          <button className="btn btn-primary">
            <Save style={{ width: "1rem", height: "1rem" }} /> Save Security
            Settings
          </button>
        </div>

        <div className="card">
          <div className="card-header">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Bell
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">Notification Settings</h2>
            </div>
          </div>
          <div
            style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}
          >
            {[
              "Email notifications for deadline reminders",
              "Notify adviser on new submission",
              "System alerts for admin",
            ].map((label, i) => (
              <label
                key={i}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.75rem",
                  cursor: "pointer",
                }}
              >
                <input
                  type="checkbox"
                  defaultChecked
                  style={{
                    width: "1rem",
                    height: "1rem",
                    accentColor: "var(--maroon)",
                  }}
                />
                <span style={{ fontSize: "0.875rem" }}>{label}</span>
              </label>
            ))}
          </div>
          <button className="btn btn-primary" style={{ marginTop: "1rem" }}>
            <Save style={{ width: "1rem", height: "1rem" }} /> Save Preferences
          </button>
        </div>

        <div className="card">
          <div className="card-header">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Settings
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">System Info</h2>
            </div>
          </div>
          <div
            style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}
          >
            {[
              { label: "Application", value: "IntelliTrack 2.0" },
              { label: "Backend", value: "Spring Boot 3.x" },
              { label: "Frontend", value: "React 18.2" },
              { label: "Java Version", value: "Java 17" },
              { label: "Environment", value: "Development" },
            ].map((item, i) => (
              <div
                key={i}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  padding: "0.5rem 0",
                  borderBottom: "1px solid #f3f4f6",
                }}
              >
                <span style={{ color: "#6b7280", fontSize: "0.875rem" }}>
                  {item.label}
                </span>
                <span style={{ fontWeight: "500", fontSize: "0.875rem" }}>
                  {item.value}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SystemConfig;
