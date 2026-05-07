import React, { useState } from "react";
import { useAuth } from "../../context/AuthContext";

const AdviserProfile = () => {
  const { user } = useAuth();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({
    firstName: user?.firstName || "",
    lastName: user?.lastName || "",
    email: user?.email || "",
    department: user?.department || "",
    phone: user?.phone || "",
    specialization: user?.specialization || "",
  });

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSave = () => {
    // TODO: call PUT /api/users/{id}/profile
    setEditing(false);
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
              <button className="btn btn-primary" onClick={handleSave}>
                Save
              </button>
              <button
                className="btn btn-secondary"
                onClick={() => setEditing(false)}
              >
                Cancel
              </button>
            </div>
          )}
        </div>
        <div className="card-content">
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
            <label className="form-label">Specialization</label>
            <input
              className="form-input"
              name="specialization"
              value={form.specialization}
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
