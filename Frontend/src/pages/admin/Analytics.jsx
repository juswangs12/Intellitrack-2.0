import React, { useEffect, useState } from "react";
import {
  BarChart2,
  TrendingUp,
  Users,
  FileText,
  CheckCircle,
  Clock,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";

const Analytics = () => {
  const { user } = useAuth();
  const [trackingData, setTrackingData] = useState(null);
  const [adminData, setAdminData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    const token = localStorage.getItem("token");
    const headers = { Authorization: `Bearer ${token}` };

    Promise.all([
      fetch("http://localhost:8080/api/analytics/tracking", { headers }).then(
        (r) => r.json(),
      ),
      fetch(`http://localhost:8080/api/dashboard/admin/${user.id}`, {
        headers,
      }).then((r) => r.json()),
    ])
      .then(([tracking, admin]) => {
        setTrackingData(tracking);
        setAdminData(admin);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user]);

  const byRole = adminData?.byRole ?? {};

  const submissionStats = [
    {
      label: "Total Students",
      value: loading ? "…" : (byRole.students ?? 0),
      icon: FileText,
      color: "maroon",
    },
    {
      label: "Total Advisers",
      value: loading ? "…" : (byRole.advisers ?? 0),
      icon: CheckCircle,
      color: "green",
    },
    {
      label: "Coordinators",
      value: loading ? "…" : (byRole.coordinators ?? 0),
      icon: Clock,
      color: "gold",
    },
    {
      label: "Total Users",
      value: loading ? "…" : (adminData?.totalUsers ?? 0),
      icon: TrendingUp,
      color: "blue",
    },
  ];

  const submissionTrend = [];
  const maxCount = 1;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Analytics</h1>
        <p className="page-description">
          Submission trends and system usage statistics.
        </p>
      </div>

      <div className="stats-grid">
        {submissionStats.map((stat) => {
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
              <BarChart2
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">Submission Trend</h2>
            </div>
          </div>
          <div
            style={{
              display: "flex",
              alignItems: "flex-end",
              gap: "1rem",
              height: "160px",
              padding: "0 0.5rem",
            }}
          >
            {submissionTrend.map((t) => (
              <div
                key={t.month}
                style={{
                  flex: 1,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "0.5rem",
                  height: "100%",
                  justifyContent: "flex-end",
                }}
              >
                <span
                  style={{
                    fontSize: "0.75rem",
                    fontWeight: "600",
                    color: "#374151",
                  }}
                >
                  {t.count}
                </span>
                <div
                  style={{
                    width: "100%",
                    background:
                      "linear-gradient(180deg, var(--maroon), var(--maroon-light))",
                    borderRadius: "0.25rem 0.25rem 0 0",
                    height: `${(t.count / maxCount) * 120}px`,
                    minHeight: "4px",
                  }}
                />
                <span style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                  {t.month}
                </span>
              </div>
            ))}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Users
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">By Role</h2>
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
                      No data yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
