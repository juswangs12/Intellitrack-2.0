import React, { useEffect, useState } from "react";
import {
  Users,
  UserCheck,
  BookOpen,
  Clock,
  Activity,
  CheckCircle,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const AdminHome = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [groupsCount, setGroupsCount] = useState(0);
  const [pendingCount, setPendingCount] = useState(0);
  const [auditLogs, setAuditLogs] = useState([]);
  const [system, setSystem] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const [dashboard, groups, pending, audit, systemConfig] = await Promise.all([
          apiService.requestJson(`/dashboard/admin/${user.id}`),
          apiService.requestJson("/groups"),
          apiService.getPendingSubmissions(),
          apiService.requestJson("/audit?limit=10"),
          apiService.requestJson("/system-config"),
        ]);

        if (!mounted) return;

        setStats(dashboard);
        setGroupsCount(Array.isArray(groups) ? groups.length : 0);
        setPendingCount(Array.isArray(pending) ? pending.length : 0);
        setAuditLogs(Array.isArray(audit) ? audit : []);
        setSystem(systemConfig?.system || null);
      } catch (err) {
        if (mounted) {
          setError("Failed to load admin dashboard data.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    if (user?.id) {
      load();
    }

    return () => {
      mounted = false;
    };
  }, [user]);

  const cards = [
    { label: "Total Students", value: stats?.byRole?.students ?? 0, icon: Users, color: "maroon" },
    { label: "Total Advisers", value: stats?.byRole?.advisers ?? 0, icon: UserCheck, color: "gold" },
    { label: "Active Projects", value: groupsCount, icon: BookOpen, color: "green" },
    { label: "Pending Reviews", value: pendingCount, icon: Clock, color: "blue" },
  ];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Admin Dashboard</h1>
        <p className="page-description">
          System overview and administrative controls.
        </p>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="stats-grid">
        {cards.map((stat) => {
          const Icon = stat.icon;
          return (
            <div className="stat-card" key={stat.label}>
              <div className={`stat-icon ${stat.color}`}>
                <Icon style={{ width: "1.5rem", height: "1.5rem" }} />
              </div>
              <div className="stat-content">
                <p className="stat-label">{stat.label}</p>
                <p className="stat-value">{loading ? "—" : stat.value}</p>
              </div>
            </div>
          );
        })}
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "2fr 1fr",
          gap: "1.5rem",
        }}
      >
        <div className="card">
          <div className="card-header">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Activity
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">Recent Activity</h2>
            </div>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Action</th>
                  <th>Module</th>
                  <th>Actor</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan="4" style={{ textAlign: "center", padding: "2rem", color: "#6b7280" }}>
                      Loading activity...
                    </td>
                  </tr>
                ) : auditLogs.length === 0 ? (
                  <tr>
                    <td colSpan="4" style={{ textAlign: "center", padding: "2rem", color: "#6b7280" }}>
                      No audit activity recorded yet.
                    </td>
                  </tr>
                ) : (
                  auditLogs.map((log) => (
                    <tr key={log.id}>
                      <td>{log.action}</td>
                      <td style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                        {log.targetModule}
                      </td>
                      <td style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                        {log.performedBy}
                      </td>
                      <td style={{ color: "#9ca3af", fontSize: "0.75rem" }}>
                        {log.timestamp ? new Date(log.timestamp).toLocaleString() : "—"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <CheckCircle
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">System Health</h2>
            </div>
          </div>
          <div
            style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}
          >
            {[
              { service: "Backend API", status: "operational", detail: "Authenticated endpoints responding" },
              { service: "Database", status: system?.databaseProduct ? "operational" : "unknown", detail: system?.databaseProduct || "Unknown" },
              { service: "Environment", status: "active", detail: system?.environment || "development" },
              { service: "Java Runtime", status: "active", detail: system?.javaVersion ? `Java ${system.javaVersion}` : "Java" },
            ].map((s, i) => (
              <div
                key={i}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  padding: "0.75rem",
                  border: "1px solid #e5e7eb",
                  borderRadius: "0.5rem",
                }}
              >
                <div>
                  <p style={{ fontWeight: "500", fontSize: "0.875rem", margin: 0 }}>
                    {s.service}
                  </p>
                  <p style={{ fontSize: "0.75rem", color: "#6b7280", margin: "0.125rem 0 0" }}>
                    {s.detail}
                  </p>
                </div>
                <span className="badge success" style={{ fontSize: "0.7rem" }}>
                  <CheckCircle style={{ width: "0.75rem", height: "0.75rem", marginRight: "0.25rem" }} />
                  {s.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminHome;
