import React, { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { FileText, CheckCircle, Clock, AlertCircle, ChevronLeft } from "lucide-react";
import apiService from "../../services/ApiService";
import { useAuth } from "../../context/AuthContext";

const DocumentSubmission = () => {
  const { deliverableId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [deliverable, setDeliverable] = useState(null);
  const [currentSubmission, setCurrentSubmission] = useState(null);
  const [history, setHistory] = useState([]);
  const [feedback, setFeedback] = useState(null);
  const [file, setFile] = useState(null);
  const [notes, setNotes] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    if (!deliverableId || !user?.id) return;
    
    try {
      setLoading(true);
      setError(null);
      setFeedback(null);
      
      const activeDeliverables = await apiService.requestJson('/deliverables/active');
      const current = (activeDeliverables || []).find(d => d.id === parseInt(deliverableId));
      setDeliverable(current);
      
      if (user.groupId) {
        const submissions = await apiService.getSubmissionsByGroup(user.groupId);
        const submission = (submissions || []).find(
          (s) => s.deliverableId === parseInt(deliverableId),
        );
        setCurrentSubmission(submission || null);

        if (submission?.id) {
          const versions = await apiService.getSubmissionVersions(submission.id);
          setHistory(Array.isArray(versions) ? versions : []);

          const fb = await apiService.getStudentSubmissionFeedback(user.id, submission.id);
          setFeedback(fb || null);
        } else {
          setHistory([]);
          setFeedback(null);
        }
      } else {
        setCurrentSubmission(null);
        setHistory([]);
        setFeedback(null);
      }
    } catch (err) {
      console.error("Failed to fetch submission data", err);
      setError("Failed to load submission details. Please try again.");
    } finally {
      setLoading(false);
    }
  }, [deliverableId, user]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file || !user?.id) return;
    if (file.size > 10 * 1024 * 1024) {
      setError("File too large. Max file size is 10MB.");
      return;
    }
    
    setSubmitting(true);
    setError(null);
    
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('groupId', user.groupId);
      formData.append('deliverableId', deliverableId);
      formData.append('userId', user.id);
      if (notes) formData.append('notes', notes);

      await apiService.uploadSubmission(formData);

      setSubmitted(true);
      setFile(null);
      setNotes("");
      fetchData(); // Refresh history
    } catch (err) {
      console.error("Submission failed", err);
      setError(err.message || "Failed to submit document. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDownloadLatest = async (submissionId, fileName) => {
    try {
      const blob = await apiService.downloadSubmission(submissionId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName || `submission-${submissionId}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert("Download failed: " + err.message);
    }
  };

  const handleDownloadVersion = async (versionId, fileName) => {
    try {
      const blob = await apiService.downloadSubmissionVersion(versionId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName || `submission-version-${versionId}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert("Download failed: " + err.message);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin text-maroon" style={{ width: '40px', height: '40px', border: '4px solid', borderRadius: '50%', borderTopColor: 'transparent' }}></div>
      </div>
    );
  }

  if (!deliverable && !loading) {
    return (
      <div className="card text-center p-8">
        <AlertCircle size={48} className="text-red-500 mx-auto mb-4" />
        <h2 className="text-xl font-bold mb-2">Deliverable Not Found</h2>
        <p className="text-gray-600 mb-6">The deliverable you are looking for does not exist or is inactive.</p>
        <button className="btn btn-primary" onClick={() => navigate('/student/home')}>Return to Dashboard</button>
      </div>
    );
  }

  return (
    <div className="fade-in">
      <div className="page-header">
        <button 
          onClick={() => navigate('/student/home')}
          className="btn btn-secondary mb-4"
          style={{ padding: '0.25rem 0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <ChevronLeft size={16} /> Back to Dashboard
        </button>
        <h1 className="page-title">{deliverable.name}</h1>
        <p className="page-description">
          Stage: <span className="badge info">{deliverable.stage}</span>
        </p>
      </div>

      {error && (
        <div className="card mb-6" style={{ borderLeft: '4px solid #ef4444' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: '#b91c1c' }}>
            <AlertCircle size={20} />
            <p style={{ margin: 0 }}>{error}</p>
          </div>
        </div>
      )}

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1.5fr 1fr",
          gap: "1.5rem",
        }}
      >
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">
              {submitted ? "Submission Received" : "Upload New Version"}
            </h2>
          </div>

          {currentSubmission && (
            <div className="card-body" style={{ padding: "1rem 1.5rem 0 1.5rem" }}>
              <div
                style={{
                  border: "1px solid #e5e7eb",
                  borderRadius: "0.75rem",
                  padding: "0.75rem",
                  display: "flex",
                  justifyContent: "space-between",
                  gap: "1rem",
                  flexWrap: "wrap",
                  alignItems: "center",
                }}
              >
                <div>
                  <p style={{ margin: 0, fontWeight: 700 }}>
                    Latest Version: {currentSubmission.versionNumber || 1}
                  </p>
                  <p style={{ margin: "0.25rem 0 0 0", color: "#6b7280", fontSize: "0.75rem" }}>
                    {currentSubmission.submittedAt ? new Date(currentSubmission.submittedAt).toLocaleString() : "—"}
                  </p>
                </div>
                <div style={{ display: "flex", gap: "0.5rem", alignItems: "center", flexWrap: "wrap" }}>
                  <span className={`badge ${
                    currentSubmission.status === 'APPROVED' ? 'success' :
                    currentSubmission.status === 'NEEDS_REVISION' ? 'danger' :
                    currentSubmission.status === 'REJECTED' ? 'danger' :
                    currentSubmission.status === 'LATE' ? 'danger' :
                    currentSubmission.status === 'SUBMITTED' || currentSubmission.status === 'UPDATED' ? 'info' : 'secondary'
                  }`}>
                    {String(currentSubmission.status || "PENDING").replaceAll('_', ' ')}
                  </span>
                  <button
                    className="btn btn-secondary"
                    style={{ padding: "0.25rem 0.5rem" }}
                    onClick={() =>
                      handleDownloadLatest(currentSubmission.id, `submission-${currentSubmission.id}`)
                    }
                  >
                    <FileText size={14} style={{ marginRight: '0.4rem' }} /> Download Latest
                  </button>
                </div>
              </div>
            </div>
          )}
          
          {submitted ? (
            <div className="card-body text-center py-8">
              <CheckCircle
                style={{
                  width: "4rem",
                  height: "4rem",
                  color: "#10b981",
                  margin: "0 auto 1.5rem",
                  display: "block",
                }}
              />
              <h3 style={{ color: "#10b981", marginBottom: "0.75rem", fontSize: '1.25rem', fontWeight: '600' }}>
                Success!
              </h3>
              <p style={{ color: '#4b5563', marginBottom: '1.5rem' }}>
                Your document has been uploaded and queued for AI analysis and adviser review.
              </p>
              <button
                className="btn btn-primary"
                onClick={() => setSubmitted(false)}
              >
                Upload Another Version
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="card-body" style={{ padding: '1.5rem' }}>
              <div className="form-group mb-6">
                <label className="form-label" style={{ fontWeight: '600' }}>Select Document (PDF/DOCX)</label>
                <div
                  style={{
                    border: "2px dashed #cbd5e1",
                    borderRadius: "0.75rem",
                    padding: "2.5rem 1.5rem",
                    textAlign: "center",
                    cursor: "pointer",
                    background: file ? "rgba(16, 185, 129, 0.05)" : "#f8fafc",
                    transition: 'all 0.2s'
                  }}
                  onMouseOver={(e) => e.currentTarget.style.borderColor = 'var(--maroon)'}
                  onMouseOut={(e) => e.currentTarget.style.borderColor = '#cbd5e1'}
                  onClick={() => document.getElementById(`file-upload`).click()}
                >
                  <FileText
                    style={{
                      width: "2.5rem",
                      height: "2.5rem",
                      margin: "0 auto 1rem",
                      display: "block",
                      color: file ? "#10b981" : "#64748b",
                    }}
                  />
                  <p
                    style={{
                      margin: 0,
                      color: file ? "#10b981" : "#334155",
                      fontSize: "1rem",
                      fontWeight: file ? '600' : '400'
                    }}
                  >
                    {file ? file.name : "Click to select or drag and drop your file here"}
                  </p>
                  <p style={{ fontSize: '0.75rem', color: '#94a3b8', marginTop: '0.5rem' }}>
                    Max file size: 10MB
                  </p>
                  <input
                    id="file-upload"
                    type="file"
                    accept=".pdf,.doc,.docx"
                    style={{ display: "none" }}
                    onChange={(e) => setFile(e.target.files[0])}
                  />
                </div>
              </div>
              
              <div className="form-group mb-6">
                <label className="form-label" style={{ fontWeight: '600' }}>
                  Submission Notes
                </label>
                <textarea
                  className="form-control"
                  rows={4}
                  placeholder="What's new in this version? Any specific areas for feedback?"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  style={{ minHeight: '100px' }}
                />
              </div>
              
              <button
                type="submit"
                className="btn btn-primary w-full"
                disabled={!file || submitting}
                style={{ width: '100%', padding: '0.75rem' }}
              >
                {submitting ? (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                    <div className="animate-spin" style={{ width: '20px', height: '20px', border: '2px solid', borderRadius: '50%', borderTopColor: 'transparent' }}></div>
                    Processing Submission...
                  </div>
                ) : "Submit Deliverable"}
              </button>
            </form>
          )}

          <div className="card-body" style={{ padding: "0 1.5rem 1.5rem 1.5rem" }}>
            <div className="card" style={{ marginTop: "1rem", borderLeft: "4px solid var(--maroon)" }}>
              <div className="card-content" style={{ padding: "1rem" }}>
                <p style={{ margin: 0, fontWeight: 700 }}>AI Summary</p>
                {currentSubmission?.aiSummary ? (
                  <p style={{ margin: "0.5rem 0 0 0", color: "#111827" }}>
                    {currentSubmission.aiSummary}
                  </p>
                ) : (
                  <p style={{ margin: "0.5rem 0 0 0", color: "#6b7280" }}>
                    AI analysis is pending or not available yet.
                  </p>
                )}
                {currentSubmission?.aiHighlights && (
                  <p style={{ margin: "0.5rem 0 0 0", color: "#6b7280", fontSize: "0.875rem" }}>
                    {currentSubmission.aiHighlights}
                  </p>
                )}
              </div>
            </div>

            <div className="card" style={{ marginTop: "1rem" }}>
              <div className="card-header">
                <h3 className="card-title">Feedback</h3>
              </div>
              <div className="card-body" style={{ padding: "1rem" }}>
                {!feedback ? (
                  <p style={{ margin: 0, color: "#6b7280" }}>
                    No feedback posted for the latest submission yet.
                  </p>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", gap: "0.75rem", flexWrap: "wrap" }}>
                      <div>
                        <p style={{ margin: 0, fontWeight: 700 }}>{feedback.evaluatorName}</p>
                        <p style={{ margin: "0.25rem 0 0 0", color: "#6b7280", fontSize: "0.75rem" }}>
                          {feedback.evaluatedAt ? new Date(feedback.evaluatedAt).toLocaleString() : "—"}
                        </p>
                      </div>
                      <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                        <span className={`badge ${
                          feedback.status === 'APPROVED' ? 'success' :
                          feedback.status === 'NEEDS_REVISION' ? 'danger' :
                          feedback.status === 'REJECTED' ? 'danger' : 'info'
                        }`}>
                          {String(feedback.status || "").replaceAll('_', ' ')}
                        </span>
                        {feedback.totalScore !== null && feedback.totalScore !== undefined && (
                          <span className="badge info">Score: {feedback.totalScore}</span>
                        )}
                      </div>
                    </div>
                    {feedback.generalComments && (
                      <p style={{ margin: 0, color: "#111827" }}>{feedback.generalComments}</p>
                    )}
                    {Array.isArray(feedback.criteria) && feedback.criteria.length > 0 && (
                      <div className="table-container">
                        <table className="data-table">
                          <thead>
                            <tr>
                              <th>Criterion</th>
                              <th>Score</th>
                              <th>Weight</th>
                              <th>Notes</th>
                            </tr>
                          </thead>
                          <tbody>
                            {feedback.criteria.map((c) => (
                              <tr key={c.criterionId}>
                                <td>{c.name}</td>
                                <td>{c.score}/{c.maxPoints}</td>
                                <td>{c.weight}%</td>
                                <td style={{ color: "#6b7280", fontSize: "0.75rem" }}>{c.comments}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-header" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Clock size={18} className="text-maroon" />
            <h2 className="card-title">History</h2>
          </div>
          <div className="card-body" style={{ padding: '1rem' }}>
            {history.length === 0 ? (
              <div className="text-center py-8">
                <Clock size={32} style={{ color: '#cbd5e1', margin: '0 auto 0.75rem' }} />
                <p style={{ fontSize: '0.875rem', color: '#64748b' }}>No previous submissions found.</p>
              </div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
                {history.map((h) => (
                  <div
                    key={h.id}
                    style={{
                      padding: "1rem",
                      border: "1px solid #e2e8f0",
                      borderRadius: "0.75rem",
                      background: "#fff"
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "flex-start",
                        marginBottom: "0.5rem",
                      }}
                    >
                      <div>
                        <span style={{ fontWeight: "700", fontSize: "0.875rem", color: '#1e293b' }}>
                          Version {h.versionNumber}
                        </span>
                        <p style={{ fontSize: "0.75rem", color: "#64748b", margin: "0.125rem 0 0 0" }}>
                          {new Date(h.submittedAt).toLocaleDateString(undefined, { 
                            month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' 
                          })}
                        </p>
                      </div>
                      <span className={`badge ${
                        h.status === 'APPROVED' ? 'success' : 
                        h.status === 'NEEDS_REVISION' ? 'danger' : 
                        h.status === 'SUBMITTED' ? 'info' : 'secondary'
                      }`}>
                        {h.status.replace('_', ' ')}
                      </span>
                    </div>
                    
                    {h.notes && (
                      <p style={{ fontSize: "0.8125rem", color: "#475569", margin: "0.5rem 0", fontStyle: 'italic' }}>
                        "{h.notes}"
                      </p>
                    )}

                    {h.aiSummary && (
                      <div style={{ marginTop: "0.5rem" }}>
                        <p style={{ margin: 0, fontWeight: 600, fontSize: "0.75rem", color: "#111827" }}>AI Summary</p>
                        <p style={{ margin: "0.25rem 0 0 0", color: "#6b7280", fontSize: "0.75rem" }}>
                          {h.aiSummary}
                        </p>
                      </div>
                    )}
                    
                    <button 
                      className="btn btn-secondary" 
                      style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem', width: '100%', marginTop: '0.5rem' }}
                      onClick={() => handleDownloadVersion(h.id, h.fileName)}
                    >
                      <FileText size={14} style={{ marginRight: '0.4rem' }} /> Download this version
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DocumentSubmission;
