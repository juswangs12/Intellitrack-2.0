import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FileText, CheckCircle, Clock, AlertCircle, ChevronLeft, ExternalLink } from 'lucide-react';
import apiService from '../../services/ApiService';
import { useAuth } from '../../context/AuthContext';
import CommentSection from '../../components/CommentSection';

const DocumentSubmissions = () => {
  const { deliverableId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  const [workspace, setWorkspace] = useState(null);
  const [deliverable, setDeliverable] = useState(null);
  const [currentSubmission, setCurrentSubmission] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [googleDocUrl, setGoogleDocUrl] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const isMounted = useRef(true);

  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    
    const resetState = () => {
      setCurrentSubmission(null);
      setFeedback(null);
      setGoogleDocUrl('');
      setNotes('');
      setError(null);
      setIsEditing(false);
      setLoading(true);
    };

    const fetchData = async () => {
      console.log('=== fetchData CALLED ===');
      console.log('deliverableId (param):', deliverableId);
      console.log('deliverableId (parsed):', parseInt(deliverableId));
      
      if (!deliverableId || !user?.id) {
        setLoading(false);
        return;
      }
      
      try {
        // Fetch student workspace first (to get groupId and deliverables as source of truth)
        const ws = await apiService.getStudentWorkspace();
        if (!active) return;
        console.log('Student Workspace:', ws);
        setWorkspace(ws);
        
        const current = (ws?.deliverables || []).find(d => d.deliverableId === parseInt(deliverableId));
        console.log('Current Deliverable from Workspace:', current);
        if (current) {
          setDeliverable({
            id: current.deliverableId,
            name: current.deliverableName,
            stage: current.stage,
          });
        } else {
          setDeliverable(null);
        }
        
        const groupId = ws?.header?.groupId || user?.groupId;
        console.log('Group ID:', groupId);
        
        if (groupId) {
          const submissions = await apiService.getSubmissionsByGroup(groupId);
          console.log('All Submissions for Group:', submissions);
          
          if (!active) return;
          
          // Filter EXACTLY by deliverableId
          const parsedDeliverableId = parseInt(deliverableId, 10);
          const submission = (submissions || []).find(
            (s) => {
              console.log(`Checking submission: id=${s.id}, deliverableId=${s.deliverableId} (looking for ${parsedDeliverableId})`);
              return Number(s.deliverableId) === parsedDeliverableId;
            },
          );
          console.log('Selected Submission:', submission);
          
          setCurrentSubmission(submission || null);
          setGoogleDocUrl(submission?.googleDocUrl || "");
          setNotes(submission?.notes || "");

          if (submission?.id) {
            const fb = await apiService.getStudentSubmissionFeedback(submission.id);
            if (active) {
              setFeedback(fb || null);
            }
          } else {
            setFeedback(null);
          }
        } else {
          setCurrentSubmission(null);
          setFeedback(null);
          setGoogleDocUrl("");
          setNotes("");
        }
      } catch (err) {
        if (active) {
          console.error("Failed to fetch submission data", err);
          setError("Failed to load submission details. Please try again.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    resetState();
    fetchData();

    return () => {
      active = false;
    };
  }, [deliverableId, user?.id]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const groupId = workspace?.header?.groupId || user?.groupId;
    if (!googleDocUrl || !user?.id || !groupId) {
      setError("Google Doc URL is required.");
      return;
    }
    
    setSubmitting(true);
    setError(null);
    
    try {
      await apiService.submitGoogleDoc({
        googleDocUrl,
        groupId,
        deliverableId: parseInt(deliverableId, 10),
        notes,
      });

      setIsEditing(false);
      // Refresh data after submitting
      const ws = await apiService.getStudentWorkspace();
      setWorkspace(ws);
      const submissions = await apiService.getSubmissionsByGroup(groupId);
      
      // Filter EXACTLY again
      const parsedDeliverableId = parseInt(deliverableId, 10);
      const submission = (submissions || []).find(
        (s) => Number(s.deliverableId) === parsedDeliverableId,
      );
      console.log('After submit, selected submission:', submission);
      setCurrentSubmission(submission || null);
      setGoogleDocUrl(submission?.googleDocUrl || "");
      setNotes(submission?.notes || "");
      if (submission?.id) {
        const fb = await apiService.getStudentSubmissionFeedback(submission.id);
        setFeedback(fb || null);
      } else {
        setFeedback(null);
      }
    } catch (err) {
      console.error("Submission failed", err);
      setError(err.message || "Failed to submit document. Please try again.");
    } finally {
      setSubmitting(false);
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

      <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '1.5rem' }}>
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">
              {isEditing ? "Edit Your Submission" : "Submit Your Document"}
            </h2>
          </div>

          {currentSubmission && Number(currentSubmission.deliverableId) === Number(deliverableId) && (
            <div className="card-body" style={{ padding: '1rem 1.5rem 0 1.5rem' }}>
              <div style={{ border: '1px solid #e5e7eb', borderRadius: '0.75rem', padding: '1rem', marginBottom: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                  <div>
                    <p style={{ margin: 0, fontWeight: 700 }}>Current Submission</p>
                    {currentSubmission.lastModifiedAt && (
                      <p style={{ margin: '0.25rem 0 0 0', color: '#6b7280', fontSize: '0.75rem' }}>
                        Last updated: {new Date(currentSubmission.lastModifiedAt).toLocaleString()}
                      </p>
                    )}
                    {currentSubmission.lastAnalyzedAt && (
                      <p style={{ margin: '0.25rem 0 0 0', color: '#6b7280', fontSize: '0.75rem' }}>
                        Last analyzed: {new Date(currentSubmission.lastAnalyzedAt).toLocaleString()}
                      </p>
                    )}
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                    <span className={`badge ${
                      currentSubmission.status === 'SUBMITTED_ON_TIME' ? 'success' :
                      currentSubmission.status === 'SUBMITTED_LATE' ? 'danger' :
                      currentSubmission.status === 'NOT_SUBMITTED' ? 'danger' :
                      currentSubmission.status === 'COMMENTED' ? 'info' : 'secondary'
                    }`}>
                      {String(currentSubmission.status || 'PENDING').replaceAll('_', ' ')}
                    </span>
                    {currentSubmission.googleDocUrl && (
                      <a 
                        href={currentSubmission.googleDocUrl} 
                        target="_blank" 
                        rel="noopener noreferrer"
                        className="btn btn-secondary"
                        style={{ padding: '0.25rem 0.5rem', display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                      >
                        <ExternalLink size={14} /> Open Document
                      </a>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}
          
          {currentSubmission && Number(currentSubmission.deliverableId) === Number(deliverableId) && !isEditing ? (
            <div className="card-body text-center py-8">
              <CheckCircle style={{ width: '4rem', height: '4rem', color: '#10b981', margin: '0 auto 1.5rem', display: 'block' }} />
              <h3 style={{ color: '#10b981', marginBottom: '0.75rem', fontSize: '1.25rem', fontWeight: 600 }}>
                Submission Saved!
              </h3>
              <p style={{ color: '#4b5563', marginBottom: '1.5rem' }}>
                Your Google Doc has been saved. You can continue editing it anytime.
              </p>
              <button className="btn btn-primary" onClick={() => setIsEditing(true)}>
                Make Changes
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="card-body" style={{ padding: '1.5rem' }}>
              <div className="form-group mb-6">
                <label className="form-label" style={{ fontWeight: 600 }}>Google Doc URL</label>
                <input 
                  type="url" 
                  className="form-control" 
                  value={googleDocUrl} 
                  onChange={(e) => setGoogleDocUrl(e.target.value)}
                  placeholder="https://docs.google.com/document/d/..."
                  required
                />
                <p style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: '0.5rem' }}>
                  Paste your Google Docs or Google Drive file URL
                </p>
              </div>
              
              <div className="form-group mb-6">
                <label className="form-label" style={{ fontWeight: 600 }}>Notes (Optional)</label>
                <textarea 
                  className="form-control" 
                  rows={4} 
                  placeholder="Add any notes about your submission"
                  value={notes} 
                  onChange={(e) => setNotes(e.target.value)} 
                  style={{ minHeight: '100px' }}
                />
              </div>
              
              <button 
                type="submit" 
                className="btn btn-primary w-full" 
                disabled={!googleDocUrl || submitting} 
                style={{ width: '100%', padding: '0.75rem' }}
              >
                {submitting ? (
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                    <div className="animate-spin" style={{ width: '20px', height: '20px', border: '2px solid', borderRadius: '50%', borderTopColor: 'transparent' }}></div>
                    Saving...
                  </div>
                ) : currentSubmission ? "Update Submission" : "Save Submission"}
              </button>
            </form>
          )}
        </div>

        <div className="card-body" style={{ padding: '0 1.5rem 1.5rem 1.5rem' }}>
          <div className="card" style={{ marginTop: '1rem', borderLeft: '4px solid var(--maroon)' }}>
            <div className="card-content" style={{ padding: '1rem' }}>
              <p style={{ margin: 0, fontWeight: 700 }}>AI Summary</p>
              {currentSubmission && Number(currentSubmission.deliverableId) === Number(deliverableId) && currentSubmission?.aiSummary ? (
                <p style={{ margin: '0.5rem 0 0 0', color: '#111827' }}>
                  {currentSubmission.aiSummary}
                </p>
              ) : (
                <p style={{ margin: '0.5rem 0 0 0', color: '#6b7280' }}>
                  AI analysis is pending or not available yet.
                </p>
              )}
              {currentSubmission && Number(currentSubmission.deliverableId) === Number(deliverableId) && currentSubmission?.aiHighlights && (
                <p style={{ margin: '0.5rem 0 0 0', color: '#6b7280', fontSize: '0.875rem' }}>
                  {currentSubmission.aiHighlights}
                </p>
              )}
            </div>
          </div>

          <div className="card" style={{ marginTop: '1rem' }}>
            <div className="card-header">
              <h3 className="card-title">Feedback</h3>
            </div>
            <div className="card-body" style={{ padding: '1rem' }}>
              {!currentSubmission || Number(currentSubmission.deliverableId) !== Number(deliverableId) ? (
                <p style={{ margin: 0, color: '#6b7280' }}>
                  No submission yet for this deliverable.
                </p>
              ) : !feedback ? (
                <p style={{ margin: 0, color: '#6b7280' }}>
                  No feedback posted yet.
                </p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.75rem', flexWrap: 'wrap' }}>
                    <div>
                      <p style={{ margin: 0, fontWeight: 700 }}>{feedback.evaluatorName}</p>
                      <p style={{ margin: '0.25rem 0 0 0', color: '#6b7280', fontSize: '0.75rem' }}>
                        {feedback.evaluatedAt ? new Date(feedback.evaluatedAt).toLocaleString() : '—'}
                      </p>
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <span className={`badge ${
                        feedback.status === 'SUBMITTED_ON_TIME' ? 'success' :
                        feedback.status === 'SUBMITTED_LATE' ? 'danger' :
                        feedback.status === 'NOT_SUBMITTED' ? 'danger' :
                        feedback.status === 'COMMENTED' ? 'info' : 'secondary'
                      }`}>
                        {String(feedback.status || '').replaceAll('_', ' ')}
                      </span>
                      {feedback.totalScore !== null && feedback.totalScore !== undefined && (
                        <span className="badge info">Score: {feedback.totalScore}</span>
                      )}
                    </div>
                  </div>
                  {feedback.generalComments && (
                    <p style={{ margin: 0, color: '#111827' }}>{feedback.generalComments}</p>
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
                              <td style={{ color: '#6b7280', fontSize: '0.75rem' }}>{c.notes}</td>
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
          <h2 className="card-title">Deadline Info</h2>
        </div>
        <div className="card-body" style={{ padding: '1rem' }}>
          {(() => {
            const current = (workspace?.deliverables || []).find(d => d.deliverableId === parseInt(deliverableId));
            if (current?.dueAt) {
              const dueDate = new Date(current.dueAt);
              const now = new Date();
              const diffMs = dueDate - now;
              const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
              
              let status;
              let badgeClass;
              
              if (currentSubmission?.status === 'SUBMITTED_ON_TIME') {
                status = 'Submitted on time';
                badgeClass = 'success';
              } else if (currentSubmission?.status === 'SUBMITTED_LATE') {
                status = 'Submitted late';
                badgeClass = 'danger';
              } else if (diffDays < 0) {
                status = 'Overdue by ' + Math.abs(diffDays) + ' days';
                badgeClass = 'danger';
              } else if (diffDays === 0) {
                status = 'Due today';
                badgeClass = 'warning';
              } else if (diffDays <= 3) {
                status = 'Due in ' + diffDays + ' days';
                badgeClass = 'warning';
              } else {
                status = 'Due in ' + diffDays + ' days';
                badgeClass = 'info';
              }

              return (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <p style={{ margin: 0, fontWeight: 600 }}>Deadline:</p>
                    <p style={{ margin: 0 }}>{dueDate.toLocaleString()}</p>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <span className={`badge ${badgeClass}`}>{status}</span>
                  </div>
                </div>
              );
            }
            return (
              <div className="text-center py-8">
                <Clock size={32} style={{ color: '#cbd5e1', margin: '0 auto 0.75rem' }} />
                <p style={{ fontSize: '0.875rem', color: '#64748b' }}>No deadline set for this deliverable.</p>
              </div>
            );
          })()}
        </div>
      </div>
      
      {/* Comment Section */}
      {currentSubmission && currentSubmission?.id && Number(currentSubmission.deliverableId) === Number(deliverableId) && (
        <div style={{ marginTop: '1.5rem' }}>
          <CommentSection submissionId={currentSubmission.id} />
        </div>
      )}
    </div>
  );
};

export default DocumentSubmissions;