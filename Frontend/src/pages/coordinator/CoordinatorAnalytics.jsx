import { useState, useEffect, useCallback } from "react";
import {
  Sparkles,
  BookOpen,
  User,
  ChevronDown,
  ChevronRight,
  RefreshCw,
} from "lucide-react";
import apiService from "../../services/ApiService";

/* ─── tiny helpers ─────────────────────────────────────── */
const rateColor = (pct) =>
  pct >= 80 ? "#10b981" : pct >= 50 ? "#f59e0b" : "#ef4444";

const RateBar = ({ value }) => (
  <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
    <div style={{ flex: 1, height: "6px", backgroundColor: "#e5e7eb", borderRadius: "9999px", overflow: "hidden" }}>
      <div style={{ height: "100%", width: `${Math.min(value, 100)}%`, backgroundColor: rateColor(value), borderRadius: "9999px", transition: "width 0.4s ease" }} />
    </div>
    <span style={{ fontSize: "0.8125rem", fontWeight: 600, color: rateColor(value), minWidth: "2.5rem", textAlign: "right" }}>
      {value}%
    </span>
  </div>
);

const RiskBadge = ({ level }) => {
  const cfg = level === "HIGH"
    ? { bg: "#fef2f2", color: "#b91c1c", border: "#fecaca" }
    : { bg: "#fff7ed", color: "#9a3412", border: "#fed7aa" };
  return (
    <span style={{ padding: "0.2rem 0.6rem", borderRadius: "9999px", fontSize: "0.75rem", fontWeight: 600, backgroundColor: cfg.bg, color: cfg.color, border: `1px solid ${cfg.border}` }}>
      {level}
    </span>
  );
};

const DeliverableStatusBadge = ({ status }) => {
  const config = {
    SUBMITTED_ON_TIME: { label: "On Time", bg: "#f0fdf4", color: "#166534", border: "#bbf7d0" },
    SUBMITTED_LATE: { label: "Late", bg: "#fef2f2", color: "#b91c1c", border: "#fecaca" },
    NOT_SUBMITTED: { label: "Missing", bg: "#fff7ed", color: "#9a3412", border: "#fed7aa" },
    PENDING: { label: "Pending", bg: "#f9fafb", color: "#6b7280", border: "#e5e7eb" },
    COMMENTED: { label: "Commented", bg: "#eff6ff", color: "#1d4ed8", border: "#bfdbfe" },
  };
  const cfg = config[status] || { label: status || "—", bg: "#f9fafb", color: "#6b7280", border: "#e5e7eb" };
  return (
    <span style={{ 
      padding: "0.25rem 0.625rem", 
      borderRadius: "9999px", 
      fontSize: "0.75rem", 
      fontWeight: 600, 
      backgroundColor: cfg.bg, 
      color: cfg.color, 
      border: `1px solid ${cfg.border}` 
    }}>
      {cfg.label}
    </span>
  );
};

