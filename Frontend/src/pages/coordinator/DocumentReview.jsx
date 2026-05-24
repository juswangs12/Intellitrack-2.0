import { useState, useEffect, useCallback } from "react";
import { X, Download, Save, FileText, Filter } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const STATUS_CONFIG = {
  SUBMITTED_ON_TIME: { label: "On Time", bg: "#f0fdf4", color: "#166534", border: "#bbf7d0" },
  SUBMITTED_LATE:    { label: "Late",    bg: "#fef2f2", color: "#b91c1c", border: "#fecaca" },
  NOT_SUBMITTED:     { label: "Missing", bg: "#fff7ed", color: "#9a3412", border: "#fed7aa" },
  PENDING:           { label: "Pending", bg: "#f9fafb", color: "#6b7280", border: "#e5e7eb" },
  COMMENTED:         { label: "Commented", bg: "#eff6ff", color: "#1d4ed8", border: "#bfdbfe" },
};

const StatusBadge = ({ status }) => {
  const cfg = STATUS_CONFIG[status] || { label: status || "â€”", bg: "#f9fafb", color: "#6b7280", border: "#e5e7eb" };
  return (
    <span style={{
      padding: "0.25rem 0.625rem", borderRadius: "9999px", fontSize: "0.75rem",
      fontWeight: 600, backgroundColor: cfg.bg, color: cfg.color,
      border: `1px solid ${cfg.border}`,
    }}>{cfg.label}</span>
  );
};

