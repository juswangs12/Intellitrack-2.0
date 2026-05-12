import { useState, useEffect, useCallback } from "react";
import { Users, FileText, ChevronDown, ChevronRight, CheckCircle2, XCircle, Link2 } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const CoordinatorClasslistView = () => {
  const { user } = useAuth();
  const [subjectsWithSections, setSubjectsWithSections] = useState([]);
  const [expandedSubjects, setExpandedSubjects] = useState({});
  const [expandedSections, setExpandedSections] = useState({});
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [linkingEnrollmentId, setLinkingEnrollmentId] = useState(null);
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [saving, setSaving] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [subjectData, userData] = await Promise.all([
        apiService.requestJson("/subjects/with-sections"),
        apiService.getAllUsers()
      ]);
      setSubjectsWithSections(Array.isArray(subjectData) ? subjectData : []);
      setUsers(Array.isArray(userData) ? userData : []);
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

  const handleManualLink = async (enrollmentId) => {
    if (!selectedUserId) return;
    
    try {
      setSaving(true);
      await apiService.linkUserToEnrollment(enrollmentId, selectedUserId);
      setLinkingEnrollmentId(null);
      setSelectedUserId(null);
      fetchData();
    } catch (err) {
      console.error("Failed to link user", err);
      setError("Failed to link user: " + (err.message || "Unknown error"));
    } finally {
      setSaving(false);
    }
  };

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
                                  <th>Actions</th>
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
                                    <td>
                                      {!enrollment.userId && (
                                        linkingEnrollmentId === enrollment.id ? (
                                          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                                            <select
                                              className="form-select"
                                              style={{ width: 'auto', minWidth: 200 }}
                                              value={selectedUserId || ""}
                                              onChange={(e) => setSelectedUserId(e.target.value ? Number(e.target.value) : null)}
                                            >
                                              <option value="">Select user...</option>
                                              {users.filter(u => u.role === 'student').map((u) => (
                                                <option key={u.id} value={u.id}>
                                                  {u.firstName} {u.lastName} ({u.email})
                                                </option>
                                              ))}
                                            </select>
                                            <button 
                                              className="btn btn-primary btn-sm"
                                              onClick={() => handleManualLink(enrollment.id)}
                                              disabled={!selectedUserId || saving}
                                            >
                                              {saving ? 'Saving...' : 'Save'}
                                            </button>
                                            <button 
                                              className="btn btn-secondary btn-sm"
                                              onClick={() => {
                                                setLinkingEnrollmentId(null);
                                                setSelectedUserId(null);
                                              }}
                                              disabled={saving}
                                            >
                                              Cancel
                                            </button>
                                          </div>
                                        ) : (
                                          <button 
                                            className="btn btn-primary btn-sm"
                                            onClick={() => setLinkingEnrollmentId(enrollment.id)}
                                          >
                                            <Link2 size={14} /> Link Account
                                          </button>
                                        )
                                      )}
                                    </td>
                                  </tr>
                                ))}
                                {(!section.enrollments || section.enrollments.length === 0) && (
                                  <tr>
                                    <td
                                      colSpan="5"
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