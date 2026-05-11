import React, { useEffect, useState } from "react";
import { Settings, Database, Shield, Bell, Save } from "lucide-react";
import apiService from "../../services/ApiService";

const SystemConfig = () => {
  const [config, setConfig] = useState(null);
  const [form, setForm] = useState({
    jwtExpiryHours: "24",
    sessionTimeoutMinutes: "60",
    emailDeadlines: true,
    notifyAdviserOnSubmission: true,
    adminAlerts: true,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await apiService.requestJson("/system-config");
        if (!mounted) return;

        setConfig(data);
        setForm({
          jwtExpiryHours: data.settings?.["security.jwtExpiryHours"] ?? "24",
          sessionTimeoutMinutes:
            data.settings?.["security.sessionTimeoutMinutes"] ?? "60",
          emailDeadlines:
            (data.settings?.["notifications.emailDeadlines"] ?? "true") ===
            "true",
          notifyAdviserOnSubmission:
            (data.settings?.["notifications.notifyAdviserOnSubmission"] ??
              "true") === "true",
          adminAlerts:
            (data.settings?.["notifications.adminAlerts"] ?? "true") === "true",
        });
      } catch (err) {
        if (mounted) {
          setError("Failed to load system configuration.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    load();

    return () => {
      mounted = false;
    };
  }, []);

  const save = async () => {
    setSaving(true);
    setError("");
    try {
      const updates = {
        "security.jwtExpiryHours": String(form.jwtExpiryHours),
        "security.sessionTimeoutMinutes": String(form.sessionTimeoutMinutes),
        "notifications.emailDeadlines": form.emailDeadlines ? "true" : "false",
        "notifications.notifyAdviserOnSubmission": form.notifyAdviserOnSubmission
          ? "true"
          : "false",
        "notifications.adminAlerts": form.adminAlerts ? "true" : "false",
      };

      const updated = await apiService.requestJson("/system-config", {
        method: "PUT",
        body: JSON.stringify(updates),
      });
      setConfig(updated);
    } catch (err) {
      setError("Failed to save configuration.");
    } finally {
      setSaving(false);
    }
  };

  const system = config?.system;
  const isH2 =
    (system?.databaseProduct || "").toLowerCase().includes("h2") ||
    (system?.datasourceUrl || "").toLowerCase().includes("h2");

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
        {loading && (
          <div
            className="card"
            style={{ gridColumn: "1 / -1", textAlign: "center", color: "#6b7280" }}
          >
            <div className="card-content" style={{ padding: "2rem" }}>
              Loading configuration...
            </div>
          </div>
        )}
        {error && (
          <div className="error-message" style={{ gridColumn: "1 / -1" }}>
            {error}
          </div>
        )}

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
            <input
              className="form-input"
              value={system?.databaseProduct || "Unknown"}
              disabled
            />
          </div>
          <div className="form-group" style={{ marginBottom: "1rem" }}>
            <label className="form-label">JDBC URL</label>
            <input
              className="form-input"
              value={system?.datasourceUrl || ""}
              disabled
            />
          </div>
          <div className="form-group">
            <label className="form-label">Console URL</label>
            {isH2 ? (
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
            ) : (
              <input className="form-input" value="Not available" disabled />
            )}
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
            <select
              className="form-select"
              value={form.jwtExpiryHours}
              onChange={(e) =>
                setForm((prev) => ({ ...prev, jwtExpiryHours: e.target.value }))
              }
            >
              <option value="24">24 hours</option>
              <option value="48">48 hours</option>
              <option value="168">7 days</option>
            </select>
          </div>
          <div className="form-group" style={{ marginBottom: "1rem" }}>
            <label className="form-label">Session Timeout</label>
            <select
              className="form-select"
              value={form.sessionTimeoutMinutes}
              onChange={(e) =>
                setForm((prev) => ({
                  ...prev,
                  sessionTimeoutMinutes: e.target.value,
                }))
              }
            >
              <option value="30">30 minutes</option>
              <option value="60">1 hour</option>
              <option value="120">2 hours</option>
            </select>
          </div>
          <button className="btn btn-primary" onClick={save} disabled={saving}>
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
            <label
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.75rem",
                cursor: "pointer",
              }}
            >
              <input
                type="checkbox"
                checked={form.emailDeadlines}
                onChange={(e) =>
                  setForm((prev) => ({
                    ...prev,
                    emailDeadlines: e.target.checked,
                  }))
                }
                style={{
                  width: "1rem",
                  height: "1rem",
                  accentColor: "var(--maroon)",
                }}
              />
              <span style={{ fontSize: "0.875rem" }}>
                Email notifications for deadline reminders
              </span>
            </label>
            <label
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.75rem",
                cursor: "pointer",
              }}
            >
              <input
                type="checkbox"
                checked={form.notifyAdviserOnSubmission}
                onChange={(e) =>
                  setForm((prev) => ({
                    ...prev,
                    notifyAdviserOnSubmission: e.target.checked,
                  }))
                }
                style={{
                  width: "1rem",
                  height: "1rem",
                  accentColor: "var(--maroon)",
                }}
              />
              <span style={{ fontSize: "0.875rem" }}>
                Notify adviser on new submission
              </span>
            </label>
            <label
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.75rem",
                cursor: "pointer",
              }}
            >
              <input
                type="checkbox"
                checked={form.adminAlerts}
                onChange={(e) =>
                  setForm((prev) => ({ ...prev, adminAlerts: e.target.checked }))
                }
                style={{
                  width: "1rem",
                  height: "1rem",
                  accentColor: "var(--maroon)",
                }}
              />
              <span style={{ fontSize: "0.875rem" }}>
                System alerts for admin
              </span>
            </label>
          </div>
          <button
            className="btn btn-primary"
            style={{ marginTop: "1rem" }}
            onClick={save}
            disabled={saving}
          >
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
              { label: "Application", value: system?.application || "IntelliTrack" },
              { label: "Backend", value: system?.springBoot ? `Spring Boot ${system.springBoot}` : "Spring Boot" },
              { label: "Frontend", value: "React 18.2" },
              { label: "Java Version", value: system?.javaVersion ? `Java ${system.javaVersion}` : "Java" },
              { label: "Environment", value: system?.environment || "development" },
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
