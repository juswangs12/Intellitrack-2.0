import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";
import {
  Users,
  FileText,
  Clock,
  Star,
  Calendar,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Eye,
  MessageSquare,
  Activity,
  Sparkles,
  RefreshCw,
} from "lucide-react";

const AdviserGroupDetails = () => {
  const { groupId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [group, setGroup] = useState(null);
  const [activeTab, setActiveTab] = useState("overview");
  const [feedbackHistory, setFeedbackHistory] = useState([]);
  const [submissions, setSubmissions] = useState([]);
  const [deliverables, setDeliverables] = useState([]);
  const [aiSummary, setAiSummary] = useState(null);
  const [generatingAi, setGeneratingAi] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      if (!groupId) return;
      try {
        setLoading(true);
        setError(null);
        const [groupData, submissionsData, deliverablesData] = await Promise.all([
          apiService.getAdviserGroupDetails(groupId),
          apiService.getSubmissionsByGroup(groupId).catch(() => []),
          apiService.getAdviserGroupDeliverables(groupId).catch(() => []),
        ]);
        console.log("=== DEBUG ===");
        console.log("groupData:", groupData);
        console.log("submissionsData:", submissionsData);
        console.log("deliverablesData:", deliverablesData);
        setGroup(groupData);
        setFeedbackHistory([]);
        setSubmissions(Array.isArray(submissionsData) ? submissionsData : []);
        setDeliverables(Array.isArray(deliverablesData) ? deliverablesData : []);
      } catch (err) {
        console.error("Failed to fetch group details", err);
        setError("Failed to load group details.");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [groupId]);

  if (loading) {
    return (
      <div
        className="fade-in"
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          minHeight: "400px",
          fontSize: "1.125rem",
          color: "#6b7280",
        }}
      >
        Loading group details...
      </div>
    );
  }

  if (error || !group) {
    return (
      <div className="fade-in" style={{ padding: "2rem" }}>
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => navigate("/adviser/groups")}
          style={{ marginBottom: "1rem" }}
        >
          <ArrowLeft size={16} /> Back to Groups
        </button>
        <div
          style={{
            padding: "2rem",
            backgroundColor: "#fef2f2",
            border: "1px solid #fecaca",
            borderRadius: "0.5rem",
            textAlign: "center",
          }}
        >
          <AlertCircle
            size={48}
            style={{ color: "#ef4444", margin: "0 auto 1rem" }}
          />
          <h3 style={{ color: "#b91c1c", marginBottom: "0.5rem" }}>
            Failed to Load
          </h3>
          <p style={{ color: "#7f1d1d" }}>{error || "Group not found"}</p>
        </div>
      </div>
    );
  }

  const tabs = [
    { id: "overview", name: "Overview", icon: Star },
    { id: "members", name: "Members", icon: Users },
    { id: "deliverables", name: "Deliverables", icon: FileText },
    { id: "insights", name: "Status Summary", icon: Activity },
  ];

  const totalCount = deliverables.length;

const completedCount = deliverables.filter(
    (d) =>
        d.status === "SUBMITTED_ON_TIME" ||
        d.status === "SUBMITTED_LATE"
).length;

const pendingCount = deliverables.filter(
  (deliverable) => deliverable.status === "NOT_SUBMITTED"
).length;

