import { useState, useEffect, useCallback } from "react";
import { Users, FileText, ChevronDown, ChevronRight, CheckCircle2, XCircle, Link2, UserPlus } from "lucide-react";
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

  // Add-student search modal state
  const [addingToSectionId, setAddingToSectionId] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [addError, setAddError] = useState(null);
  const [addSaving, setAddSaving] = useState(false);

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

  const openAddModal = (sectionId) => {
    setAddingToSectionId(sectionId);
    setSearchQuery("");
    setSearchResults([]);
    setSelectedStudent(null);
    setAddError(null);
  };

  const closeAddModal = () => {
    setAddingToSectionId(null);
    setAddError(null);
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    setSearchLoading(true);
    setAddError(null);
    setSelectedStudent(null);
    try {
      const results = await apiService.searchStudents(searchQuery.trim());
      setSearchResults(Array.isArray(results) ? results : (results?.data ?? []));
    } catch (err) {
      setAddError("Search failed: " + (err.message || "Unknown error"));
    } finally {
      setSearchLoading(false);
    }
  };

  const handleEnrollStudent = async () => {
    if (!selectedStudent) return;
    setAddError(null);
    setAddSaving(true);
    try {
      await apiService.addStudentManually({
        userId: selectedStudent.id,
        classSectionId: addingToSectionId,
      });
      closeAddModal();
      fetchData();
    } catch (err) {
      setAddError(err.message || "Failed to enroll student");
    } finally {
      setAddSaving(false);
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

  // Modal overlay style
  const overlayStyle = {
    position: "fixed", inset: 0, backgroundColor: "rgba(0,0,0,0.4)",
    display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000,
  };
  const modalStyle = {
    background: "#fff", borderRadius: "0.75rem", padding: "1.5rem",
    width: "100%", maxWidth: 440, boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
  };

  return (
    <div className="fade-in">
      {/* Add Student Search Modal */}
      {addingToSectionId !== null && (
        <div style={overlayStyle}>
          <div style={{ ...modalStyle, maxWidth: 500 }}>
            <h2 style={{ margin: "0 0 0.25rem 0", fontSize: "1.25rem" }}>Add Late Enrollee</h2>
            <p style={{ margin: "0 0 1rem 0", color: "#6b7280", fontSize: "0.875rem" }}>
              Search for a student account to enroll into this section.
            </p>

            {/* Search bar */}
            <form onSubmit={handleSearch} style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
              <input
                className="form-input"
                style={{ flex: 1 }}
                placeholder="Search by name, student ID, or email…"
                value={searchQuery}
                onChange={(e) => { setSearchQuery(e.target.value); setSelectedStudent(null); }}
                autoFocus
              />
              <button type="submit" className="btn btn-primary" disabled={searchLoading || !searchQuery.trim()}>
                {searchLoading ? "Searching…" : "Search"}
              </button>
            </form>

            {/* Results list */}
            {searchResults.length > 0 && (
              <div style={{
                border: "1px solid #e5e7eb", borderRadius: "0.5rem",
                maxHeight: 220, overflowY: "auto", marginBottom: "1rem"
              }}>
                {searchResults.map((u) => {
                  const isSelected = selectedStudent?.id === u.id;
                  return (
                    <div
                      key={u.id}
                      onClick={() => setSelectedStudent(u)}
                      style={{
                        padding: "0.625rem 0.875rem",
                        cursor: "pointer",
                        borderBottom: "1px solid #f3f4f6",
                        backgroundColor: isSelected ? "#eff6ff" : "transparent",
                        borderLeft: isSelected ? "3px solid #3b82f6" : "3px solid transparent",
                        transition: "background 0.15s",
                      }}
                    >
                      <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                        {u.lastName ? `${u.lastName}, ${u.firstName}` : u.firstName}
                      </div>
                      <div style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                        {u.studentId && <span style={{ marginRight: "0.75rem" }}>{u.studentId}</span>}
                        {u.email}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {searchResults.length === 0 && !searchLoading && searchQuery && (
              <p style={{ color: "#6b7280", fontSize: "0.875rem", marginBottom: "1rem" }}>
                No student accounts found.
              </p>
            )}

            {addError && (
              <p style={{ color: "#dc2626", fontSize: "0.875rem", marginBottom: "0.75rem" }}>{addError}</p>
            )}

            {/* Selected preview */}
            {selectedStudent && (
              <div style={{
                background: "#f0fdf4", border: "1px solid #bbf7d0",
                borderRadius: "0.5rem", padding: "0.625rem 0.875rem",
                marginBottom: "1rem", fontSize: "0.875rem"
              }}>
                <strong>Selected:</strong>{" "}
                {selectedStudent.lastName ? `${selectedStudent.lastName}, ${selectedStudent.firstName}` : selectedStudent.firstName}
                {selectedStudent.studentId && ` — ${selectedStudent.studentId}`}
              </div>
            )}

            <div style={{ display: "flex", gap: "0.75rem", justifyContent: "flex-end" }}>
              <button className="btn btn-secondary" onClick={closeAddModal} disabled={addSaving}>
                Cancel
              </button>
              <button
                className="btn btn-primary"
                onClick={handleEnrollStudent}
                disabled={!selectedStudent || addSaving}
              >
                {addSaving ? "Enrolling…" : "Enroll Student"}
              </button>
            </div>
          </div>
        </div>
      )}
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
                        <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }} onClick={(e) => e.stopPropagation()}>
                          <span className="badge info">
                            {section.enrollments?.length || 0} Students
                          </span>
                          <button
                            className="btn btn-primary btn-sm"
                            title="Add late enrollee"
                            onClick={() => openAddModal(section.id)}
                          >
                            <UserPlus size={14} style={{ marginRight: "0.25rem" }} />
                            Add Student
                          </button>
                        </div>
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