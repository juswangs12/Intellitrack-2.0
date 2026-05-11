import { useState, useEffect, useCallback } from "react";
import { BarChart3, TrendingUp, Users, AlertTriangle, Sparkles, CheckCircle2, Clock, XCircle } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const CoordinatorAnalytics = () => {
  const { user } = useAuth();
  const [dashboardData, setDashboardData] = useState(null);
  const [trackingData, setTrackingData] = useState(null);
  const [insightsData, setInsightsData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [dashboard, tracking, insights] = await Promise.all([
        apiService.requestJson("/analytics/coordinator"),
        apiService.requestJson("/analytics/tracking"),
        apiService.requestJson("/analytics/insights"),
      ]);

      setDashboardData(dashboard);
      setTrackingData(tracking);
      setInsightsData(insights);
    } catch (err) {
      console.error("Failed to fetch analytics", err);
      setError("Failed to load analytics.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  if (loading) return <div>Loading...</div>;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Institutional Analytics</h1>
        <p className="page-description">
          AI-powered academic tracking and performance analytics.
        </p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      {insightsData && (
        <div className="card" style={{ marginBottom: "1.5rem", borderLeft: "4px solid var(--maroon)" }}>
          <div className="card-content" style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <div className="stat-icon maroon">
              <Sparkles size={24} />
            </div>
            <div>
              <p style={{ margin: 0, fontWeight: 600 }}>AI Strategic Insight</p>
              <p style={{ margin: "0.25rem 0 0 0" }}>
                {insightsData.summary || "No insights available yet."}
              </p>
            </div>
          </div>
        </div>
      )}

      {dashboardData && (
        <div className="stats-grid" style={{ marginBottom: "1.5rem" }}>
          <div className="stat-card">
            <div className="stat-icon maroon">
              <Users size={20} />
            </div>
            <div className="stat-content">
              <p className="stat-label">Total Students</p>
              <p className="stat-value">{dashboardData.totalStudents || 0}</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon gold">
              <Users size={20} />
            </div>
            <div className="stat-content">
              <p className="stat-label">Total Advisers</p>
              <p className="stat-value">{dashboardData.totalAdvisers || 0}</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon green">
              <BarChart3 size={20} />
            </div>
            <div className="stat-content">
              <p className="stat-label">Pending Reviews</p>
              <p className="stat-value">{dashboardData.submissionsPending || 0}</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon blue">
              <AlertTriangle size={20} />
            </div>
            <div className="stat-content">
              <p className="stat-label">Late Submissions</p>
              <p className="stat-value">{dashboardData.submissionsLate || 0}</p>
            </div>
          </div>
        </div>
      )}

      {trackingData && (
        <div className="card" style={{ marginBottom: "1.5rem" }}>
          <div className="card-header">
            <h2 className="card-title">Submission Tracking</h2>
          </div>
          <div className="card-content">
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "1rem" }}>
              <div className="card">
                <div className="card-content" style={{ padding: "1rem" }}>
                  <p style={{ margin: 0, fontWeight: 600 }}>Submitted</p>
                  <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: 700 }}>
                    {trackingData.submitted || 0}
                  </p>
                </div>
              </div>
              <div className="card">
                <div className="card-content" style={{ padding: "1rem" }}>
                  <p style={{ margin: 0, fontWeight: 600 }}>Pending</p>
                  <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: 700 }}>
                    {trackingData.pending || 0}
                  </p>
                </div>
              </div>
              <div className="card">
                <div className="card-content" style={{ padding: "1rem" }}>
                  <p style={{ margin: 0, fontWeight: 600 }}>Needs Revision</p>
                  <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: 700 }}>
                    {trackingData.needsRevision || 0}
                  </p>
                </div>
              </div>
              <div className="card">
                <div className="card-content" style={{ padding: "1rem" }}>
                  <p style={{ margin: 0, fontWeight: 600 }}>Late</p>
                  <p style={{ margin: "0.5rem 0 0 0", fontSize: "1.5rem", fontWeight: 700 }}>
                    {trackingData.late || 0}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {trackingData && trackingData.groupProgress && trackingData.groupProgress.length > 0 && (
        <div className="card" style={{ marginBottom: "1.5rem" }}>
          <div className="card-header">
            <h2 className="card-title">Group Progress Breakdown</h2>
          </div>
          <div className="card-content">
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Group Code</th>
                    <th>Group Title</th>
                    <th>Completed</th>
                    <th>Total Deliverables</th>
                    <th>Completion Rate</th>
                  </tr>
                </thead>
                <tbody>
                  {trackingData.groupProgress.map((group) => (
                    <tr key={group.groupId}>
                      <td>{group.groupCode}</td>
                      <td>{group.groupTitle}</td>
                      <td>
                        <span style={{ display: "flex", alignItems: "center", gap: "0.25rem" }}>
                          <CheckCircle2 size={14} style={{ color: "#166534" }} />
                          {group.submittedCount || 0}
                        </span>
                      </td>
                      <td>{group.totalDeliverables || 0}</td>
                      <td>
                        <span className={`badge ${
                          (group.completionRate || 0) >= 80 ? "success" :
                          (group.completionRate || 0) >= 50 ? "info" : "warning"
                        }`}>
                          {group.completionRate || 0}%
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {insightsData && insightsData.recommendations?.length > 0 && (
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">AI Recommendations</h2>
          </div>
          <div className="card-content">
            <ul style={{ margin: 0, paddingLeft: "1.25rem" }}>
              {insightsData.recommendations.map((rec, index) => (
                <li key={index} style={{ marginBottom: "0.5rem" }}>
                  {rec}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};

export default CoordinatorAnalytics;