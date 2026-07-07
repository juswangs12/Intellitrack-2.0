import React, { useEffect, useState } from "react";
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
        if (mounted) setStats(data);
      } catch {
        if (mounted) setError("Unable to load admin analytics right now.");
      } finally {
        if (mounted) setLoading(false);
      }
    };
    if (user?.id) load();
    return () => { mounted = false; };
  }, [user]);

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Analytics</h1>
        <p className="page-description">
          System-wide submission tracking, InsightHub metrics, and AI summaries.
        </p>
      </div>

      {loading && (
        <div style={{ display: "flex", alignItems: "center", justifyContent: "center", minHeight: "300px", color: "#6b7280" }}>
          Loading analytics…
        </div>
      )}

      {error && (
        <div style={{ marginBottom: "1rem", padding: "0.75rem 1rem", backgroundColor: "#fef2f2", border: "1px solid #fecaca", borderRadius: "0.5rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      {error && (
        <div style={{ marginBottom: "1rem", padding: "0.75rem 1rem", backgroundColor: "#fef2f2", border: "1px solid #fecaca", borderRadius: "0.5rem", color: "#b91c1c" }}>
          {error}
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
