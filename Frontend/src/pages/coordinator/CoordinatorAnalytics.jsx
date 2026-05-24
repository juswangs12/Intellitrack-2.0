import { useState, useEffect, useCallback } from "react";
import {
  AlertTriangle,
  Sparkles,
  CheckCircle2,
  Clock,
  Activity,
  BookOpen,
} from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";
import SubmissionTrackingDashboard from "../../components/SubmissionTrackingDashboard";
import InsightHubDashboard from "../../components/InsightHubDashboard";
import InstitutionalOversightDashboard from "../../components/InstitutionalOversightDashboard";

/* ─── tiny helpers ─────────────────────────────────────── */
const rateColor = (pct) =>
  pct >= 80 ? "#10b981" : pct >= 50 ? "#f59e0b" : "#ef4444";

const RateBar = ({ value }) => (
  <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
    <div style={{ flex: 1, height: "6px", backgroundColor: "#e5e7eb", borderRadius: "9999px", overflow: "hidden" }}>
      <div style={{ height: "100%", width: `${Math.min(value, 100)}%`, backgroundColor: rateColor(value), borderRadius: "9999px", transition: "width 0.4s ease" }} />
    </div>
    <span style={{ fontSize: "0.8125rem", fontWeight: 600, color: rateColor(value), minWidth: "2.5rem", textAlign: "right" }}>
      {value}%
    </span>
  </div>
);

const RiskBadge = ({ level }) => {
  const cfg = level === "HIGH"
    ? { bg: "#fef2f2", color: "#b91c1c", border: "#fecaca" }
    : { bg: "#fff7ed", color: "#9a3412", border: "#fed7aa" };
  return (
    <span style={{ padding: "0.2rem 0.6rem", borderRadius: "9999px", fontSize: "0.75rem", fontWeight: 600, backgroundColor: cfg.bg, color: cfg.color, border: `1px solid ${cfg.border}` }}>
      {level}
    </span>
  );
};

const CoordinatorAnalytics = () => {
  const { user } = useAuth();
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const dashboard = await apiService.requestJson("/analytics/coordinator");
      setDashboardData(dashboard);
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

  if (loading)
    return (
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", minHeight: "400px", color: "#6b7280" }}>
        Loading analytics…
      </div>
    );

  const stats = dashboardData?.submissionStats;
  const statCards = [
    { label: "Total Submissions", value: stats?.total || 0, icon: <BookOpen size={22} />, gradient: "linear-gradient(135deg,#7f1d1d,#b91c1c)" },
    { label: "On Time",           value: stats?.onTime || 0, icon: <CheckCircle2 size={22} />, gradient: "linear-gradient(135deg,#065f46,#10b981)" },
    { label: "Pending",           value: stats?.pending || 0, icon: <Clock size={22} />,       gradient: "linear-gradient(135deg,#92400e,#f59e0b)" },
    { label: "Late",              value: stats?.late || 0,    icon: <AlertTriangle size={22} />, gradient: "linear-gradient(135deg,#1e3a8a,#3b82f6)" },
  ];

  const thStyle = {
    padding: "0.75rem 1rem", textAlign: "left", fontSize: "0.75rem",
    fontWeight: 600, color: "#6b7280", textTransform: "uppercase",
    letterSpacing: "0.05em", borderBottom: "1px solid #e5e7eb",
    backgroundColor: "#f9fafb",
  };
  const tdStyle = (i) => ({ padding: "0.875rem 1rem", backgroundColor: i % 2 === 0 ? "#fff" : "#fafafa" });

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Institutional Analytics</h1>
        <p className="page-description">AI-powered academic tracking and performance analytics.</p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", padding: "0.75rem 1rem", backgroundColor: "#fef2f2", border: "1px solid #fecaca", borderRadius: "0.5rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      {/* AI insight banner */}
      {dashboardData?.aiStrategicInsight && (
        <div style={{ marginBottom: "1.5rem", padding: "1rem 1.25rem", background: "linear-gradient(135deg,#7f1d1d 0%,#b91c1c 100%)", borderRadius: "0.75rem", display: "flex", alignItems: "flex-start", gap: "1rem", boxShadow: "0 4px 15px rgba(127,29,29,0.25)" }}>
          <div style={{ padding: "0.5rem", backgroundColor: "rgba(255,255,255,0.2)", borderRadius: "0.5rem", flexShrink: 0 }}>
            <Sparkles size={22} color="#fff" />
          </div>
          <div>
            <p style={{ margin: 0, fontWeight: 700, color: "#fff", fontSize: "0.75rem", letterSpacing: "0.06em", textTransform: "uppercase", marginBottom: "0.25rem" }}>AI Strategic Insight</p>
            <p style={{ margin: 0, color: "rgba(255,255,255,0.9)", lineHeight: 1.6 }}>{dashboardData.aiStrategicInsight}</p>
          </div>
        </div>
      )}

      {/* Stat cards */}
      {stats && (
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

      {/* Adviser workload */}
      {dashboardData?.adviserWorkload && dashboardData.adviserWorkload.length > 0 && (
        <div className="card" style={{ marginBottom: "1.5rem" }}>
          <div className="card-header" style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <Activity size={18} style={{ color: "var(--maroon)" }} />
            <h2 className="card-title" style={{ margin: 0 }}>Adviser Workload</h2>
          </div>
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  {["Adviser", "Groups", "Submissions", "Completion Rate"].map((h) => <th key={h} style={thStyle}>{h}</th>)}
                </tr>
              </thead>
              <tbody>
                {dashboardData.adviserWorkload.map((w, i) => (
                  <tr key={w.adviserId}>
                    <td style={{ ...tdStyle(i), fontWeight: 600, color: "#1f2937" }}>{w.name}</td>
                    <td style={tdStyle(i)}>{w.assignedGroups}</td>
                    <td style={tdStyle(i)}>{w.pendingReviews}</td>
                    <td style={{ ...tdStyle(i), minWidth: "200px" }}><RateBar value={w.completionRate || 0} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* At-risk groups */}
      {dashboardData?.atRiskGroups && dashboardData.atRiskGroups.length > 0 && (
        <div className="card" style={{ marginBottom: "1.5rem" }}>
          <div className="card-header" style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <AlertTriangle size={18} style={{ color: "#ef4444" }} />
            <h2 className="card-title" style={{ margin: 0 }}>At-Risk Groups</h2>
          </div>
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  {["Group", "Risk Level", "Reason", "Late Submissions"].map((h) => <th key={h} style={thStyle}>{h}</th>)}
                </tr>
              </thead>
              <tbody>
                {dashboardData.atRiskGroups.map((g, i) => (
                  <tr key={g.groupId}>
                    <td style={{ ...tdStyle(i), fontWeight: 600, color: "#1f2937" }}>{g.groupTitle}</td>
                    <td style={tdStyle(i)}><RiskBadge level={g.riskLevel} /></td>
                    <td style={{ ...tdStyle(i), maxWidth: "320px" }}>{g.reason}</td>
                    <td style={{ ...tdStyle(i), fontWeight: 600, color: "#ef4444" }}>{g.missedDeadlines}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Embedded dashboards */}
      <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
        <SubmissionTrackingDashboard title="Submission Tracking (All Groups)" />
        <InsightHubDashboard />
        <InstitutionalOversightDashboard />
      </div>
    </div>
  );
};

export default CoordinatorAnalytics;