const CoordinatorAnalytics = () => {
  const [subjectAnalytics, setSubjectAnalytics] = useState([]);
  const [studentAnalytics, setStudentAnalytics] = useState([]);
  const [aiSummary, setAiSummary] = useState("");
  const [loading, setLoading] = useState(true);
  const [generatingAi, setGeneratingAi] = useState(false);
  const [error, setError] = useState(null);
  const [expandedSubjects, setExpandedSubjects] = useState({});
  const [expandedStudents, setExpandedStudents] = useState({});
  const [activeTab, setActiveTab] = useState("subjects");

  const fetchAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const summary = await apiService.requestJson("/analytics/coordinator-summary");
      setSubjectAnalytics(summary.subjectAnalytics);
      setStudentAnalytics(summary.studentAnalytics);
      setAiSummary(summary.aiSummary);
      
      const subjectMap = {};
      if (summary.subjectAnalytics) {
        summary.subjectAnalytics.forEach((subject, index) => {
          subjectMap[index] = true;
        });
      }
      setExpandedSubjects(subjectMap);
      
      const studentMap = {};
      if (summary.studentAnalytics) {
        summary.studentAnalytics.forEach((student, index) => {
          studentMap[index] = true;
        });
      }
      setExpandedStudents(studentMap);
    } catch (err) {
      console.error("Failed to fetch analytics", err);
      setError("Failed to load analytics.");
    } finally {
      setLoading(false);
    }
  }, []);

  const generateAiSummary = useCallback(async () => {
    try {
      setGeneratingAi(true);
      setError(null);
      const summary = await apiService.requestJson("/analytics/generate-coordinator-ai-summary", {
        method: "POST",
      });
      if (summary) {
        setAiSummary(summary);
      }
    } catch (err) {
      console.error("Failed to generate AI summary", err);
      setError("Failed to generate AI summary.");
    } finally {
      setGeneratingAi(false);
    }
  }, []);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  const toggleSubject = (index) => {
    setExpandedSubjects((prev) => ({
      ...prev,
      [index]: !prev[index],
    }));
  };

  const toggleStudent = (index) => {
    setExpandedStudents((prev) => ({
      ...prev,
      [index]: !prev[index],
    }));
  };

  const thStyle = {
    padding: "0.75rem 1rem", textAlign: "left", fontSize: "0.75rem",
    fontWeight: 600, color: "#6b7280", textTransform: "uppercase",
    letterSpacing: "0.05em", borderBottom: "1px solid #e5e7eb",
    backgroundColor: "#f9fafb",
  };
  const tdStyle = (i) => ({ padding: "0.875rem 1rem", backgroundColor: i % 2 === 0 ? "#fff" : "#fafafa" });

  if (loading)
    return (
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", minHeight: "400px", color: "#6b7280" }}>
        Loading analytics…
      </div>
    );

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Analytics</h1>
        <p className="page-description">View subject and student-level analytics.</p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", padding: "0.75rem 1rem", backgroundColor: "#fef2f2", border: "1px solid #fecaca", borderRadius: "0.5rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

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
            <span style={{ fontWeight: 700, fontSize: "0.875rem", color: "var(--maroon)" }}>AI Analytics Summary</span>
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
      </div>

      {/* Tabs */}
      <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
        <button 
          style={{
            padding: "0.75rem 1.25rem",
            borderRadius: "0.5rem",
            border: "none",
            fontWeight: 600,
            cursor: "pointer",
            backgroundColor: activeTab === "subjects" ? "var(--maroon)" : "#f3f4f6",
            color: activeTab === "subjects" ? "#fff" : "#374151",
          }}
          onClick={() => setActiveTab("subjects")}
        >
          <BookOpen size={16} style={{ display: "inline", marginRight: "0.5rem" }} />
          Subject Analytics
        </button>
        <button 
          style={{
            padding: "0.75rem 1.25rem",
            borderRadius: "0.5rem",
            border: "none",
            fontWeight: 600,
            cursor: "pointer",
            backgroundColor: activeTab === "students" ? "var(--maroon)" : "#f3f4f6",
            color: activeTab === "students" ? "#fff" : "#374151",
          }}
          onClick={() => setActiveTab("students")}
        >
          <User size={16} style={{ display: "inline", marginRight: "0.5rem" }} />
          Student Analytics
        </button>
      </div>

      {/* Subject Analytics Tab */}
      {activeTab === "subjects" && (
        <>
          {subjectAnalytics.map((subject, si) => (
            <div key={si} className="card" style={{ marginBottom: "1rem" }}>
              {/* Subject header */}
              <div
                onClick={() => toggleSubject(si)}
                style={{
                  display: "flex", alignItems: "center", gap: "0.75rem", cursor: "pointer",
                  padding: "1rem 1.25rem",
                  borderBottom: expandedSubjects[si] ? "1px solid #e5e7eb" : "none",
                  userSelect: "none"
                }}
              >
                {expandedSubjects[si] ? <ChevronDown size={18} /> : <ChevronRight size={18} />}
                <BookOpen size={18} style={{ color: "var(--maroon)" }} />
                <div style={{ flex: 1 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                    <span style={{ fontWeight: 700, fontSize: "1rem", color: "#1f2937" }}>
                      {subject.subjectCode} - {subject.subjectName}
                    </span>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginTop: "0.25rem" }}>
                    <span style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                      {subject.totalGroups} groups • {subject.totalStudents} students • {subject.totalDeliverables} deliverables
                    </span>
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                  <div style={{ minWidth: "150px" }}>
                    <RateBar value={subject.complianceRate} />
                  </div>
                </div>
              </div>

              {expandedSubjects[si] && (
                <div style={{ padding: "0 1.25rem 1.25rem" }}>
                  {/* Subject stats */}
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(140px,1fr))", gap: "1rem", marginBottom: "1rem", marginTop: "1rem" }}>
                    <div style={{ padding: "1rem", backgroundColor: "#f0fdf4", borderRadius: "0.5rem", border: "1px solid #bbf7d0" }}>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#166534", fontWeight: 600 }}>Compliance</p>
                      <p style={{ margin: 0, fontSize: "1.5rem", fontWeight: 800, color: "#15803d" }}>{subject.complianceRate}%</p>
                    </div>
                    <div style={{ padding: "1rem", backgroundColor: "#fffbeb", borderRadius: "0.5rem", border: "1px solid #fde68a" }}>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#92400e", fontWeight: 600 }}>Late Rate</p>
                      <p style={{ margin: 0, fontSize: "1.5rem", fontWeight: 800, color: "#d97706" }}>{subject.lateSubmissionRate}%</p>
                    </div>
                    <div style={{ padding: "1rem", backgroundColor: "#fef2f2", borderRadius: "0.5rem", border: "1px solid #fecaca" }}>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#b91c1c", fontWeight: 600 }}>Missing Rate</p>
                      <p style={{ margin: 0, fontSize: "1.5rem", fontWeight: 800, color: "#dc2626" }}>{subject.missingSubmissionRate}%</p>
                    </div>
                  </div>

                  {/* Group compliance table */}
                  <div style={{ overflowX: "auto" }}>
                    <table style={{ width: "100%", borderCollapse: "collapse" }}>
                      <thead>
                        <tr>
                          {["Group", "Compliance", "On-Time", "Late", "Missing", "On-Time Students", "Late Students", "Missing Students"].map((h) => (
                            <th key={h} style={thStyle}>{h}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {subject.groupCompliance.map((group, gi) => (
                          <tr key={group.groupId}>
                            <td style={{ ...tdStyle(gi), fontWeight: 600, color: "#1f2937" }}>
                              {group.groupTitle} ({group.groupCode})
                            </td>
                            <td style={{ ...tdStyle(gi), minWidth: "180px" }}>
                              <RateBar value={group.complianceRate} />
                            </td>
                            <td style={tdStyle(gi)}>{group.submittedDeliverables - group.lateDeliverables}</td>
                            <td style={tdStyle(gi)}>{group.lateDeliverables}</td>
                            <td style={tdStyle(gi)}>{group.missingDeliverables}</td>
                            <td style={tdStyle(gi)}>{group.onTimeStudents}</td>
                            <td style={tdStyle(gi)}>{group.lateStudents}</td>
                            <td style={tdStyle(gi)}>{group.missingStudents}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* At-risk groups for this subject */}
                  {subject.riskGroups && subject.riskGroups.length > 0 && (
                    <div style={{ marginTop: "1.5rem" }}>
                      <h3 style={{ margin: "0 0 0.75rem 0", fontSize: "0.875rem", fontWeight: 600, color: "#374151" }}>
                        At-Risk Groups in {subject.subjectCode}
                      </h3>
                      <div style={{ overflowX: "auto" }}>
                        <table style={{ width: "100%", borderCollapse: "collapse" }}>
                          <thead>
                            <tr>
                              {["Group", "Risk Level", "Reason", "Late Submissions"].map((h) => (
                                <th key={h} style={thStyle}>{h}</th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {subject.riskGroups.map((rg, rgi) => (
                              <tr key={rg.groupId}>
                                <td style={{ ...tdStyle(rgi), fontWeight: 600, color: "#1f2937" }}>{rg.groupTitle}</td>
                                <td style={tdStyle(rgi)}><RiskBadge level={rg.riskLevel} /></td>
                                <td style={{ ...tdStyle(rgi), maxWidth: "320px" }}>{rg.reason}</td>
                                <td style={{ ...tdStyle(rgi), fontWeight: 600, color: "#ef4444" }}>{rg.missedDeadlines}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}
                  {/* AI Summary */}
                  {subject.aiSummary && (
                    <div style={{
                      marginTop: "1.5rem", padding: "1rem 1.25rem",
                      backgroundColor: "#fdf8f0", borderRadius: "0.5rem",
                      borderLeft: "4px solid var(--maroon)",
                      border: "1px solid #f3e8d0",
                      borderLeftColor: "var(--maroon)",
                    }}>
                      <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", marginBottom: "0.5rem" }}>
                        <Sparkles size={16} style={{ color: "var(--maroon)" }} />
                        <span style={{ fontWeight: 700, fontSize: "0.875rem", color: "var(--maroon)" }}>AI Summary</span>
                      </div>
                      <p style={{ margin: 0, fontSize: "0.9rem", color: "#374151", lineHeight: 1.6 }}>
                        {subject.aiSummary}
                      </p>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </>
      )}

      {/* Student Analytics Tab */}
      {activeTab === "students" && (
        <>
          {studentAnalytics.map((student, si) => (
            <div key={si} className="card" style={{ marginBottom: "1rem" }}>
              {/* Student header */}
              <div
                onClick={() => toggleStudent(si)}
                style={{
                  display: "flex", alignItems: "center", gap: "0.75rem", cursor: "pointer",
                  padding: "1rem 1.25rem",
                  borderBottom: expandedStudents[si] ? "1px solid #e5e7eb" : "none",
                  userSelect: "none"
                }}
              >
                {expandedStudents[si] ? <ChevronDown size={18} /> : <ChevronRight size={18} />}
                <User size={18} style={{ color: "var(--maroon)" }} />
                <div style={{ flex: 1 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                    <span style={{ fontWeight: 700, fontSize: "1rem", color: "#1f2937" }}>
                      {student.studentName}
                    </span>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: "1rem", marginTop: "0.25rem", flexWrap: "wrap" }}>
                    <span style={{ fontSize: "0.75rem", color: "#6b7280" }}>
                      {student.groupTitle} ({student.groupCode}) • {student.subjectCode} • {student.sectionName}
                    </span>
                  </div>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                  <div style={{ minWidth: "150px" }}>
                    <RateBar value={student.complianceRate} />
                  </div>
                </div>
              </div>

              {expandedStudents[si] && (
                <div style={{ padding: "0 1.25rem 1.25rem" }}>
                  {/* Student stats */}
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(140px,1fr))", gap: "1rem", marginBottom: "1rem", marginTop: "1rem" }}>
                    <div style={{ padding: "1rem", backgroundColor: "#f0fdf4", borderRadius: "0.5rem", border: "1px solid #bbf7d0" }}>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#166534", fontWeight: 600 }}>On Time</p>
                      <p style={{ margin: 0, fontSize: "1.5rem", fontWeight: 800, color: "#15803d" }}>{student.onTimeDeliverables}</p>
                    </div>
                    <div style={{ padding: "1rem", backgroundColor: "#fffbeb", borderRadius: "0.5rem", border: "1px solid #fde68a" }}>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#92400e", fontWeight: 600 }}>Late</p>
                      <p style={{ margin: 0, fontSize: "1.5rem", fontWeight: 800, color: "#d97706" }}>{student.lateDeliverables}</p>
                    </div>
                    <div style={{ padding: "1rem", backgroundColor: "#fef2f2", borderRadius: "0.5rem", border: "1px solid #fecaca" }}>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#b91c1c", fontWeight: 600 }}>Missing</p>
                      <p style={{ margin: 0, fontSize: "1.5rem", fontWeight: 800, color: "#dc2626" }}>{student.missingDeliverables}</p>
                    </div>
                    <div style={{ padding: "1rem", backgroundColor: "#eff6ff", borderRadius: "0.5rem", border: "1px solid #bfdbfe" }}>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#1d4ed8", fontWeight: 600 }}>Total</p>
                      <p style={{ margin: 0, fontSize: "1.5rem", fontWeight: 800, color: "#1e40af" }}>{student.totalDeliverables}</p>
                    </div>
                  </div>

                  {/* Deliverable status table */}
                  <div style={{ overflowX: "auto" }}>
                    <table style={{ width: "100%", borderCollapse: "collapse" }}>
                      <thead>
                        <tr>
                          {["Deliverable", "Stage", "Status", "Submitted At", "Late Days"].map((h) => (
                            <th key={h} style={thStyle}>{h}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {student.deliverableStatuses.map((deliverable, di) => (
                          <tr key={deliverable.deliverableId}>
                            <td style={{ ...tdStyle(di), fontWeight: 600, color: "#1f2937" }}>
                              {deliverable.deliverableName}
                            </td>
                            <td style={tdStyle(di)}>{deliverable.stage}</td>
                            <td style={tdStyle(di)}>
                              <DeliverableStatusBadge status={deliverable.status} />
                            </td>
                            <td style={tdStyle(di)}>
                              {deliverable.submittedAt ? new Date(deliverable.submittedAt).toLocaleString() : "—"}
                            </td>
                            <td style={tdStyle(di)}>
                              {deliverable.lateDays && deliverable.lateDays > 0 ? (
                                <span style={{ color: "#b91c1c", fontWeight: 600 }}>{deliverable.lateDays} days</span>
                              ) : "—"}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          ))}

          {studentAnalytics.length === 0 && (
            <div className="card">
              <div className="card-content" style={{ textAlign: "center", padding: "2.5rem" }}>
                <User size={40} style={{ margin: "0 auto 1rem", opacity: 0.3, display: "block" }} />
                <p style={{ margin: 0, color: "#6b7280" }}>No student analytics available yet.</p>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default CoordinatorAnalytics;
