import { useState, useEffect, useCallback } from "react";
import {
  AlertTriangle,
  Sparkles,
  CheckCircle2,
  Clock,
  Activity,
  BookOpen,
  ChevronDown,
  ChevronRight,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
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

const CoordinatorAnalytics = () => {
  const { user } = useAuth();
  const [subjectAnalytics, setSubjectAnalytics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedSubjects, setExpandedSubjects] = useState({});

  const fetchAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const subjects = await apiService.requestJson("/analytics/subjects");
      setSubjectAnalytics(subjects);
      const subjectMap = {};
      subjects.forEach((subject, index) => {
        subjectMap[index] = true;
      });
      setExpandedSubjects(subjectMap);
    } catch (err) {
      console.error("Failed to fetch analytics", err);
      setError("Failed to load analytics.");
    } finally {
      setLoading(false);
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
        <h1 className="page-title">Subject Analytics</h1>
        <p className="page-description">Analytics grouped by subject, groups, and compliance.</p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", padding: "0.75rem 1rem", backgroundColor: "#fef2f2", border: "1px solid #fecaca", borderRadius: "0.5rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

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
    </div>
  );
};

export default CoordinatorAnalytics;
