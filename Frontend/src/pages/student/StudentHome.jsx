import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  BookOpen,
  Clock,
  CheckCircle,
  AlertCircle,
  FileText,
  Sparkles,
  ChevronRight,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const StudentHome = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [workspace, setWorkspace] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchWorkspace = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const response = await apiService.getStudentWorkspace(user.id);
      setWorkspace(response);
    } catch (err) {
      setError("Failed to load your workspace. Please try again.");
    } finally {
      setLoading(false);
    }
  }, [user.id]);

  useEffect(() => {
    if (user?.id) {
      fetchWorkspace();
    }
  }, [user, fetchWorkspace]);

  const header = workspace?.header;
  const overview = workspace?.submissionOverview;
  const deliverables = workspace?.deliverables || [];
  const deadlines = workspace?.upcomingDeadlines || [];
  const reminders = workspace?.aiReminders || [];
  const recentFeedback = workspace?.recentFeedback || [];
  const timeline = workspace?.timeline || [];

  const statusTone = (status) =>
    ({
      APPROVED: "success",
      SUBMITTED: "info",
      UPDATED: "info",
      NEEDS_REVISION: "danger",
      REJECTED: "danger",
      LATE: "danger",
      PENDING: "secondary",
    })[status] || "secondary";

  const formatCountdown = (hoursRemaining) => {
    if (hoursRemaining === null || hoursRemaining === undefined) return "—";
    if (hoursRemaining < 0) return `${Math.abs(hoursRemaining)}h overdue`;
    if (hoursRemaining < 24) return `${hoursRemaining}h remaining`;
    const days = Math.floor(hoursRemaining / 24);
    return `${days}d remaining`;
  };

  const stats = [
    {
      label: "Total Deliverables",
      value: overview?.totalDeliverables ?? 0,
      icon: FileText,
      color: "maroon",
    },
    {
      label: "Under Review",
      value: overview?.underReviewDeliverables ?? 0,
      icon: Clock,
      color: "gold",
    },
    {
      label: "Needs Revision",
      value: overview?.needsRevisionDeliverables ?? 0,
      icon: AlertCircle,
      color: "blue",
    },
    {
      label: "Approved",
      value: overview?.approvedDeliverables ?? 0,
      icon: CheckCircle,
      color: "green",
    },
  ];

  if (loading) return <div>Loading...</div>;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">
          Student Workspace {header?.groupTitle ? `- ${header.groupTitle}` : ""}
        </h1>
        <p className="page-description">
          Manage deliverables, track deadlines, and monitor feedback with
          InsightHub AI.
        </p>
      </div>

      {error && <div className="error-message">{error}</div>}

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

      {(workspace?.aiInsight || reminders.length > 0) && (
        <div
          className="card"
          style={{
            marginTop: "1.5rem",
            borderLeft: "4px solid var(--maroon)",
          }}
        >
          <div
            className="card-content"
            style={{ display: "flex", alignItems: "flex-start", gap: "1rem" }}
          >
            <div
              className="stat-icon maroon"
              style={{ padding: "0.5rem", borderRadius: "50%" }}
            >
              <Sparkles size={20} />
            </div>
            <div style={{ width: "100%" }}>
              <p
                style={{
                  margin: 0,
                  fontSize: "0.75rem",
                  color: "#6b7280",
                  fontWeight: "600",
                  textTransform: "uppercase",
                  letterSpacing: "0.025em",
                }}
              >
                InsightHub AI
              </p>

              {workspace?.aiInsight && (
                <div style={{ marginTop: "0.5rem", color: "#111827" }}>
                  {String(workspace.aiInsight)
                    .split("\n")
                    .filter((line) => line.trim())
                    .map((line, idx) => (
                      <p
                        key={idx}
                        style={{
                          margin: "0.25rem 0 0 0",
                          fontWeight: "500",
                        }}
                      >
                        {line}
                      </p>
                    ))}
                </div>
              )}

              {reminders.length > 0 && (
                <div style={{ marginTop: "0.75rem" }}>
                  <p style={{ margin: 0, fontWeight: 600 }}>Smart Reminders</p>
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      gap: "0.5rem",
                      marginTop: "0.5rem",
                    }}
                  >
                    {reminders.slice(0, 3).map((r) => (
                      <div
                        key={`${r.deliverableId}-${r.riskLevel}-${r.message}`}
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          gap: "0.75rem",
                          flexWrap: "wrap",
                        }}
                      >
                        <span style={{ color: "#111827" }}>{r.message}</span>
                        <span
                          className={`badge ${
                            r.riskLevel === "AT_RISK" ? "danger" : "success"
                          }`}
                        >
                          {r.riskLevel}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1.5fr 1fr",
          gap: "1.5rem",
          marginTop: "1.5rem",
        }}
      >
        <div className="card">
          <div
            className="card-header"
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              gap: "1rem",
              flexWrap: "wrap",
            }}
          >
            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
              <Clock
                style={{
                  width: "1.25rem",
                  height: "1.25rem",
                  color: "var(--maroon)",
                }}
              />
              <h2 className="card-title">Upcoming Deadlines</h2>
            </div>
            <span className="badge info">{deadlines.length} Items</span>
          </div>
          <div className="card-body" style={{ padding: "1rem" }}>
            {deadlines.length === 0 ? (
              <p style={{ color: "#6b7280", margin: 0 }}>
                No upcoming deadlines scheduled.
              </p>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                {deadlines.map((d) => (
                  <div
                    key={d.deadlineId ?? d.deliverableId}
                    style={{
                      border: "1px solid #e5e7eb",
                      borderRadius: "0.75rem",
                      padding: "0.75rem",
                      display: "flex",
                      justifyContent: "space-between",
                      gap: "1rem",
                      flexWrap: "wrap",
                      cursor: "pointer",
                    }}
                    onClick={() => navigate(`/student/submissions/${d.deliverableId}`)}
                  >
                    <div>
                      <p style={{ margin: 0, fontWeight: 600 }}>{d.deliverableName}</p>
                      <p style={{ margin: "0.25rem 0 0 0", color: "#6b7280", fontSize: "0.75rem" }}>
                        Due {d.dueAt ? new Date(d.dueAt).toLocaleString() : "TBD"} • {d.stage}
                      </p>
                    </div>
                    <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
                      <span className={`badge ${d.riskLevel === "AT_RISK" ? "danger" : "success"}`}>
                        {d.countdownLabel}
                      </span>
                      <span className={`badge ${statusTone(d.status)}`}>{d.status}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Recent Feedback</h2>
          </div>
          <div className="card-body" style={{ padding: "1rem" }}>
            {recentFeedback.length === 0 ? (
              <p style={{ color: "#6b7280", margin: 0 }}>No feedback received yet.</p>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                {recentFeedback.map((f) => (
                  <div
                    key={`${f.submissionId}-${f.evaluatedAt}`}
                    style={{
                      border: "1px solid #e5e7eb",
                      borderRadius: "0.75rem",
                      padding: "0.75rem",
                      cursor: "pointer",
                    }}
                    onClick={() => f.deliverableId && navigate(`/student/submissions/${f.deliverableId}`)}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", gap: "0.75rem", flexWrap: "wrap" }}>
                      <div>
                        <p style={{ margin: 0, fontWeight: 600 }}>{f.deliverableName}</p>
                        <p style={{ margin: "0.25rem 0 0 0", color: "#6b7280", fontSize: "0.75rem" }}>
                          {f.evaluatorName} • {f.evaluatedAt ? new Date(f.evaluatedAt).toLocaleString() : "—"}
                        </p>
                      </div>
                      <span className={`badge ${statusTone(f.status)}`}>{f.status}</span>
                    </div>
                    {f.generalComments && (
                      <p style={{ margin: "0.5rem 0 0 0", color: "#111827", fontSize: "0.875rem" }}>
                        {f.generalComments}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: "1.5rem" }}>
        <div
          className="card-header"
          style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <FileText
              style={{
                width: "1.25rem",
                height: "1.25rem",
                color: "var(--maroon)",
              }}
            />
            <h2 className="card-title">Deliverables</h2>
          </div>
          <span className="badge info">{deliverables.length} Items</span>
        </div>
        <div className="card-body" style={{ padding: 0 }}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
              gap: "1px",
              background: "#e5e7eb",
            }}
          >
            {deliverables.map((deliv) => (
              <div
                key={deliv.deliverableId}
                className="hover-bg"
                style={{
                  padding: "1.25rem",
                  background: "#fff",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  cursor: "pointer",
                  transition: "background 0.2s",
                }}
                onClick={() => navigate(`/student/submissions/${deliv.deliverableId}`)}
              >
                <div>
                  <p style={{ margin: 0, fontWeight: "600", color: "#111827" }}>
                    {deliv.deliverableName}
                  </p>
                  <p style={{ margin: "0.25rem 0 0 0", fontSize: "0.75rem", color: "#6b7280" }}>
                    Stage: {deliv.stage} • {deliv.dueAt ? formatCountdown(deliv.hoursRemaining) : "No deadline"}
                  </p>
                  <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.5rem", flexWrap: "wrap" }}>
                    <span className={`badge ${statusTone(deliv.status)}`}>{deliv.status}</span>
                    {deliv.revisionCount > 0 && (
                      <span className="badge warning">Revisions: {deliv.revisionCount}</span>
                    )}
                    {deliv.hasAiSummary && <span className="badge info">AI Summary</span>}
                  </div>
                </div>
                <ChevronRight size={18} style={{ color: "#9ca3af" }} />
              </div>
            ))}
            {deliverables.length === 0 && (
              <div
                style={{
                  padding: "2rem",
                  textAlign: "center",
                  background: "#fff",
                  gridColumn: "1 / -1",
                }}
              >
                <p style={{ color: "#6b7280", margin: 0 }}>
                  No active deliverables found.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: "1.5rem" }}>
        <div className="card-header">
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <BookOpen
              style={{
                width: "1.25rem",
                height: "1.25rem",
                color: "var(--maroon)",
              }}
            />
            <h2 className="card-title">Activity Timeline</h2>
          </div>
        </div>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Event</th>
                <th>Details</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              {timeline.map((activity, i) => (
                <tr key={i}>
                  <td>
                    <span
                      className={`badge ${
                        activity.type === "DEADLINE"
                          ? "warning"
                          : activity.type === "FEEDBACK"
                            ? "info"
                            : "success"
                      }`}
                      style={{ marginRight: "0.5rem" }}
                    >
                      {activity.type}
                    </span>
                    {activity.title}
                  </td>
                  <td style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                    {activity.detail}
                  </td>
                  <td style={{ color: "#9ca3af", fontSize: "0.75rem" }}>
                    {activity.occurredAt ? new Date(activity.occurredAt).toLocaleString() : "—"}
                  </td>
                </tr>
              ))}
              {timeline.length === 0 && (
                <tr>
                  <td colSpan="3" style={{ textAlign: "center" }}>
                    No activity recorded yet
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default StudentHome;
