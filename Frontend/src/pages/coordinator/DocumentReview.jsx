import { useState, useEffect, useCallback } from "react";
import { X, Download, Save } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const DocumentReview = () => {
  const { user } = useAuth();
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [selected, setSelected] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentDraft, setCommentDraft] = useState("");
  const [reviewNotes, setReviewNotes] = useState("");
  const [reviewStatus, setReviewStatus] = useState("NEEDS_REVISION");
  const [saving, setSaving] = useState(false);
  const [rubrics, setRubrics] = useState([]);
  const [selectedRubricId, setSelectedRubricId] = useState("");
  const [criterionInputs, setCriterionInputs] = useState({});

  const fetchSubmissions = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiService.getPendingSubmissions();
      setSubmissions(data || []);
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

  const openReview = async (submission) => {
    setSelected(submission);
    setShowModal(true);
    setComments([]);
    setCommentDraft("");
    setReviewNotes("");
    setReviewStatus("NEEDS_REVISION");
    setSelectedRubricId("");
    setCriterionInputs({});

    try {
      if (rubrics.length === 0) {
        const rubricData = await apiService.requestJson(`/feedback/rubrics`);
        setRubrics(Array.isArray(rubricData) ? rubricData : []);
      }

      const data = await apiService.requestJson(
        `/comments/submission/${submission.id}`,
      );
      setComments(Array.isArray(data) ? data : []);
    } catch (err) {
      setComments([]);
    }
  };

  const closeReview = () => {
    setShowModal(false);
    setSelected(null);
    setComments([]);
    setCommentDraft("");
    setReviewNotes("");
    setSaving(false);
  };

  const addComment = async () => {
    if (!selected?.id || !user?.id || !commentDraft.trim()) return;

    setSaving(true);
    try {
      await apiService.requestJson(
        `/comments/submission/${selected.id}?userId=${user.id}`,
        {
          method: "POST",
          body: JSON.stringify({ content: commentDraft.trim() }),
        },
      );
      const updated = await apiService.requestJson(
        `/comments/submission/${selected.id}`,
      );
      setComments(Array.isArray(updated) ? updated : []);
      setCommentDraft("");
    } catch (err) {
      setError("Failed to post comment.");
    } finally {
      setSaving(false);
    }
  };

  const submitReview = async () => {
    if (!selected?.id || !user?.id) return;

    setSaving(true);
    setError(null);
    try {
      const evaluations = [];
      const rubric = rubrics.find((r) => String(r.id) === String(selectedRubricId));
      if (rubric?.criteria?.length) {
        rubric.criteria.forEach((criterion) => {
          const input = criterionInputs[String(criterion.id)];
          if (!input) return;
          const score = Number(input.score);
          if (Number.isNaN(score)) return;
          evaluations.push({
            criterionId: criterion.id,
            score,
            comments: input.comments || "",
          });
        });
      }

      await apiService.requestJson(
        `/feedback/evaluate/${selected.id}?adviserId=${user.id}`,
        {
          method: "POST",
          body: JSON.stringify({
            comments: reviewNotes,
            status: reviewStatus,
            evaluations,
          }),
        },
      );
      closeReview();
      await fetchSubmissions();
    } catch (err) {
      setError("Failed to submit review.");
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

  if (loading) return <div>Loading...</div>;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Document Review</h1>
        <p>Review team submissions.</p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Pending Documents ({submissions.length})</h2>
        </div>
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Team</th>
                <th>Document</th>
                <th>Submitted</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {submissions.map((doc) => (
                <tr key={doc.id}>
                  <td>{doc.groupTitle}</td>
                  <td>{doc.deliverableName}</td>
                  <td>
                    {doc.submittedAt ? new Date(doc.submittedAt).toLocaleString() : "—"}
                  </td>
                  <td>
                    <button className="btn btn-primary" onClick={() => openReview(doc)}>
                      Review
                    </button>
                  </td>
                </tr>
              ))}
              {submissions.length === 0 && (
                <tr>
                  <td colSpan="4" style={{ textAlign: "center", padding: "2rem", color: "#6b7280" }}>
                    No submissions pending review.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && selected && (
        <div className="modal-overlay">
          <div className="modal" style={{ maxWidth: 820 }}>
            <div className="modal-header">
              <h2 className="modal-title">Review Submission</h2>
              <button className="modal-close" onClick={closeReview}>
                <X style={{ width: "1.25rem", height: "1.25rem" }} />
              </button>
            </div>

            <div className="card" style={{ marginBottom: "1rem" }}>
              <div className="card-content" style={{ padding: "1rem" }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: "1rem", flexWrap: "wrap" }}>
                  <div>
                    <p style={{ margin: 0, fontWeight: 600 }}>{selected.groupTitle || "Unknown group"}</p>
                    <p style={{ margin: "0.25rem 0 0 0", color: "#6b7280", fontSize: "0.875rem" }}>
                      {selected.deliverableName} • Version {selected.versionNumber || 1}
                    </p>
                  </div>
                  <button className="btn btn-secondary" onClick={download}>
                    <Download style={{ width: "1rem", height: "1rem" }} /> Download
                  </button>
                </div>
              </div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
              <div className="card">
                <div className="card-header">
                  <h3 className="card-title">Thread</h3>
                </div>
                <div className="card-content" style={{ maxHeight: 240, overflowY: "auto" }}>
                  {comments.length === 0 ? (
                    <p style={{ color: "#6b7280", margin: 0 }}>No comments yet.</p>
                  ) : (
                    comments.map((c) => (
                      <div key={c.id} style={{ padding: "0.75rem 0", borderBottom: "1px solid #f3f4f6" }}>
                        <p style={{ margin: 0, fontWeight: 600, fontSize: "0.875rem" }}>
                          {c.authorFirstName} {c.authorLastName}
                        </p>
                        <p style={{ margin: "0.25rem 0 0 0", color: "#111827" }}>{c.content}</p>
                        <p style={{ margin: "0.25rem 0 0 0", color: "#9ca3af", fontSize: "0.75rem" }}>
                          {c.createdAt ? new Date(c.createdAt).toLocaleString() : ""}
                        </p>
                      </div>
                    ))
                  )}
                </div>
                <div className="card-content">
                  <div className="form-group" style={{ marginBottom: "0.5rem" }}>
                    <label className="form-label">Add Comment</label>
                    <textarea
                      className="form-input"
                      rows={3}
                      value={commentDraft}
                      onChange={(e) => setCommentDraft(e.target.value)}
                      placeholder="Write a comment for the group/adviser..."
                      style={{ resize: "vertical" }}
                    />
                  </div>
                  <button className="btn btn-secondary" onClick={addComment} disabled={saving || !commentDraft.trim()}>
                    <Save style={{ width: "1rem", height: "1rem" }} /> Post Comment
                  </button>
                </div>
              </div>

              <div className="card">
                <div className="card-header">
                  <h3 className="card-title">Decision</h3>
                </div>
                <div className="card-content">
                  {(selected.aiSummary || selected.aiHighlights) && (
                    <div
                      className="card"
                      style={{
                        marginBottom: "1rem",
                        borderLeft: "4px solid var(--maroon)",
                      }}
                    >
                      <div className="card-content" style={{ padding: "1rem" }}>
                        <p style={{ margin: 0, fontWeight: 600 }}>AI Summary</p>
                        {selected.aiSummary && (
                          <p style={{ margin: "0.5rem 0 0 0", color: "#111827" }}>
                            {selected.aiSummary}
                          </p>
                        )}
                        {selected.aiHighlights && (
                          <p style={{ margin: "0.5rem 0 0 0", color: "#6b7280", fontSize: "0.875rem" }}>
                            {selected.aiHighlights}
                          </p>
                        )}
                      </div>
                    </div>
                  )}

                  <div className="form-group" style={{ marginBottom: "1rem" }}>
                    <label className="form-label">Rubric</label>
                    <select
                      className="form-select"
                      value={selectedRubricId}
                      onChange={(e) => {
                        const nextId = e.target.value;
                        setSelectedRubricId(nextId);

                        const rubric = rubrics.find((r) => String(r.id) === String(nextId));
                        const nextInputs = {};
                        if (rubric?.criteria?.length) {
                          rubric.criteria.forEach((criterion) => {
                            nextInputs[String(criterion.id)] = {
                              score: "",
                              comments: "",
                            };
                          });
                        }
                        setCriterionInputs(nextInputs);
                      }}
                    >
                      <option value="">No rubric</option>
                      {rubrics.map((r) => (
                        <option key={r.id} value={String(r.id)}>
                          {r.title}
                        </option>
                      ))}
                    </select>
                  </div>

                  {selectedRubricId &&
                    (() => {
                      const rubric = rubrics.find(
                        (r) => String(r.id) === String(selectedRubricId),
                      );
                      const criteria = rubric?.criteria || [];
                      if (criteria.length === 0) {
                        return (
                          <div style={{ marginBottom: "1rem", color: "#6b7280" }}>
                            Selected rubric has no criteria.
                          </div>
                        );
                      }

                      const totalScore = criteria.reduce((sum, c) => {
                        const raw = criterionInputs[String(c.id)]?.score;
                        const score = raw === "" ? 0 : Number(raw);
                        return sum + (Number.isNaN(score) ? 0 : score);
                      }, 0);

                      const maxScore = criteria.reduce(
                        (sum, c) => sum + (c.maxPoints || 0),
                        0,
                      );

                      return (
                        <div style={{ marginBottom: "1rem" }}>
                          <div
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              alignItems: "center",
                              marginBottom: "0.75rem",
                            }}
                          >
                            <p style={{ margin: 0, fontWeight: 600 }}>
                              Rubric Scoring
                            </p>
                            <span className="badge info">
                              Total: {totalScore}/{maxScore}
                            </span>
                          </div>
                          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                            {criteria.map((criterion) => (
                              <div
                                key={criterion.id}
                                style={{
                                  border: "1px solid #e5e7eb",
                                  borderRadius: "0.5rem",
                                  padding: "0.75rem",
                                }}
                              >
                                <div
                                  style={{
                                    display: "flex",
                                    justifyContent: "space-between",
                                    gap: "0.75rem",
                                    alignItems: "flex-start",
                                    flexWrap: "wrap",
                                  }}
                                >
                                  <div>
                                    <p style={{ margin: 0, fontWeight: 600 }}>
                                      {criterion.name}
                                    </p>
                                    {criterion.description && (
                                      <p
                                        style={{
                                          margin: "0.25rem 0 0 0",
                                          color: "#6b7280",
                                          fontSize: "0.75rem",
                                        }}
                                      >
                                        {criterion.description}
                                      </p>
                                    )}
                                    <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.5rem" }}>
                                      <span className="badge secondary" style={{ fontSize: "0.7rem" }}>
                                        Max {criterion.maxPoints}
                                      </span>
                                      <span className="badge info" style={{ fontSize: "0.7rem" }}>
                                        Weight {criterion.weight}%
                                      </span>
                                    </div>
                                  </div>
                                  <div style={{ minWidth: 140 }}>
                                    <label className="form-label">Score</label>
                                    <input
                                      className="form-input"
                                      type="number"
                                      min="0"
                                      max={criterion.maxPoints}
                                      value={criterionInputs[String(criterion.id)]?.score ?? ""}
                                      onChange={(e) =>
                                        setCriterionInputs((prev) => ({
                                          ...prev,
                                          [String(criterion.id)]: {
                                            ...prev[String(criterion.id)],
                                            score: e.target.value,
                                          },
                                        }))
                                      }
                                    />
                                  </div>
                                </div>
                                <div className="form-group" style={{ marginTop: "0.5rem" }}>
                                  <label className="form-label">Comments</label>
                                  <textarea
                                    className="form-input"
                                    rows={2}
                                    value={criterionInputs[String(criterion.id)]?.comments ?? ""}
                                    onChange={(e) =>
                                      setCriterionInputs((prev) => ({
                                        ...prev,
                                        [String(criterion.id)]: {
                                          ...prev[String(criterion.id)],
                                          comments: e.target.value,
                                        },
                                      }))
                                    }
                                    style={{ resize: "vertical" }}
                                  />
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      );
                    })()}

                  <div className="form-group" style={{ marginBottom: "1rem" }}>
                    <label className="form-label">Status</label>
                    <select
                      className="form-select"
                      value={reviewStatus}
                      onChange={(e) => setReviewStatus(e.target.value)}
                    >
                      <option value="APPROVED">APPROVED</option>
                      <option value="NEEDS_REVISION">NEEDS_REVISION</option>
                      <option value="REJECTED">REJECTED</option>
                    </select>
                  </div>
                  <div className="form-group" style={{ marginBottom: "1rem" }}>
                    <label className="form-label">Review Notes</label>
                    <textarea
                      className="form-input"
                      rows={6}
                      value={reviewNotes}
                      onChange={(e) => setReviewNotes(e.target.value)}
                      placeholder="Decision rationale and next steps..."
                      style={{ resize: "vertical" }}
                    />
                  </div>
                  <button className="btn btn-primary" onClick={submitReview} disabled={saving}>
                    <Save style={{ width: "1rem", height: "1rem" }} />{" "}
                    {saving ? "Submitting..." : "Submit Review"}
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
