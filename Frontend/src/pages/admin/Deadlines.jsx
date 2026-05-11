import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Plus, Edit2, Trash2, X, Save } from "lucide-react";
import apiService from "../../services/ApiService";

const normalizeLocalDateTime = (value) => {
  if (!value) return "";
  if (value.length === 16) return `${value}:00`;
  return value;
};

const Deadlines = () => {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [showDeadlineModal, setShowDeadlineModal] = useState(false);
  const [deadlineEditing, setDeadlineEditing] = useState(null);
  const [deadlineForm, setDeadlineForm] = useState({
    deliverableId: "",
    dueAt: "",
    academicTerm: "",
  });
  const [savingDeadline, setSavingDeadline] = useState(false);

  const [showDeliverableModal, setShowDeliverableModal] = useState(false);
  const [deliverableForm, setDeliverableForm] = useState({
    name: "",
    stage: "",
  });
  const [savingDeliverable, setSavingDeliverable] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const data = await apiService.requestJson("/deadlines/admin");
      setRows(Array.isArray(data) ? data : []);
    } catch (err) {
      setError("Failed to load deadlines. Please try again.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const deliverableOptions = useMemo(
    () =>
      rows.map((row) => ({
        id: String(row.deliverableId),
        name: row.deliverableName,
        stage: row.stage,
        active: row.active,
      })),
    [rows],
  );

  const openCreateDeadline = () => {
    setDeadlineEditing(null);
    const firstActive = deliverableOptions.find((d) => d.active);
    setDeadlineForm({
      deliverableId: firstActive ? firstActive.id : "",
      dueAt: "",
      academicTerm: "",
    });
    setShowDeadlineModal(true);
  };

  const openEditDeadline = (row) => {
    setDeadlineEditing(row);
    setDeadlineForm({
      deliverableId: String(row.deliverableId),
      dueAt: row.dueAt ? String(row.dueAt).slice(0, 16) : "",
      academicTerm: row.academicTerm || "",
    });
    setShowDeadlineModal(true);
  };

  const saveDeadline = async () => {
    if (!deadlineForm.deliverableId || !deadlineForm.dueAt || !deadlineForm.academicTerm) {
      return;
    }

    setSavingDeadline(true);
    setError("");

    try {
      const payload = {
        deliverableId: Number(deadlineForm.deliverableId),
        dueAt: normalizeLocalDateTime(deadlineForm.dueAt),
        academicTerm: deadlineForm.academicTerm,
      };

      if (deadlineEditing?.deadlineId) {
        await apiService.requestJson(`/deadlines/${deadlineEditing.deadlineId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
      } else {
        await apiService.requestJson("/deadlines", {
          method: "POST",
          body: JSON.stringify(payload),
        });
      }

      setShowDeadlineModal(false);
      await load();
    } catch (err) {
      setError("Failed to save deadline. Please check the values and try again.");
    } finally {
      setSavingDeadline(false);
    }
  };

  const deleteDeadline = async (row) => {
    if (!row.deadlineId) return;
    if (!window.confirm(`Delete the deadline for "${row.deliverableName}"?`)) return;

    setError("");
    try {
      await apiService.apiCall(`/deadlines/${row.deadlineId}`, { method: "DELETE" });
      await load();
    } catch (err) {
      setError("Failed to delete deadline.");
    }
  };

  const openCreateDeliverable = () => {
    setDeliverableForm({ name: "", stage: "" });
    setShowDeliverableModal(true);
  };

  const saveDeliverable = async () => {
    if (!deliverableForm.name || !deliverableForm.stage) return;

    setSavingDeliverable(true);
    setError("");
    try {
      await apiService.requestJson("/deadlines/deliverables", {
        method: "POST",
        body: JSON.stringify({
          name: deliverableForm.name,
          stage: deliverableForm.stage,
        }),
      });
      setShowDeliverableModal(false);
      await load();
    } catch (err) {
      setError("Failed to create deliverable. Names must be unique.");
    } finally {
      setSavingDeliverable(false);
    }
  };

  const getStatusBadge = (row) => {
    if (!row.dueAt) {
      return { label: "Unassigned", tone: "secondary" };
    }
    const due = new Date(row.dueAt);
    if (Number.isNaN(due.getTime())) {
      return { label: "Invalid", tone: "danger" };
    }
    if (due.getTime() < Date.now()) {
      return { label: "Overdue", tone: "danger" };
    }
    const days = (due.getTime() - Date.now()) / (1000 * 60 * 60 * 24);
    if (days <= 7) return { label: "Urgent", tone: "warning" };
    return { label: "Scheduled", tone: "success" };
  };

  return (
    <div>
      <div
        className="page-header"
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          gap: "1rem",
          flexWrap: "wrap",
        }}
      >
        <div>
          <h1 className="page-title">Deadlines</h1>
          <p className="page-description">
            Manage deliverables and their submission deadlines.
          </p>
        </div>
        <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
          <button className="btn btn-secondary" onClick={openCreateDeliverable}>
            <Plus style={{ width: "1rem", height: "1rem" }} /> Add Deliverable
          </button>
          <button className="btn btn-primary" onClick={openCreateDeadline}>
            <Plus style={{ width: "1rem", height: "1rem" }} /> Add Deadline
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="card">
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Deliverable</th>
                <th>Stage</th>
                <th>Due At</th>
                <th>Term</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6" style={{ textAlign: "center", padding: "2rem", color: "#6b7280" }}>
                    Loading deadlines...
                  </td>
                </tr>
              ) : rows.length === 0 ? (
                <tr>
                  <td colSpan="6" style={{ textAlign: "center", padding: "2rem", color: "#6b7280" }}>
                    No deliverables found.
                  </td>
                </tr>
              ) : (
                rows.map((row) => {
                  const status = getStatusBadge(row);
                  return (
                    <tr key={row.deliverableId}>
                      <td>
                        <strong>{row.deliverableName}</strong>
                        {!row.active && (
                          <span className="badge secondary" style={{ marginLeft: "0.5rem" }}>
                            Inactive
                          </span>
                        )}
                      </td>
                      <td>{row.stage}</td>
                      <td>
                        {row.dueAt ? new Date(row.dueAt).toLocaleString() : "—"}
                      </td>
                      <td>{row.academicTerm || "—"}</td>
                      <td>
                        <span className={`badge ${status.tone}`}>{status.label}</span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: "0.5rem" }}>
                          <button
                            className="btn btn-secondary"
                            style={{ padding: "0.25rem 0.5rem" }}
                            onClick={() => openEditDeadline(row)}
                          >
                            <Edit2 style={{ width: "0.875rem", height: "0.875rem" }} />
                          </button>
                          <button
                            className="btn btn-danger"
                            style={{ padding: "0.25rem 0.5rem" }}
                            onClick={() => deleteDeadline(row)}
                            disabled={!row.deadlineId}
                            title={row.deadlineId ? "Delete deadline" : "No deadline to delete"}
                          >
                            <Trash2 style={{ width: "0.875rem", height: "0.875rem" }} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showDeadlineModal && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h2 className="modal-title">
                {deadlineEditing?.deadlineId ? "Edit Deadline" : "Add Deadline"}
              </h2>
              <button className="modal-close" onClick={() => setShowDeadlineModal(false)}>
                <X style={{ width: "1.25rem", height: "1.25rem" }} />
              </button>
            </div>

            <div className="form-group" style={{ marginBottom: "1rem" }}>
              <label className="form-label">Deliverable *</label>
              <select
                className="form-select"
                value={deadlineForm.deliverableId}
                onChange={(e) =>
                  setDeadlineForm((prev) => ({ ...prev, deliverableId: e.target.value }))
                }
                disabled={Boolean(deadlineEditing)}
              >
                <option value="">Select deliverable</option>
                {deliverableOptions.map((d) => (
                  <option key={d.id} value={d.id} disabled={!d.active}>
                    {d.name} ({d.stage}){d.active ? "" : " - inactive"}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Due At *</label>
                <input
                  className="form-input"
                  type="datetime-local"
                  value={deadlineForm.dueAt}
                  onChange={(e) => setDeadlineForm((prev) => ({ ...prev, dueAt: e.target.value }))}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Academic Term *</label>
                <input
                  className="form-input"
                  value={deadlineForm.academicTerm}
                  onChange={(e) =>
                    setDeadlineForm((prev) => ({ ...prev, academicTerm: e.target.value }))
                  }
                  placeholder="e.g., 2025-2026 First Semester"
                />
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowDeadlineModal(false)}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={saveDeadline} disabled={savingDeadline}>
                <Save style={{ width: "1rem", height: "1rem" }} />{" "}
                {savingDeadline ? "Saving..." : "Save"}
              </button>
            </div>
          </div>
        </div>
      )}

      {showDeliverableModal && (
        <div className="modal-overlay">
          <div className="modal">
            <div className="modal-header">
              <h2 className="modal-title">Add Deliverable</h2>
              <button className="modal-close" onClick={() => setShowDeliverableModal(false)}>
                <X style={{ width: "1.25rem", height: "1.25rem" }} />
              </button>
            </div>

            <div className="form-group" style={{ marginBottom: "1rem" }}>
              <label className="form-label">Name *</label>
              <input
                className="form-input"
                value={deliverableForm.name}
                onChange={(e) =>
                  setDeliverableForm((prev) => ({ ...prev, name: e.target.value }))
                }
                placeholder="Deliverable name"
              />
            </div>

            <div className="form-group" style={{ marginBottom: "1rem" }}>
              <label className="form-label">Stage *</label>
              <input
                className="form-input"
                value={deliverableForm.stage}
                onChange={(e) =>
                  setDeliverableForm((prev) => ({ ...prev, stage: e.target.value }))
                }
                placeholder="e.g., Proposal, Midterm, Final"
              />
            </div>

            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowDeliverableModal(false)}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={saveDeliverable} disabled={savingDeliverable}>
                <Save style={{ width: "1rem", height: "1rem" }} />{" "}
                {savingDeliverable ? "Saving..." : "Save"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Deadlines;
