import { useState, useEffect } from "react";
import {
  ChevronDown,
  ChevronRight,
  BookOpen,
  Users,
  Clock,
  AlertTriangle,
  Eye,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import apiService from "../../services/ApiService";

const CoordinatorSubmissionTracking = () => {
  const navigate = useNavigate();
  const [trackingData, setTrackingData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedSubjects, setExpandedSubjects] = useState({});
  const [expandedSections, setExpandedSections] = useState({});

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const res = await apiService.getSubmissionTrackingSummary();
        setTrackingData(res.data || []);
        // Auto-expand all by default
        const subjectMap = {};
        const sectionMap = {};
        (res.data || []).forEach((subject, si) => {
          subjectMap[si] = true;
          (subject.sections || []).forEach((_, secI) => {
            sectionMap[`${si}-${secI}`] = true;
          });
        });
        setExpandedSubjects(subjectMap);
        setExpandedSections(sectionMap);
      } catch (err) {
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

  const toggleSection = (key) => {
    setExpandedSections((prev) => ({ ...prev, [key]: !prev[key] }));
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

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Submission Tracking</h1>
        <p>
          Overview of submission statuses by class code, section, and group.
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

      {trackingData.length === 0 && !error && (
        <div
          className="card"
          style={{ textAlign: "center", padding: "3rem", color: "#6b7280" }}
        >
          <BookOpen size={48} style={{ margin: "0 auto 1rem", opacity: 0.3 }} />
          <p>No groups found.</p>
        </div>
      )}

      {trackingData.map((subject, si) => (
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
            <BookOpen size={18} style={{ color: "#6366f1" }} />
            <span
              style={{ fontWeight: 700, fontSize: "1rem", color: "#1f2937" }}
            >
              {subject.subjectCode}
            </span>
            <span style={{ fontSize: "0.8125rem", color: "#6b7280" }}>
              {subject.sections?.length} section
              {subject.sections?.length !== 1 ? "s" : ""}
            </span>
          </div>

          {expandedSubjects[si] && (
            <div style={{ padding: "0 0 0.5rem 0" }}>
              {(subject.sections || []).map((section, secI) => {
                const sectionKey = `${si}-${secI}`;
                return (
                  <div key={secI} style={{ margin: "0.5rem 1.25rem 0.75rem" }}>
                    {/* Section header */}
                    <div
                      onClick={() => toggleSection(sectionKey)}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: "0.625rem",
                        cursor: "pointer",
                        padding: "0.625rem 0.875rem",
                        backgroundColor: "#f9fafb",
                        borderRadius: "0.5rem",
                        border: "1px solid #e5e7eb",
                        userSelect: "none",
                        marginBottom: "0.5rem",
                      }}
                    >
                      {expandedSections[sectionKey] ? (
                        <ChevronDown size={16} />
                      ) : (
                        <ChevronRight size={16} />
                      )}
                      <Users size={16} style={{ color: "#059669" }} />
                      <span style={{ fontWeight: 600, fontSize: "0.9375rem" }}>
                        Section {section.sectionName}
                      </span>
                      <span style={{ fontSize: "0.8125rem", color: "#6b7280" }}>
                        {section.groups?.length} group
                        {section.groups?.length !== 1 ? "s" : ""}
                      </span>
                    </div>

                    {expandedSections[sectionKey] && (
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
                              <th>Adviser</th>
                              <th>Students</th>
                              <th>
                                <span
                                  style={{
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "0.375rem",
                                  }}
                                >
                                  <Eye size={14} style={{ color: "#059669" }} />{" "}
                                  On Time
                                </span>
                              </th>
                              <th>
                                <span
                                  style={{
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "0.375rem",
                                  }}
                                >
                                  <AlertTriangle
                                    size={14}
                                    style={{ color: "#d97706" }}
                                  />{" "}
                                  Late
                                </span>
                              </th>
                              <th>
                                <span
                                  style={{
                                    display: "flex",
                                    alignItems: "center",
                                    gap: "0.375rem",
                                  }}
                                >
                                  <Clock
                                    size={14}
                                    style={{ color: "#6366f1" }}
                                  />{" "}
                                  Pending
                                </span>
                              </th>
                              <th>Review Pending</th>
                              <th>Action</th>
                            </tr>
                          </thead>
                          <tbody>
                            {(section.groups || []).map((group) => (
                              <tr key={group.groupId}>
                                <td>
                                  <div>
                                    <div style={{ fontWeight: 600 }}>
                                      {group.groupTitle}
                                    </div>
                                    <div
                                      style={{
                                        fontSize: "0.8125rem",
                                        color: "#6b7280",
                                      }}
                                    >
                                      {group.groupCode}
                                    </div>
                                  </div>
                                </td>
                                <td
                                  style={{
                                    fontSize: "0.875rem",
                                    color: "#374151",
                                  }}
                                >
                                  {group.adviser || (
                                    <span style={{ color: "#9ca3af" }}>
                                      Not assigned
                                    </span>
                                  )}
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
                                    {group.studentCount}
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
                                    {group.onTime}
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
                                    {group.late}
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
                                    {group.pending}
                                  </span>
                                </td>
                                <td>
                                  {group.reviewPending > 0 ? (
                                    <span
                                      style={{
                                        padding: "0.25rem 0.625rem",
                                        borderRadius: "9999px",
                                        fontSize: "0.75rem",
                                        backgroundColor: "#fef2f2",
                                        color: "#b91c1c",
                                        border: "1px solid #fecaca",
                                      }}
                                    >
                                      {group.reviewPending}
                                    </span>
                                  ) : (
                                    <span
                                      style={{
                                        color: "#9ca3af",
                                        fontSize: "0.8125rem",
                                      }}
                                    >
                                      —
                                    </span>
                                  )}
                                </td>
                                <td>
                                  <button
                                    className="btn btn-secondary"
                                    style={{
                                      fontSize: "0.8125rem",
                                      padding: "0.375rem 0.75rem",
                                    }}
                                    onClick={() =>
                                      navigate("/coordinator/document-review")
                                    }
                                  >
                                    Review
                                  </button>
                                </td>
                              </tr>
                            ))}
                            {(section.groups || []).length === 0 && (
                              <tr>
                                <td
                                  colSpan="8"
                                  style={{
                                    textAlign: "center",
                                    padding: "1.5rem",
                                    color: "#6b7280",
                                  }}
                                >
                                  No groups in this section.
                                </td>
                              </tr>
                            )}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default CoordinatorSubmissionTracking;
