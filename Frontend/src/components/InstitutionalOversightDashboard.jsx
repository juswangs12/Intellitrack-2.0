import React, { useEffect, useState } from "react";
import ActivityFeed from "./ActivityFeed";
import apiService from "../services/ApiService";

const STATUS_STYLE = {
  submitted:  { bg: "#dcfce7", color: "#15803d", border: "#bbf7d0" },
  late:       { bg: "#fef2f2", color: "#b91c1c", border: "#fecaca" },
  pending:    { bg: "#fff7ed", color: "#9a3412", border: "#fed7aa" },
  commented:  { bg: "#eff6ff", color: "#1d4ed8", border: "#bfdbfe" },
  resubmitted:{ bg: "#f5f3ff", color: "#6d28d9", border: "#ddd6fe" },
};

const StatusChip = ({ status }) => {
  const key = (status || "").toLowerCase();
  const s = STATUS_STYLE[key] || { bg: "#f3f4f6", color: "#374151", border: "#e5e7eb" };
  return (
    <span style={{ padding: "0.2rem 0.6rem", borderRadius: "9999px", fontSize: "0.72rem", fontWeight: 600, backgroundColor: s.bg, color: s.color, border: `1px solid ${s.border}`, whiteSpace: "nowrap" }}>
      {status}
    </span>
  );
};

const cardStyle = {
  backgroundColor: "#fff",
  borderRadius: "0.75rem",
  border: "1px solid #e5e7eb",
  boxShadow: "0 1px 6px rgba(0,0,0,0.06)",
  overflow: "hidden",
};

const thStyle = {
  padding: "0.65rem 0.875rem", textAlign: "left", fontSize: "0.72rem",
  fontWeight: 600, color: "#6b7280", textTransform: "uppercase",
  letterSpacing: "0.05em", borderBottom: "1px solid #e5e7eb",
  backgroundColor: "#f9fafb",
};
const tdStyle = (i) => ({
  padding: "0.75rem 0.875rem",
  backgroundColor: i % 2 === 0 ? "#fff" : "#fafafa",
  fontSize: "0.8125rem",
  color: "#374151",
  verticalAlign: "middle",
});