const DocumentReview = () => {
  const { user } = useAuth();
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedClassCode, setSelectedClassCode] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("");

  const [selected, setSelected] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentDraft, setCommentDraft] = useState("");
  const [saving, setSaving] = useState(false);

  const fetchSubmissions = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiService.getAllSubmissions();
      setSubmissions(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to fetch submissions", err);
      setError("Failed to load submissions.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSubmissions();
  }, [fetchSubmissions]);

  const openDetail = async (submission) => {
    setSelected(submission);
    setShowModal(true);
    setComments([]);
    setCommentDraft("");
    try {
      const data = await apiService.requestJson(`/comments/submission/${submission.id}`);
      setComments(Array.isArray(data) ? data : []);
    } catch (err) {
      setComments([]);
    }
  };

  const closeDetail = () => {
    setShowModal(false);
    setSelected(null);
    setComments([]);
    setCommentDraft("");
    setSaving(false);
  };

  const addComment = async () => {
    if (!selected?.id || !commentDraft.trim()) return;
    setSaving(true);
    try {
      await apiService.requestJson(`/comments/submission/${selected.id}`, {
        method: "POST",
        body: JSON.stringify({ content: commentDraft.trim() }),
      });
      const updated = await apiService.requestJson(`/comments/submission/${selected.id}`);
      setComments(Array.isArray(updated) ? updated : []);
      setCommentDraft("");
    } catch (err) {
      setError("Failed to post comment.");
    } finally {
      setSaving(false);
    }
  };

  const download = async () => {
    if (!selected?.id) return;
    try {
      const blob = await apiService.downloadSubmission(selected.id);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", `submission-${selected.id}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError("Download failed.");
    }
  };

  if (loading) return <div style={{ padding: "2rem", color: "#6b7280" }}>Loading submissions...</div>;

  const classCodes = [...new Set(submissions.map((s) => s.subjectCode).filter(Boolean))].sort();
  const filtered = submissions.filter((s) => {
    if (selectedClassCode && s.subjectCode !== selectedClassCode) return false;
    if (selectedStatus && s.status !== selectedStatus) return false;
    return true;
  });

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Submission Review</h1>
        <p>Monitor all group submissions, statuses, and adviser comments.</p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", padding: "0.75rem 1rem", backgroundColor: "#fef2f2",
          color: "#b91c1c", borderRadius: "0.5rem", border: "1px solid #fecaca" }}>
          {error}
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">All Submissions ({filtered.length})</h2>
          <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", flexWrap: "wrap" }}>
            <Filter size={16} style={{ color: "#6b7280" }} />
            <select className="form-select" style={{ minWidth: 150, fontSize: "0.875rem" }}
              value={selectedClassCode} onChange={(e) => setSelectedClassCode(e.target.value)}>
              <option value="">All Subjects</option>
              {classCodes.map((code) => <option key={code} value={code}>{code}</option>)}
            </select>
            <select className="form-select" style={{ minWidth: 150, fontSize: "0.875rem" }}
              value={selectedStatus} onChange={(e) => setSelectedStatus(e.target.value)}>
              <option value="">All Statuses</option>
              <option value="SUBMITTED_ON_TIME">On Time</option>
              <option value="SUBMITTED_LATE">Late</option>
              <option value="NOT_SUBMITTED">Missing</option>
              <option value="PENDING">Pending</option>
              <option value="COMMENTED">Commented</option>
            </select>
          </div>
        </div>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Group</th>
                <th>Subject / Section</th>
                <th>Deliverable</th>
                <th>Submitted At</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((doc) => (
                <tr key={doc.id}>
                  <td>
                    <div style={{ display: "flex", flexDirection: "column", gap: "0.2rem" }}>
                      <span style={{ fontWeight: 600 }}>{doc.groupTitle || "â€”"}</span>
                      <span style={{ fontSize: "0.8125rem", color: "#6b7280" }}>{doc.groupCode}</span>
                    </div>
                  </td>
                  <td>
                    <div style={{ display: "flex", flexDirection: "column", gap: "0.2rem" }}>
                      <span style={{ fontWeight: 600, fontSize: "0.875rem" }}>{doc.subjectCode || "â€”"}</span>
                      <span style={{ fontSize: "0.8125rem", color: "#6b7280" }}>{doc.sectionName || "â€”"}</span>
                    </div>
                  </td>
                  <td>{doc.deliverableName || "â€”"}</td>
                  <td style={{ fontSize: "0.875rem" }}>
                    {doc.submittedAt ? new Date(doc.submittedAt).toLocaleString() : <span style={{ color: "#9ca3af" }}>Not submitted</span>}
                  </td>
                  <td><StatusBadge status={doc.status} /></td>
                  <td>
                    <button className="btn btn-primary" style={{ fontSize: "0.8125rem", padding: "0.375rem 0.75rem" }}
                      onClick={() => openDetail(doc)}>
                      View
                    </button>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan="6" style={{ textAlign: "center", padding: "2.5rem", color: "#6b7280" }}>
                    <FileText size={40} style={{ margin: "0 auto 1rem", opacity: 0.3, display: "block" }} />
                    No submissions found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && selected && (
        <div className="modal-overlay">
          <div className="modal" style={{ maxWidth: 700 }}>
            <div className="modal-header">
              <h2 className="modal-title">Submission Detail</h2>
              <button className="modal-close" onClick={closeDetail}>
                <X style={{ width: "1.25rem", height: "1.25rem" }} />
              </button>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
              {/* Info card */}
              <div className="card">
                <div className="card-content" style={{ padding: "1rem" }}>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0.75rem" }}>
                    <div>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#6b7280" }}>Group</p>
                      <p style={{ margin: 0, fontWeight: 600 }}>{selected.groupTitle}</p>
                      <p style={{ margin: 0, fontSize: "0.8125rem", color: "#6b7280" }}>{selected.groupCode}</p>
                    </div>
                    <div>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#6b7280" }}>Deliverable</p>
                      <p style={{ margin: 0, fontWeight: 600 }}>{selected.deliverableName}</p>
                      <p style={{ margin: 0, fontSize: "0.8125rem", color: "#6b7280" }}>{selected.stage}</p>
                    </div>
                    <div>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#6b7280" }}>Status</p>
                      <StatusBadge status={selected.status} />
                    </div>
                    <div>
                      <p style={{ margin: 0, fontSize: "0.75rem", color: "#6b7280" }}>Submitted At</p>
                      <p style={{ margin: 0, fontSize: "0.875rem" }}>
                        {selected.submittedAt ? new Date(selected.submittedAt).toLocaleString() : "Not submitted"}
                      </p>
                    </div>
                  </div>
                  {selected.fileUrl && (
                    <div style={{ marginTop: "0.75rem" }}>
                      <button className="btn btn-secondary" onClick={download}>
                        <Download style={{ width: "1rem", height: "1rem" }} /> Download File
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* Students */}
              {selected.students?.length > 0 && (
                <div className="card">
                  <div className="card-header"><h3 className="card-title">Members</h3></div>
                  <div className="card-content" style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
                    {selected.students.map((s) => (
                      <span key={s.id} style={{ padding: "0.25rem 0.625rem", backgroundColor: "#f3f4f6",
                        borderRadius: "9999px", fontSize: "0.8125rem" }}>
                        {s.fullName}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* AI Summary */}
              {selected.aiSummary && (
                <div className="card" style={{ borderLeft: "4px solid var(--maroon)" }}>
                  <div className="card-header"><h3 className="card-title">AI Summary</h3></div>
                  <div className="card-content">
                    <p style={{ margin: 0 }}>{selected.aiSummary}</p>
                    {selected.aiHighlights && (
                      <p style={{ margin: "0.5rem 0 0 0", color: "#6b7280", fontSize: "0.875rem" }}>{selected.aiHighlights}</p>
                    )}
                  </div>
                </div>
              )}

              {/* Comment thread */}
              <div className="card">
                <div className="card-header"><h3 className="card-title">Comments</h3></div>
                <div className="card-content">
                  <div style={{ maxHeight: 200, overflowY: "auto", marginBottom: "1rem" }}>
                    {comments.length === 0 ? (
                      <p style={{ color: "#6b7280", margin: 0 }}>No comments yet.</p>
                    ) : (
                      comments.map((c) => (
                        <div key={c.id} style={{ padding: "0.625rem 0", borderBottom: "1px solid #f3f4f6" }}>
                          <p style={{ margin: 0, fontWeight: 600, fontSize: "0.875rem" }}>
                            {c.authorFirstName} {c.authorLastName}
                          </p>
                          <p style={{ margin: "0.2rem 0 0 0" }}>{c.content}</p>
                          <p style={{ margin: "0.2rem 0 0 0", color: "#9ca3af", fontSize: "0.75rem" }}>
                            {c.createdAt ? new Date(c.createdAt).toLocaleString() : ""}
                          </p>
                        </div>
                      ))
                    )}
                  </div>
                  <div className="form-group" style={{ marginBottom: "0.5rem" }}>
                    <label className="form-label">Add Comment</label>
                    <textarea className="form-input" rows={3} value={commentDraft}
                      onChange={(e) => setCommentDraft(e.target.value)}
                      placeholder="Write a comment..." style={{ resize: "vertical" }} />
                  </div>
                  <button className="btn btn-secondary" onClick={addComment}
                    disabled={saving || !commentDraft.trim()}>
                    <Save style={{ width: "1rem", height: "1rem" }} /> {saving ? "Posting..." : "Post Comment"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DocumentReview;
