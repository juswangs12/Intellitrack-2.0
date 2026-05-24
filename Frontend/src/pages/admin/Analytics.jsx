import React, { useEffect, useState } from "react";
import { Users, GraduationCap, BookOpen, UserCog, LayoutDashboard } from "lucide-react";
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

  const statCards = stats
    ? [
        { label: "Total Users",   value: stats.totalUsers ?? 0,             icon: <Users size={22} />,        gradient: "linear-gradient(135deg,#7f1d1d,#b91c1c)" },
        { label: "Students",      value: stats.byRole?.students ?? 0,        icon: <GraduationCap size={22} />, gradient: "linear-gradient(135deg,#065f46,#10b981)" },
        { label: "Advisers",      value: stats.byRole?.advisers ?? 0,        icon: <BookOpen size={22} />,      gradient: "linear-gradient(135deg,#92400e,#f59e0b)" },
        { label: "Coordinators",  value: stats.byRole?.coordinators ?? 0,    icon: <UserCog size={22} />,       gradient: "linear-gradient(135deg,#1e3a8a,#3b82f6)" },
      ]
    : [];

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

      {!loading && !error && stats && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(180px,1fr))", gap: "1rem", marginBottom: "1.5rem" }}>
          {statCards.map((c) => (
            <div key={c.label} style={{ background: c.gradient, borderRadius: "0.75rem", padding: "1.25rem 1rem", display: "flex", alignItems: "center", gap: "1rem", boxShadow: "0 4px 12px rgba(0,0,0,0.12)", color: "#fff" }}>
              <div style={{ padding: "0.625rem", backgroundColor: "rgba(255,255,255,0.18)", borderRadius: "0.5rem", flexShrink: 0 }}>{c.icon}</div>
              <div>
                <p style={{ margin: 0, fontSize: "0.75rem", opacity: 0.85, fontWeight: 500 }}>{c.label}</p>
                <p style={{ margin: 0, fontSize: "1.75rem", fontWeight: 800, lineHeight: 1.2 }}>{c.value}</p>
              </div>
            </div>
          ))}
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
