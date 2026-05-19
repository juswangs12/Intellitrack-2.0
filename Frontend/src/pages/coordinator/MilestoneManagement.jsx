import { useState, useEffect, useCallback } from "react";
import { FileText, Edit, Trash2, CheckCircle2, XCircle, Calendar } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const MilestoneManagement = () => {
  const { user } = useAuth();
  const [deliverables, setDeliverables] = useState([]);
  const [deadlines, setDeadlines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showDeliverableModal, setShowDeliverableModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deletingType, setDeletingType] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const [newDeliverable, setNewDeliverable] = useState({
    name: "",
    stage: "",
    active: true,
    dueAt: "",
    academicTerm: "",
    deadlineId: null,
  });
  const [editingId, setEditingId] = useState(null);
  const [editingType, setEditingType] = useState(null);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [deliverablesData, deadlinesData] = await Promise.all([
        apiService.requestJson("/deliverables"),
        apiService.requestJson("/deadlines/admin"),
      ]);
      setDeliverables(Array.isArray(deliverablesData) ? deliverablesData : []);
      setDeadlines(Array.isArray(deadlinesData) ? deadlinesData : []);
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

  const handleSaveDeliverable = async () => {
    if (!newDeliverable.name || !newDeliverable.stage) {
      setError("Please fill in all required fields");
      return;
    }

    try {
      let savedDeliverableId = editingId;
      const deliverablePayload = {
        name: newDeliverable.name,
        stage: newDeliverable.stage,
        active: newDeliverable.active,
      };

      if (editingId && editingType === "deliverable") {
        await apiService.requestJson(`/deliverables/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(deliverablePayload),
        });
      } else {
        const created = await apiService.requestJson("/deliverables", {
          method: "POST",
          body: JSON.stringify(deliverablePayload),
        });
        savedDeliverableId = created?.id ?? created?.deliverableId ?? null;
      }

      if (newDeliverable.dueAt && newDeliverable.academicTerm && savedDeliverableId) {
        const deadlinePayload = {
          deliverableId: Number(savedDeliverableId),
          dueAt: newDeliverable.dueAt.length === 16
            ? `${newDeliverable.dueAt}:00`
            : newDeliverable.dueAt,
          academicTerm: newDeliverable.academicTerm,
        };
        if (newDeliverable.deadlineId) {
          await apiService.requestJson(`/deadlines/${newDeliverable.deadlineId}`, {
            method: "PUT",
            body: JSON.stringify(deadlinePayload),
          });
        } else {
          await apiService.requestJson("/deadlines", {
            method: "POST",
            body: JSON.stringify(deadlinePayload),
          });
        }
      }

      setShowDeliverableModal(false);
      setNewDeliverable({ name: "", stage: "", active: true, dueAt: "", academicTerm: "", deadlineId: null });
      setEditingId(null);
      setEditingType(null);
      await fetchData();
    } catch (err) {
      console.error("Failed to save deliverable", err);
      setError("Failed to save deliverable.");
    }
  };

  const handleEditDeliverable = (deliverable) => {
    setEditingType("deliverable");
    setEditingId(deliverable.id);
    const deadline = getDeadlineForDeliverable(deliverable.id);
    setNewDeliverable({
      name: deliverable.name,
      stage: deliverable.stage,
      active: deliverable.active,
      dueAt: deadline?.dueAt ? new Date(deadline.dueAt).toISOString().slice(0, 16) : "",
      academicTerm: deadline?.academicTerm || "",
      deadlineId: deadline?.id ?? deadline?.deadlineId ?? null,
    });
    setShowDeliverableModal(true);
  };

  const handleEditDeadline = (deliverable) => {
    handleEditDeliverable(deliverable);
  };

  const handleDelete = (type, id) => {
    setDeletingType(type);
    setDeletingId(id);
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    try {
      if (deletingType === "deliverable") {
        await apiService.requestJson(`/deliverables/${deletingId}`, {
          method: "DELETE",
        });
      } else if (deletingType === "deadline") {
        await apiService.requestJson(`/deadlines/${deletingId}`, {
          method: "DELETE",
        });
      }
      await fetchData();
    } catch (err) {
      console.error("Failed to delete", err);
      setError("Failed to delete.");
    } finally {
      setShowDeleteConfirm(false);
      setDeletingType(null);
      setDeletingId(null);
    }
  };

  const getDeadlineForDeliverable = (deliverableId) => {
    return deadlines.find(d => d.deliverableId === deliverableId);
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Milestone Management</h1>
        <p className="page-description">
          Manage all academic deliverables and deadlines in one unified view.
        </p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Milestones ({deliverables.length})</h2>
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <button
              className="btn btn-primary"
              onClick={() => {
                setEditingType(null);
                setEditingId(null);
                setNewDeliverable({ name: "", stage: "", active: true, dueAt: "", academicTerm: "", deadlineId: null });
                setShowDeliverableModal(true);
              }}
            >
              <FileText size={16} /> New Deliverable
            </button>
          </div>
        </div>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Deliverable Name</th>
                <th>Stage</th>
                <th>Active</th>
                <th>Academic Term</th>
                <th>Due Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {deliverables.map((deliverable) => {
                const deadline = getDeadlineForDeliverable(deliverable.id);
                return (
                  <tr key={deliverable.id}>
                    <td>{deliverable.name}</td>
                    <td>{deliverable.stage}</td>
                    <td>
                      <span
                        className={`badge ${deliverable.active ? "success" : "secondary"}`}
                      >
                        {deliverable.active ? "Yes" : "No"}
                      </span>
                    </td>
                    <td>{deadline?.academicTerm || "—"}</td>
                    <td>
                      {deadline?.dueAt ? new Date(deadline.dueAt).toLocaleString() : "—"}
                    </td>
                    <td>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => handleEditDeliverable(deliverable)}
                        title="Edit Deliverable"
                      >
                        <Edit size={14} />
                      </button>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => handleEditDeliverable(deliverable)}
                        title={deadline ? "Edit Deadline" : "Add Deadline"}
                      >
                        <Calendar size={14} />
                      </button>
                      <button
                        className="btn btn-danger btn-sm"
                        onClick={() => handleDelete("deliverable", deliverable.id)}
                        title="Delete Deliverable"
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                );
              })}
              {deliverables.length === 0 && (
                <tr>
                  <td colSpan="6" style={{ textAlign: "center", padding: "2rem", color: "#6b7280" }}>
                    No milestones created yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Deliverable Modal */}
      {showDeliverableModal && (
        <div className="modal-overlay">
          <div className="modal" style={{ maxWidth: 500 }}>
            <div className="modal-header">
              <h2 className="modal-title">
                {editingType === "deliverable" ? "Edit Deliverable" : "Create New Deliverable"}
              </h2>
              <button className="modal-close" onClick={() => {
                setShowDeliverableModal(false);
                setEditingId(null);
                setEditingType(null);
                setNewDeliverable({ name: "", stage: "", active: true, dueAt: "", academicTerm: "", deadlineId: null });
              }}>
                <Trash2 size={20} />
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Deliverable Name</label>
                <input
                  type="text"
                  className="form-input"
                  value={newDeliverable.name}
                  onChange={(e) => setNewDeliverable({ ...newDeliverable, name: e.target.value })}
                  placeholder="e.g., Project Proposal"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Academic Stage</label>
                <select
                  className="form-select"
                  value={newDeliverable.stage}
                  onChange={(e) => setNewDeliverable({ ...newDeliverable, stage: e.target.value })}
                >
                  <option value="">Select a stage</option>
                  <option value="Prelims">Prelims</option>
                  <option value="Midterms">Midterms</option>
                  <option value="Prefinals">Prefinals</option>
                  <option value="Finals">Finals</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Active</label>
                <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                  <input
                    type="checkbox"
                    checked={newDeliverable.active}
                    onChange={(e) =>
                      setNewDeliverable({ ...newDeliverable, active: e.target.checked })
                  }
                  />
                  <span>Make this deliverable active</span>
                </div>
              </div>
              <hr style={{ margin: "1rem 0", borderColor: "#e5e7eb" }} />
              <p style={{ fontSize: "0.875rem", color: "#6b7280", marginBottom: "0.75rem" }}>
                Deadline (optional)
              </p>
              <div className="form-group">
                <label className="form-label">Academic Term</label>
                <input
                  type="text"
                  className="form-input"
                  value={newDeliverable.academicTerm}
                  onChange={(e) => setNewDeliverable({ ...newDeliverable, academicTerm: e.target.value })}
                  placeholder="e.g., 2025-2026 First Semester"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Due Date & Time</label>
                <input
                  type="datetime-local"
                  className="form-input"
                  value={newDeliverable.dueAt}
                  onChange={(e) => setNewDeliverable({ ...newDeliverable, dueAt: e.target.value })}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setShowDeliverableModal(false);
                  setEditingId(null);
                  setEditingType(null);
                  setNewDeliverable({ name: "", stage: "", active: true, dueAt: "", academicTerm: "", deadlineId: null });
                }}
              >
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleSaveDeliverable}>
                <CheckCircle2 size={16} /> {editingType === "deliverable" ? "Update" : "Create"} Deliverable
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation */}
      {showDeleteConfirm && (
        <div className="modal-overlay">
          <div className="modal" style={{ maxWidth: 400 }}>
            <div className="modal-header">
              <h2 className="modal-title">Delete {deletingType === "deliverable" ? "Deliverable" : "Deadline"}</h2>
              <button className="modal-close" onClick={() => {
                setShowDeleteConfirm(false);
                setDeletingType(null);
                setDeletingId(null);
              }}>
                <Trash2 size={20} />
              </button>
            </div>
            <div className="modal-body">
              <p>Are you sure you want to delete this {deletingType}?</p>
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setShowDeleteConfirm(false);
                  setDeletingType(null);
                  setDeletingId(null);
                }}
              >
                Cancel
              </button>
              <button
                className="btn btn-danger"
                onClick={confirmDelete}
              >
                <XCircle size={16} /> Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MilestoneManagement;
