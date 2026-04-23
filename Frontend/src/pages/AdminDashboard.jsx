import React, { useState, useEffect } from 'react';
import UserProfile from '../components/UserProfile';
import '../styles/Dashboard.css';

const AdminDashboard = () => {
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
      fetch(`http://localhost:8080/api/dashboard/admin/${uid}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
        .then(res => res.json())
        .then(data => setStats(data))
        .catch(err => console.error('Failed to load admin dashboard:', err));
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
          <p>Admin Portal</p>
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
            className={`nav-item ${activeTab === 'users' ? 'active' : ''}`}
            onClick={() => setActiveTab('users')}
          >
            <span className="nav-icon">👥</span>
            User Management
          </button>

          <button
            className={`nav-item ${activeTab === 'system' ? 'active' : ''}`}
            onClick={() => setActiveTab('system')}
          >
            <span className="nav-icon">⚙️</span>
            System Settings
          </button>

          <button
            className={`nav-item ${activeTab === 'audit' ? 'active' : ''}`}
            onClick={() => setActiveTab('audit')}
          >
            <span className="nav-icon">📋</span>
            Audit Logs
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
            <span className="role-badge" style={{ backgroundColor: '#ef4444' }}>Administrator</span>
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
                    <h3>{stats ? stats.totalUsers : '—'}</h3>
                    <p>Total Users</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">🎓</div>
                  <div className="stat-content">
                    <h3>{stats?.byRole?.students ?? '—'}</h3>
                    <p>Students</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">📚</div>
                  <div className="stat-content">
                    <h3>{stats?.byRole?.advisers ?? '—'}</h3>
                    <p>Advisers</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">🔧</div>
                  <div className="stat-content">
                    <h3>{stats?.byRole?.coordinators ?? '—'}</h3>
                    <p>Coordinators</p>
                  </div>
                </div>
              </div>

              <div className="recent-activity">
                <h2>System Status</h2>
                <div className="activity-list">
                  <div className="activity-item">
                    <div className="activity-icon">✅</div>
                    <div className="activity-content">
                      <p>System Health: <strong>Excellent</strong></p>
                      <span className="activity-time">All services operational</span>
                    </div>
                  </div>

                  <div className="activity-item">
                    <div className="activity-icon">🔄</div>
                    <div className="activity-content">
                      <p>Last Database Backup: <strong>2 hours ago</strong></p>
                      <span className="activity-time">Status: Successful</span>
                    </div>
                  </div>

                  <div className="activity-item">
                    <div className="activity-icon">⚠️</div>
                    <div className="activity-content">
                      <p>Active User Sessions: <strong>127</strong></p>
                      <span className="activity-time">Current load: Normal</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'users' && (
            <div className="section">
              <h2>User Management</h2>
              <div className="users-management">
                <div className="user-management-card">
                  <h3>👨‍🎓 Students</h3>
                  <p>42 active students</p>
                  <button>Manage Students</button>
                </div>
                <div className="user-management-card">
                  <h3>👨‍🏫 Advisers</h3>
                  <p>8 active advisers</p>
                  <button>Manage Advisers</button>
                </div>
                <div className="user-management-card">
                  <h3>📋 Coordinators</h3>
                  <p>2 active coordinators</p>
                  <button>Manage Coordinators</button>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'system' && (
            <div className="section">
              <h2>System Settings</h2>
              <div className="settings-panel">
                <div className="setting-item">
                  <h3>Email Domain Whitelist</h3>
                  <p>Allowed domains: @university.edu, @school.edu, @institution.edu, @gmail.com</p>
                </div>
                <div className="setting-item">
                  <h3>Session Timeout</h3>
                  <p>Configured: 24 hours for access tokens</p>
                </div>
                <div className="setting-item">
                  <h3>Database</h3>
                  <p>Current: H2 In-Memory (Development)</p>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'audit' && (
            <div className="section">
              <h2>Audit Logs</h2>
              <p>System audit logs and user activity tracking.</p>
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

export default AdminDashboard;
