import { useState, useEffect } from "react";
import {
  ChevronDown,
  ChevronRight,
  BookOpen,
  Users,
  Clock,
  AlertTriangle,
  CheckCircle2,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import apiService from "../../services/ApiService";

const CoordinatorSubmissionTracking = () => {
  const navigate = useNavigate();
  const [subjectAnalytics, setSubjectAnalytics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedSubjects, setExpandedSubjects] = useState({});

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const data = await apiService.requestJson("/analytics/subjects");
        setSubjectAnalytics(data);
        // Auto-expand all by default
        const subjectMap = {};
        data.forEach((subject, si) => {
          subjectMap[si] = true;
        });
        setExpandedSubjects(subjectMap);
      } catch (err) {
        console.error("Failed to load tracking data", err);
        setError("Failed to load tracking data.");
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const toggleSubject = (si) => {
    setExpandedSubjects((prev) => ({ ...prev, [si]: !prev[si] }));
  };

  if (loading) {
    return (
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          minHeight: 200,
        }}
      >
        <div style={{ textAlign: "center", color: "#6b7280" }}>
          Loading submission tracking data...
        </div>
      </div>
    );
  }

  const thStyle = {
    padding: "0.75rem 1rem", textAlign: "left", fontSize: "0.75rem",
    fontWeight: 600, color: "#6b7280", textTransform: "uppercase",
    letterSpacing: "0.05em", borderBottom: "1px solid #e5e7eb",
    backgroundColor: "#f9fafb",
  };
  const tdStyle = (i) => ({ padding: "0.875rem 1rem", backgroundColor: i % 2 === 0 ? "#fff" : "#fafafa" });

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Submission Tracking</h1>
        <p>
          Overview of submission statuses by subject and group.
        </p>
      </div>

      {error && (
        <div
          style={{
            marginBottom: "1rem",
            padding: "0.75rem 1rem",
            backgroundColor: "#fef2f2",
            color: "#b91c1c",
            borderRadius: "0.5rem",
            border: "1px solid #fecaca",
          }}
        >
          {error}
        </div>
      )}

      {subjectAnalytics.length === 0 && !error && (
        <div
          className="card"
          style={{ textAlign: "center", padding: "3rem", color: "#6b7280" }}
        >
          <BookOpen size={48} style={{ margin: "0 auto 1rem", opacity: 0.3 }} />
          <p>No groups found.</p>
        </div>
      )}

      {subjectAnalytics.map((subject, si) => (
        <div key={si} className="card" style={{ marginBottom: "1rem" }}>
          {/* Subject header */}
          <div
            onClick={() => toggleSubject(si)}
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.75rem",
              cursor: "pointer",
              padding: "1rem 1.25rem",
              borderBottom: expandedSubjects[si] ? "1px solid #e5e7eb" : "none",
              userSelect: "none",
            }}
          >
            {expandedSubjects[si] ? (
              <ChevronDown size={18} />
            ) : (
              <ChevronRight size={18} />
            )}
            <BookOpen size={18} style={{ color: "var(--maroon)" }} />
            <span
              style={{ fontWeight: 700, fontSize: "1rem", color: "#1f2937" }}
            >
              {subject.subjectCode} - {subject.subjectName}
            </span>
            <span style={{ fontSize: "0.8125rem", color: "#6b7280" }}>
              {subject.totalGroups} group{subject.totalGroups !== 1 ? "s" : ""} • {subject.totalStudents} student{subject.totalStudents !== 1 ? "s" : ""}
            </span>
          </div>

          {expandedSubjects[si] && (
            <div style={{ padding: "0 1.25rem 1.25rem" }}>
              <div
                className="table-container"
                style={{
                  borderRadius: "0.5rem",
                  border: "1px solid #e5e7eb",
                }}
              >
                <table className="data-table" style={{ margin: 0 }}>
                  <thead>
                    <tr>
                      <th>Group</th>
                      <th>Students</th>
                      <th>
                        <span style={{ display: "flex", alignItems: "center", gap: "0.375rem" }}>
                          <CheckCircle2 size={14} style={{ color: "#059669" }} /> On-Time
                        </span>
                      </th>
                      <th>
                        <span style={{ display: "flex", alignItems: "center", gap: "0.375rem" }}>
                          <AlertTriangle size={14} style={{ color: "#d97706" }} /> Late
                        </span>
                      </th>
                      <th>
                        <span style={{ display: "flex", alignItems: "center", gap: "0.375rem" }}>
                          <Clock size={14} style={{ color: "#6366f1" }} /> Missing
                        </span>
                      </th>
                      <th>On-Time Students</th>
                      <th>Late Students</th>
                      <th>Missing Students</th>
                    </tr>
                  </thead>
                  <tbody>
                    {subject.groupCompliance.map((group, gi) => (
                      <tr key={group.groupId}>
                        <td>
                          <div>
                            <div style={{ fontWeight: 600 }}>
                              {group.groupTitle}
                            </div>
                            <div style={{ fontSize: "0.8125rem", color: "#6b7280" }}>
                              {group.groupCode}
                            </div>
                          </div>
                        </td>
                        <td>
                          <span
                            style={{
                              padding: "0.25rem 0.625rem",
                              borderRadius: "9999px",
                              fontSize: "0.75rem",
                              backgroundColor: "#eff6ff",
                              color: "#1d4ed8",
                              border: "1px solid #bfdbfe",
                            }}
                          >
                            {group.onTimeStudents + group.lateStudents + group.missingStudents}
                          </span>
                        </td>
                        <td>
                          <span
                            style={{
                              padding: "0.25rem 0.625rem",
                              borderRadius: "9999px",
                              fontSize: "0.75rem",
                              backgroundColor: "#f0fdf4",
                              color: "#166534",
                              border: "1px solid #bbf7d0",
                            }}
                          >
                            {group.submittedDeliverables - group.lateDeliverables}
                          </span>
                        </td>
                        <td>
                          <span
                            style={{
                              padding: "0.25rem 0.625rem",
                              borderRadius: "9999px",
                              fontSize: "0.75rem",
                              backgroundColor: "#fffbeb",
                              color: "#92400e",
                              border: "1px solid #fde68a",
                            }}
                          >
                            {group.lateDeliverables}
                          </span>
                        </td>
                        <td>
                          <span
                            style={{
                              padding: "0.25rem 0.625rem",
                              borderRadius: "9999px",
                              fontSize: "0.75rem",
                              backgroundColor: "#eef2ff",
                              color: "#3730a3",
                              border: "1px solid #c7d2fe",
                            }}
                          >
                            {group.missingDeliverables}
                          </span>
                        </td>
                        <td>
                          <span
                            style={{
                              padding: "0.25rem 0.625rem",
                              borderRadius: "9999px",
                              fontSize: "0.75rem",
                              backgroundColor: "#f0fdf4",
                              color: "#166534",
                              border: "1px solid #bbf7d0",
                            }}
                          >
                            {group.onTimeStudents}
                          </span>
                        </td>
                        <td>
                          <span
                            style={{
                              padding: "0.25rem 0.625rem",
                              borderRadius: "9999px",
                              fontSize: "0.75rem",
                              backgroundColor: "#fffbeb",
                              color: "#92400e",
                              border: "1px solid #fde68a",
                            }}
                          >
                            {group.lateStudents}
                          </span>
                        </td>
                        <td>
                          <span
                            style={{
                              padding: "0.25rem 0.625rem",
                              borderRadius: "9999px",
                              fontSize: "0.75rem",
                              backgroundColor: "#eef2ff",
                              color: "#3730a3",
                              border: "1px solid #c7d2fe",
                            }}
                          >
                            {group.missingStudents}
                          </span>
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
    </div>
  );
};

export default CoordinatorSubmissionTracking;
