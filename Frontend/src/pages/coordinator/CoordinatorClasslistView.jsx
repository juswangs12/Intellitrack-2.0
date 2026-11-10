import { useState, useEffect, useCallback } from "react";
import {
  FileText,
  ChevronDown,
  ChevronRight,
  CheckCircle2,
  XCircle,
  Link2,
  UserPlus,
  Trash2,
  ArrowRightLeft,
} from "lucide-react";
import apiService from "../../services/ApiService";
import {Link} from 'react-router-dom';
import AwaitingEnrollment from '../../components/AwaitingEnrollment';

const CoordinatorClasslistView = () => {
  const [subjectsWithSections, setSubjectsWithSections] = useState([]);
  const [schoolYear,setSchoolYear]=useState(''),[semester,setSemester]=useState('');
  const years=[...new Set(subjectsWithSections.flatMap(s=>(s.sections||[]).filter(c=>!c.semesterArchived).map(c=>c.academicTerm?.split(' / ')[0]).filter(Boolean)))].sort().reverse();
  const visibleSubjects=subjectsWithSections.map(s=>({...s,sections:(s.sections||[]).filter(c=>!c.semesterArchived).filter(c=>(!schoolYear||c.academicTerm?.startsWith(schoolYear+' / '))&&(!semester||c.academicTerm?.endsWith(' / '+semester)))})).filter(s=>s.sections.length);
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

  // Unenroll confirmation modal state
  const [unenrollTarget, setUnenrollTarget] = useState(null); // enrollment object
  const [unenrollSaving, setUnenrollSaving] = useState(false);

  // Move student modal state
  const [moveModalData, setMoveModalData] = useState(null); // { enrollment, currentSectionId, availableSections }
  const [moveTargetSectionId, setMoveTargetSectionId] = useState("");
  const [moveSaving, setMoveSaving] = useState(false);
  const [moveError, setMoveError] = useState(null);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [subjectData, userData] = await Promise.all([
        apiService.requestJson("/subjects/with-sections"),
        apiService.getAllUsers(),
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
      setSearchResults(
        Array.isArray(results) ? results : (results?.data ?? []),
      );
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

  const handleUnenroll = async () => {
    if (!unenrollTarget) return;
    setUnenrollSaving(true);
    try {
      await apiService.unenrollStudent(unenrollTarget.id);
      setUnenrollTarget(null);
      fetchData();
    } catch (err) {
      setError(
        "Failed to unenroll student: " + (err.message || "Unknown error"),
      );
      setUnenrollTarget(null);
    } finally {
      setUnenrollSaving(false);
    }
  };

  const handleDeleteEmptySection = async (section) => {
    if (!window.confirm(`Delete empty section ${section.section} (${section.academicTerm})? This removes the section record from the database.`)) return;
    try {
      await apiService.requestJson(`/academic/sections/${section.id}`, { method: "DELETE" });
      setError(null);
      fetchData();
    } catch (err) {
      setError("Failed to delete section: " + (err.message || "Unknown error"));
    }
  };

  const openMoveModal = (enrollment, section, subject) => {
    const availableSections = subjectsWithSections.filter(s=>['IT332','IT411'].includes(s.code)).flatMap(s=>(s.sections||[]).map(c=>({...c,subjectLabel:`${s.code} — ${s.name}`})))
      .filter((s) => s.id !== section.id && !s.semesterArchived)
      .sort((a, b) =>
        a.section.localeCompare(b.section, undefined, {
          numeric: true,
          sensitivity: "base",
        }),
      );
    setMoveModalData({
      enrollment,
      currentSectionId: section.id,
      availableSections,
    });
    setMoveTargetSectionId("");
    setMoveError(null);
  };

  const closeMoveModal = () => {
    setMoveModalData(null);
    setMoveError(null);
  };

  const handleMoveStudent = async () => {
    if (!moveModalData || !moveTargetSectionId) return;
    setMoveSaving(true);
    setMoveError(null);
    try {
      await apiService.moveStudent(
        moveModalData.enrollment.id,
        moveTargetSectionId,
      );
      closeMoveModal();
      fetchData();
    } catch (err) {
      setMoveError(err.message || "Failed to move student");
    } finally {
      setMoveSaving(false);
    }
  };

  const toggleSubject = (subjectId) => {
    setExpandedSubjects((prev) => ({
      ...prev,
      [subjectId]: !prev[subjectId],
    }));
  };

  const toggleSection = (sectionId) => {
    setExpandedSections((prev) => ({
      ...prev,
      [sectionId]: !prev[sectionId],
    }));
  };

  if (loading) return <div>Loading...</div>;

  // Modal overlay style
  const overlayStyle = {
    position: "fixed",
    inset: 0,
    backgroundColor: "rgba(0,0,0,0.4)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 1000,
  };
  const modalStyle = {
    background: "#fff",
    borderRadius: "0.75rem",
    padding: "1.5rem",
    width: "100%",
    maxWidth: 440,
    boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
  };

  return (
    <div className="fade-in coordinator-management coordinator-classlist">
      {/* Unenroll Confirmation Modal */}
      {unenrollTarget && (
        <div style={overlayStyle}>
          <div style={modalStyle}>
            <h2 style={{ margin: "0 0 0.75rem 0", fontSize: "1.125rem" }}>
              Confirm Unenrollment
            </h2>
            <p style={{ margin: "0 0 1.5rem 0", color: "#374151" }}>
              Permanently remove <strong>{unenrollTarget.fullName}</strong> (
              {unenrollTarget.studentId}) from this classlist? Any current group assignment will be removed too. Enrollments with submission or progression history are kept so records remain intact.
            </p>
            <div
              style={{
                display: "flex",
                gap: "0.75rem",
                justifyContent: "flex-end",
              }}
            >
              <button
                className="btn btn-secondary"
                onClick={() => setUnenrollTarget(null)}
                disabled={unenrollSaving}
              >
                Cancel
              </button>
              <button
                className="btn btn-danger"
                onClick={handleUnenroll}
                disabled={unenrollSaving}
              >
                {unenrollSaving ? "Unenrolling…" : "Unenroll"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Move Student Modal */}
      {moveModalData && (
        <div style={overlayStyle}>
          <div style={modalStyle}>
            <h2 style={{ margin: "0 0 0.25rem 0", fontSize: "1.125rem" }}>
              Correct student enrollment
            </h2>
            <p
              style={{
                margin: "0 0 1rem 0",
                color: "#6b7280",
                fontSize: "0.875rem",
              }}
            >
              Moving <strong>{moveModalData.enrollment.fullName}</strong> (
              {moveModalData.enrollment.studentId})
            </p>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: 500,
                fontSize: "0.875rem",
              }}
            >
              Target subject, section and term
            </label>
            <select
              className="form-select"
              style={{ width: "100%", marginBottom: "1rem" }}
              value={moveTargetSectionId}
              onChange={(e) => setMoveTargetSectionId(e.target.value)}
            >
              <option value="">Select a section…</option>
              {moveModalData.availableSections.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.subjectLabel} · {s.section} · {s.academicTerm || 'Unassigned term'}
                </option>
              ))}
            </select>
            <p className="management-help">Choose any active section. The old enrollment is replaced. Existing group or progression history prevents a subject/term change; add a separate enrollment in that case.</p>
            {moveError && (
              <p
                style={{
                  color: "#dc2626",
                  fontSize: "0.875rem",
                  marginBottom: "0.75rem",
                }}
              >
                {moveError}
              </p>
            )}
            <div
              style={{
                display: "flex",
                gap: "0.75rem",
                justifyContent: "flex-end",
              }}
            >
              <button
                className="btn btn-secondary"
                onClick={closeMoveModal}
                disabled={moveSaving}
              >
                Cancel
              </button>
              <button
                className="btn btn-primary"
                onClick={handleMoveStudent}
                disabled={!moveTargetSectionId || moveSaving}
              >
                {moveSaving ? "Moving…" : "Move Student"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Student Search Modal */}
      {addingToSectionId !== null && (
        <div style={overlayStyle}>
          <div style={{ ...modalStyle, maxWidth: 500 }}>
            <h2 style={{ margin: "0 0 0.25rem 0", fontSize: "1.25rem" }}>
              Add Late Enrollee
            </h2>
            <p
              style={{
                margin: "0 0 1rem 0",
                color: "#6b7280",
                fontSize: "0.875rem",
              }}
            >
              Search for a student account to enroll into this section.
            </p>

            {/* Search bar */}
            <form
              onSubmit={handleSearch}
              style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}
            >
              <input
                className="form-input"
                style={{ flex: 1 }}
                placeholder="Search by name, student ID, or email…"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setSelectedStudent(null);
                }}
                autoFocus
              />
              <button
                type="submit"
                className="btn btn-primary"
                disabled={searchLoading || !searchQuery.trim()}
              >
                {searchLoading ? "Searching…" : "Search"}
              </button>
            </form>

            {/* Results list */}
            {searchResults.length > 0 && (
              <div
                style={{
                  border: "1px solid #e5e7eb",
                  borderRadius: "0.5rem",
                  maxHeight: 220,
                  overflowY: "auto",
                  marginBottom: "1rem",
                }}
              >
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
                        borderLeft: isSelected
                          ? "3px solid #3b82f6"
                          : "3px solid transparent",
                        transition: "background 0.15s",
                      }}
                    >
                      <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                        {u.lastName
                          ? `${u.lastName}, ${u.firstName}`
                          : u.firstName}
                      </div>
                      <div style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                        {u.studentId && (
                          <span style={{ marginRight: "0.75rem" }}>
                            {u.studentId}
                          </span>
                        )}
                        {u.email}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {searchResults.length === 0 && !searchLoading && searchQuery && (
              <p
                style={{
                  color: "#6b7280",
                  fontSize: "0.875rem",
                  marginBottom: "1rem",
                }}
              >
                No student accounts found.
              </p>
            )}

            {addError && (
              <p
                style={{
                  color: "#dc2626",
                  fontSize: "0.875rem",
                  marginBottom: "0.75rem",
                }}
              >
                {addError}
              </p>
            )}

            {/* Selected preview */}
            {selectedStudent && (
              <div
                style={{
                  background: "#f0fdf4",
                  border: "1px solid #bbf7d0",
                  borderRadius: "0.5rem",
                  padding: "0.625rem 0.875rem",
                  marginBottom: "1rem",
                  fontSize: "0.875rem",
                }}
              >
                <strong>Selected:</strong>{" "}
                {selectedStudent.lastName
                  ? `${selectedStudent.lastName}, ${selectedStudent.firstName}`
                  : selectedStudent.firstName}
                {selectedStudent.studentId && ` — ${selectedStudent.studentId}`}
              </div>
            )}

            <div
              style={{
                display: "flex",
                gap: "0.75rem",
                justifyContent: "flex-end",
              }}
            >
              <button
                className="btn btn-secondary"
                onClick={closeAddModal}
                disabled={addSaving}
              >
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
        <h1 className="page-title">Students & sections</h1>
        <p className="page-description">
          View imported classlists grouped by subject and section.
        </p>
      </div>

      <AwaitingEnrollment subjects={subjectsWithSections} onChanged={fetchData}/>
      <div className="tracking-filters"><label>School year<select value={schoolYear} onChange={e=>setSchoolYear(e.target.value)}><option value="">All years</option>{years.map(y=><option key={y}>{y}</option>)}</select></label><label>Semester<select value={semester} onChange={e=>setSemester(e.target.value)}><option value="">All semesters</option><option value="1">First semester</option><option value="2">Second semester</option></select></label></div>
      <p className="management-help">Showing active classlists. <Link to="/coordinator/setup?tab=semesters">View semester history</Link> or <Link to="/coordinator/classlist">import a new classlist</Link>.</p>
      {!visibleSubjects.length&&subjectsWithSections.length>0&&<p>No sections match these filters.</p>}
      {error && (
        <div style={{ marginBottom: "1rem", color: "#b91c1c" }}>{error}</div>
      )}

      <div>
        {subjectsWithSections.length === 0 ? (
          <div className="card">
            <div
              className="card-content"
              style={{ textAlign: "center", padding: "3rem" }}
            >
              <FileText
                size={48}
                style={{ color: "#9ca3af", marginBottom: "1rem" }}
              />
              <h3 style={{ margin: "0 0 0.5rem 0" }}>Set up your first classlist</h3>
              <p style={{ color: "#6b7280", margin: 0 }}>
                Use the Classlist Import page to upload student classlists.
              </p>
              <div className="tracking-actions" style={{justifyContent:'center'}}><Link className="btn btn-primary" to="/coordinator/classlist">Import a classlist</Link><Link className="btn btn-secondary" to="/coordinator/setup?tab=subjects">Create subjects & sections</Link></div>
            </div>
          </div>
        ) : (
          visibleSubjects.map((subject) => (
            <div
              key={subject.id}
              className="card"
              style={{ marginBottom: "1rem" }}
            >
              <div
                className="card-header"
                style={{ cursor: "pointer", userSelect: "none" }}
                onClick={() => toggleSubject(subject.id)}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
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
                  {subject.sections
                    ?.slice()
                    .sort((a, b) =>
                      a.section.localeCompare(b.section, undefined, {
                        numeric: true,
                        sensitivity: "base",
                      }),
                    )
                    .map((section) => (
                      <div
                        key={section.id}
                        style={{
                          borderTop: "1px solid #e5e7eb",
                          paddingTop: "1rem",
                          marginTop: "1rem",
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
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: "0.5rem",
                            }}
                          >
                            {expandedSections[section.id] ? (
                              <ChevronDown size={16} />
                            ) : (
                              <ChevronRight size={16} />
                            )}
                            <div>
                              <h3 style={{ margin: 0, fontSize: "1.125rem" }}>
                                Section: {section.section}
                                {['IT332','IT411'].includes(subject.code)&&section.academicTerm&&<small style={{marginLeft:12}}>{section.academicTerm.endsWith(subject.code==='IT332'?' / 2':' / 1')?'On-semester':'Off-semester'}</small>}
                              </h3>
                              {['IT332','IT411'].includes(subject.code)&&section.academicTerm&&!new RegExp(section.academicTerm.endsWith(subject.code==='IT332'?' / 2':' / 1')?'^G[1-9][0-9]*$':'^G0[1-9][0-9]*$','i').test((section.section||'').trim())&&<small className="section-mismatch">This section code does not match the semester convention. Move students to the correct section; G02 and G2 stay separate.</small>}
                            </div>
                          </div>
                          <div
                            style={{
                              display: "flex",
                              alignItems: "center",
                              gap: "0.75rem",
                            }}
                            onClick={(e) => e.stopPropagation()}
                          >
                            <span className="badge info">
                              {section.enrollments?.length || 0} Students
                            </span>
                            <button
                              className="btn btn-primary btn-sm"
                              title="Add late enrollee"
                              onClick={() => openAddModal(section.id)}
                            >
                              <UserPlus
                                size={14}
                                style={{ marginRight: "0.25rem" }}
                              />
                              Add Student
                            </button>
                            {!(section.enrollments?.length) && !section.semesterArchived && (
                              <button className="btn btn-danger btn-sm" title="Delete empty section" onClick={() => handleDeleteEmptySection(section)}>
                                <Trash2 size={14} style={{ marginRight: "0.25rem" }} /> Delete empty section
                              </button>
                            )}
                          </div>
                        </div>
                        {expandedSections[section.id] && (
                          <div style={{ marginTop: "1rem" }}>
                            <div className="table-container">
                              <table
                                className="data-table"
                                style={{
                                  tableLayout: "fixed",
                                  width: "100%",
                                  minWidth: "760px",
                                }}
                              >
                                <thead>
                                  <tr>
                                    <th style={{ width: "130px" }}>
                                      Student ID
                                    </th>
                                    <th style={{ width: "20%" }}>Full Name</th>
                                    <th style={{ width: "25%" }}>Email</th>
                                    <th style={{ width: "140px" }}>
                                      Account status
                                    </th>
                                    <th style={{ width: "260px" }}>Actions</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {section.enrollments?.map((enrollment) => (
                                    <tr key={enrollment.id}>
                                      <td
                                        style={{
                                          overflow: "hidden",
                                          textOverflow: "ellipsis",
                                          whiteSpace: "nowrap",
                                        }}
                                      >
                                        {enrollment.studentId}
                                      </td>
                                      <td
                                        style={{
                                          overflow: "hidden",
                                          textOverflow: "ellipsis",
                                          whiteSpace: "nowrap",
                                        }}
                                      >
                                        {enrollment.fullName}
                                      </td>
                                      <td
                                        style={{
                                          overflow: "hidden",
                                          textOverflow: "ellipsis",
                                          whiteSpace: "nowrap",
                                        }}
                                      >
                                        {enrollment.email}
                                      </td>
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
                                        <div
                                          style={{
                                            display: "flex",
                                            gap: "0.375rem",
                                            alignItems: "center",
                                            flexWrap: "wrap",
                                          }}
                                        >
                                          {/* Move button — only show when there are other sections */}
                                          {subjectsWithSections.some(s=>(s.sections||[]).some(c=>!c.semesterArchived && c.id!==section.id)) && (
                                              <button
                                                className="btn btn-secondary btn-sm"
                                                title="Move to another section"
                                                onClick={() =>
                                                  openMoveModal(
                                                    enrollment,
                                                    section,
                                                    subject,
                                                  )
                                                }
                                              >
                                                <ArrowRightLeft
                                                  size={13}
                                                  style={{
                                                    marginRight: "0.2rem",
                                                  }}
                                                />
                                                Move
                                              </button>
                                            )}
                                          {/* Unenroll button */}
                                          <button
                                            className="btn btn-danger btn-sm"
                                            title="Remove from classlist"
                                            onClick={() =>
                                              setUnenrollTarget(enrollment)
                                            }
                                          >
                                            <Trash2
                                              size={13}
                                              style={{ marginRight: "0.2rem" }}
                                            />
                                            Unenroll
                                          </button>
                                          {/* Link Account */}
                                          {!enrollment.userId &&
                                            (linkingEnrollmentId ===
                                            enrollment.id ? (
                                              <div
                                                style={{
                                                  display: "flex",
                                                  gap: "0.5rem",
                                                  alignItems: "center",
                                                  flexWrap: "wrap",
                                                }}
                                              >
                                                <select
                                                  className="form-select"
                                                  style={{
                                                    width: "auto",
                                                    minWidth: 200,
                                                  }}
                                                  value={selectedUserId || ""}
                                                  onChange={(e) =>
                                                    setSelectedUserId(
                                                      e.target.value
                                                        ? Number(e.target.value)
                                                        : null,
                                                    )
                                                  }
                                                >
                                                  <option value="">
                                                    Select user...
                                                  </option>
                                                  {users
                                                    .filter(
                                                      (u) =>
                                                        u.role === "student",
                                                    )
                                                    .map((u) => (
                                                      <option
                                                        key={u.id}
                                                        value={u.id}
                                                      >
                                                        {u.firstName}{" "}
                                                        {u.lastName} ({u.email})
                                                      </option>
                                                    ))}
                                                </select>
                                                <button
                                                  className="btn btn-primary btn-sm"
                                                  onClick={() =>
                                                    handleManualLink(
                                                      enrollment.id,
                                                    )
                                                  }
                                                  disabled={
                                                    !selectedUserId || saving
                                                  }
                                                >
                                                  {saving
                                                    ? "Saving..."
                                                    : "Save"}
                                                </button>
                                                <button
                                                  className="btn btn-secondary btn-sm"
                                                  onClick={() => {
                                                    setLinkingEnrollmentId(
                                                      null,
                                                    );
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
                                                onClick={() =>
                                                  setLinkingEnrollmentId(
                                                    enrollment.id,
                                                  )
                                                }
                                              >
                                                <Link2 size={14} /> Link Account
                                              </button>
                                            ))}
                                        </div>
                                      </td>
                                    </tr>
                                  ))}
                                  {(!section.enrollments ||
                                    section.enrollments.length === 0) && (
                                    <tr>
                                      <td
                                        colSpan="5"
                                        style={{
                                          textAlign: "center",
                                          padding: "2rem",
                                          color: "#6b7280",
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
