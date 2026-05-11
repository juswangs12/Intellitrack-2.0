import React, { useState } from "react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const AdviserProfile = () => {
  const { user, updateProfile } = useAuth();
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    firstName: user?.firstName || "",
    lastName: user?.lastName || "",
    email: user?.email || "",
    department: user?.department || "",
    phone: user?.phone || "",
  });

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSave = async () => {
    if (!user?.id) return;
    if (!form.firstName || !form.lastName) {
      setError("First name and last name are required.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      const updated = await apiService.requestJson(`/users/${user.id}/profile`, {
        method: "PUT",
        body: JSON.stringify({
          firstName: form.firstName,
          lastName: form.lastName,
          phone: form.phone,
          department: form.department,
          year: user?.year || "",
        }),
      });
      updateProfile(updated);
      setEditing(false);
    } catch (err) {
      setError("Failed to update profile. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">My Profile</h1>
        <p className="page-description">
          Manage your adviser account information.
        </p>
      </div>
      <div className="card" style={{ maxWidth: 640 }}>
        <div
          className="card-header"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <h2 className="card-title">Profile Details</h2>
          {!editing ? (
            <button
              className="btn btn-primary"
              onClick={() => setEditing(true)}
            >
              Edit
            </button>
          ) : (
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
                {saving ? "Saving..." : "Save"}
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setForm({
                    firstName: user?.firstName || "",
                    lastName: user?.lastName || "",
                    email: user?.email || "",
                    department: user?.department || "",
                    phone: user?.phone || "",
                  });
                  setError("");
                  setEditing(false);
                }}
              >
                Cancel
              </button>
            </div>
          )}
        </div>
        <div className="card-content">
          {error && <div className="error-message">{error}</div>}
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">First Name</label>
              <input
                className="form-input"
                name="firstName"
                value={form.firstName}
                onChange={handleChange}
                disabled={!editing}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Last Name</label>
              <input
                className="form-input"
                name="lastName"
                value={form.lastName}
                onChange={handleChange}
                disabled={!editing}
              />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Email</label>
            <input
              className="form-input"
              name="email"
              value={form.email}
              disabled
            />
          </div>
          <div className="form-group">
            <label className="form-label">Department</label>
            <input
              className="form-input"
              name="department"
              value={form.department}
              onChange={handleChange}
              disabled={!editing}
            />
          </div>
          <div className="form-group">
            <label className="form-label">Phone</label>
            <input
              className="form-input"
              name="phone"
              value={form.phone}
              onChange={handleChange}
              disabled={!editing}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdviserProfile;
