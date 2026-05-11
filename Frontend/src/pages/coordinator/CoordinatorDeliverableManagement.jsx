import { useState, useEffect, useCallback } from "react";
import { FileText, Plus, Edit, Trash2, CheckCircle2, XCircle } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const CoordinatorDeliverableManagement = () => {
  const { user } = useAuth();
  const [deliverables, setDeliverables] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [newDeliverable, setNewDeliverable] = useState({ name: "", stage: "", active: true });
  const [editingId, setEditingId] = useState(null);

  const fetchDeliverables = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiService.requestJson("/deliverables");
      setDeliverables(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to fetch deliverables", err);
      setError("Failed to load deliverables.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDeliverables();
  }, [fetchDeliverables]);

  const handleSaveDeliverable = async () => {
    if (!newDeliverable.name || !newDeliverable.stage) {
      setError("Please fill in all required fields");
      return;
    }

    try {
      if (editingId) {
        await apiService.requestJson(`/deliverables/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(newDeliverable),
        });
      } else {
        await apiService.requestJson("/deliverables", {
          method: "POST",
          body: JSON.stringify(newDeliverable),
        });
      }
      setShowModal(false);
      setNewDeliverable({ name: "", stage: "", active: true });
      setEditingId(null);
      await fetchDeliverables();
    } catch (err) {
      console.error("Failed to save deliverable", err);
      setError("Failed to save deliverable.");
    }
  };

  const handleEditDeliverable = (deliverable) => {
    setEditingId(deliverable.id);
    setNewDeliverable({
      name: deliverable.name,
      stage: deliverable.stage,
      active: deliverable.active,
    });
    setShowModal(true);
  };

  const handleDeleteDeliverable = async (id) => {
    setDeletingId(id);
    setShowDeleteConfirm(true);
  };

  const confirmDeleteDeliverable = async () => {
    try {
      await apiService.requestJson(`/deliverables/${deletingId}`, {
        method: "DELETE",
      });
      await fetchDeliverables();
    } catch (err) {
      console.error("Failed to delete deliverable", err);
      setError("Failed to delete deliverable.");
    } finally {
      setShowDeleteConfirm(false);
      setDeletingId(null);
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Deliverable Management</h1>
        <p className="page-description">
          Create, edit, and manage academic deliverables and milestones.
        </p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      <div className="card">
        <div className="card-header">
        <h2 className="card-title">Deliverables ({deliverables.length})</h2>
        <button
          className="btn btn-primary"
          onClick={() => {
            setEditingId(null);
            setNewDeliverable({ name: "", stage: "", active: true });
            setShowModal(true);
          }}
        >
          <Plus size={16} /> New Deliverable
        </button>
      </div>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Stage</th>
                <th>Active</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {deliverables.map((deliverable) => (
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
                  <td>
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={() => handleEditDeliverable(deliverable)}
                    >
                      <Edit size={14} />
                    </button>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => handleDeleteDeliverable(deliverable.id)}
                    >
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))}
              {deliverables.length === 0 && (
                <tr>
                  <td colSpan="4" style={{ textAlign: "center", padding: "2rem", color: "#6b7280" }}>
                    No deliverables created yet.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <div className="modal-overlay">
          <div className="modal" style={{ maxWidth: 500 }}>
            <div className="modal-header">
              <h2 className="modal-title">
                {editingId ? "Edit Deliverable" : "Create New Deliverable"}
              </h2>
              <button className="modal-close" onClick={() => {
                setShowModal(false);
                setEditingId(null);
                setNewDeliverable({ name: "", stage: "", active: true });
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
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setShowModal(false);
                  setEditingId(null);
                  setNewDeliverable({ name: "", stage: "", active: true });
                }}
              >
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleSaveDeliverable}>
                <CheckCircle2 size={16} /> {editingId ? "Update" : "Create"} Deliverable
              </button>
            </div>
          </div>
        </div>
      )}

      {showDeleteConfirm && (
        <div className="modal-overlay">
          <div className="modal" style={{ maxWidth: 400 }}>
            <div className="modal-header">
              <h2 className="modal-title">Delete Deliverable</h2>
              <button className="modal-close" onClick={() => {
                setShowDeleteConfirm(false);
                setDeletingId(null);
              }}>
                <Trash2 size={20} />
              </button>
            </div>
            <div className="modal-body">
              <p>Are you sure you want to delete this deliverable?</p>
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setShowDeleteConfirm(false);
                  setDeletingId(null);
                }}
              >
                Cancel
              </button>
              <button
                className="btn btn-danger"
                onClick={confirmDeleteDeliverable}
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

export default CoordinatorDeliverableManagement;