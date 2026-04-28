import React, { useState, useEffect } from "react";
import NotificationCenter from "../components/NotificationCenter";
import StatusMonitoringPanel from "../components/StatusMonitoringPanel";
import SubmissionTrackerDashboard from "../components/SubmissionTrackerDashboard";
import UserProfile from "../components/UserProfile";
import "../styles/Dashboard.css";

const StudentDashboard = () => {
  const [user, setUser] = useState(null);
  const [activeTab, setActiveTab] = useState("dashboard");
  const [stats, setStats] = useState(null);
  const [recentActivity, setRecentActivity] = useState([]);

  useEffect(() => {
    // Get user data from localStorage (set during login)
    const userData = localStorage.getItem("user");
    if (userData) {
      setUser(JSON.parse(userData));
    }
    // fetch dashboard data if available
    const token = localStorage.getItem("token");
    if (userData && token) {
      const uid = JSON.parse(userData).id;
      fetch(`http://localhost:8080/api/dashboard/student/${uid}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
        .then((res) => res.json())
        .then((data) => {
          setStats({
            totalDeliverables: data.totalDeliverables,
            completed: data.completed,
            pending: data.pending,
            overdue: data.overdue,
          });
          setRecentActivity(data.recentActivity || []);
        })
        .catch((err) =>
          console.error("Failed to load student dashboard:", err),
        );
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("user");
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    window.location.href = "/";
  };

  return (
    <div className="dashboard">
      <div className="sidebar">
        <div className="sidebar-header">
          <h2>IntelliTrack</h2>
          <p>Student Portal</p>
        </div>

        <nav className="sidebar-nav">
          <button
            className={`nav-item ${activeTab === "dashboard" ? "active" : ""}`}
            onClick={() => setActiveTab("dashboard")}
          >
            <span className="nav-icon">📊</span>
            Dashboard
          </button>

          <button
            className={`nav-item ${activeTab === "profile" ? "active" : ""}`}
            onClick={() => setActiveTab("profile")}
          >
            <span className="nav-icon">👤</span>
            Profile
          </button>

          <button
            className={`nav-item ${activeTab === "deliverables" ? "active" : ""}`}
            onClick={() => setActiveTab("deliverables")}
          >
            <span className="nav-icon">📝</span>
            Deliverables
          </button>

          <button
            className={`nav-item ${activeTab === "analytics" ? "active" : ""}`}
            onClick={() => setActiveTab("analytics")}
          >
            <span className="nav-icon">📈</span>
            Analytics
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
            <span className="role-badge">Student</span>
            <span className="user-email">{user?.email}</span>
          </div>
        </header>

        <main className="dashboard-content">
          {activeTab === "dashboard" && (
            <div className="dashboard-overview">
              <div className="stats-grid">
                <div className="stat-card">
                  <div className="stat-icon">📋</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.totalDeliverables : "—"}</h3>
                    <p>Total Deliverables</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">✅</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.completed : "—"}</h3>
                    <p>Completed</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">⏰</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.pending : "—"}</h3>
                    <p>Pending</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">⚠️</div>
                  <div className="stat-content">
                    <h3>{stats ? stats.overdue : "—"}</h3>
                    <p>Overdue</p>
                  </div>
                </div>
              </div>

              <div className="recent-activity">
                <h2>Recent Activity</h2>
                <div className="activity-list">
                  {recentActivity.map((act, idx) => (
                    <div className="activity-item" key={idx}>
                      <div className="activity-icon">📤</div>
                      <div className="activity-content">
                        <p>{act.text}</p>
                        <span className="activity-time">{act.time}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <NotificationCenter userId={user?.id} />

              <StatusMonitoringPanel groupId={user?.groupId} />
            </div>
          )}

          {activeTab === "profile" && (
            <div className="profile-section">
              <h2>My Profile</h2>
              <UserProfile userId={user?.id} />
            </div>
          )}

          {activeTab === "deliverables" && (
            <div className="deliverables-section">
              <h2>My Deliverables</h2>
              <SubmissionTrackerDashboard groupId={user?.groupId} />
              <div className="section-spacer" />
              <StatusMonitoringPanel groupId={user?.groupId} />
            </div>
          )}

          {activeTab === "analytics" && (
            <div className="analytics-section">
              <SubmissionTrackerDashboard groupId={user?.groupId} />
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default StudentDashboard;
