import React, { useState, useEffect } from 'react';
import UserProfile from '../components/UserProfile';
import '../styles/Dashboard.css';

const AdviserDashboard = () => {
  const [user, setUser] = useState(null);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [stats, setStats] = useState(null);
  const [assignedStudents, setAssignedStudents] = useState([]);

  useEffect(() => {
    const userData = localStorage.getItem('user');
    if (userData) {
      setUser(JSON.parse(userData));
    }
    const token = localStorage.getItem('token');
    if (userData && token) {
      const uid = JSON.parse(userData).id;
      fetch(`http://localhost:8080/api/dashboard/adviser/${uid}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
        .then(res => res.json())
        .then(data => {
          setStats({
            assignedCount: data.assignedCount,
            activeSubmissions: data.activeSubmissions,
            reviewed: data.reviewed,
            pendingReview: data.pendingReview
          });
          setAssignedStudents(data.assignedStudents || []);
        })
        .catch(err => console.error('Failed to load adviser dashboard:', err));
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('user');
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    window.location.href = '/';
  };

  return (
    <div className="dashboard">
      <div className="sidebar">
        <div className="sidebar-header">
          <h2>IntelliTrack</h2>
          <p>Adviser Portal</p>
        </div>

        <nav className="sidebar-nav">
          <button
            className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            <span className="nav-icon">📊</span>
            Dashboard
          </button>

          <button
            className={`nav-item ${activeTab === 'students' ? 'active' : ''}`}
            onClick={() => setActiveTab('students')}
          >
            <span className="nav-icon">👥</span>
            Assigned Students
          </button>

          <button
            className={`nav-item ${activeTab === 'submissions' ? 'active' : ''}`}
            onClick={() => setActiveTab('submissions')}
          >
            <span className="nav-icon">📋</span>
            Submissions
          </button>

          <button
            className={`nav-item ${activeTab === 'profile' ? 'active' : ''}`}
            onClick={() => setActiveTab('profile')}
          >
            <span className="nav-icon">👤</span>
            Profile
          </button>
        </nav>

        <div className="sidebar-footer">
          <button className="logout-btn" onClick={handleLogout}>
            <span className="nav-icon">🚪</span>
            Logout
          </button>
        </div>
      </div>

      <div className="main-content">
        <header className="dashboard-header">
          <h1>Welcome back, {user?.firstName}!</h1>
          <div className="user-info">
            <span className="role-badge" style={{ backgroundColor: '#6366f1' }}>Adviser</span>
            <span className="user-email">{user?.email}</span>
          </div>
        </header>

        <main className="dashboard-content">
          {activeTab === 'dashboard' && (
            <div className="dashboard-overview">
              <div className="stats-grid">
                <div className="stat-card">
                  <div className="stat-icon">👥</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.assignedCount : '—'}</h3>
                    <p>Assigned Students</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">📋</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.activeSubmissions : '—'}</h3>
                    <p>Active Submissions</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">✅</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.reviewed : '—'}</h3>
                    <p>Reviewed</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">📝</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.pendingReview : '—'}</h3>
                    <p>Pending Review</p>
                  </div>
                </div>
              </div>

              <div className="recent-activity">
                <h2>Recent Submissions</h2>
                <div className="activity-list">
                  <div className="activity-item">
                    <div className="activity-icon">📤</div>
                    <div className="activity-content">
                      <p><strong>Joshua Omondi</strong> - Submitted Project Proposal</p>
                      <span className="activity-time">2 hours ago</span>
                    </div>
                  </div>

                  <div className="activity-item">
                    <div className="activity-icon">📤</div>
                    <div className="activity-content">
                      <p><strong>Sarah Johnson</strong> - Submitted Milestone 1</p>
                      <span className="activity-time">5 hours ago</span>
                    </div>
                  </div>

                  <div className="activity-item">
                    <div className="activity-icon">💬</div>
                    <div className="activity-content">
                      <p>You reviewed <strong>Mike Chen</strong>'s Final Report</p>
                      <span className="activity-time">1 day ago</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'students' && (
            <div className="section">
              <h2>Assigned Students</h2>
              <div className="students-list">
                {assignedStudents.map((student, idx) => (
                  <div className="student-card" key={idx}>
                    <h3>{student.firstName} {student.lastName}</h3>
                    <p>{student.email}</p>
                    <div className="student-stats">
                      <span>✅ {/* Placeholder */} 0 Submitted</span>
                      <span>⏳ {/* Placeholder */} 0 Pending</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {activeTab === 'submissions' && (
            <div className="section">
              <h2>Submissions Review Queue</h2>
              <p>View and manage student submissions for review.</p>
            </div>
          )}

          {activeTab === 'profile' && (
            <div className="profile-section">
              <h2>My Profile</h2>
              <UserProfile userId={user?.id} />
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default AdviserDashboard;
