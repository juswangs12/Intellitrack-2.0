import React, { useState, useEffect } from 'react';
import UserProfile from '../components/UserProfile';
import '../styles/Dashboard.css';

const CoordinatorDashboard = () => {
  const [user, setUser] = useState(null);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [stats, setStats] = useState(null);

  useEffect(() => {
    const userData = localStorage.getItem('user');
    if (userData) {
      setUser(JSON.parse(userData));
    }
    const token = localStorage.getItem('token');
    if (userData && token) {
      const uid = JSON.parse(userData).id;
      fetch(`http://localhost:8080/api/dashboard/coordinator/${uid}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
        .then(res => res.json())
        .then(data => setStats(data))
        .catch(err => console.error('Failed to load coordinator dashboard:', err));
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
          <p>Coordinator Portal</p>
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
            className={`nav-item ${activeTab === 'analytics' ? 'active' : ''}`}
            onClick={() => setActiveTab('analytics')}
          >
            <span className="nav-icon">📈</span>
            Analytics
          </button>

          <button
            className={`nav-item ${activeTab === 'submissions' ? 'active' : ''}`}
            onClick={() => setActiveTab('submissions')}
          >
            <span className="nav-icon">📋</span>
            All Submissions
          </button>

          <button
            className={`nav-item ${activeTab === 'reports' ? 'active' : ''}`}
            onClick={() => setActiveTab('reports')}
          >
            <span className="nav-icon">📑</span>
            Reports
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
            <span className="role-badge" style={{ backgroundColor: '#f59e0b' }}>Coordinator</span>
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
                    <h3>{stats ? stats.totalStudents : '—'}</h3>
                    <p>Total Students</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">📋</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.totalSubmissions ?? '—' : '—'}</h3>
                    <p>Total Submissions</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">✅</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.completed : '—'}</h3>
                    <p>Completed</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">⚠️</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.overdue : '—'}</h3>
                    <p>Overdue</p>
                  </div>
                </div>
              </div>

              <div className="recent-activity">
                <h2>System Overview</h2>
                <div className="activity-list">
                  <div className="activity-item">
                    <div className="activity-icon">📈</div>
                    <div className="activity-content">
                      <p>Completion Rate: <strong>92.9%</strong></p>
                      <span className="activity-time">As of today</span>
                    </div>
                  </div>

                  <div className="activity-item">
                    <div className="activity-icon">📊</div>
                    <div className="activity-content">
                      <p>Average Submission Time: <strong>3.2 days early</strong></p>
                      <span className="activity-time">This semester</span>
                    </div>
                  </div>

                  <div className="activity-item">
                    <div className="activity-icon">⏰</div>
                    <div className="activity-content">
                      <p>Deadlines Today: <strong>5 submissions</strong></p>
                      <span className="activity-time">Action required</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'analytics' && (
            <div className="section">
              <h2>Analytics & Reports</h2>
              <div className="analytics-charts">
                <div className="chart-card">
                  <h3>Submission Trends</h3>
                  <p>Chart visualization coming soon...</p>
                </div>
                <div className="chart-card">
                  <h3>Student Performance</h3>
                  <p>Chart visualization coming soon...</p>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'submissions' && (
            <div className="section">
              <h2>All Submissions</h2>
              <p>View and manage all student submissions across the system.</p>
            </div>
          )}

          {activeTab === 'reports' && (
            <div className="section">
              <h2>Generate Reports</h2>
              <p>Generate system reports for auditing and analytics.</p>
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

export default CoordinatorDashboard;
