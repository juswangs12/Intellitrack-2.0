import React from "react";
import {
  BarChart2,
  TrendingUp,
  Users,
  FileText,
  CheckCircle,
  Clock,
} from "lucide-react";

const Analytics = () => {
  const submissionStats = [
    { label: "Total Submissions", value: 142, icon: FileText, color: "maroon" },
    { label: "Approved", value: 89, icon: CheckCircle, color: "green" },
    { label: "Pending", value: 38, icon: Clock, color: "gold" },
    { label: "Rejected", value: 15, icon: TrendingUp, color: "blue" },
  ];

  const byRole = [
    { role: "Students", count: 48, submissions: 142, approvalRate: "62%" },
    { role: "Advisers", count: 12, submissions: 89, approvalRate: "74%" },
    { role: "Coordinators", count: 5, submissions: 36, approvalRate: "81%" },
  ];

  const submissionTrend = [
    { month: "Oct", count: 12 },
    { month: "Nov", count: 28 },
    { month: "Dec", count: 45 },
    { month: "Jan", count: 32 },
    { month: "Feb", count: 25 },
  ];

  const maxCount = Math.max(...submissionTrend.map((t) => t.count));

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
                  <th>Users</th>
                  <th>Approval</th>
                </tr>
              </thead>
              <tbody>
                {byRole.map((r, i) => (
                  <tr key={i}>
                    <td>{r.role}</td>
                    <td>{r.count}</td>
                    <td>
                      <span className="badge success">{r.approvalRate}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
