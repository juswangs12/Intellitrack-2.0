import React, { useEffect, useState } from "react";
import {
  BookOpen,
  Clock,
  CheckCircle,
  AlertCircle,
  FileText,
  Calendar,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";

const StudentHome = () => {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    const token = localStorage.getItem("token");
    fetch(`http://localhost:8080/api/dashboard/student/${user.id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setData(d))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [user]);

  const stats = [
    {
      label: "Total Submissions",
      value: loading ? "…" : (data?.totalDeliverables ?? 0),
      icon: FileText,
      color: "maroon",
    },
    {
      label: "Pending",
      value: loading ? "…" : (data?.pending ?? 0),
      icon: Clock,
      color: "gold",
    },
    {
      label: "Completed",
      value: loading ? "…" : (data?.completed ?? 0),
      icon: CheckCircle,
      color: "green",
    },
    {
      label: "Overdue",
      value: loading ? "…" : (data?.overdue ?? 0),
      icon: AlertCircle,
      color: "blue",
    },
  ];

  const recentActivity = data?.recentActivity ?? [];

  const getStatusBadge = (status) => {
    const map = {
      approved: "success",
      pending: "warning",
      revision: "danger",
      upcoming: "info",
      urgent: "danger",
      SUBMITTED: "success",
      PENDING: "warning",
      LATE: "danger",
    };
    return map[status] || "info";
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Student Dashboard</h1>
        <p className="page-description">
          Track your capstone project submissions and deadlines.
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
          gridTemplateColumns: "1fr 1fr",
          gap: "1.5rem",
        }}
      >
        <div className="card" style={{ gridColumn: "1 / -1" }}>
          <div className="card-header">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <BookOpen
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
            {recentActivity.length === 0 ? (
              <p
                style={{
                  padding: "1rem",
                  color: "#6b7280",
                  fontSize: "0.875rem",
                }}
              >
                No submission activity yet.
              </p>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Activity</th>
                    <th>Timeline</th>
                  </tr>
                </thead>
                <tbody>
                  {recentActivity.map((a, i) => (
                    <tr key={i}>
                      <td>{a.text}</td>
                      <td style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                        {a.time}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default StudentHome;
