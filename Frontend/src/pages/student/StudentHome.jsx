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
  RefreshCw,
  MessageSquare,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const StudentHome = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [workspace, setWorkspace] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [aiInsight, setAiInsight] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [generatingAi, setGeneratingAi] = useState(false);

  const renderInline = (text) => {
    const parts = text.split(/(\*\*[^*]+\*\*|\*[^*]+\*)/g);
    return parts.map((part, i) => {
      if (/^\*\*[^*]+\*\*$/.test(part)) {
        return <strong key={i}>{part.slice(2, -2)}</strong>;
      }
      if (/^\*[^*]+\*$/.test(part)) {
        return <em key={i}>{part.slice(1, -1)}</em>;
      }
      return part;
    });
  };

  const generateAiInsight = async () => {
    try {
      setGeneratingAi(true);
      const insight = await apiService.requestJson(
        "/student/workspace/generate-ai-insight",
        { method: "POST" }
      );
      if (insight) {
        setAiInsight(insight);
      }
    } catch (err) {
      console.error("Failed to generate AI insight", err);
    } finally {
      setGeneratingAi(false);
    }
  };

  const fetchWorkspace = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const response = await apiService.getStudentWorkspace();
      setWorkspace(response);
    } catch (err) {
      setError("Failed to load your workspace. Please try again.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (user?.id) {
      fetchWorkspace();
    }
  }, [user, fetchWorkspace]);

  const header = workspace?.header;
  const overview = workspace?.submissionOverview;
  const deliverables = workspace?.deliverables || [];
  const reminders = workspace?.aiReminders || [];
  const timeline = workspace?.timeline || [];

  const statusTone = (status) =>
    ({
      SUBMITTED_ON_TIME: "success",
      SUBMITTED_LATE: "danger",
      NOT_SUBMITTED: "danger",
      COMMENTED: "info",
      PENDING: "secondary",
    })[status] || "secondary";

  const formatCountdown = (hoursRemaining) => {
    if (hoursRemaining === null || hoursRemaining === undefined) return "—";
    if (hoursRemaining < 0) return `${Math.abs(hoursRemaining)}h overdue`;
    if (hoursRemaining < 24) return `${hoursRemaining}h remaining`;
    const days = Math.floor(hoursRemaining / 24);
    return `${days}d remaining`;
  };

  const getTimelineIcon = (type) => {
    switch (type) {
      case "DEADLINE":
        return { icon: Clock, color: "#f59e0b", bg: "#fffbeb" };
      case "FEEDBACK":
        return { icon: MessageSquare, color: "#3b82f6", bg: "#eff6ff" };
      case "SUBMISSION":
        return { icon: CheckCircle, color: "#10b981", bg: "#ecfdf5" };
      default:
        return { icon: FileText, color: "#6b7280", bg: "#f9fafb" };
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "—";
    const date = new Date(dateStr);
    const now = new Date();
    
    // Reset time to midnight for both dates to avoid time of day issues
    const todayMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const targetMidnight = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    
    // Compare calendar dates (ignoring time)
    const isToday = 
      date.getDate() === now.getDate() && 
      date.getMonth() === now.getMonth() && 
      date.getFullYear() === now.getFullYear();
      
    const tomorrow = new Date(todayMidnight);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const isTomorrow = 
      targetMidnight.getTime() === tomorrow.getTime();
      
    const yesterday = new Date(todayMidnight);
    yesterday.setDate(yesterday.getDate() - 1);
    const isYesterday = 
      targetMidnight.getTime() === yesterday.getTime();
      
    // Calculate difference in calendar days
    const oneDay = 24 * 60 * 60 * 1000;
    const diffMs = targetMidnight - todayMidnight;
    const calendarDiffDays = Math.round(diffMs / oneDay);
    const absDiffDays = Math.abs(calendarDiffDays);
    
    if (isToday) {
      return "Today";
    } else if (isTomorrow) {
      return "Tomorrow";
    } else if (isYesterday) {
      return "Yesterday";
    } else if (calendarDiffDays > 0 && calendarDiffDays < 7) {
      return `in ${calendarDiffDays} days`;
    } else if (calendarDiffDays < 0 && absDiffDays < 7) {
      return `${absDiffDays} days ago`;
    }
    
    return date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
  };
  
  const formatFullDate = (dateStr) => {
    if (!dateStr) return "";
    const date = new Date(dateStr);
    return date.toLocaleDateString([], { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric', 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const stats = [
    {
      label: "Total Deliverables",
      value: overview?.totalDeliverables ?? 0,
      icon: FileText,
      color: "maroon",
    },
    {
      label: "On Time",
      value: overview?.onTimeDeliverables ?? 0,
      icon: CheckCircle,
      color: "green",
    },
    {
      label: "Late",
      value: overview?.lateDeliverables ?? 0,
      icon: AlertCircle,
      color: "red",
    },
    {
      label: "Not Submitted",
      value: overview?.notSubmittedDeliverables ?? 0,
      icon: Clock,
      color: "yellow",
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

      {true && (
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
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.75rem" }}>
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
                <button
                  onClick={generateAiInsight}
                  disabled={generatingAi}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                    padding: "0.375rem 0.75rem",
                    borderRadius: "0.375rem",
                    border: "1px solid var(--maroon)",
                    backgroundColor: "var(--maroon)",
                    color: "#fff",
                    fontWeight: 600,
                    cursor: generatingAi ? "not-allowed" : "pointer",
                    opacity: generatingAi ? 0.7 : 1,
                    fontSize: "0.8125rem",
                  }}
                >
                  <RefreshCw size={14} className={generatingAi ? "animate-spin" : ""} />
                  {generatingAi ? "Generating..." : "Generate AI Insights"}
                </button>
              </div>

              {aiInsight ? (
                <div style={{ marginTop: "0.5rem", color: "#111827" }}>
                  {String(aiInsight)
                    .split("\n")
                    .filter((line) => line.trim())
                    .map((line, idx) => {
                      const trimmed = line.trim();
                      if (/^#{1,3}\s/.test(trimmed)) {
                        return (
                          <p
                            key={idx}
                            style={{
                              margin: "0.75rem 0 0.25rem 0",
                              fontWeight: 700,
                              fontSize: "0.9375rem",
                              color: "#111827",
                            }}
                          >
                            {trimmed.replace(/^#{1,3}\s*/, "")}
                          </p>
                        );
                      }
                      if (/^[-*]\s/.test(trimmed)) {
                        const content = trimmed.replace(/^[-*]\s*/, "");
                        return (
                          <div
                            key={idx}
                            style={{
                              display: "flex",
                              gap: "0.5rem",
                              margin: "0.2rem 0 0 0.5rem",
                            }}
                          >
                            <span style={{ color: "#7f1d1d", flexShrink: 0 }}>
                              •
                            </span>
                            <span style={{ fontWeight: 500 }}>
                              {renderInline(content)}
                            </span>
                          </div>
                        );
                      }
                      return (
                        <p
                          key={idx}
                          style={{ margin: "0.25rem 0 0 0", fontWeight: 500 }}
                        >
                          {renderInline(trimmed)}
                        </p>
                      );
                    })}
                </div>
              ) : (
                <p
                  style={{
                    marginTop: "0.5rem",
                    color: "#9ca3af",
                    fontStyle: "italic",
                    fontSize: "0.875rem",
                  }}
                >
                  {generatingAi
                    ? "Generating AI insights…"
                    : "Click 'Generate AI Insights' to get deliverable-focused AI summary."}
                </p>
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
                          cursor: "default",
                          pointerEvents: "none",
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

      <div className="card" style={{ marginTop: "1.5rem" }}>
        <div
          className="card-header"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
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
        <div className="card-body" style={{ padding: "1rem" }}>
          {deliverables.length === 0 ? (
            <div
              style={{
                padding: "3rem 1rem",
                textAlign: "center",
                background: "#f9fafb",
                borderRadius: "0.75rem",
                border: "1px dashed #e5e7eb",
              }}
            >
              <FileText style={{ width: "2.5rem", height: "2.5rem", color: "#d1d5db", margin: "0 auto 0.75rem auto", display: "block" }} />
              <p style={{ color: "#6b7280", margin: 0, fontSize: "0.875rem" }}>
                No active deliverables found.
              </p>
            </div>
          ) : (
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
                gap: "1rem",
              }}
            >
              {deliverables.map((deliv) => {
                const isSubmitted = 
                  deliv.status === "SUBMITTED_ON_TIME" || 
                  deliv.status === "SUBMITTED_LATE";
                
                return (
                <div
                  key={deliv.deliverableId}
                  className="hover-bg"
                  style={{
                    padding: "1.25rem",
                    border: `2px solid ${isSubmitted ? "#d1fae5" : "#e5e7eb"}`,
                    borderRadius: "0.75rem",
                    background: isSubmitted ? "#ecfdf5" : "#fff",
                    boxShadow: "0 1px 3px rgba(0,0,0,0.05)",
                    cursor: "pointer",
                    transition: "all 0.2s",
                  }}
                  onClick={() =>
                    navigate(`/student/submissions/${deliv.deliverableId}`)
                  }
                  onMouseEnter={(e) => {
                    e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,0,0,0.08)";
                    e.currentTarget.style.transform = "translateY(-2px)";
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.boxShadow = "0 1px 3px rgba(0,0,0,0.05)";
                    e.currentTarget.style.transform = "translateY(0)";
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "0.75rem" }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{ 
                        margin: 0, 
                        fontWeight: "700", 
                        color: "#111827",
                        fontSize: "1rem",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}>
                        {deliv.deliverableName}
                      </p>
                      <p
                        style={{
                          margin: "0.25rem 0 0 0",
                          fontSize: "0.75rem",
                          color: "#6b7280",
                        }}
                      >
                        Stage: {deliv.stage}
                      </p>
                    </div>
                    <ChevronRight size={20} style={{ color: "#9ca3af", flexShrink: 0, marginLeft: "0.5rem" }} />
                  </div>
                  
                  {deliv.dueAt && (
                    <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "0.75rem" }}>
                      <Clock size={14} style={{ color: "#6b7280" }} />
                      <span style={{ fontSize: "0.75rem", color: "#4b5563" }}>
                        {formatCountdown(deliv.hoursRemaining)}
                      </span>
                    </div>
                  )}

                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      flexWrap: "wrap",
                    }}
                  >
                    <span className={`badge ${statusTone(deliv.status)}`}>
                      {deliv.status}
                    </span>
                    {deliv.hasAiSummary && (
                      <span className="badge info">AI Summary</span>
                    )}
                  </div>
                </div>
              )})}
            </div>
          )}
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
        <div className="card-body" style={{ padding: "1rem" }}>
          {timeline.length === 0 ? (
            <div
              style={{
                padding: "3rem 1rem",
                textAlign: "center",
                background: "#f9fafb",
                borderRadius: "0.75rem",
                border: "1px dashed #e5e7eb",
              }}
            >
              <BookOpen style={{ width: "2.5rem", height: "2.5rem", color: "#d1d5db", margin: "0 auto 0.75rem auto", display: "block" }} />
              <p style={{ color: "#6b7280", margin: 0, fontSize: "0.875rem" }}>
                No activity recorded yet. Start submitting your deliverables!
              </p>
            </div>
          ) : (
            <div style={{ position: "relative" }}>
              {/* Timeline Vertical Line */}
              <div
                style={{
                  position: "absolute",
                  left: "1.75rem",
                  top: "0.5rem",
                  bottom: "0.5rem",
                  width: "2px",
                  background: "#e5e7eb",
                }}
              />
              
              <div style={{ display: "flex", flexDirection: "column", gap: "0" }}>
                {timeline.map((activity, idx) => {
                  const { icon: Icon, color, bg } = getTimelineIcon(activity.type);
                  
                  return (
                    <div
                      key={idx}
                      style={{
                        display: "flex",
                        gap: "1rem",
                        padding: "1rem 0",
                        position: "relative",
                      }}
                    >
                      {/* Icon Container */}
                      <div
                        style={{
                          width: "3.5rem",
                          display: "flex",
                          justifyContent: "center",
                          flexShrink: 0,
                          zIndex: 1,
                        }}
                      >
                        <div
                          style={{
                            width: "2.5rem",
                            height: "2.5rem",
                            borderRadius: "50%",
                            background: bg,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            boxShadow: "0 1px 2px rgba(0,0,0,0.05)",
                            border: "2px solid white",
                          }}
                        >
                          <Icon size={18} style={{ color }} />
                        </div>
                      </div>
                      
                      {/* Content */}
                      <div style={{ flex: 1, paddingTop: "0.25rem" }}>
                        <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "1rem", flexWrap: "wrap" }}>
                            <p style={{ 
                              margin: 0, 
                              fontWeight: 700, 
                              color: "#111827",
                              fontSize: "0.9375rem",
                            }}>
                              {activity.title}
                            </p>
                            <p style={{ 
                              margin: 0, 
                              color: "#9ca3af", 
                              fontSize: "0.75rem",
                              whiteSpace: "nowrap",
                            }}>
                              {formatDate(activity.occurredAt)}
                            </p>
                          </div>
                          
                          <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                            {activity.detail && (
                              <p style={{
                                margin: 0,
                                fontSize: "0.875rem",
                                color: "#6b7280",
                                lineHeight: 1.5,
                              }}>
                                {activity.detail}
                              </p>
                            )}
                            
                            {activity.occurredAt && (
                              <p style={{
                                margin: 0,
                                fontSize: "0.75rem",
                                color: "#9ca3af",
                                display: "flex",
                                alignItems: "center",
                                gap: "0.25rem"
                              }}>
                                <Clock size={12} />
                                {activity.type === "DEADLINE" ? "Deadline: " : "Date: "}
                                {formatFullDate(activity.occurredAt)}
                              </p>
                            )}
                            
                            <div style={{ display: "flex", alignItems: "flex-start", gap: "0.5rem", flexWrap: "wrap" }}>
                              {activity.type === "SUBMISSION" && activity.detail && (() => {
                                let status = "PENDING";
                                if (activity.detail.includes("SUBMITTED_ON_TIME")) {
                                  status = "SUBMITTED_ON_TIME";
                                } else if (activity.detail.includes("SUBMITTED_LATE")) {
                                  status = "SUBMITTED_LATE";
                                } else if (activity.detail.includes("NOT_SUBMITTED")) {
                                  status = "NOT_SUBMITTED";
                                } else if (activity.detail.includes("COMMENTED")) {
                                  status = "COMMENTED";
                                }
                                
                                return (
                                  <span className={`badge ${statusTone(status)}`}>
                                    {status.replaceAll("_", " ")}
                                  </span>
                                );
                              })()}
                              
                              {activity.type === "FEEDBACK" && activity.detail && (() => {
                                let status = "PENDING";
                                if (activity.detail.includes("SUBMITTED_ON_TIME")) {
                                  status = "SUBMITTED_ON_TIME";
                                } else if (activity.detail.includes("SUBMITTED_LATE")) {
                                  status = "SUBMITTED_LATE";
                                } else if (activity.detail.includes("NOT_SUBMITTED")) {
                                  status = "NOT_SUBMITTED";
                                } else if (activity.detail.includes("COMMENTED")) {
                                  status = "COMMENTED";
                                }
                                
                                return (
                                  <span className={`badge ${statusTone(status)}`}>
                                    {status.replaceAll("_", " ")}
                                  </span>
                                );
                              })()}
                              
                              {activity.type === "DEADLINE" && activity.occurredAt && (() => {
                                const dueDate = new Date(activity.occurredAt);
                                const now = new Date();
                                const diffMs = dueDate - now;
                                const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
                                
                                let statusText = "Upcoming";
                                let statusClass = "info";
                                
                                if (diffDays < 0) {
                                  statusText = "Overdue";
                                  statusClass = "danger";
                                } else if (diffDays === 0) {
                                  statusText = "Due Today";
                                  statusClass = "warning";
                                } else if (diffDays <= 3) {
                                  statusText = "Due Soon";
                                  statusClass = "warning";
                                }
                                
                                return (
                                  <span className={`badge ${statusClass}`}>
                                    {statusText}
                                  </span>
                                );
                              })()}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default StudentHome;
