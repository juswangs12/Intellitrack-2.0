import { useState, useEffect, useCallback } from "react";
import { Users, FileText, ChevronDown, ChevronRight, CheckCircle2, XCircle } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const CoordinatorClasslistView = () => {
  const { user } = useAuth();
  const [subjectsWithSections, setSubjectsWithSections] = useState([]);
  const [expandedSubjects, setExpandedSubjects] = useState({});
  const [expandedSections, setExpandedSections] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiService.requestJson("/subjects/with-sections");
      setSubjectsWithSections(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to fetch classlists", err);
      setError("Failed to load classlists.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const toggleSubject = (subjectId) => {
    setExpandedSubjects(prev => ({
      ...prev,
      [subjectId]: !prev[subjectId]
    }));
  };

  const toggleSection = (sectionId) => {
    setExpandedSections(prev => ({
      ...prev,
      [sectionId]: !prev[sectionId]
    }));
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Classlist Management</h1>
        <p className="page-description">
          View imported classlists grouped by subject and section.
        </p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      <div>
        {subjectsWithSections.length === 0 ? (
          <div className="card">
            <div className="card-content" style={{ textAlign: "center", padding: "3rem" }}>
              <FileText size={48} style={{ color: "#9ca3af", marginBottom: "1rem" }} />
              <h3 style={{ margin: "0 0 0.5rem 0" }}>No Classlists Imported</h3>
              <p style={{ color: "#6b7280", margin: 0 }}>
                Use the Classlist Import page to upload student classlists.
              </p>
            </div>
          </div>
        ) : (
          subjectsWithSections.map((subject) => (
            <div key={subject.id} className="card" style={{ marginBottom: "1rem" }}>
              <div
                className="card-header"
                style={{ cursor: "pointer", userSelect: "none" }}
                onClick={() => toggleSubject(subject.id)}
              >
                <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                  {expandedSubjects[subject.id] ? (
                    <ChevronDown size={20} />
                  ) : (
                    <ChevronRight size={20} />
                  )}
                  <h2 className="card-title" style={{ margin: 0 }}>
                    {subject.name} — {subject.code}
                  </h2>
                </div>
                <span className="badge info">
                  {subject.sections?.length || 0} Sections
                </span>
              </div>
              {expandedSubjects[subject.id] && (
                <div className="card-content" style={{ paddingTop: 0 }}>
                  {subject.sections?.map((section) => (
                    <div
                      key={section.id}
                      style={{
                        borderTop: "1px solid #e5e7eb",
                        paddingTop: "1rem",
                        marginTop: "1rem"
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                          cursor: "pointer",
                          userSelect: "none",
                        }}
                        onClick={() => toggleSection(section.id)}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                          {expandedSections[section.id] ? (
                            <ChevronDown size={16} />
                          ) : (
                            <ChevronRight size={16} />
                          )}
                          <h3 style={{ margin: 0, fontSize: "1.125rem" }}>
                            Section: {section.section}
                          </h3>
                        </div>
                        <span className="badge info">
                          {section.enrollments?.length || 0} Students
                        </span>
                      </div>
                      {expandedSections[section.id] && (
                        <div style={{ marginTop: "1rem" }}>
                          <div className="table-container">
                            <table className="data-table">
                              <thead>
                                <tr>
                                  <th>Student ID</th>
                                  <th>Full Name</th>
                                  <th>Email</th>
                                  <th>Linked to Account</th>
                                </tr>
                              </thead>
                              <tbody>
                                {section.enrollments?.map((enrollment) => (
                                  <tr key={enrollment.id}>
                                    <td>{enrollment.studentId}</td>
                                    <td>{enrollment.fullName}</td>
                                    <td>{enrollment.email}</td>
                                    <td>
                                      {enrollment.userId ? (
                                        <span className="badge success">
                                          <CheckCircle2 size={12} /> Linked
                                        </span>
                                      ) : (
                                        <span className="badge warning">
                                          <XCircle size={12} /> Not Linked
                                        </span>
                                      )}
                                    </td>
                                  </tr>
                                ))}
                                {(!section.enrollments || section.enrollments.length === 0) && (
                                  <tr>
                                    <td
                                      colSpan="4"
                                      style={{
                                        textAlign: "center",
                                        padding: "2rem",
                                        color: "#6b7280"
                                      }}
                                    >
                                      No students in this section
                                    </td>
                                  </tr>
                                )}
                              </tbody>
                            </table>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default CoordinatorClasslistView;