const InstitutionalOversightDashboard = ({ adviserId = null }) => {
  const [groups, setGroups] = useState([]);
  const [selectedGroupId, setSelectedGroupId] = useState("");
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    const loadGroups = async () => {
      try {
        const tracking = await apiService.getTrackingAnalytics(adviserId || undefined);
        if (!mounted) return;
        setGroups(tracking.groupProgress || []);
        if ((tracking.groupProgress || []).length > 0)
          setSelectedGroupId(String(tracking.groupProgress[0].groupId));
      } catch {
        if (mounted) setError("Unable to load groups for summary view.");
      }
    };
    loadGroups();
    return () => { mounted = false; };
  }, [adviserId]);

  useEffect(() => {
    if (!selectedGroupId) { setSummary(null); return; }
    let mounted = true;
    const loadSummary = async () => {
      setLoading(true); setError("");
      try {
        const result = await apiService.getSubmissionSummary(selectedGroupId);
        if (mounted) setSummary(result);
      } catch {
        if (mounted) setError("Unable to load submission summary right now.");
      } finally {
        if (mounted) setLoading(false);
      }
    };
    loadSummary();
    return () => { mounted = false; };
  }, [selectedGroupId]);

  return (
    <div style={cardStyle}>
      {/* Panel header */}
      <div style={{ padding: "1.25rem 1.5rem", background: "linear-gradient(135deg,#1e3a8a 0%,#3b82f6 100%)", borderRadius: "0.75rem 0.75rem 0 0", display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "0.75rem" }}>
        <div>
          <h2 style={{ margin: 0, color: "#fff", fontSize: "1.0625rem", fontWeight: 700 }}>Institutional Oversight Dashboard</h2>
          <p style={{ margin: "0.2rem 0 0", color: "rgba(255,255,255,0.8)", fontSize: "0.8125rem" }}>
            AI-generated submission summaries, revision history, and deliverable status.
          </p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <label style={{ fontSize: "0.75rem", color: "rgba(255,255,255,0.85)", fontWeight: 600, whiteSpace: "nowrap" }}>Student Group</label>
          <select
            value={selectedGroupId}
            onChange={(e) => setSelectedGroupId(e.target.value)}
            style={{ padding: "0.35rem 0.65rem", borderRadius: "0.375rem", border: "1px solid rgba(255,255,255,0.35)", backgroundColor: "rgba(255,255,255,0.15)", color: "#fff", fontSize: "0.8125rem", cursor: "pointer", outline: "none", maxWidth: "220px" }}
          >
            {groups.map((g) => (
              <option key={g.groupId} value={g.groupId} style={{ color: "#111" }}>
                {g.groupCode} — {g.groupTitle}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div style={{ padding: "1.5rem", display: "flex", flexDirection: "column", gap: "1.5rem" }}>
        {loading && <div style={{ textAlign: "center", padding: "3rem", color: "#6b7280" }}>Loading submission summary…</div>}
        {error && (
          <div style={{ padding: "0.75rem 1rem", backgroundColor: "#fef2f2", border: "1px solid #fecaca", borderRadius: "0.5rem", color: "#b91c1c", fontSize: "0.875rem" }}>{error}</div>
        )}

        {!loading && !error && summary && (
          <>
            {/* Hero summary card */}
            <div style={{ padding: "1.25rem 1.5rem", background: "linear-gradient(135deg,#f0f9ff,#e0f2fe)", borderRadius: "0.75rem", border: "1px solid #bae6fd" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "0.625rem", marginBottom: "0.5rem" }}>
                <span style={{ padding: "0.2rem 0.65rem", backgroundColor: "#1e3a8a", color: "#fff", borderRadius: "9999px", fontSize: "0.72rem", fontWeight: 700, letterSpacing: "0.04em" }}>
                  {summary.groupCode}
                </span>
              </div>
              <h3 style={{ margin: "0 0 0.4rem", fontSize: "1.0625rem", fontWeight: 700, color: "#1e3a8a" }}>{summary.groupTitle}</h3>
              <p style={{ margin: "0 0 0.5rem", fontWeight: 600, color: "#1e40af", lineHeight: 1.5 }}>{summary.headlineSummary}</p>
              {summary.detailSummary && (
                <p style={{ margin: 0, fontSize: "0.8125rem", color: "#475569", lineHeight: 1.6 }}>{summary.detailSummary}</p>
              )}
            </div>

            {/* Bottom two-col: deliverables table + timeline */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(300px,1fr))", gap: "1.25rem" }}>

              {/* Deliverable Summary */}
              <div style={cardStyle}>
                <div style={{ padding: "1rem 1.25rem", borderBottom: "1px solid #f3f4f6" }}>
                  <h3 style={{ margin: 0, fontSize: "0.9375rem", fontWeight: 700, color: "#111827" }}>Deliverable Summary</h3>
                </div>
                <div style={{ overflowX: "auto" }}>
                  <table style={{ width: "100%", borderCollapse: "collapse" }}>
                    <thead>
                      <tr>
                        {["Deliverable", "Status", "Submitted", "Rev."].map((h) => (
                          <th key={h} style={thStyle}>{h}</th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {summary.deliverables.map((item, i) => (
                        <tr key={item.deliverableId}>
                          <td style={tdStyle(i)}>
                            <p style={{ margin: 0, fontWeight: 600, color: "#111827" }}>{item.deliverableName}</p>
                            <p style={{ margin: 0, fontSize: "0.72rem", color: "#9ca3af" }}>{item.stage}</p>
                          </td>
                          <td style={tdStyle(i)}><StatusChip status={item.status} /></td>
                          <td style={{ ...tdStyle(i), fontSize: "0.75rem", color: item.submittedAt ? "#374151" : "#9ca3af" }}>
                            {item.submittedAt ? new Date(item.submittedAt).toLocaleString() : "Not yet submitted"}
                          </td>
                          <td style={{ ...tdStyle(i), textAlign: "center", fontWeight: 700, color: item.revisionCount > 0 ? "#d97706" : "#9ca3af" }}>{item.revisionCount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Submission Timeline */}
              <div style={cardStyle}>
                <div style={{ padding: "1rem 1.25rem", borderBottom: "1px solid #f3f4f6" }}>
                  <h3 style={{ margin: 0, fontSize: "0.9375rem", fontWeight: 700, color: "#111827" }}>Submission Timeline</h3>
                </div>
                <div style={{ padding: "1.25rem", maxHeight: "360px", overflowY: "auto" }}>
                  <ActivityFeed items={summary.timeline} />
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default InstitutionalOversightDashboard;
