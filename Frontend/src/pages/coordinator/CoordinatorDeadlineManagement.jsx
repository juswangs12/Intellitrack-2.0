import { useState, useEffect, useCallback } from "react";
import { Calendar, Plus, Edit, Trash2, CheckCircle2 } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const CoordinatorDeadlineManagement = () => {
  const { user } = useAuth();
  const [deadlines, setDeadlines] = useState([]);
  const [deliverables, setDeliverables] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [newDeadline, setNewDeadline] = useState({
    deliverableId: "",
    dueAt: "",
    academicTerm: "",
  });

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [deadlinesData, deliverablesData] = await Promise.all([
        apiService.requestJson("/deadlines/admin"),
        apiService.requestJson("/deliverables"),
      ]);
      setDeadlines(Array.isArray(deadlinesData) ? deadlinesData : []);
      setDeliverables(Array.isArray(deliverablesData) ? deliverablesData : []);
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

  const handleSaveDeadline = async () => {
    if (!newDeadline.deliverableId || !newDeadline.dueAt || !newDeadline.academicTerm) {
      setError("Please fill in all required fields");
      return;
    }

    try {
      if (editingId) {
        await apiService.requestJson(`/deadlines/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(newDeadline),
        });
      } else {
        await apiService.requestJson("/deadlines", {
          method: "POST",
          body: JSON.stringify(newDeadline),
        });
      }
      setShowModal(false);
      setNewDeadline({ deliverableId: "", dueAt: "", academicTerm: "" });
      setEditingId(null);
      await fetchData();
    } catch (err) {
      console.error("Failed to save deadline", err);
      setError("Failed to save deadline.");
    }
  };

  const handleEditDeadline = (deadline) => {
    setEditingId(deadline.id);
    setNewDeadline({
      deliverableId: String(deadline.deliverableId),
      dueAt: deadline.dueAt ? new Date(deadline.dueAt).toISOString().slice(0, 16) : "",
      academicTerm: deadline.academicTerm,
    });
    setShowModal(true);
  };

  const handleDeleteDeadline = async (id) => {
    try {
      await apiService.requestJson(`/deadlines/${id}`, {
        method: "DELETE",
      });
      await fetchData();
    } catch (err) {
      console.error("Failed to delete deadline", err);
      setError("Failed to delete deadline.");
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Deadline Management</h1>
        <p className="page-description">
          Create, edit, and manage academic deadlines and milestone schedules.
        </p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Deadlines ({deadlines.length})</h2>
          <button
            className="btn btn-primary"
            onClick={() => {
              setEditingId(null);
              setNewDeadline({ deliverableId: "", dueAt: "", academicTerm: "" });
              setShowModal(true);
            }}
          >
            <Plus size={16} /> New Deadline
          </button>
        </div>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Deliverable</th>
                <th>Academic Term</th>
                <th>Due Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {deadlines.map((deadline) => (
                <tr key={deadline.id}>
                  <td>{deadline.deliverableName}</td>
                  <td>{deadline.academicTerm}</td>
                  <td>
                    {deadline.dueAt ? new Date(deadline.dueAt).toLocaleString() : "—"}
                  </td>
                  <td>
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={() => handleEditDeadline(deadline)}
                    >
                      <Edit size={14} />
                    </button>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => handleDeleteDeadline(deadline.id)}
                    >
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))}
              {deadlines.length === 0 && (
                <tr>
                  <td colSpan="4" style={{ textAlign: "center", padding: "2rem", color: "#6b7280" }}>
                    No deadlines created yet.
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
                {editingId ? "Edit Deadline" : "Create New Deadline"}
              </h2>
              <button className="modal-close" onClick={() => {
                setShowModal(false);
                setEditingId(null);
                setNewDeadline({ deliverableId: "", dueAt: "", academicTerm: "" });
              }}>
                <Trash2 size={20} />
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Deliverable</label>
                <select
                  className="form-select"
                  value={newDeadline.deliverableId}
                  onChange={(e) => setNewDeadline({ ...newDeadline, deliverableId: e.target.value })}
                >
                  <option value="">Select a deliverable</option>
                  {deliverables.map((deliverable) => (
                    <option key={deliverable.id} value={String(deliverable.id)}>
                      {deliverable.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Academic Term</label>
                <input
                  type="text"
                  className="form-input"
                  value={newDeadline.academicTerm}
                  onChange={(e) => setNewDeadline({ ...newDeadline, academicTerm: e.target.value })}
                  placeholder="e.g., 2025-2026 First Semester"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Due Date & Time</label>
                <input
                  type="datetime-local"
                  className="form-input"
                  value={newDeadline.dueAt}
                  onChange={(e) => setNewDeadline({ ...newDeadline, dueAt: e.target.value })}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setShowModal(false);
                  setEditingId(null);
                  setNewDeadline({ deliverableId: "", dueAt: "", academicTerm: "" });
                }}
              >
                Cancel
              </button>
              <button className="btn btn-primary" onClick={handleSaveDeadline}>
                <CheckCircle2 size={16} /> {editingId ? "Update" : "Create"} Deadline
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CoordinatorDeadlineManagement;