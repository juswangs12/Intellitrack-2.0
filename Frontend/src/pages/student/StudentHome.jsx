import React from "react";
import {
  BookOpen,
  Clock,
  CheckCircle,
  AlertCircle,
  FileText,
  Calendar,
} from "lucide-react";

const StudentHome = ({ user }) => {
  const stats = [
    { label: "Total Submissions", value: "3", icon: FileText, color: "maroon" },
    { label: "Pending Reviews", value: "1", icon: Clock, color: "gold" },
    { label: "Approved", value: "1", icon: CheckCircle, color: "green" },
    { label: "Needs Revision", value: "1", icon: AlertCircle, color: "blue" },
  ];

  const deadlines = [
    {
      document: "SDD Final Draft",
      dueDate: "Dec 20, 2025",
      status: "upcoming",
      daysLeft: 15,
    },
    {
      document: "Project Proposal v2",
      dueDate: "Dec 10, 2025",
      status: "urgent",
      daysLeft: 5,
    },
    {
      document: "SRS Document",
      dueDate: "Jan 10, 2026",
      status: "upcoming",
      daysLeft: 36,
    },
  ];

  const submissions = [
    {
      document: "Project Proposal",
      submittedDate: "Nov 15, 2025",
      status: "approved",
      adviser: "Dr. Santos",
    },
    {
      document: "SRS Document",
      submittedDate: "Nov 28, 2025",
      status: "pending",
      adviser: "Dr. Santos",
    },
    {
      document: "SDD Chapter 1",
      submittedDate: "Dec 1, 2025",
      status: "revision",
      adviser: "Dr. Santos",
    },
  ];

  const getStatusBadge = (status) => {
    const map = {
      approved: "success",
      pending: "warning",
      revision: "danger",
      upcoming: "info",
      urgent: "danger",
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
        <div className="card">
          <div className="card-header">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Calendar
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">Upcoming Deadlines</h2>
            </div>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Document</th>
                  <th>Due Date</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {deadlines.map((d, i) => (
                  <tr key={i}>
                    <td>{d.document}</td>
                    <td>{d.dueDate}</td>
                    <td>
                      <span className={`badge ${getStatusBadge(d.status)}`}>
                        {d.daysLeft} days left
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
              <h2 className="card-title">Recent Submissions</h2>
            </div>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Document</th>
                  <th>Submitted</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {submissions.map((s, i) => (
                  <tr key={i}>
                    <td>{s.document}</td>
                    <td>{s.submittedDate}</td>
                    <td>
                      <span className={`badge ${getStatusBadge(s.status)}`}>
                        {s.status.charAt(0).toUpperCase() + s.status.slice(1)}
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

export default StudentHome;