const overdueCount = deliverables.filter(
    (d) =>
        d.status === "OVERDUE" ||
        d.status === "SUBMITTED_LATE"
).length;

  const progressPercent =
    totalCount > 0
      ? Math.round((completedCount / totalCount) * 100)
      : 0;

  const statusTone = (status) => {
    const tones = {
      SUBMITTED_ON_TIME: "success",
      SUBMITTED_LATE: "danger",
      NOT_SUBMITTED: "warning",
      PENDING: "secondary",
      COMMENTED: "info"
    };
    return tones[status] || "secondary";
  };

  const generateAiSummary = async () => {
    if (!groupId) return;
    try {
      setGeneratingAi(true);
      const summary = await apiService.requestJson(
        `/adviser/groups/${groupId}/ai-summary`,
        { method: "POST" }
      );
      if (summary) {
        setAiSummary(summary);
      }
    } catch (err) {
      console.error("Failed to generate AI summary", err);
    } finally {
      setGeneratingAi(false);
    }
  };

  return (
    <div className="fade-in">
      <div style={{ marginBottom: "1.5rem" }}>
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => navigate("/adviser/groups")}
          style={{ marginBottom: "1rem" }}
        >
          <ArrowLeft size={16} /> Back to Groups
        </button>
        <div className="page-header" style={{ margin: 0, padding: 0 }}>
          <h1 className="page-title">
            {group.code} - {group.title}
          </h1>
          <p className="page-description">
            Capstone group supervision workspace
          </p>
        </div>
      </div>

      {/* AI Summary Section */}
      <div style={{
        marginBottom: "1rem", padding: "1rem 1.25rem",
        backgroundColor: aiSummary && !aiSummary.includes("Click the") ? "#fdf8f0" : "#fffbeb", 
        borderRadius: "0.5rem",
        borderLeft: "4px solid var(--maroon)",
        border: "1px solid #f3e8d0",
        borderLeftColor: "var(--maroon)",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "0.75rem", justifyContent: "space-between" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <Sparkles size={16} style={{ color: "var(--maroon)" }} />
            <span style={{ fontWeight: 700, fontSize: "0.875rem", color: "var(--maroon)" }}>Group Progress AI Summary</span>
          </div>
          <button
            onClick={generateAiSummary}
            disabled={generatingAi}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              padding: "0.5rem 1rem",
              borderRadius: "0.5rem",
              border: "1px solid var(--maroon)",
              backgroundColor: "var(--maroon)",
              color: "#fff",
              fontWeight: 600,
              cursor: generatingAi ? "not-allowed" : "pointer",
              opacity: generatingAi ? 0.7 : 1,
              fontSize: "0.875rem",
            }}
          >
            <RefreshCw size={16} className={generatingAi ? "animate-spin" : ""} />
            {generatingAi ? "Generating..." : "Generate AI Summary"}
          </button>
        </div>
        {aiSummary && (
          <p style={{ margin: 0, fontSize: "0.9rem", color: "#374151", lineHeight: 1.6, whiteSpace: "pre-wrap" }}>
            {aiSummary}
          </p>
        )}
        {!aiSummary && (
          <p style={{ margin: 0, fontSize: "0.9rem", color: "#6b7280", lineHeight: 1.6, whiteSpace: "pre-wrap" }}>
            Click the "Generate AI Summary" button to get an AI-powered summary of this group's progress.
          </p>
        )}
      </div>

      {error && (
        <div
          style={{
            marginBottom: "1rem",
            padding: "0.75rem 1rem",
            backgroundColor: "#fef2f2",
            border: "1px solid #fecaca",
            borderRadius: "0.375rem",
            color: "#b91c1c",
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
          }}
        >
          <AlertCircle size={18} />
          {error}
        </div>
      )}

      <div className="card">
        <div
          style={{
            display: "flex",
            borderBottom: "1px solid #e5e7eb",
            marginBottom: "1.5rem",
          }}
        >
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                style={{
                  padding: "0.75rem 1.25rem",
                  border: "none",
                  backgroundColor:
                    activeTab === tab.id ? "#f9fafb" : "transparent",
                  color: activeTab === tab.id ? "#7f1d1d" : "#6b7280",
                  fontWeight: activeTab === tab.id ? 600 : 400,
                  cursor: "pointer",
                  borderBottom:
                    activeTab === tab.id
                      ? "2px solid #7f1d1d"
                      : "2px solid transparent",
                  display: "flex",
                  alignItems: "center",
                  gap: "0.5rem",
                  transition: "all 0.15s",
                }}
              >
                <Icon size={16} />
                {tab.name}
              </button>
            );
          })}
        </div>

        <div className="card-content">
          {activeTab === "overview" && (
            <div>
              <h3
                style={{
                  fontSize: "1.125rem",
                  fontWeight: 600,
                  marginBottom: "1.5rem",
                }}
              >
                Group Overview
              </h3>
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr",
                  gap: "2rem",
                }}
              >
                <div>
                  <div style={{ marginBottom: "1.5rem" }}>
                    <h4
                      style={{
                        fontSize: "0.875rem",
                        fontWeight: 600,
                        color: "#374151",
                        marginBottom: "0.5rem",
                        textTransform: "uppercase",
                        letterSpacing: "0.05em",
                      }}
                    >
                      Group Information
                    </h4>
                    <div
                      style={{
                        backgroundColor: "#f9fafb",
                        padding: "1rem",
                        borderRadius: "0.5rem",
                      }}
                    >
                      <div style={{ marginBottom: "0.75rem" }}>
                        <span
                          style={{ color: "#6b7280", fontSize: "0.8125rem" }}
                        >
                          Code:
                        </span>
                        <span style={{ marginLeft: "0.5rem", fontWeight: 600 }}>
                          {group.code}
                        </span>
                      </div>
                      <div style={{ marginBottom: "0.75rem" }}>
                        <span
                          style={{ color: "#6b7280", fontSize: "0.8125rem" }}
                        >
                          Title:
                        </span>
                        <span style={{ marginLeft: "0.5rem", fontWeight: 600 }}>
                          {group.title}
                        </span>
                      </div>
                      <div style={{ marginBottom: "0.75rem" }}>
                        <span
                          style={{ color: "#6b7280", fontSize: "0.8125rem" }}
                        >
                          Adviser:
                        </span>
                        <span style={{ marginLeft: "0.5rem" }}>
                          {group.adviserName || "Unassigned"}
                        </span>
                      </div>
                      <div>
                        <span
                          style={{ color: "#6b7280", fontSize: "0.8125rem" }}
                        >
                          Created:
                        </span>
                        <span style={{ marginLeft: "0.5rem" }}>
                          {group.createdAt
                            ? new Date(group.createdAt).toLocaleDateString()
                            : "N/A"}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div>
                  <h4
                    style={{
                      fontSize: "0.875rem",
                      fontWeight: 600,
                      color: "#374151",
                      marginBottom: "0.5rem",
                      textTransform: "uppercase",
                      letterSpacing: "0.05em",
                    }}
                  >
                    Progress Summary
                  </h4>
                  <div
                    style={{
                      backgroundColor: "#f9fafb",
                      padding: "1rem",
                      borderRadius: "0.5rem",
                    }}
                  >
                    <div style={{ marginBottom: "1rem" }}>
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          marginBottom: "0.5rem",
                        }}
                      >
                        <span style={{ color: "#374151" }}>
                          Overall Progress
                        </span>
                        <span style={{ fontWeight: 700 }}>
                          {progressPercent}%
                        </span>
                      </div>
                      <div
                        style={{
                          height: "0.75rem",
                          backgroundColor: "#e5e7eb",
                          borderRadius: "9999px",
                          overflow: "hidden",
                        }}
                      >
                        <div
                          style={{
                            width: `${progressPercent}%`,
                            height: "100%",
                            backgroundColor: "#10b981",
                            transition: "width 0.3s",
                          }}
                        />
                      </div>
                    </div>
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr 1fr",
                        gap: "0.75rem",
                      }}
                    >
                      <div
                        style={{
                          textAlign: "center",
                          padding: "0.75rem",
                          backgroundColor: "#ecfdf5",
                          borderRadius: "0.375rem",
                        }}
                      >
                        <CheckCircle2
                          size={20}
                          style={{ color: "#10b981", margin: "0 auto 0.25rem" }}
                        />
                        <div
                          style={{
                            fontWeight: 700,
                            color: "#166534",
                            fontSize: "1.25rem",
                          }}
                        >
                          {completedCount}
                        </div>
                        <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                          Completed
                        </div>
                      </div>
                      <div
                        style={{
                          textAlign: "center",
                          padding: "0.75rem",
                          backgroundColor: "#fefce8",
                          borderRadius: "0.375rem",
                        }}
                      >
                        <Clock
                          size={20}
                          style={{ color: "#f59e0b", margin: "0 auto 0.25rem" }}
                        />
                        <div
                          style={{
                            fontWeight: 700,
                            color: "#92400e",
                            fontSize: "1.25rem",
                          }}
                        >
                          {pendingCount}
                        </div>
                        <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                          Pending
                        </div>
                      </div>
                      <div
                        style={{
                          textAlign: "center",
                          padding: "0.75rem",
                          backgroundColor: "#fef2f2",
                          borderRadius: "0.375rem",
                        }}
                      >
                        <AlertCircle
                          size={20}
                          style={{ color: "#ef4444", margin: "0 auto 0.25rem" }}
                        />
                        <div
                          style={{
                            fontWeight: 700,
                            color: "#b91c1c",
                            fontSize: "1.25rem",
                          }}
                        >
                          {overdueCount}
                        </div>
                        <div style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                          Overdue
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                
                </div>   {/* closes the 2-column grid */}
                {submissions.length > 0 && (
                  <div style={{ marginTop: "2rem" }}>
                    <h4
                      style={{
                        fontSize: "0.875rem",
                        fontWeight: 600,
                        color: "#374151",
                        marginBottom: "0.75rem",
                        textTransform: "uppercase",
                        letterSpacing: "0.05em",
                      }}
                    >
                      Deliverable Breakdown
                    </h4>

                    <div className="table-container">
                      <table className="data-table">
                        <thead>
                          <tr>
                            <th>Deliverable</th>
                            <th>Stage</th>
                            <th>Due Date</th>
                            <th>Status</th>
                            <th>Submitted</th>
                          </tr>
                        </thead>

                        <tbody>
                          {deliverables.map((deliverable) => {
                            const submission = submissions.find(
                              (s) => s.deliverableId === deliverable.id
                            );

                            return (
                              <tr key={deliverable.id}>
                                <td style={{ fontWeight: 500 }}>
                                  {deliverable.name}
                                </td>

                                <td>{deliverable.stage || "—"}</td>

                                <td>
                                  {deliverable.dueAt
                                    ? new Date(deliverable.dueAt).toLocaleDateString()
                                    : "—"}
                                </td>

                                <td>
                                  <span className={`badge ${statusTone(deliverable.status)}`}>
                                    {deliverable.status}
                                  </span>
                                </td>

                                <td>
                                  {submission?.submittedAt
                                    ? new Date(submission.submittedAt).toLocaleDateString()
                                    : "—"}
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
          )}

          {activeTab === "members" && (
            <div>
              <h3
                style={{
                  fontSize: "1.125rem",
                  fontWeight: 600,
                  marginBottom: "1.5rem",
                }}
              >
                Group Members
              </h3>
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Student Name</th>
                      <th>Student ID</th>
                      <th>Email</th>
                      <th>Section</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {group.students?.map((student) => (
                      <tr key={student.id}>
                        <td style={{ fontWeight: 500 }}>{student.fullName}</td>
                        <td>{student.studentId}</td>
                        <td>{student.email}</td>
                        <td>{student.sectionName}</td>
                        <td>
                          <span
                            style={{
                              display: "inline-flex",
                              alignItems: "center",
                              gap: "0.375rem",
                              padding: "0.25rem 0.75rem",
                              backgroundColor: student.userId
                                ? "#ecfdf5"
                                : "#fffbeb",
                              color: student.userId ? "#166534" : "#92400e",
                              borderRadius: "9999px",
                              fontSize: "0.8125rem",
                              fontWeight: 500,
                            }}
                          >
                            {student.userId ? (
                              <CheckCircle2 size={14} />
                            ) : (
                              <AlertCircle size={14} />
                            )}
                            {student.userId ? "Linked" : "Not Linked"}
                          </span>
                        </td>
                      </tr>
                    ))}
                    {(!group.students || group.students.length === 0) && (
                      <tr>
                        <td
                          colSpan="5"
                          style={{
                            textAlign: "center",
                            padding: "3rem",
                            color: "#6b7280",
                          }}
                        >
                          <Users
                            size={40}
                            style={{ margin: "0 auto 1rem", opacity: 0.3 }}
                          />
                          <div>No members in this group</div>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === "deliverables" && (
            <div>
              <h3
                style={{
                  fontSize: "1.125rem",
                  fontWeight: 600,
                  marginBottom: "1.5rem",
                }}
              >
                Deliverables
              </h3>
              {deliverables.length === 0 ? (
                <div
                  style={{
                    textAlign: "center",
                    padding: "3rem",
                    color: "#6b7280",
                  }}
                >
                  <FileText
                    size={40}
                    style={{ margin: "0 auto 1rem", opacity: 0.3 }}
                  />
                  <div>No deliverables assigned yet</div>
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Deliverable</th>
                        <th>Stage</th>
                        <th>Due Date</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {deliverables.map((deliverable) => {
                        const submission = submissions.find(
                          (s) => s.deliverableId === deliverable.id
                        );
                        return (
                          <tr key={deliverable.id}>
                            <td style={{ fontWeight: 500 }}>
                              {deliverable.name}
                            </td>
                            <td>{deliverable.stage || "—"}</td>
                            <td>
                              {deliverable.dueAt
                                ? new Date(deliverable.dueAt).toLocaleDateString()
                                : "—"}
                            </td>
                            <td>
                              {submission ? (
                                <span
                                  className={`badge ${statusTone(submission.status)}`}
                                >
                                  {submission.status}
                                </span>
                              ) : (
                                <span className="badge warning">Not Submitted</span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {activeTab === "feedback" && (
            <div>
              <h3
                style={{
                  fontSize: "1.125rem",
                  fontWeight: 600,
                  marginBottom: "1.5rem",
                }}
              >
                Review History
              </h3>
              {feedbackHistory.length === 0 ? (
                <div
                  style={{
                    textAlign: "center",
                    padding: "3rem",
                    color: "#6b7280",
                  }}
                >
                  <MessageSquare
                    size={40}
                    style={{ margin: "0 auto 1rem", opacity: 0.3 }}
                  />
                  <div>No reviews yet</div>
                </div>
              ) : (
                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "1rem",
                  }}
                >
                  {feedbackHistory.map((feedback, idx) => (
                    <div
                      key={`${feedback.submissionId}-${idx}`}
                      className="card"
                      style={{ margin: 0 }}
                    >
                      <div className="card-header">
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                            flexWrap: "wrap",
                            gap: "1rem",
                          }}
                        >
                          <div>
                            <h4 style={{ margin: 0, fontWeight: 600 }}>
                              Evaluation #{idx + 1}
                            </h4>
                            <p
                              style={{
                                margin: "0.25rem 0 0 0",
                                color: "#6b7280",
                                fontSize: "0.875rem",
                              }}
                            >
                              By {feedback.evaluatorName} •{" "}
                              {feedback.evaluatedAt
                                ? new Date(
                                    feedback.evaluatedAt,
                                  ).toLocaleString()
                                : "—"}
                            </p>
                          </div>
                          {feedback.status && (
                            <span
                              className={`badge ${statusTone(feedback.status)}`}
                            >
                              {feedback.status}
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="card-content">
                        {feedback.totalScore !== null &&
                          feedback.totalScore !== undefined && (
                            <div
                              style={{
                                marginBottom: "1rem",
                                paddingBottom: "1rem",
                                borderBottom: "1px solid #e5e7eb",
                              }}
                            >
                              <p
                                style={{
                                  fontSize: "0.875rem",
                                  color: "#6b7280",
                                  margin: 0,
                                  textTransform: "uppercase",
                                  fontWeight: 600,
                                  letterSpacing: "0.05em",
                                }}
                              >
                                Total Score
                              </p>
                              <p
                                style={{
                                  fontSize: "2rem",
                                  fontWeight: 700,
                                  color: "#7f1d1d",
                                  margin: 0,
                                }}
                              >
                                {feedback.totalScore}
                              </p>
                            </div>
                          )}
                        {feedback.generalComments && (
                          <div style={{ marginBottom: "1rem" }}>
                            <p
                              style={{
                                fontSize: "0.875rem",
                                color: "#6b7280",
                                margin: 0,
                                textTransform: "uppercase",
                                fontWeight: 600,
                                letterSpacing: "0.05em",
                                marginBottom: "0.5rem",
                              }}
                            >
                              General Comments
                            </p>
                            <p
                              style={{
                                color: "#111827",
                                margin: 0,
                                whiteSpace: "pre-wrap",
                              }}
                            >
                              {feedback.generalComments}
                            </p>
                          </div>
                        )}
                        {feedback.criteria && feedback.criteria.length > 0 && (
                          <div>
                            <p
                              style={{
                                fontSize: "0.875rem",
                                color: "#6b7280",
                                margin: 0,
                                textTransform: "uppercase",
                                fontWeight: 600,
                                letterSpacing: "0.05em",
                                marginBottom: "0.75rem",
                              }}
                            >
                              Criteria
                            </p>
                            <div className="table-container">
                              <table className="data-table">
                                <thead>
                                  <tr>
                                    <th>Criterion</th>
                                    <th>Score</th>
                                    <th>Weight</th>
                                    <th>Comments</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {feedback.criteria.map((criterion, cIdx) => (
                                    <tr key={criterion.criterionId || cIdx}>
                                      <td style={{ fontWeight: 500 }}>
                                        {criterion.name}
                                      </td>
                                      <td>
                                        <span
                                          style={{
                                            fontWeight: 700,
                                            color: "#7f1d1d",
                                          }}
                                        >
                                          {criterion.score}/
                                          {criterion.maxPoints}
                                        </span>
                                      </td>
                                      <td>
                                        <span className="badge info">
                                          {criterion.weight}%
                                        </span>
                                      </td>
                                      <td>{criterion.comments || "—"}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === "insights" && (
            <div>
              <h3
                style={{
                  fontSize: "1.125rem",
                  fontWeight: 600,
                  marginBottom: "1.5rem",
                }}
              >
                Submission Status Summary
              </h3>
              {deliverables.length === 0 ? (
                <div
                  style={{
                    textAlign: "center",
                    padding: "3rem",
                    color: "#6b7280",
                  }}
                >
                  <FileText
                    size={40}
                    style={{ margin: "0 auto 1rem", opacity: 0.3 }}
                  />
                  <div>No deliverables assigned yet</div>
                </div>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Deliverable</th>
                        <th>Stage</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {deliverables.map((deliverable) => {
                        const submission = submissions.find(
                          (s) => s.deliverableId === deliverable.id
                        );
                        
                        let status = "NOT_SUBMITTED";
                        let statusText = "Missing";
                        
                        if (submission) {
                          status = submission.status;
                          if (status === "SUBMITTED_ON_TIME") {
                            statusText = "On Time (Passed)";
                          } else if (status === "SUBMITTED_LATE") {
                            statusText = "Late";
                          } else if (status === "NOT_SUBMITTED") {
                            statusText = "Missing";
                          } else {
                            statusText = status;
                          }
                        }
                        
                        return (
                          <tr key={deliverable.id}>
                            <td style={{ fontWeight: 500 }}>
                              {deliverable.name}
                            </td>
                            <td>{deliverable.stage || "—"}</td>
                            <td>
                              <span className={`badge ${statusTone(status)}`}>
                                {statusText}
                              </span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {activeTab === "timeline" && (
            <div>
              <h3
                style={{
                  fontSize: "1.125rem",
                  fontWeight: 600,
                  marginBottom: "1.5rem",
                }}
              >
                Activity Timeline
              </h3>
              <div
                style={{
                  textAlign: "center",
                  padding: "3rem",
                  color: "#6b7280",
                }}
              >
                <Calendar
                  size={40}
                  style={{ margin: "0 auto 1rem", opacity: 0.3 }}
                />
                <div>No timeline activity yet</div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdviserGroupDetails;
