import { useState, useEffect } from "react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";
import { Users, FileText, AlertCircle, CheckCircle2, Clock, Eye, Star, ChevronRight } from "lucide-react";
import { useNavigate } from "react-router-dom";

const AdviserGroups = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchGroups = async () => {
      if (!user?.id) return;
      try {
        setLoading(true);
        setError(null);
        const data = await apiService.getAdviserGroups(user.id);
        setGroups(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Failed to fetch adviser groups", err);
        setError("Failed to load groups.");
      } finally {
        setLoading(false);
      }
    };

    fetchGroups();
  }, [user]);

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
        Loading groups...
      </div>
    );
  }

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">My Groups</h1>
        <p className="page-description">
          Manage and monitor your assigned capstone groups.
        </p>
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

      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fill, minmax(500px, 1fr))', 
        gap: '1.5rem' 
      }}>
        {groups.map((group) => {
          const completedDeliverables = 0;
          const totalDeliverables = 0;
          const pendingDeliverables = 0;
          const overdueDeliverables = 0;
          const submissionPercentage = totalDeliverables > 0 
            ? Math.round((completedDeliverables / totalDeliverables) * 100) 
            : 0;

          return (
            <div 
              key={group.id} 
              className="card"
              style={{ 
                cursor: 'pointer',
                transition: 'transform 0.2s, box-shadow 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-2px)';
                e.currentTarget.style.boxShadow = '0 10px 25px -5px rgba(0, 0, 0, 0.1)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)';
                e.currentTarget.style.boxShadow = '0 4px 6px -1px rgba(0, 0, 0, 0.1)';
              }}
              onClick={() => navigate(`/adviser/groups/${group.id}`)}
            >
              <div className="card-header" style={{ paddingBottom: '0.75rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <h3 className="card-title" style={{ marginBottom: '0.25rem' }}>
                      {group.code} - {group.title}
                    </h3>
                    <p style={{ color: '#6b7280', fontSize: '0.875rem', margin: 0 }}>
                      Adviser: {group.adviserName || 'Unassigned'}
                    </p>
                  </div>
                  <ChevronRight size={20} style={{ color: '#9ca3af' }} />
                </div>
              </div>
              <div className="card-content">
                <div style={{ marginBottom: '1rem' }}>
                  <div style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    gap: '0.5rem', 
                    marginBottom: '0.5rem' 
                  }}>
                    <Users size={16} style={{ color: '#4f46e5' }} />
                    <span style={{ fontWeight: 500, color: '#374151' }}>
                      Members ({group.students?.length || 0})
                    </span>
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {group.students?.map((student) => (
                      <span 
                        key={student.id}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.375rem',
                          padding: '0.375rem 0.75rem',
                          backgroundColor: student.userId ? '#ecfdf5' : '#fffbeb',
                          color: student.userId ? '#166534' : '#92400e',
                          borderRadius: '0.375rem',
                          fontSize: '0.8125rem',
                          border: '1px solid',
                          borderColor: student.userId ? '#86efac' : '#fde68a'
                        }}
                      >
                        {student.fullName}
                        {!student.userId && (
                          <span style={{ fontSize: '0.625rem' }}>(Not linked)</span>
                        )}
                      </span>
                    ))}
                    {(!group.students || group.students.length === 0) && (
                      <span style={{ color: '#9ca3af', fontSize: '0.875rem' }}>
                        No members
                      </span>
                    )}
                  </div>
                </div>

                <div style={{ marginBottom: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                    <span style={{ fontWeight: 500, color: '#374151', fontSize: '0.875rem' }}>
                      Progress
                    </span>
                    <span style={{ fontWeight: 600, color: '#4f46e5', fontSize: '0.875rem' }}>
                      {submissionPercentage}%
                    </span>
                  </div>
                  <div style={{ 
                    height: '0.5rem', 
                    backgroundColor: '#e5e7eb', 
                    borderRadius: '9999px',
                    overflow: 'hidden'
                  }}>
                    <div 
                      style={{
                        height: '100%',
                        width: `${submissionPercentage}%`,
                        backgroundColor: submissionPercentage >= 70 ? '#10b981' : submissionPercentage >= 40 ? '#f59e0b' : '#ef4444',
                        transition: 'width 0.3s ease'
                      }}
                    />
                  </div>
                </div>

                <div style={{ 
                  display: 'grid', 
                  gridTemplateColumns: 'repeat(3, 1fr)', 
                  gap: '0.75rem',
                  paddingTop: '0.75rem',
                  borderTop: '1px solid #e5e7eb'
                }}>
                  <div style={{ 
                    padding: '0.75rem', 
                    backgroundColor: '#f0fdf4', 
                    borderRadius: '0.375rem',
                    textAlign: 'center'
                  }}>
                    <CheckCircle2 size={20} style={{ color: '#10b981', margin: '0 auto 0.25rem' }} />
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#166534' }}>
                      {completedDeliverables}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                      Completed
                    </div>
                  </div>
                  <div style={{ 
                    padding: '0.75rem', 
                    backgroundColor: '#fefce8', 
                    borderRadius: '0.375rem',
                    textAlign: 'center'
                  }}>
                    <Clock size={20} style={{ color: '#f59e0b', margin: '0 auto 0.25rem' }} />
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#92400e' }}>
                      {pendingDeliverables}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                      Pending
                    </div>
                  </div>
                  <div style={{ 
                    padding: '0.75rem', 
                    backgroundColor: '#fef2f2', 
                    borderRadius: '0.375rem',
                    textAlign: 'center'
                  }}>
                    <AlertCircle size={20} style={{ color: '#ef4444', margin: '0 auto 0.25rem' }} />
                    <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#b91c1c' }}>
                      {overdueDeliverables}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                      Overdue
                    </div>
                  </div>
                </div>
              </div>
            </div>
          );
        })}

        {groups.length === 0 && (
          <div style={{ 
            gridColumn: '1 / -1',
            textAlign: 'center',
            padding: '4rem 2rem',
            color: '#6b7280'
          }}>
            <Users size={48} style={{ margin: '0 auto 1rem', opacity: 0.3 }} />
            <h3 style={{ fontSize: '1.125rem', fontWeight: 600, color: '#374151', marginBottom: '0.5rem' }}>
              No groups assigned
            </h3>
            <p style={{ fontSize: '0.875rem' }}>
              You haven't been assigned to any capstone groups yet.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdviserGroups;
