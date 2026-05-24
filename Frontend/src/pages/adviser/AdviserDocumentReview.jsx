import { useState, useEffect, useCallback } from "react";
import { X, Download, Save, FileText } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const STATUS_CONFIG = {
  SUBMITTED_ON_TIME: {
    label: "On Time",
    bg: "#f0fdf4",
    color: "#166534",
    border: "#bbf7d0",
  },
  SUBMITTED_LATE: {
    label: "Late",
    bg: "#fef2f2",
    color: "#b91c1c",
    border: "#fecaca",
  },
  NOT_SUBMITTED: {
    label: "Missing",
    bg: "#fff7ed",
    color: "#9a3412",
    border: "#fed7aa",
  },
  PENDING: {
    label: "Pending",
    bg: "#f9fafb",
    color: "#6b7280",
    border: "#e5e7eb",
  },
  COMMENTED: {
    label: "Commented",
    bg: "#eff6ff",
    color: "#1d4ed8",
    border: "#bfdbfe",
  },
};

const StatusBadge = ({ status }) => {
  const cfg = STATUS_CONFIG[status] || {
    label: status || "â€”",
    bg: "#f9fafb",
    color: "#6b7280",
    border: "#e5e7eb",
  };
  return (
    <span
      style={{
        padding: "0.25rem 0.625rem",
        borderRadius: "9999px",
        fontSize: "0.75rem",
        fontWeight: 600,
        backgroundColor: cfg.bg,
        color: cfg.color,
        border: `1px solid ${cfg.border}`,
      }}
    >
      {cfg.label}
    </span>
  );
};

