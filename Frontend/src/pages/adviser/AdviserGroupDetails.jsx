import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";
import { 
  Users, 
  FileText, 
  Clock, 
  Star, 
  Calendar, 
  ArrowLeft, 
  CheckCircle2, 
  AlertCircle,
  Eye,
  MessageSquare,
  Activity
} from "lucide-react";

const AdviserGroupDetails = () => {
  const { groupId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [group, setGroup] = useState(null);
  const [activeTab, setActiveTab] = useState("overview");
  const [feedbackHistory, setFeedbackHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      if (!groupId) return;
      try {
        setLoading(true);
        setError(null);
        const [groupData, feedbackData] = await Promise.all([
          apiService.getAdviserGroupDetails(groupId),
          apiService.getFeedbackHistoryForGroup(groupId)
        ]);
        setGroup(groupData);
        setFeedbackHistory(Array.isArray(feedbackData) ? feedbackData : []);
      } catch (err) {
        console.error("Failed to fetch group details", err);
        setError("Failed to load group details.");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [groupId]);

  if (loading) {
    return (
      <div className="fade-in" style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        minHeight: '400px',
        fontSize: '1.125rem',
        color: '#6b7280'
      }}>
        Loading group details...
      </div>
    );
  }

  if (error || !group) {
    return (
      <div className="fade-in" style={{ padding: '2rem' }}>
        <button 
          className="btn btn-secondary btn-sm"
          onClick={() => navigate('/adviser/groups')}
          style={{ marginBottom: '1rem' }}
        >
          <ArrowLeft size={16} /> Back to Groups
        </button>
        <div style={{ 
          padding: '2rem', 
          backgroundColor: '#fef2f2', 
          border: '1px solid #fecaca', 
          borderRadius: '0.5rem',
          textAlign: 'center'
        }}>
          <AlertCircle size={48} style={{ color: '#ef4444', margin: '0 auto 1rem' }} />
          <h3 style={{ color: '#b91c1c', marginBottom: '0.5rem' }}>Failed to Load</h3>
          <p style={{ color: '#7f1d1d' }}>{error || 'Group not found'}</p>
        </div>
      </div>
    );
  }

  const tabs = [
    { id: 'overview', name: 'Overview', icon: Star },
    { id: 'members', name: 'Members', icon: Users },
    { id: 'deliverables', name: 'Deliverables', icon: FileText },
    { id: 'feedback', name: 'Review History', icon: MessageSquare },
    { id: 'insights', name: 'AI Insights', icon: Star },
    { id: 'timeline', name: 'Timeline', icon: Calendar },
  ];

  const statusTone = (status) => {
    const tones = {
      APPROVED: 'success',
      SUBMITTED: 'info',
      UPDATED: 'info',
      NEEDS_REVISION: 'warning',
      REJECTED: 'danger',
      LATE: 'danger',
      PENDING: 'secondary'
    };
    return tones[status] || 'secondary';
  };

  return (
    <div className="fade-in">
      <div style={{ marginBottom: '1.5rem' }}>
        <button 
          className="btn btn-secondary btn-sm"
          onClick={() => navigate('/adviser/groups')}
          style={{ marginBottom: '1rem' }}
        >
          <ArrowLeft size={16} /> Back to Groups
        </button>
        <div className="page-header" style={{ margin: 0, padding: 0 }}>
          <h1 className="page-title">{group.code} - {group.title}</h1>
          <p className="page-description">
            Capstone group supervision workspace
          </p>
        </div>
      </div>

      {error && (
        <div style={{ 
          marginBottom: "1rem", 
          padding: "0.75rem 1rem", 
          backgroundColor: "#fef2f2", 
          border: "1px solid #fecaca", 
          borderRadius: "0.375rem",
          color: "#b91c1c",
          display: "flex",
          alignItems: "center",
          gap: "0.5rem"
        }}>
          <AlertCircle size={18} />
          {error}
        </div>
      )}

      <div className="card">
        <div style={{ 
          display: 'flex', 
          borderBottom: '1px solid #e5e7eb',
          marginBottom: '1.5rem'
        }}>
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                style={{
                  padding: '0.75rem 1.25rem',
                  border: 'none',
                  backgroundColor: activeTab === tab.id ? '#f9fafb' : 'transparent',
                  color: activeTab === tab.id ? '#7f1d1d' : '#6b7280',
                  fontWeight: activeTab === tab.id ? 600 : 400,
                  cursor: 'pointer',
                  borderBottom: activeTab === tab.id ? '2px solid #7f1d1d' : '2px solid transparent',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  transition: 'all 0.15s'
                }}
              >
                <Icon size={16} />
                {tab.name}
              </button>
            );
          })}
        </div>

        <div className="card-content">
          {activeTab === 'overview' && (
            <div>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1.5rem' }}>
                Group Overview
              </h3>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
                <div>
                  <div style={{ marginBottom: '1.5rem' }}>
                    <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: '#374151', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                      Group Information
                    </h4>
                    <div style={{ backgroundColor: '#f9fafb', padding: '1rem', borderRadius: '0.5rem' }}>
                      <div style={{ marginBottom: '0.75rem' }}>
                        <span style={{ color: '#6b7280', fontSize: '0.8125rem' }}>Code:</span>
                        <span style={{ marginLeft: '0.5rem', fontWeight: 600 }}>{group.code}</span>
                      </div>
                      <div style={{ marginBottom: '0.75rem' }}>
                        <span style={{ color: '#6b7280', fontSize: '0.8125rem' }}>Title:</span>
                        <span style={{ marginLeft: '0.5rem', fontWeight: 600 }}>{group.title}</span>
                      </div>
                      <div style={{ marginBottom: '0.75rem' }}>
                        <span style={{ color: '#6b7280', fontSize: '0.8125rem' }}>Adviser:</span>
                        <span style={{ marginLeft: '0.5rem' }}>{group.adviserName || 'Unassigned'}</span>
                      </div>
                      <div>
                        <span style={{ color: '#6b7280', fontSize: '0.8125rem' }}>Created:</span>
                        <span style={{ marginLeft: '0.5rem' }}>
                          {group.createdAt ? new Date(group.createdAt).toLocaleDateString() : 'N/A'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div>
                  <h4 style={{ fontSize: '0.875rem', fontWeight: 600, color: '#374151', marginBottom: '0.5rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    Progress Summary
                  </h4>
                  <div style={{ backgroundColor: '#f9fafb', padding: '1rem', borderRadius: '0.5rem' }}>
                    <div style={{ marginBottom: '1rem' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                        <span style={{ color: '#374151' }}>Overall Progress</span>
                        <span style={{ fontWeight: 700 }}>0%</span>
                      </div>
                      <div style={{ height: '0.75rem', backgroundColor: '#e5e7eb', borderRadius: '9999px', overflow: 'hidden' }}>
                        <div style={{ width: '0%', height: '100%', backgroundColor: '#10b981', transition: 'width 0.3s' }} />
                      </div>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
                      <div style={{ textAlign: 'center', padding: '0.75rem', backgroundColor: '#ecfdf5', borderRadius: '0.375rem' }}>
                        <CheckCircle2 size={20} style={{ color: '#10b981', margin: '0 auto 0.25rem' }} />
                        <div style={{ fontWeight: 700, color: '#166534', fontSize: '1.25rem' }}>0</div>
                        <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>Completed</div>
                      </div>
                      <div style={{ textAlign: 'center', padding: '0.75rem', backgroundColor: '#fefce8', borderRadius: '0.375rem' }}>
                        <Clock size={20} style={{ color: '#f59e0b', margin: '0 auto 0.25rem' }} />
                        <div style={{ fontWeight: 700, color: '#92400e', fontSize: '1.25rem' }}>0</div>
                        <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>Pending</div>
                      </div>
                      <div style={{ textAlign: 'center', padding: '0.75rem', backgroundColor: '#fef2f2', borderRadius: '0.375rem' }}>
                        <AlertCircle size={20} style={{ color: '#ef4444', margin: '0 auto 0.25rem' }} />
                        <div style={{ fontWeight: 700, color: '#b91c1c', fontSize: '1.25rem' }}>0</div>
                        <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>Overdue</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'members' && (
            <div>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1.5rem' }}>
                Group Members
              </h3>
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Student Name</th>
                      <th>Student ID</th>
                      <th>Email</th>
                      <th>Section</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {group.students?.map((student) => (
                      <tr key={student.id}>
                        <td style={{ fontWeight: 500 }}>{student.fullName}</td>
                        <td>{student.studentId}</td>
                        <td>{student.email}</td>
                        <td>{student.sectionName}</td>
                        <td>
                          <span style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.375rem',
                            padding: '0.25rem 0.75rem',
                            backgroundColor: student.userId ? '#ecfdf5' : '#fffbeb',
                            color: student.userId ? '#166534' : '#92400e',
                            borderRadius: '9999px',
                            fontSize: '0.8125rem',
                            fontWeight: 500
                          }}>
                            {student.userId ? <CheckCircle2 size={14} /> : <AlertCircle size={14} />}
                            {student.userId ? 'Linked' : 'Not Linked'}
                          </span>
                        </td>
                      </tr>
                    ))}
                    {(!group.students || group.students.length === 0) && (
                      <tr>
                        <td colSpan="5" style={{ textAlign: 'center', padding: '3rem', color: '#6b7280' }}>
                          <Users size={40} style={{ margin: '0 auto 1rem', opacity: 0.3 }} />
                          <div>No members in this group</div>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'deliverables' && (
            <div>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1.5rem' }}>
                Deliverables
              </h3>
              <div style={{ textAlign: 'center', padding: '3rem', color: '#6b7280' }}>
                <FileText size={40} style={{ margin: '0 auto 1rem', opacity: 0.3 }} />
                <div>No deliverables assigned yet</div>
              </div>
            </div>
          )}

          {activeTab === 'feedback' && (
            <div>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1.5rem' }}>
                Review History
              </h3>
              {feedbackHistory.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '3rem', color: '#6b7280' }}>
                  <MessageSquare size={40} style={{ margin: '0 auto 1rem', opacity: 0.3 }} />
                  <div>No reviews yet</div>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  {feedbackHistory.map((feedback, idx) => (
                    <div 
                      key={`${feedback.submissionId}-${idx}`}
                      className="card"
                      style={{ margin: 0 }}
                    >
                      <div className="card-header">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                          <div>
                            <h4 style={{ margin: 0, fontWeight: 600 }}>
                              Evaluation #{idx + 1}
                            </h4>
                            <p style={{ margin: '0.25rem 0 0 0', color: '#6b7280', fontSize: '0.875rem' }}>
                              By {feedback.evaluatorName} • {feedback.evaluatedAt ? new Date(feedback.evaluatedAt).toLocaleString() : '—'}
                            </p>
                          </div>
                          {feedback.status && (
                            <span className={`badge ${statusTone(feedback.status)}`}>
                              {feedback.status}
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="card-content">
                        {feedback.totalScore !== null && feedback.totalScore !== undefined && (
                          <div style={{ marginBottom: '1rem', paddingBottom: '1rem', borderBottom: '1px solid #e5e7eb' }}>
                            <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: 0, textTransform: 'uppercase', fontWeight: 600, letterSpacing: '0.05em' }}>
                              Total Score
                            </p>
                            <p style={{ fontSize: '2rem', fontWeight: 700, color: '#7f1d1d', margin: 0 }}>
                              {feedback.totalScore}
                            </p>
                          </div>
                        )}
                        {feedback.generalComments && (
                          <div style={{ marginBottom: '1rem' }}>
                            <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: 0, textTransform: 'uppercase', fontWeight: 600, letterSpacing: '0.05em', marginBottom: '0.5rem' }}>
                              General Comments
                            </p>
                            <p style={{ color: '#111827', margin: 0, whiteSpace: 'pre-wrap' }}>
                              {feedback.generalComments}
                            </p>
                          </div>
                        )}
                        {feedback.criteria && feedback.criteria.length > 0 && (
                          <div>
                            <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: 0, textTransform: 'uppercase', fontWeight: 600, letterSpacing: '0.05em', marginBottom: '0.75rem' }}>
                              Criteria
                            </p>
                            <div className="table-container">
                              <table className="data-table">
                                <thead>
                                  <tr>
                                    <th>Criterion</th>
                                    <th>Score</th>
                                    <th>Weight</th>
                                    <th>Comments</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {feedback.criteria.map((criterion, cIdx) => (
                                    <tr key={criterion.criterionId || cIdx}>
                                      <td style={{ fontWeight: 500 }}>{criterion.name}</td>
                                      <td>
                                        <span style={{ fontWeight: 700, color: '#7f1d1d' }}>
                                          {criterion.score}/{criterion.maxPoints}
                                        </span>
                                      </td>
                                      <td>
                                        <span className="badge info">{criterion.weight}%</span>
                                      </td>
                                      <td>{criterion.comments || '—'}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'insights' && (
            <div>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1.5rem' }}>
                AI Insights
              </h3>
              <div style={{ textAlign: 'center', padding: '3rem', color: '#6b7280' }}>
                <Star size={40} style={{ margin: '0 auto 1rem', opacity: 0.3 }} />
                <div>No AI insights available yet</div>
              </div>
            </div>
          )}

          {activeTab === 'timeline' && (
            <div>
              <h3 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1.5rem' }}>
                Activity Timeline
              </h3>
              <div style={{ textAlign: 'center', padding: '3rem', color: '#6b7280' }}>
                <Calendar size={40} style={{ margin: '0 auto 1rem', opacity: 0.3 }} />
                <div>No timeline activity yet</div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdviserGroupDetails;
