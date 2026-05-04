import React, { useState } from "react";
import { Edit2, Save, X } from "lucide-react";
import { useAuth } from "../../context/AuthContext";

const CoordinatorProfile = () => {
  const { user } = useAuth();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({
    firstName: user?.firstName || "",
    lastName: user?.lastName || "",
    email: user?.email || "",
    department: user?.department || "",
    phone: user?.phone || "",
  });

  const handleSave = () => {
    // TODO: call PUT /api/users/{id}/profile
    setEditing(false);
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">My Profile</h1>
        <p className="page-description">
          Manage your coordinator account and personal information.
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 2fr",
          gap: "1.5rem",
        }}
      >
        <div className="card" style={{ textAlign: "center" }}>
          <div
            style={{
              width: "6rem",
              height: "6rem",
              borderRadius: "50%",
              background:
                "linear-gradient(135deg, var(--maroon), var(--maroon-light))",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              margin: "0 auto 1rem",
              fontSize: "2rem",
              fontWeight: "700",
              color: "white",
            }}
          >
            {(form.firstName?.[0] || "C").toUpperCase()}
          </div>
          <h2 style={{ margin: "0 0 0.25rem" }}>
            {form.firstName} {form.lastName}
          </h2>
          <p
            style={{
              color: "#6b7280",
              fontSize: "0.875rem",
              margin: "0 0 1rem",
            }}
          >
            Coordinator
          </p>
          <span className="badge maroon">Coordinator</span>
        </div>

        <div className="card">
          <div
            className="card-header"
            style={{ display: "flex", justifyContent: "space-between" }}
          >
            <h2 className="card-title">Personal Information</h2>
            {editing ? (
              <div style={{ display: "flex", gap: "0.5rem" }}>
                <button className="btn btn-primary" onClick={handleSave}>
                  <Save style={{ width: "1rem", height: "1rem" }} /> Save
                </button>
                <button
                  className="btn btn-secondary"
                  onClick={() => setEditing(false)}
                >
                  <X style={{ width: "1rem", height: "1rem" }} /> Cancel
                </button>
              </div>
            ) : (
              <button
                className="btn btn-secondary"
                onClick={() => setEditing(true)}
              >
                <Edit2 style={{ width: "1rem", height: "1rem" }} /> Edit
              </button>
            )}
          </div>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">First Name</label>
              <input
                className="form-input"
                value={form.firstName}
                disabled={!editing}
                onChange={(e) =>
                  setForm({ ...form, firstName: e.target.value })
                }
              />
            </div>
            <div className="form-group">
              <label className="form-label">Last Name</label>
              <input
                className="form-input"
                value={form.lastName}
                disabled={!editing}
                onChange={(e) => setForm({ ...form, lastName: e.target.value })}
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-group">
              <label className="form-label">Email</label>
              <input
                className="form-input"
                value={form.email}
                disabled
                type="email"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Department</label>
              <input
                className="form-input"
                value={form.department}
                disabled={!editing}
                onChange={(e) =>
                  setForm({ ...form, department: e.target.value })
                }
              />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Phone</label>
            <input
              className="form-input"
              value={form.phone}
              disabled={!editing}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default CoordinatorProfile;