const AdviserDocumentReview = () => {
  const { user } = useAuth();
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [selected, setSelected] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [existingFeedback, setExistingFeedback] = useState(null);
  const [commentDraft, setCommentDraft] = useState("");
  const [saving, setSaving] = useState(false);
  const [modalError, setModalError] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  const fetchSubmissions = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiService.getAdviserAllSubmissions(user.id);
      setSubmissions(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to fetch submissions", err);
      setError("Failed to load submissions.");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (user?.id) fetchSubmissions();
  }, [user, fetchSubmissions]);

  const openDetail = async (submission) => {
    setSelected(submission);
    setShowModal(true);
    setExistingFeedback(null);
    setCommentDraft("");
    setModalError(null);
    setPreviewUrl(null);
    try {
      const data = await apiService.requestJson(
        `/feedback/submission/${submission.id}`,
      );
      if (data) {
        setExistingFeedback(data);
        setCommentDraft(data.generalComments || "");
      }
    } catch {
      // no existing feedback — OK
    }
    if (submission.fileUrl) {
      setPreviewLoading(true);
      try {
        const blob = await apiService.previewSubmission(submission.id);
        setPreviewUrl(window.URL.createObjectURL(blob));
      } catch {
        setPreviewUrl(null);
      } finally {
        setPreviewLoading(false);
      }
    }
  };

  const closeDetail = () => {
    setShowModal(false);
    setSelected(null);
    setExistingFeedback(null);
    setCommentDraft("");
    setSaving(false);
    setModalError(null);
    if (previewUrl) window.URL.revokeObjectURL(previewUrl);
    setPreviewUrl(null);
    setPreviewLoading(false);
  };

  const saveComment = async () => {
    if (!selected?.id || !commentDraft.trim()) return;
    setSaving(true);
    setModalError(null);
    try {
      await apiService.requestJson(
        `/feedback/comment/${selected.id}?adviserId=${user.id}`,
        {
          method: "POST",
          body: JSON.stringify({ comments: commentDraft.trim() }),
        },
      );
      // Refresh submission list so status reflects COMMENTED
      await fetchSubmissions();
      // Update local selected status optimistically
      setSelected((prev) => ({ ...prev, status: "COMMENTED" }));
      setExistingFeedback({
        generalComments: commentDraft.trim(),
        evaluatedAt: new Date().toISOString(),
      });
    } catch (err) {
      setModalError(
        err?.message || "Failed to post comment. Please try again.",
      );
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
    } catch {
      setModalError("Download failed.");
    }
  };

  if (loading)
    return (
      <div style={{ padding: "2rem", color: "#6b7280" }}>
        Loading submissions...
      </div>
    );

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Submission Monitor</h1>
        <p>Monitor your assigned groups' submissions and leave comments.</p>
      </div>

      {error && (
        <div
          style={{
            marginBottom: "1rem",
            padding: "0.75rem 1rem",
            backgroundColor: "#fef2f2",
            color: "#b91c1c",
            borderRadius: "0.5rem",
            border: "1px solid #fecaca",
          }}
        >
          {error}
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">All Submissions ({submissions.length})</h2>
        </div>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Group</th>
                <th>Members</th>
                <th>Deliverable</th>
                <th>Submitted At</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {submissions.map((doc) => (
                <tr key={doc.id}>
                  <td>
                    <div
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: "0.2rem",
                      }}
                    >
                      <span style={{ fontWeight: 600 }}>
                        {doc.groupTitle || "â€”"}
                      </span>
                      <span style={{ fontSize: "0.8125rem", color: "#6b7280" }}>
                        {doc.groupCode}
                      </span>
                    </div>
                  </td>
                  <td>
                    <div
                      style={{
                        display: "flex",
                        flexWrap: "wrap",
                        gap: "0.35rem",
                      }}
                    >
                      {doc.students?.map((s) => (
                        <span
                          key={s.id}
                          style={{
                            padding: "0.2rem 0.5rem",
                            backgroundColor: "#f3f4f6",
                            borderRadius: "9999px",
                            fontSize: "0.75rem",
                          }}
                        >
                          {s.fullName}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td>{doc.deliverableName || "â€”"}</td>
                  <td style={{ fontSize: "0.875rem" }}>
                    {doc.submittedAt ? (
                      new Date(doc.submittedAt).toLocaleString()
                    ) : (
                      <span style={{ color: "#9ca3af" }}>Not submitted</span>
                    )}
                  </td>
                  <td>
                    <StatusBadge status={doc.status} />
                  </td>
                  <td>
                    <button
                      className="btn btn-primary"
                      style={{
                        fontSize: "0.8125rem",
                        padding: "0.375rem 0.75rem",
                      }}
                      onClick={() => openDetail(doc)}
                    >
                      View
                    </button>
                  </td>
                </tr>
              ))}
              {submissions.length === 0 && (
                <tr>
                  <td
                    colSpan="6"
                    style={{
                      textAlign: "center",
                      padding: "2.5rem",
                      color: "#6b7280",
                    }}
                  >
                    <FileText
                      size={40}
                      style={{
                        margin: "0 auto 1rem",
                        opacity: 0.3,
                        display: "block",
                      }}
                    />
                    No submissions found for your groups.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && selected && (
        <div className="modal-overlay">
          <div className="modal" style={{ maxWidth: 680 }}>
            <div className="modal-header">
              <h2 className="modal-title">Submission Detail</h2>
              <button className="modal-close" onClick={closeDetail}>
                <X style={{ width: "1.25rem", height: "1.25rem" }} />
              </button>
            </div>

            <div
              style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
            >
              {/* Info */}
              <div className="card">
                <div className="card-content" style={{ padding: "1rem" }}>
                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr 1fr",
                      gap: "0.75rem",
                    }}
                  >
                    <div>
                      <p
                        style={{
                          margin: 0,
                          fontSize: "0.75rem",
                          color: "#6b7280",
                        }}
                      >
                        Group
                      </p>
                      <p style={{ margin: 0, fontWeight: 600 }}>
                        {selected.groupTitle}
                      </p>
                      <p
                        style={{
                          margin: 0,
                          fontSize: "0.8125rem",
                          color: "#6b7280",
                        }}
                      >
                        {selected.groupCode}
                      </p>
                    </div>
                    <div>
                      <p
                        style={{
                          margin: 0,
                          fontSize: "0.75rem",
                          color: "#6b7280",
                        }}
                      >
                        Deliverable
                      </p>
                      <p style={{ margin: 0, fontWeight: 600 }}>
                        {selected.deliverableName}
                      </p>
                      <p
                        style={{
                          margin: 0,
                          fontSize: "0.8125rem",
                          color: "#6b7280",
                        }}
                      >
                        {selected.stage}
                      </p>
                    </div>
                    <div>
                      <p
                        style={{
                          margin: 0,
                          fontSize: "0.75rem",
                          color: "#6b7280",
                        }}
                      >
                        Status
                      </p>
                      <StatusBadge status={selected.status} />
                    </div>
                    <div>
                      <p
                        style={{
                          margin: 0,
                          fontSize: "0.75rem",
                          color: "#6b7280",
                        }}
                      >
                        Submitted At
                      </p>
                      <p style={{ margin: 0, fontSize: "0.875rem" }}>
                        {selected.submittedAt
                          ? new Date(selected.submittedAt).toLocaleString()
                          : "Not submitted"}
                      </p>
                    </div>
                  </div>
                  {selected.fileUrl && (
                    <div style={{ marginTop: "0.75rem" }}>
                      <button className="btn btn-secondary" onClick={download}>
                        <Download style={{ width: "1rem", height: "1rem" }} />{" "}
                        Download File
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* File Preview */}
              {selected.fileUrl && (
                <div className="card">
                  <div
                    className="card-header"
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                    }}
                  >
                    <h3 className="card-title">Document Preview</h3>
                  </div>
                  <div className="card-content" style={{ padding: 0 }}>
                    {previewLoading ? (
                      <div
                        style={{
                          padding: "2rem",
                          textAlign: "center",
                          color: "#6b7280",
                        }}
                      >
                        Loading preview...
                      </div>
                    ) : previewUrl ? (
                      <iframe
                        src={previewUrl}
                        title="Document Preview"
                        style={{
                          width: "100%",
                          height: 520,
                          border: "none",
                          borderRadius: "0 0 0.5rem 0.5rem",
                        }}
                      />
                    ) : (
                      <div
                        style={{
                          padding: "1.5rem",
                          textAlign: "center",
                          color: "#6b7280",
                        }}
                      >
                        Preview not available for this file type.
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* AI Summary */}
              {selected.aiSummary && (
                <div
                  className="card"
                  style={{ borderLeft: "4px solid var(--maroon)" }}
                >
                  <div className="card-header">
                    <h3 className="card-title">AI Summary</h3>
                  </div>
                  <div className="card-content">
                    <p style={{ margin: 0 }}>{selected.aiSummary}</p>
                    {selected.aiHighlights && (
                      <p
                        style={{
                          margin: "0.5rem 0 0 0",
                          color: "#6b7280",
                          fontSize: "0.875rem",
                        }}
                      >
                        {selected.aiHighlights}
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* Adviser Feedback */}
              <div className="card">
                <div className="card-header">
                  <h3 className="card-title">Adviser Feedback</h3>
                </div>
                <div className="card-content">
                  {modalError && (
                    <div
                      style={{
                        marginBottom: "0.75rem",
                        padding: "0.625rem 0.875rem",
                        backgroundColor: "#fef2f2",
                        color: "#b91c1c",
                        borderRadius: "0.375rem",
                        border: "1px solid #fecaca",
                        fontSize: "0.875rem",
                      }}
                    >
                      {modalError}
                    </div>
                  )}
                  {existingFeedback && (
                    <div
                      style={{
                        marginBottom: "1rem",
                        padding: "0.875rem",
                        backgroundColor: "#eff6ff",
                        borderRadius: "0.5rem",
                        border: "1px solid #bfdbfe",
                      }}
                    >
                      <p
                        style={{
                          margin: 0,
                          fontSize: "0.75rem",
                          color: "#1d4ed8",
                          fontWeight: 600,
                          marginBottom: "0.25rem",
                        }}
                      >
                        Last saved{" "}
                        {existingFeedback.evaluatedAt
                          ? new Date(
                              existingFeedback.evaluatedAt,
                            ).toLocaleString()
                          : ""}
                        {existingFeedback.adviserName
                          ? ` · ${existingFeedback.adviserName}`
                          : ""}
                      </p>
                      <p style={{ margin: 0 }}>
                        {existingFeedback.generalComments}
                      </p>
                    </div>
                  )}
                  <div
                    className="form-group"
                    style={{ marginBottom: "0.5rem" }}
                  >
                    <label className="form-label">
                      {existingFeedback ? "Update Feedback" : "Write Feedback"}
                    </label>
                    <textarea
                      className="form-input"
                      rows={4}
                      value={commentDraft}
                      onChange={(e) => setCommentDraft(e.target.value)}
                      placeholder="Write your feedback or guidance for this group…"
                      style={{ resize: "vertical" }}
                    />
                  </div>
                  <button
                    className="btn btn-primary"
                    onClick={saveComment}
                    disabled={saving || !commentDraft.trim()}
                  >
                    <Save style={{ width: "1rem", height: "1rem" }} />
                    {saving
                      ? "Saving…"
                      : existingFeedback
                        ? "Update Feedback"
                        : "Post Feedback"}
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

export default AdviserDocumentReview;
