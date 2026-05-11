import React, { useEffect, useState } from "react";
import { Edit2, Save, X } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const StudentProfile = () => {
  const { user, updateProfile } = useAuth();
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [avatarFile, setAvatarFile] = useState(null);
  const [workspaceHeader, setWorkspaceHeader] = useState(null);
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [form, setForm] = useState({
    firstName: user?.firstName || "",
    lastName: user?.lastName || "",
    email: user?.email || "",
    studentId: user?.studentId || "",
    department: user?.department || "",
    year: user?.year || "",
    phone: user?.phone || "",
  });

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      try {
        if (!user?.id) return;
        const ws = await apiService.getStudentWorkspace(user.id);
        if (mounted) {
          setWorkspaceHeader(ws?.header || null);
        }
      } catch (err) {
        if (mounted) {
          setWorkspaceHeader(null);
        }
      }
    };
    load();
    return () => {
      mounted = false;
    };
  }, [user?.id]);

  const handleSave = async () => {
    if (!user?.id) return;
    if (!form.firstName || !form.lastName) {
      setError("First name and last name are required.");
      return;
    }

    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const updated = await apiService.requestJson(`/users/${user.id}/profile`, {
        method: "PUT",
        body: JSON.stringify({
          firstName: form.firstName,
          lastName: form.lastName,
          phone: form.phone,
          department: form.department,
          year: form.year,
        }),
      });
      updateProfile(updated);
      setEditing(false);
      setSuccess("Profile updated successfully.");
    } catch (err) {
      setError("Failed to update profile. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  const uploadAvatar = async () => {
    if (!user?.id || !avatarFile) return;
    if (avatarFile.size > 5 * 1024 * 1024) {
      setError("Avatar too large. Max file size is 5MB.");
      return;
    }

    setUploadingAvatar(true);
    setError("");
    setSuccess("");
    try {
      const formData = new FormData();
      formData.append("file", avatarFile);
      const updated = await apiService.requestJson(`/users/${user.id}/avatar`, {
        method: "POST",
        body: formData,
      });
      updateProfile(updated);
      setAvatarFile(null);
      setSuccess("Avatar updated successfully.");
    } catch (err) {
      setError("Failed to upload avatar.");
    } finally {
      setUploadingAvatar(false);
    }
  };

  const changePassword = async (e) => {
    e.preventDefault();
    if (!user?.id) return;

    if (!passwordForm.currentPassword || !passwordForm.newPassword) {
      setError("Please fill in your current and new password.");
      return;
    }
    if (passwordForm.newPassword.length < 8) {
      setError("New password must be at least 8 characters long.");
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setError("New passwords do not match.");
      return;
    }

    setChangingPassword(true);
    setError("");
    setSuccess("");
    try {
      await apiService.requestJson(`/users/${user.id}/change-password`, {
        method: "POST",
        body: JSON.stringify(passwordForm),
      });
      setPasswordForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setSuccess("Password changed successfully.");
    } catch (err) {
      setError("Failed to change password. Please verify your current password.");
    } finally {
      setChangingPassword(false);
    }
  };

  const avatarSrc = user?.avatarUrl ? `http://localhost:8080${user.avatarUrl}` : null;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">My Profile</h1>
        <p className="page-description">
          Manage your personal information and account details.
        </p>
      </div>

      {error && <div className="error-message">{error}</div>}
      {success && (
        <div className="card" style={{ borderLeft: "4px solid #10b981", marginBottom: "1rem" }}>
          <div className="card-content" style={{ color: "#065f46" }}>
            {success}
          </div>
        </div>
      )}

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
              overflow: "hidden",
            }}
          >
            {avatarSrc ? (
              <img
                src={avatarSrc}
                alt="avatar"
                style={{ width: "100%", height: "100%", objectFit: "cover" }}
              />
            ) : (
              (form.firstName?.[0] || "S").toUpperCase()
            )}
          </div>
          <h2 style={{ color: "#111827", margin: "0 0 0.25rem 0" }}>
            {form.firstName} {form.lastName}
          </h2>
          <p
            style={{
              color: "#6b7280",
              fontSize: "0.875rem",
              margin: "0 0 1rem 0",
            }}
          >
            Student
          </p>
          <span className="badge success">Active</span>

          <div style={{ marginTop: "1rem" }}>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setAvatarFile(e.target.files?.[0] || null)}
            />
            <button
              className="btn btn-secondary"
              style={{ marginTop: "0.5rem", width: "100%" }}
              disabled={uploadingAvatar || !avatarFile}
              onClick={uploadAvatar}
            >
              {uploadingAvatar ? "Uploading..." : "Upload Avatar"}
            </button>
          </div>

          <div style={{ marginTop: "1rem", textAlign: "left" }}>
            <p style={{ margin: 0, fontWeight: 700 }}>Assignment</p>
            <p style={{ margin: "0.25rem 0 0 0", color: "#6b7280", fontSize: "0.875rem" }}>
              Group: {user?.groupTitle ? `${user.groupTitle} (${user.groupCode})` : "Unassigned"}
            </p>
            <p style={{ margin: "0.25rem 0 0 0", color: "#6b7280", fontSize: "0.875rem" }}>
              Adviser: {workspaceHeader?.adviserName || "Unassigned"}
            </p>
          </div>
        </div>

        <div className="card">
          <div
            className="card-header"
            style={{ display: "flex", justifyContent: "space-between" }}
          >
            <h2 className="card-title">Personal Information</h2>
            {editing ? (
              <div style={{ display: "flex", gap: "0.5rem" }}>
                <button className="btn btn-primary" onClick={handleSave} disabled={saving}>
                  <Save style={{ width: "1rem", height: "1rem" }} /> Save
                </button>
                <button
                  className="btn btn-secondary"
                  onClick={() => {
                    setForm({
                      firstName: user?.firstName || "",
                      lastName: user?.lastName || "",
                      email: user?.email || "",
                      studentId: user?.studentId || "",
                      department: user?.department || "",
                      year: user?.year || "",
                      phone: user?.phone || "",
                    });
                    setError("");
                    setEditing(false);
                  }}
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
              <label className="form-label">Student ID</label>
              <input
                className="form-input"
                value={form.studentId}
                disabled={!editing}
                onChange={(e) =>
                  setForm({ ...form, studentId: e.target.value })
                }
              />
            </div>
          </div>
          <div className="form-row">
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
            <div className="form-group">
              <label className="form-label">Year Level</label>
              <input
                className="form-input"
                value={form.year}
                disabled={!editing}
                onChange={(e) => setForm({ ...form, year: e.target.value })}
              />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Phone Number</label>
            <input
              className="form-input"
              value={form.phone}
              disabled={!editing}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </div>

          <div className="card" style={{ marginTop: "1.5rem" }}>
            <div className="card-header">
              <h2 className="card-title">Change Password</h2>
            </div>
            <form className="card-body" style={{ padding: "1rem" }} onSubmit={changePassword}>
              <div className="form-group">
                <label className="form-label">Current Password</label>
                <input
                  className="form-input"
                  type="password"
                  value={passwordForm.currentPassword}
                  onChange={(e) =>
                    setPasswordForm((prev) => ({ ...prev, currentPassword: e.target.value }))
                  }
                />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">New Password</label>
                  <input
                    className="form-input"
                    type="password"
                    value={passwordForm.newPassword}
                    onChange={(e) =>
                      setPasswordForm((prev) => ({ ...prev, newPassword: e.target.value }))
                    }
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Confirm Password</label>
                  <input
                    className="form-input"
                    type="password"
                    value={passwordForm.confirmPassword}
                    onChange={(e) =>
                      setPasswordForm((prev) => ({ ...prev, confirmPassword: e.target.value }))
                    }
                  />
                </div>
              </div>
              <button className="btn btn-primary" type="submit" disabled={changingPassword}>
                {changingPassword ? "Updating..." : "Update Password"}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StudentProfile;
