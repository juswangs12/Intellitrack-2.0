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

const AdminHome = () => {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    const token = localStorage.getItem("token");
    fetch(`http://localhost:8080/api/dashboard/admin/${user.id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setData(d))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [user]);

  const byRole = data?.byRole ?? {};

  const stats = [
    {
      label: "Total Students",
      value: loading ? "…" : (byRole.students ?? 0),
      icon: Users,
      color: "maroon",
    },
    {
      label: "Total Advisers",
      value: loading ? "…" : (byRole.advisers ?? 0),
      icon: UserCheck,
      color: "gold",
    },
    {
      label: "Coordinators",
      value: loading ? "…" : (byRole.coordinators ?? 0),
      icon: BookOpen,
      color: "green",
    },
    {
      label: "Total Users",
      value: loading ? "…" : (data?.totalUsers ?? 0),
      icon: Clock,
      color: "blue",
    },
  ];

  const systemHealth = [
    { service: "Backend API", status: "operational" },
    { service: "Database", status: "operational" },
    { service: "Auth Service", status: "operational" },
  ];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Admin Dashboard</h1>
        <p className="page-description">
          System overview and administrative controls.
        </p>
      </div>

      <div className="stats-grid">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div className="stat-card" key={stat.label}>
              <div className={`stat-icon ${stat.color}`}>
                <Icon style={{ width: "1.5rem", height: "1.5rem" }} />
              </div>
              <div className="stat-content">
                <p className="stat-label">{stat.label}</p>
                <p className="stat-value">{stat.value}</p>
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
              <h2 className="card-title">User Summary</h2>
            </div>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Role</th>
                  <th>Count</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(byRole).map(([role, count]) => (
                  <tr key={role}>
                    <td style={{ textTransform: "capitalize" }}>{role}</td>
                    <td>{count}</td>
                  </tr>
                ))}
                {Object.keys(byRole).length === 0 && (
                  <tr>
                    <td
                      colSpan={2}
                      style={{ color: "#6b7280", textAlign: "center" }}
                    >
                      No users yet.
                    </td>
                  </tr>
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
            {systemHealth.map((s, i) => (
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
                <p
                  style={{ fontWeight: "500", fontSize: "0.875rem", margin: 0 }}
                >
                  {s.service}
                </p>
                <span className="badge success" style={{ fontSize: "0.7rem" }}>
                  <CheckCircle
                    style={{
                      width: "0.75rem",
                      height: "0.75rem",
                      marginRight: "0.25rem",
                    }}
                  />
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
