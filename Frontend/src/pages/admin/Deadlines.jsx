import React, { useState } from "react";
import { Plus, Edit2, Trash2, X, Save } from "lucide-react";

const Deadlines = () => {
  const [deadlines, setDeadlines] = useState([
    {
      id: 1,
      title: "Project Proposal Submission",
      dueDate: "2025-12-10",
      affectedRoles: "student",
      description: "Submit initial project proposal",
    },
    {
      id: 2,
      title: "SRS Document Submission",
      dueDate: "2025-12-20",
      affectedRoles: "student",
      description: "Software Requirements Specification",
    },
    {
      id: 3,
      title: "SDD Submission",
      dueDate: "2026-01-10",
      affectedRoles: "student",
      description: "Software Design Document",
    },
    {
      id: 4,
      title: "Final Defense",
      dueDate: "2026-02-15",
      affectedRoles: "student",
      description: "Capstone project final defense",
    },
  ]);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({
    title: "",
    dueDate: "",
    affectedRoles: "student",
    description: "",
  });

  const openCreate = () => {
    setEditing(null);
    setForm({
      title: "",
      dueDate: "",
      affectedRoles: "student",
      description: "",
    });
    setShowModal(true);
  };
  const openEdit = (d) => {
    setEditing(d);
    setForm({
      title: d.title,
      dueDate: d.dueDate,
      affectedRoles: d.affectedRoles,
      description: d.description,
    });
    setShowModal(true);
  };

  const handleSave = () => {
    if (!form.title || !form.dueDate) return;
    if (editing) {
      setDeadlines((prev) =>
        prev.map((d) => (d.id === editing.id ? { ...d, ...form } : d)),
      );
    } else {
      setDeadlines((prev) => [...prev, { ...form, id: Date.now() }]);
    }
    setShowModal(false);
  };

  const handleDelete = (id) => {
    if (window.confirm("Delete this deadline?"))
      setDeadlines((prev) => prev.filter((d) => d.id !== id));
  };

  const isUrgent = (dateStr) => {
    const diff = (new Date(dateStr) - new Date()) / (1000 * 60 * 60 * 24);
    return diff <= 7;
  };

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
          <h1 className="page-title">Deadlines</h1>
          <p className="page-description">
            Manage submission deadlines for all roles.
          </p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>
          <Plus style={{ width: "1rem", height: "1rem" }} /> Add Deadline
        </button>
      </div>

      <div className="card">
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Due Date</th>
                <th>Affects</th>
                <th>Description</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {deadlines.map((d) => (
                <tr key={d.id}>
                  <td>
                    <strong>{d.title}</strong>
                  </td>
                  <td>
                    {new Date(d.dueDate).toLocaleDateString("en-US", {
                      month: "short",
                      day: "numeric",
                      year: "numeric",
                    })}
                  </td>
                  <td>
                    <span className="badge info">{d.affectedRoles}</span>
                  </td>
                  <td style={{ color: "#6b7280", fontSize: "0.75rem" }}>
                    {d.description}
                  </td>
                  <td>
                    <span
                      className={`badge ${isUrgent(d.dueDate) ? "danger" : "success"}`}
                    >
                      {isUrgent(d.dueDate) ? "Urgent" : "Scheduled"}
                    </span>
                  </td>
                  <td>
                    <div style={{ display: "flex", gap: "0.5rem" }}>
                      <button
                        className="btn btn-secondary"
                        style={{ padding: "0.25rem 0.5rem" }}
                        onClick={() => openEdit(d)}
                      >
                        <Edit2
                          style={{ width: "0.875rem", height: "0.875rem" }}
                        />
                      </button>
                      <button
                        className="btn btn-danger"
                        style={{ padding: "0.25rem 0.5rem" }}
                        onClick={() => handleDelete(d.id)}
                      >
                        <Trash2
                          style={{ width: "0.875rem", height: "0.875rem" }}
                        />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h2 className="modal-title">
                {editing ? "Edit Deadline" : "Add Deadline"}
              </h2>
              <button
                className="modal-close"
                onClick={() => setShowModal(false)}
              >
                <X style={{ width: "1.25rem", height: "1.25rem" }} />
              </button>
            </div>
            <div className="form-group" style={{ marginBottom: "1rem" }}>
              <label className="form-label">Title *</label>
              <input
                className="form-input"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="Deadline title"
              />
            </div>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Due Date *</label>
                <input
                  className="form-input"
                  type="date"
                  value={form.dueDate}
                  onChange={(e) =>
                    setForm({ ...form, dueDate: e.target.value })
                  }
                />
              </div>
              <div className="form-group">
                <label className="form-label">Affects</label>
                <select
                  className="form-select"
                  value={form.affectedRoles}
                  onChange={(e) =>
                    setForm({ ...form, affectedRoles: e.target.value })
                  }
                >
                  <option value="student">Students</option>
                  <option value="adviser">Advisers</option>
                  <option value="all">All Users</option>
                </select>
              </div>
            </div>
            <div className="form-group" style={{ marginBottom: "1rem" }}>
              <label className="form-label">Description</label>
              <textarea
                className="form-input"
                rows={3}
                value={form.description}
                onChange={(e) =>
                  setForm({ ...form, description: e.target.value })
                }
                placeholder="Brief description..."
                style={{ resize: "vertical" }}
              />
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => setShowModal(false)}
              >
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleSave}>
                <Save style={{ width: "1rem", height: "1rem" }} />{" "}
                {editing ? "Save Changes" : "Add Deadline"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Deadlines;
