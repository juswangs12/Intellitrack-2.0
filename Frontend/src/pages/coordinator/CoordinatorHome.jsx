import React from "react";
import { Users, Clock, CheckCircle, AlertCircle } from "lucide-react";

const CoordinatorHome = ({ user }) => {
  const stats = [
    { label: "Total Teams", value: "12", icon: Users, color: "maroon" },
    { label: "Pending Reviews", value: "4", icon: Clock, color: "gold" },
    { label: "Approved", value: "7", icon: CheckCircle, color: "green" },
    { label: "Needs Revision", value: "1", icon: AlertCircle, color: "blue" },
  ];

  const pendingReviews = [
    {
      team: "Group Alpha",
      document: "Project Proposal",
      submitted: "Dec 1, 2025",
      priority: "high",
    },
    {
      team: "Group Beta",
      document: "SRS Document",
      submitted: "Dec 3, 2025",
      priority: "medium",
    },
    {
      team: "Group Gamma",
      document: "SDD Chapter 2",
      submitted: "Dec 5, 2025",
      priority: "low",
    },
    {
      team: "Group Delta",
      document: "Project Proposal",
      submitted: "Dec 6, 2025",
      priority: "medium",
    },
  ];

  const advisedTeams = [
    {
      name: "Group Alpha",
      members: 4,
      project: "Smart Inventory System",
      status: "on-track",
    },
    {
      name: "Group Beta",
      members: 3,
      project: "E-Learning Platform",
      status: "at-risk",
    },
    {
      name: "Group Gamma",
      members: 4,
      project: "Healthcare Management",
      status: "on-track",
    },
    {
      name: "Group Delta",
      members: 3,
      project: "Smart Parking System",
      status: "on-track",
    },
    {
      name: "Group Epsilon",
      members: 4,
      project: "AI Grading System",
      status: "delayed",
    },
  ];

  const getPriorityBadge = (p) =>
    ({ high: "danger", medium: "warning", low: "info" })[p] || "info";
  const getStatusBadge = (s) =>
    ({ "on-track": "success", "at-risk": "warning", delayed: "danger" })[s] ||
    "info";

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
            <h2 className="card-title">Pending Reviews</h2>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Team</th>
                  <th>Document</th>
                  <th>Submitted</th>
                  <th>Priority</th>
                </tr>
              </thead>
              <tbody>
                {pendingReviews.map((r, i) => (
                  <tr key={i}>
                    <td>{r.team}</td>
                    <td>{r.document}</td>
                    <td>{r.submitted}</td>
                    <td>
                      <span className={`badge ${getPriorityBadge(r.priority)}`}>
                        {r.priority}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Advised Teams</h2>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Team</th>
                  <th>Project</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {advisedTeams.map((t, i) => (
                  <tr key={i}>
                    <td>
                      {t.name}{" "}
                      <span style={{ color: "#9ca3af", fontSize: "0.75rem" }}>
                        ({t.members} members)
                      </span>
                    </td>
                    <td>{t.project}</td>
                    <td>
                      <span className={`badge ${getStatusBadge(t.status)}`}>
                        {t.status}
                      </span>
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

export default CoordinatorHome;
