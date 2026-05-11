import { useState, useEffect, useCallback } from "react";
import { Users, Plus, Edit, Trash2, CheckCircle2, User, UserMinus, Search, X, ChevronRight } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const CoordinatorGroupManagement = () => {
  const { user } = useAuth();
  const [groups, setGroups] = useState([]);
  const [users, setUsers] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [showGroupModal, setShowGroupModal] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [newGroup, setNewGroup] = useState({ code: "", title: "", adviserId: "" });
  const [assignSearch, setAssignSearch] = useState("");
  const [assignSectionFilter, setAssignSectionFilter] = useState("");
  const [selectedEnrollmentIds, setSelectedEnrollmentIds] = useState([]);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [groupsData, usersData, enrollmentsData] = await Promise.all([
        apiService.getGroups(),
        apiService.getAllUsers(),
        apiService.getAllStudentEnrollments(),
      ]);
      setGroups(Array.isArray(groupsData) ? groupsData : []);
      setUsers(Array.isArray(usersData) ? usersData : []);
      setEnrollments(Array.isArray(enrollmentsData) ? enrollmentsData : []);
    } catch (err) {
      console.error("Failed to fetch data", err);
      setError("Failed to load data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const advisers = users.filter((u) => u.role === "adviser");

  const getAssignedEnrollmentIds = (group) => {
    if (!group || !group.students) return [];
    return group.students.map(s => s.id);
  };

  const getStudentGroupInfo = (enrollmentId) => {
    for (const group of groups) {
      if (group.students?.some(s => s.id === enrollmentId)) {
        return group;
      }
    }
    return null;
  };

  const availableSections = [...new Set(enrollments.map(e => e.sectionName).filter(Boolean))];

  const filteredAvailableEnrollments = enrollments.filter(e => {
    const matchesSearch = !assignSearch || 
      (e.fullName?.toLowerCase().includes(assignSearch.toLowerCase()) ||
      e.studentId?.toLowerCase().includes(assignSearch.toLowerCase()) ||
      e.subjectName?.toLowerCase().includes(assignSearch.toLowerCase()));
    const matchesSection = !assignSectionFilter || e.sectionName === assignSectionFilter;
    const notAssignedToCurrent = selectedGroup ? 
      !getAssignedEnrollmentIds(selectedGroup).includes(e.id) : true;
    return matchesSearch && matchesSection && notAssignedToCurrent;
  });

  const handleCreateOrUpdateGroup = async () => {
    if (!newGroup.code || !newGroup.title) {
      setError("Please fill in all required fields");
      return;
    }

    try {
      setError(null);
      setSuccess(null);
      if (editingId) {
        await apiService.updateGroup(editingId, newGroup);
        if (newGroup.adviserId) {
          await apiService.assignAdviserToGroup(editingId, Number(newGroup.adviserId));
        }
        setSuccess("Group updated successfully!");
      } else {
        const createdGroup = await apiService.createGroup(newGroup);
        if (createdGroup && createdGroup.id && newGroup.adviserId) {
          await apiService.assignAdviserToGroup(createdGroup.id, Number(newGroup.adviserId));
        }
        setSuccess("Group created successfully!");
      }
      await fetchData();
      setShowGroupModal(false);
      setNewGroup({ code: "", title: "", adviserId: "" });
      setEditingId(null);
    } catch (err) {
      console.error("Failed to save group", err);
      setError(err.message || "Failed to save group.");
    }
  };

  const handleEditGroup = (group) => {
    setEditingId(group.id);
    setNewGroup({
      code: group.code,
      title: group.title,
      adviserId: group.adviser?.id ? String(group.adviser.id) : "",
    });
    setShowGroupModal(true);
  };

  const handleDeleteGroup = async (groupId) => {
    if (!window.confirm("Are you sure you want to delete this group?")) return;
    try {
      setError(null);
      setSuccess(null);
      await apiService.deleteGroup(groupId);
      setSuccess("Group deleted successfully!");
      await fetchData();
    } catch (err) {
      console.error("Failed to delete group", err);
      setError("Failed to delete group.");
    }
  };

  const handleOpenAssignModal = (group) => {
    setSelectedGroup(group);
    setSelectedEnrollmentIds([]);
    setAssignSearch("");
    setAssignSectionFilter("");
    setShowAssignModal(true);
  };

  const handleAddStudent = (enrollment) => {
    const existingGroup = getStudentGroupInfo(enrollment.id);
    if (existingGroup) {
      if (!window.confirm(`${enrollment.fullName} is already in group ${existingGroup.code}. Are you sure you want to try adding them?`)) {
        return;
      }
    }
    if (!selectedEnrollmentIds.includes(enrollment.id)) {
      setSelectedEnrollmentIds([...selectedEnrollmentIds, enrollment.id]);
    }
  };

  const handleRemoveStudent = (enrollmentId) => {
    setSelectedEnrollmentIds(selectedEnrollmentIds.filter(id => id !== enrollmentId));
  };

  const handleRemoveEnrollmentFromGroup = async (enrollmentId) => {
    if (!window.confirm("Are you sure you want to remove this student from the group?")) return;
    try {
      setError(null);
      setSuccess(null);
      await apiService.removeStudentsFromGroup(selectedGroup.id, [enrollmentId]);
      setSuccess("Student removed successfully!");
      await fetchData();
    } catch (err) {
      console.error("Failed to remove student", err);
      setError("Failed to remove student.");
    }
  };

  const handleSaveAssignments = async () => {
    try {
      setError(null);
      setSuccess(null);
      if (selectedEnrollmentIds.length > 0) {
        await apiService.assignStudentsToGroup(selectedGroup.id, selectedEnrollmentIds);
      }
      setSuccess("Students assigned successfully!");
      await fetchData();
      setShowAssignModal(false);
      setSelectedGroup(null);
      setSelectedEnrollmentIds([]);
    } catch (err) {
      console.error("Failed to assign students", err);
      setError(err.message || "Failed to assign students.");
    }
  };

  if (loading) return (
    <div className="fade-in" style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      minHeight: '400px',
      fontSize: '1.125rem',
      color: '#6b7280'
    }}>
      Loading group data...
    </div>
  );

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Group Management</h1>
        <p className="page-description">
          Create, edit, and manage student groups and adviser assignments.
        </p>
      </div>

      {error && (
        <div style={{ 
          marginBottom: "1rem", 
          padding: "0.75rem 1rem", 
          backgroundColor: "#fef2f2", 
          border: "1px solid #fecaca", 
          borderRadius: "0.375rem",
          color: "#b91c1c",
          display: "flex",
          alignItems: "center",
          gap: "0.5rem"
        }}>
          <X size={18} />
          {error}
        </div>
      )}

      {success && (
        <div style={{ 
          marginBottom: "1rem", 
          padding: "0.75rem 1rem", 
          backgroundColor: "#f0fdf4", 
          border: "1px solid #bbf7d0", 
          borderRadius: "0.375rem",
          color: "#166534",
          display: "flex",
          alignItems: "center",
          gap: "0.5rem"
        }}>
          <CheckCircle2 size={18} />
          {success}
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Groups ({groups.length})</h2>
          <button
            className="btn btn-primary"
            onClick={() => {
              setEditingId(null);
              setNewGroup({ code: "", title: "", adviserId: "" });
              setShowGroupModal(true);
            }}
          >
            <Plus size={16} /> New Group
          </button>
        </div>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: '12%' }}>Code</th>
                <th style={{ width: '25%' }}>Title</th>
                <th style={{ width: '20%' }}>Adviser</th>
                <th style={{ width: '28%' }}>Members</th>
                <th style={{ width: '15%' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {groups.map((group) => (
                <tr key={group.id}>
                  <td style={{ fontWeight: 600, color: '#1f2937' }}>{group.code}</td>
                  <td>{group.title}</td>
                  <td>
                    {group.adviser
                      ? `${group.adviser.firstName} ${group.adviser.lastName}`
                      : <span style={{ color: '#9ca3af' }}>Unassigned</span>}
                  </td>
                  <td>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                      {group.students?.map((student) => (
                        <span 
                          key={student.id}
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.375rem',
                            padding: '0.25rem 0.75rem',
                            backgroundColor: '#e0f2fe',
                            color: '#0369a1',
                            borderRadius: '9999px',
                            fontSize: '0.875rem'
                          }}
                        >
                          {student.fullName}
                        </span>
                      ))}
                      {(!group.students || group.students.length === 0) && (
                        <span style={{ color: '#9ca3af', fontSize: '0.875rem' }}>No members</span>
                      )}
                    </div>
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => handleOpenAssignModal(group)}
                        title="Assign Students"
                      >
                        <Users size={14} />
                      </button>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => handleEditGroup(group)}
                        title="Edit Group"
                      >
                        <Edit size={14} />
                      </button>
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => handleDeleteGroup(group.id)}
                        title="Delete Group"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {groups.length === 0 && (
                <tr>
                  <td
                    colSpan="5"
                    style={{ textAlign: "center", padding: "3rem", color: "#6b7280" }}
                  >
                    <Users size={48} style={{ margin: '0 auto 1rem', opacity: 0.3 }} />
                    <div>No groups created yet.</div>
                    <div style={{ fontSize: '0.875rem', marginTop: '0.5rem' }}>Click "New Group" to get started.</div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showGroupModal && (
        <div className="modal-overlay">
          <div className="modal" style={{ maxWidth: 500 }}>
            <div className="modal-header">
              <h2 className="modal-title">
                {editingId ? "Edit Group" : "Create New Group"}
              </h2>
              <button
                className="modal-close"
                onClick={() => {
                  setShowGroupModal(false);
                  setEditingId(null);
                  setNewGroup({ code: "", title: "", adviserId: "" });
                }}
              >
                <X size={20} />
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Group Code</label>
                <input
                  type="text"
                  className="form-input"
                  value={newGroup.code}
                  onChange={(e) => setNewGroup({ ...newGroup, code: e.target.value })}
                  placeholder="e.g., GRP-001"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Group Title</label>
                <input
                  type="text"
                  className="form-input"
                  value={newGroup.title}
                  onChange={(e) => setNewGroup({ ...newGroup, title: e.target.value })}
                  placeholder="e.g., Group Alpha - AI Analytics"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Assign Adviser</label>
                <select
                  className="form-select"
                  value={newGroup.adviserId}
                  onChange={(e) => setNewGroup({ ...newGroup, adviserId: e.target.value })}
                >
                  <option value="">Select an adviser</option>
                  {advisers.map((adviser) => (
                    <option key={adviser.id} value={String(adviser.id)}>
                      {adviser.firstName} {adviser.lastName}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setShowGroupModal(false);
                  setEditingId(null);
                  setNewGroup({ code: "", title: "", adviserId: "" });
                }}
              >
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleCreateOrUpdateGroup}>
                <CheckCircle2 size={16} /> {editingId ? "Update" : "Create"} Group
              </button>
            </div>
          </div>
        </div>
      )}

      {showAssignModal && selectedGroup && (
        <div className="modal-overlay">
          <div className="modal" style={{ maxWidth: 900, width: '95%' }}>
            <div className="modal-header">
              <h2 className="modal-title">
                Assign Students - {selectedGroup.code}
              </h2>
              <button
                className="modal-close"
                onClick={() => {
                  setShowAssignModal(false);
                  setSelectedGroup(null);
                  setSelectedEnrollmentIds([]);
                }}
              >
                <X size={20} />
              </button>
            </div>
            <div className="modal-body" style={{ padding: '1.5rem' }}>
              <div style={{ 
                display: 'flex', 
                gap: '1rem', 
                marginBottom: '1rem',
                flexWrap: 'wrap',
                alignItems: 'center'
              }}>
                <div style={{ flex: 1, minWidth: '200px' }}>
                  <div style={{ position: 'relative' }}>
                    <Search size={16} style={{ 
                      position: 'absolute', 
                      left: '0.75rem', 
                      top: '50%', 
                      transform: 'translateY(-50%)',
                      color: '#9ca3af'
                    }} />
                    <input
                      type="text"
                      placeholder="Search students..."
                      value={assignSearch}
                      onChange={(e) => setAssignSearch(e.target.value)}
                      style={{
                        width: '100%',
                        padding: '0.5rem 0.75rem 0.5rem 2.5rem',
                        border: '1px solid #d1d5db',
                        borderRadius: '0.375rem',
                        fontSize: '0.875rem'
                      }}
                    />
                  </div>
                </div>
                <select
                  value={assignSectionFilter}
                  onChange={(e) => setAssignSectionFilter(e.target.value)}
                  style={{
                    padding: '0.5rem 0.75rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.375rem',
                    fontSize: '0.875rem',
                    minWidth: '150px'
                  }}
                >
                  <option value="">All Sections</option>
                  {availableSections.map(section => (
                    <option key={section} value={section}>{section}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
                <div>
                  <h3 style={{ 
                    fontSize: '1rem', 
                    fontWeight: 600, 
                    marginBottom: '0.75rem',
                    color: '#374151'
                  }}>
                    Available Students
                  </h3>
                  <div style={{ 
                    border: '1px solid #e5e7eb', 
                    borderRadius: '0.5rem',
                    maxHeight: '400px',
                    overflowY: 'auto',
                    backgroundColor: '#f9fafb'
                  }}>
                    {filteredAvailableEnrollments.length === 0 ? (
                      <div style={{ 
                        padding: '2rem', 
                        textAlign: 'center', 
                        color: '#9ca3af',
                        fontSize: '0.875rem'
                      }}>
                        No available students found
                      </div>
                    ) : (
                      filteredAvailableEnrollments.map(enrollment => {
                        const existingGroup = getStudentGroupInfo(enrollment.id);
                        return (
                          <div
                            key={enrollment.id}
                            style={{
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center',
                              padding: '0.75rem 1rem',
                              borderBottom: '1px solid #e5e7eb',
                              backgroundColor: existingGroup ? '#fef3c7' : 'white',
                              cursor: 'pointer',
                              transition: 'background-color 0.15s'
                            }}
                            onMouseEnter={(e) => e.currentTarget.style.backgroundColor = existingGroup ? '#fde68a' : '#f3f4f6'}
                            onMouseLeave={(e) => e.currentTarget.style.backgroundColor = existingGroup ? '#fef3c7' : 'white'}
                            onClick={() => handleAddStudent(enrollment)}
                          >
                            <div style={{ flex: 1 }}>
                              <div style={{ fontWeight: 500, color: existingGroup ? '#92400e' : '#1f2937' }}>
                                {enrollment.fullName}
                              </div>
                              <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                                {enrollment.studentId} • {enrollment.subjectCode} • {enrollment.sectionName}
                              </div>
                              {existingGroup && (
                                <div style={{ fontSize: '0.7rem', color: '#b45309', marginTop: '0.25rem' }}>
                                  Already in group: {existingGroup.code}
                                </div>
                              )}
                            </div>
                            <ChevronRight size={16} style={{ color: '#9ca3af' }} />
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>

                <div>
                  <h3 style={{ 
                    fontSize: '1rem', 
                    fontWeight: 600, 
                    marginBottom: '0.75rem',
                    color: '#374151'
                  }}>
                    Assigned Students ({getAssignedEnrollmentIds(selectedGroup).length + selectedEnrollmentIds.length})
                  </h3>
                  <div style={{ 
                    border: '1px solid #e5e7eb', 
                    borderRadius: '0.5rem',
                    maxHeight: '400px',
                    overflowY: 'auto',
                    backgroundColor: '#f9fafb'
                  }}>
                    {selectedGroup.students?.map(student => (
                      <div
                        key={student.id}
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          padding: '0.75rem 1rem',
                          borderBottom: '1px solid #e5e7eb',
                          backgroundColor: '#eff6ff'
                        }}
                      >
                        <div>
                          <div style={{ fontWeight: 500, color: '#1e40af' }}>
                            {student.fullName}
                          </div>
                          <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                            {student.studentId} • {student.subjectCode} • {student.sectionName}
                          </div>
                          <div style={{ fontSize: '0.7rem', color: student.userId ? '#166534' : '#92400e', marginTop: '0.25rem' }}>
                            {student.userId ? '✓ Linked to account' : '⚠ Not linked'}
                          </div>
                        </div>
                        <button
                          onClick={() => handleRemoveEnrollmentFromGroup(student.id)}
                          style={{
                            padding: '0.25rem 0.5rem',
                            border: 'none',
                            backgroundColor: 'transparent',
                            color: '#dc2626',
                            cursor: 'pointer',
                            borderRadius: '0.25rem'
                          }}
                          title="Remove"
                        >
                          <UserMinus size={16} />
                        </button>
                      </div>
                    ))}
                    {selectedEnrollmentIds.map(enrollmentId => {
                      const enrollment = enrollments.find(e => e.id === enrollmentId);
                      if (!enrollment) return null;
                      return (
                        <div
                          key={`new-${enrollmentId}`}
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            padding: '0.75rem 1rem',
                            borderBottom: '1px solid #e5e7eb',
                            backgroundColor: '#dcfce7'
                          }}
                        >
                          <div>
                            <div style={{ fontWeight: 500, color: '#166534' }}>
                              {enrollment.fullName}
                            </div>
                            <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                              {enrollment.studentId} • {enrollment.subjectCode} • {enrollment.sectionName}
                            </div>
                          </div>
                          <button
                            onClick={() => handleRemoveStudent(enrollmentId)}
                            style={{
                              padding: '0.25rem 0.5rem',
                              border: 'none',
                              backgroundColor: 'transparent',
                              color: '#dc2626',
                              cursor: 'pointer',
                              borderRadius: '0.25rem'
                            }}
                            title="Remove"
                          >
                            <X size={16} />
                          </button>
                        </div>
                      );
                    })}
                    {getAssignedEnrollmentIds(selectedGroup).length === 0 && selectedEnrollmentIds.length === 0 && (
                      <div style={{ 
                        padding: '2rem', 
                        textAlign: 'center', 
                        color: '#9ca3af',
                        fontSize: '0.875rem'
                      }}>
                        No students assigned yet
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setShowAssignModal(false);
                  setSelectedGroup(null);
                  setSelectedEnrollmentIds([]);
                }}
              >
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleSaveAssignments}>
                <CheckCircle2 size={16} /> Save Assignments
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CoordinatorGroupManagement;
