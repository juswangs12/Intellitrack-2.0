import React from "react";
import {
  Users,
  UserCheck,
  BookOpen,
  Clock,
  Activity,
  CheckCircle,
} from "lucide-react";

const AdminHome = ({ user }) => {
  const stats = [
    { label: "Total Students", value: "48", icon: Users, color: "maroon" },
    { label: "Total Advisers", value: "12", icon: UserCheck, color: "gold" },
    { label: "Active Projects", value: "36", icon: BookOpen, color: "green" },
    { label: "Pending Reviews", value: "9", icon: Clock, color: "blue" },
  ];

  const recentActivity = [
    {
      action: "New user registered",
      user: "student@example.com",
      role: "student",
      time: "5 mins ago",
      type: "info",
    },
    {
      action: "Deadline created",
      user: "admin@university.edu",
      role: "administrator",
      time: "1 hour ago",
      type: "success",
    },
    {
      action: "Document approved",
      user: "adviser@university.edu",
      role: "adviser",
      time: "2 hours ago",
      type: "success",
    },
    {
      action: "Submission rejected",
      user: "coordinator@university.edu",
      role: "coordinator",
      time: "3 hours ago",
      type: "danger",
    },
    {
      action: "User role updated",
      user: "admin@university.edu",
      role: "administrator",
      time: "5 hours ago",
      type: "warning",
    },
  ];

  const systemHealth = [
    { service: "Backend API", status: "operational", uptime: "99.9%" },
    { service: "Database (H2)", status: "operational", uptime: "100%" },
    { service: "File Storage", status: "operational", uptime: "99.7%" },
    { service: "Auth Service", status: "operational", uptime: "99.9%" },
  ];

  const getTypeBadge = (t) =>
    ({
      info: "info",
      success: "success",
      danger: "danger",
      warning: "warning",
    })[t] || "info";

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
              <h2 className="card-title">Recent Activity</h2>
            </div>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Action</th>
                  <th>User</th>
                  <th>Role</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {recentActivity.map((a, i) => (
                  <tr key={i}>
                    <td>
                      <span
                        className={`badge ${getTypeBadge(a.type)}`}
                        style={{ marginRight: "0.5rem" }}
                      ></span>
                      {a.action}
                    </td>
                    <td style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                      {a.user}
                    </td>
                    <td>
                      <span
                        className="badge info"
                        style={{ fontSize: "0.7rem" }}
                      >
                        {a.role}
                      </span>
                    </td>
                    <td style={{ color: "#9ca3af", fontSize: "0.75rem" }}>
                      {a.time}
                    </td>
                  </tr>
                ))}
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
                <div>
                  <p
                    style={{
                      fontWeight: "500",
                      fontSize: "0.875rem",
                      margin: 0,
                    }}
                  >
                    {s.service}
                  </p>
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#6b7280",
                      margin: "0.125rem 0 0",
                    }}
                  >
                    Uptime: {s.uptime}
                  </p>
                </div>
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
