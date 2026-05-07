import React, { useEffect, useState } from "react";
import { Users, Clock, CheckCircle, AlertCircle } from "lucide-react";
import { useAuth } from "../../context/AuthContext";

const CoordinatorHome = () => {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    const token = localStorage.getItem("token");
    fetch(`http://localhost:8080/api/dashboard/coordinator/${user.id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setData(d))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [user]);

  const stats = [
    {
      label: "Total Students",
      value: loading ? "…" : (data?.totalStudents ?? 0),
      icon: Users,
      color: "maroon",
    },
    {
      label: "Total Advisers",
      value: loading ? "…" : (data?.totalAdvisers ?? 0),
      icon: Clock,
      color: "gold",
    },
    {
      label: "Pending Submissions",
      value: loading ? "…" : (data?.submissionsPending ?? 0),
      icon: CheckCircle,
      color: "green",
    },
    {
      label: "Notifications",
      value: loading ? "…" : (data?.recentNotifications?.length ?? 0),
      icon: AlertCircle,
      color: "blue",
    },
  ];

  const notifications = data?.recentNotifications ?? [];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Coordinator Dashboard</h1>
        <p className="page-description">
          Manage team submissions and track capstone progress.
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
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">System Notifications</h2>
          </div>
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "0.5rem",
              padding: "0.5rem 0",
            }}
          >
            {notifications.length === 0 ? (
              <p
                style={{
                  color: "#6b7280",
                  fontSize: "0.875rem",
                  padding: "0.5rem 1rem",
                }}
              >
                No notifications.
              </p>
            ) : (
              notifications.map((n, i) => (
                <div
                  key={i}
                  style={{
                    padding: "0.75rem 1rem",
                    borderRadius: "0.5rem",
                    background: "#f9fafb",
                    fontSize: "0.875rem",
                  }}
                >
                  {n}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CoordinatorHome;
