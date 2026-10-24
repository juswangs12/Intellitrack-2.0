import React, { useState, useEffect, useCallback, useRef } from "react";
import { Plus, Edit2, Trash2, Search, X, Save } from "lucide-react";
import apiService from "../../services/ApiService";

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    role: "student",
    department: "",
  });
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

  const requestVersion = useRef(0);
  const dialogRef = useRef(null);
  useEffect(() => {
    if (!showModal) return;
    const previous = document.activeElement;
    const dialog = dialogRef.current;
    const controls = () => [...dialog.querySelectorAll('button:not(:disabled),input:not(:disabled),select:not(:disabled),[tabindex="0"]')];
    controls()[0]?.focus();
    const onKey = event => {
      if (event.key === 'Escape' && !saving) setShowModal(false);
      if (event.key === 'Tab') {
        const items = controls(), first = items[0], last = items[items.length - 1];
        if (event.shiftKey && document.activeElement === first) {event.preventDefault();last?.focus();}
        else if (!event.shiftKey && document.activeElement === last) {event.preventDefault();first?.focus();}
      }
    };
    dialog.addEventListener('keydown', onKey);
    return () => {dialog.removeEventListener('keydown', onKey);previous?.focus();};
  }, [showModal, saving]);
  const fetchUsers = useCallback(async () => {
    const version = ++requestVersion.current;
    setLoading(true);
    setError("");
    try {
      const query = roleFilter ? `?role=${roleFilter}` : "";
      const data = await apiService.requestJson(`/users${query}`, {
        method: "GET",
      });
      if (version === requestVersion.current) setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      if (version === requestVersion.current) setError("Failed to load users. Make sure the backend is running.");
    } finally {
      if (version === requestVersion.current) setLoading(false);
    }
  }, [roleFilter]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const openCreateModal = () => {
    setEditingUser(null);
    setFormData({
      firstName: "",
      lastName: "",
      email: "",
      password: "",
      role: "student",
      department: "",
    });
    setFormError("");
    setShowModal(true);
  };

  const openEditModal = (user) => {
    setEditingUser(user);
    setFormData({
      firstName: user.firstName || "",
      lastName: user.lastName || "",
      email: user.email || "",
      password: "",
      role: user.role || "student",
      department: user.department || "",
    });
    setFormError("");
    setShowModal(true);
  };

  const handleSave = async () => {
    if (!formData.firstName || !formData.lastName || !formData.email) {
      setFormError("First name, last name, and email are required.");
      return;
    }
    if (!editingUser && !formData.password) {
      setFormError("Password is required for new users.");
      return;
    }
    setSaving(true);
    setFormError("");
    try {
      if (editingUser) {
        const payload = {
          firstName: formData.firstName,
          lastName: formData.lastName,
          department: formData.department,
          role: formData.role,
        };
        await apiService.requestJson(`/users/${editingUser.id}/account`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      } else {
        const payload = {
          firstName: formData.firstName,
          lastName: formData.lastName,
          email: formData.email,
          password: formData.password,
          role: formData.role,
          department: formData.department,
        };
        await apiService.requestJson(`/users`, {
          method: "POST",
          body: JSON.stringify(payload),
        });
      }
      setShowModal(false);
      fetchUsers();
    } catch (err) {
      setFormError(
        err.message || "Failed to save user. Please check the details and try again.",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (user) => {
    if (
      !window.confirm(
        `Delete user "${user.firstName} ${user.lastName}"? This action cannot be undone.`,
      )
    )
      return;
    try {
      await apiService.requestJson(`/users/${user.id}`, {
        method: "DELETE",
      });
      fetchUsers();
    } catch (err) {
      setError(err.message || "Failed to delete user.");
    }
  };

  const filteredUsers = users.filter((u) => {
    const q = searchQuery.trim().toLowerCase();
    return (
      !q ||
      u.email?.toLowerCase().includes(q) ||
      `${u.firstName || ""} ${u.lastName || ""}`.toLowerCase().includes(q) ||
      u.lastName?.toLowerCase().includes(q)
    );
  });

  const getRoleBadge = (role) =>
    ({
      student: "info",
      adviser: "success",
      coordinator: "warning",
      administrator: "danger",
    })[role] || "info";

  return (
    <div>
      <div
        className="page-header"
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
        }}
      >
        <div>
          <h1 className="page-title">User Management</h1>
          <p className="page-description">
            Create, view, update, and delete system users.
          </p>
        </div>
        <button className="btn btn-primary" onClick={openCreateModal}>
          <Plus style={{ width: "1rem", height: "1rem" }} /> Add User
        </button>
      </div>

      <div className="card">
        <div
          style={{
            display: "flex",
            gap: "1rem",
            marginBottom: "1.5rem",
            alignItems: "center",
            flexWrap: "wrap",
          }}
        >
          <div style={{ position: "relative", flex: 1, minWidth: "200px" }}>
            <Search
              style={{
                position: "absolute",
                left: "0.75rem",
                top: "50%",
                transform: "translateY(-50%)",
                width: "1rem",
                height: "1rem",
                color: "#9ca3af",
              }}
            />
            <input
              className="form-input"
              style={{ paddingLeft: "2.5rem" }}
              placeholder="Search by name or email..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <select
            className="form-select"
            style={{ width: "auto", minWidth: "160px" }}
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
          >
            <option value="">All Roles</option>
            <option value="student">Students</option>
            <option value="adviser">Advisers</option>
            <option value="coordinator">Coordinators</option>
            <option value="administrator">Administrators</option>
          </select>
          <button className="btn btn-secondary" onClick={fetchUsers}>
            Refresh
          </button>
        </div>

        {loading ? (
          <div
            style={{ textAlign: "center", padding: "3rem", color: "#6b7280" }}
          >
            Loading users...
          </div>
        ) : error ? (
          <div className="error-message">{error}</div>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Department</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5}
                      style={{
                        textAlign: "center",
                        color: "#6b7280",
                        padding: "2rem",
                      }}
                    >
                      No users found
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map((u) => (
                    <tr key={u.id}>
                      <td>
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.75rem",
                          }}
                        >
                          <div
                            style={{
                              width: "2rem",
                              height: "2rem",
                              borderRadius: "50%",
                              background:
                                "linear-gradient(135deg, var(--maroon), var(--maroon-light))",
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "center",
                              color: "white",
                              fontSize: "0.75rem",
                              fontWeight: "600",
                              flexShrink: 0,
                            }}
                          >
                            {(
                              u.firstName?.[0] ||
                              u.email?.[0] ||
                              "?"
                            ).toUpperCase()}
                          </div>
                          <span style={{ fontWeight: "500" }}>
                            {u.firstName} {u.lastName}
                          </span>
                        </div>
                      </td>
                      <td style={{ color: "#6b7280" }}>{u.email}</td>
                      <td>
                        <span className={`badge ${getRoleBadge(u.role)}`}>
                          {u.role}
                        </span>
                      </td>
                      <td style={{ color: "#6b7280" }}>
                        {u.department || "—"}
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: "0.5rem" }}>
                          <button
                            className="btn btn-secondary"
                            style={{ padding: "0.25rem 0.5rem" }}
                            aria-label={`Edit ${u.firstName} ${u.lastName}`} title="Edit account"
                            onClick={() => openEditModal(u)}
                          >
                            <Edit2
                              style={{ width: "0.875rem", height: "0.875rem" }}
                            />
                          </button>
                          <button
                            className="btn btn-danger"
                            style={{ padding: "0.25rem 0.5rem" }}
                            aria-label={`Delete ${u.firstName} ${u.lastName}`} title="Delete account"
                            onClick={() => handleDelete(u)}
                          >
                            <Trash2
                              style={{ width: "0.875rem", height: "0.875rem" }}
                            />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
        {!loading && !error && (
          <p
            style={{
              color: "#9ca3af",
              fontSize: "0.75rem",
              marginTop: "1rem",
              textAlign: "right",
            }}
          >
            {filteredUsers.length} of {users.length} user
            {users.length !== 1 ? "s" : ""}
          </p>
        )}
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal" ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby="account-dialog-title">
            <div className="modal-header">
              <h2 id="account-dialog-title" className="modal-title">
                {editingUser ? "Edit User" : "Add New User"}
              </h2>
              <button
                className="modal-close" aria-label="Close account editor" disabled={saving}
                onClick={() => setShowModal(false)}
              >
                <X style={{ width: "1.25rem", height: "1.25rem" }} />
              </button>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="account-firstName">First Name *</label>
                <input
                  className="form-input"
                  id="account-firstName" value={formData.firstName}
                  onChange={(e) =>
                    setFormData({ ...formData, firstName: e.target.value })
                  }
                  placeholder="John"
                />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="account-lastName">Last Name *</label>
                <input
                  className="form-input"
                  id="account-lastName" value={formData.lastName}
                  onChange={(e) =>
                    setFormData({ ...formData, lastName: e.target.value })
                  }
                  placeholder="Doe"
                />
              </div>
            </div>
            <div className="form-group" style={{ marginBottom: "1rem" }}>
              <label className="form-label" htmlFor="account-email">Email *</label>
              <input
                className="form-input"
                type="email"
                id="account-email" value={formData.email}
                disabled={!!editingUser}
                onChange={(e) =>
                  setFormData({ ...formData, email: e.target.value })
                }
                placeholder="user@university.edu"
              />
              {!editingUser && <p className="tracking-help">
                For Google sign-in, enter the person's exact Google email and select
                their assigned role below. Their first Google sign-in will use this
                account and open that role's dashboard. The password below is for
                IntelliTrack email sign-in, not their Google password.
              </p>}
            </div>
            {!editingUser && (
              <div className="form-group" style={{ marginBottom: "1rem" }}>
                <label className="form-label" htmlFor="account-password">Password *</label>
                <input
                  className="form-input"
                  type="password"
                  id="account-password" value={formData.password}
                  onChange={(e) =>
                    setFormData({ ...formData, password: e.target.value })
                  }
                  placeholder="••••••••"
                />
              </div>
            )}
            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="account-role">Role</label>
                <select
                  className="form-select"
                  id="account-role" value={formData.role}
                  onChange={(e) =>
                    setFormData({ ...formData, role: e.target.value })
                  }
                >
                  <option value="student">Student</option>
                  <option value="adviser">Adviser</option>
                  <option value="coordinator">Coordinator</option>
                  <option value="administrator">Administrator</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="account-department">Department</label>
                <input
                  className="form-input"
                  id="account-department" value={formData.department}
                  onChange={(e) =>
                    setFormData({ ...formData, department: e.target.value })
                  }
                  placeholder="e.g. Computer Science"
                />
              </div>
            </div>
            {formError && (
              <p className="error-message" style={{ marginBottom: "1rem" }}>
                {formError}
              </p>
            )}
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => setShowModal(false)}
              >
                Cancel
              </button>
              <button
                className="btn btn-primary"
                onClick={handleSave}
                disabled={saving}
              >
                <Save style={{ width: "1rem", height: "1rem" }} />{" "}
                {saving
                  ? "Saving..."
                  : editingUser
                    ? "Save Changes"
                    : "Create User"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default UserManagement;
