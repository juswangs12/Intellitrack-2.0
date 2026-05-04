import React, { useState, useEffect, useCallback } from "react";
import { Plus, Edit2, Trash2, Search, X, Save } from "lucide-react";
import { useAuth } from "../../context/AuthContext";

const BASE_URL = "http://localhost:8080/api";

const UserManagement = () => {
  const { token } = useAuth();
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

  const authHeaders = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const url = roleFilter
        ? `${BASE_URL}/users?role=${roleFilter}`
        : `${BASE_URL}/users`;
      const res = await fetch(url, { headers: authHeaders });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setUsers(data);
    } catch (err) {
      setError("Failed to load users. Make sure the backend is running.");
    } finally {
      setLoading(false);
    }
  }, [token, roleFilter]);

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
        const res = await fetch(`${BASE_URL}/users/${editingUser.id}`, {
          method: "PUT",
          headers: authHeaders,
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
      } else {
        const payload = {
          firstName: formData.firstName,
          lastName: formData.lastName,
          email: formData.email,
          password: formData.password,
          role: formData.role,
          department: formData.department,
        };
        const res = await fetch(`${BASE_URL}/users`, {
          method: "POST",
          headers: authHeaders,
          body: JSON.stringify(payload),
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
      }
      setShowModal(false);
      fetchUsers();
    } catch (err) {
      setFormError(
        "Failed to save user. Please check the details and try again.",
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
      const res = await fetch(`${BASE_URL}/users/${user.id}`, {
        method: "DELETE",
        headers: authHeaders,
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      fetchUsers();
    } catch (err) {
      alert("Failed to delete user. Please try again.");
    }
  };

  const filteredUsers = users.filter((u) => {
    const q = searchQuery.toLowerCase();
    return (
      !q ||
      u.email?.toLowerCase().includes(q) ||
      u.firstName?.toLowerCase().includes(q) ||
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
                            onClick={() => openEditModal(u)}
                          >
                            <Edit2
                              style={{ width: "0.875rem", height: "0.875rem" }}
                            />
                          </button>
                          <button
                            className="btn btn-danger"
                            style={{ padding: "0.25rem 0.5rem" }}
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
          <div className="modal">
            <div className="modal-header">
              <h2 className="modal-title">
                {editingUser ? "Edit User" : "Add New User"}
              </h2>
              <button
                className="modal-close"
                onClick={() => setShowModal(false)}
              >
                <X style={{ width: "1.25rem", height: "1.25rem" }} />
              </button>
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">First Name *</label>
                <input
                  className="form-input"
                  value={formData.firstName}
                  onChange={(e) =>
                    setFormData({ ...formData, firstName: e.target.value })
                  }
                  placeholder="John"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Last Name *</label>
                <input
                  className="form-input"
                  value={formData.lastName}
                  onChange={(e) =>
                    setFormData({ ...formData, lastName: e.target.value })
                  }
                  placeholder="Doe"
                />
              </div>
            </div>
            <div className="form-group" style={{ marginBottom: "1rem" }}>
              <label className="form-label">Email *</label>
              <input
                className="form-input"
                type="email"
                value={formData.email}
                disabled={!!editingUser}
                onChange={(e) =>
                  setFormData({ ...formData, email: e.target.value })
                }
                placeholder="user@university.edu"
              />
            </div>
            {!editingUser && (
              <div className="form-group" style={{ marginBottom: "1rem" }}>
                <label className="form-label">Password *</label>
                <input
                  className="form-input"
                  type="password"
                  value={formData.password}
                  onChange={(e) =>
                    setFormData({ ...formData, password: e.target.value })
                  }
                  placeholder="••••••••"
                />
              </div>
            )}
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Role</label>
                <select
                  className="form-select"
                  value={formData.role}
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
                <label className="form-label">Department</label>
                <input
                  className="form-input"
                  value={formData.department}
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
