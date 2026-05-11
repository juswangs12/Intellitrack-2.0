import React, { useEffect, useState } from "react";
import { Users } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";
import SubmissionTrackingDashboard from "../../components/SubmissionTrackingDashboard";
import InsightHubDashboard from "../../components/InsightHubDashboard";
import InstitutionalOversightDashboard from "../../components/InstitutionalOversightDashboard";

const Analytics = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await apiService.requestJson(`/dashboard/admin/${user.id}`);
        if (mounted) {
          setStats(data);
        }
      } catch (err) {
        if (mounted) {
          setError("Unable to load admin analytics right now.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    if (user?.id) {
      load();
    }

    return () => {
      mounted = false;
    };
  }, [user]);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Analytics</h1>
        <p className="page-description">
          System-wide submission tracking, InsightHub metrics, and AI summaries.
        </p>
      </div>

      {loading && (
        <div style={{ textAlign: "center", padding: "2rem", color: "#6b7280" }}>
          Loading analytics...
        </div>
      )}
      {error && <div className="error-message">{error}</div>}

      {!loading && !error && stats && (
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon maroon">
              <Users style={{ width: "1.5rem", height: "1.5rem" }} />
            </div>
            <div className="stat-content">
              <p className="stat-label">Total Users</p>
              <p className="stat-value">{stats.totalUsers}</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon gold">
              <Users style={{ width: "1.5rem", height: "1.5rem" }} />
            </div>
            <div className="stat-content">
              <p className="stat-label">Students</p>
              <p className="stat-value">{stats.byRole?.students ?? 0}</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon green">
              <Users style={{ width: "1.5rem", height: "1.5rem" }} />
            </div>
            <div className="stat-content">
              <p className="stat-label">Advisers</p>
              <p className="stat-value">{stats.byRole?.advisers ?? 0}</p>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon blue">
              <Users style={{ width: "1.5rem", height: "1.5rem" }} />
            </div>
            <div className="stat-content">
              <p className="stat-label">Coordinators</p>
              <p className="stat-value">{stats.byRole?.coordinators ?? 0}</p>
            </div>
          </div>
        </div>
      )}

      {!loading && !error && (
        <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
          <SubmissionTrackingDashboard title="Submission Tracking (All Groups)" />
          <InsightHubDashboard />
          <InstitutionalOversightDashboard />
        </div>
      )}
    </div>
  );
};

export default Analytics;
