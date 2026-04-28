import React, { useState, useEffect } from "react";
import InsightHubDashboard from "../components/InsightHubDashboard";
import InstitutionalOversightDashboard from "../components/InstitutionalOversightDashboard";
import SubmissionTrackingDashboard from "../components/SubmissionTrackingDashboard";
import UserProfile from "../components/UserProfile";
import "../styles/Dashboard.css";

const CoordinatorDashboard = () => {
  const [user, setUser] = useState(null);
  const [activeTab, setActiveTab] = useState("dashboard");
  const [stats, setStats] = useState(null);

  useEffect(() => {
    const userData = localStorage.getItem("user");
    if (userData) {
      setUser(JSON.parse(userData));
    }
    const token = localStorage.getItem("token");
    if (userData && token) {
      fetch("http://localhost:8080/api/analytics/tracking", {
        headers: { Authorization: `Bearer ${token}` },
      })
        .then((res) => res.json())
        .then((data) => setStats(data))
        .catch((err) =>
          console.error("Failed to load coordinator dashboard:", err),
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
          <p>Coordinator Portal</p>
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
            className={`nav-item ${activeTab === "analytics" ? "active" : ""}`}
            onClick={() => setActiveTab("analytics")}
          >
            <span className="nav-icon">📈</span>
            Analytics
          </button>

          <button
            className={`nav-item ${activeTab === "submissions" ? "active" : ""}`}
            onClick={() => setActiveTab("submissions")}
          >
            <span className="nav-icon">📋</span>
            All Submissions
          </button>

          <button
            className={`nav-item ${activeTab === "reports" ? "active" : ""}`}
            onClick={() => setActiveTab("reports")}
          >
            <span className="nav-icon">📑</span>
            Reports
          </button>

          <button
            className={`nav-item ${activeTab === "profile" ? "active" : ""}`}
            onClick={() => setActiveTab("profile")}
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
            <span className="role-badge" style={{ backgroundColor: "#f59e0b" }}>
              Coordinator
            </span>
            <span className="user-email">{user?.email}</span>
          </div>
        </header>

        <main className="dashboard-content">
          {activeTab === "dashboard" && (
            <div className="dashboard-overview">
              <div className="stats-grid">
                <div className="stat-card">
                  <div className="stat-icon">👥</div>
                  <div className="stat-content">
                    <h3>
                      {stats?.groupProgress ? stats.groupProgress.length : "—"}
                    </h3>
                    <p>Active Groups</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">📋</div>
                  <div className="stat-content">
                    <h3>
                      {stats?.metricCards?.[0]
                        ? stats.metricCards[0].value
                        : "—"}
                    </h3>
                    <p>Total Deliverables</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">✅</div>
                  <div className="stat-content">
                    <h3>
                      {stats?.metricCards?.[1]
                        ? stats.metricCards[1].value
                        : "—"}
                    </h3>
                    <p>Submitted</p>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon">⚠️</div>
                  <div className="stat-content">
                    <h3>
                      {stats?.metricCards?.[3]
                        ? stats.metricCards[3].value
                        : "—"}
                    </h3>
                    <p>Late</p>
                  </div>
                </div>
              </div>

              <SubmissionTrackingDashboard title="Institutional Oversight Dashboard" />
            </div>
          )}

          {activeTab === "analytics" && <InsightHubDashboard />}

          {activeTab === "submissions" && (
            <div className="section">
              <SubmissionTrackingDashboard title="All Submission Tracking" />
            </div>
          )}

          {activeTab === "reports" && (
            <div className="section">
              <InstitutionalOversightDashboard />
            </div>
          )}

          {activeTab === "profile" && (
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
