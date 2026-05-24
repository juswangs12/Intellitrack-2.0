import React, { useEffect, useState } from "react";
import {
  BarChart,
  Bar,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  CartesianGrid,
} from "recharts";
import MetricCard from "./MetricCard";
import apiService from "../services/ApiService";

const BAR_COLORS = ["#065f46", "#b91c1c", "#d97706", "#1e3a8a", "#7c3aed"];
const STATUS_COLORS = ["#10b981", "#ef4444", "#f59e0b", "#3b82f6", "#8b5cf6"];

const cardStyle = {
  backgroundColor: "#fff",
  borderRadius: "0.75rem",
  border: "1px solid #e5e7eb",
  boxShadow: "0 1px 6px rgba(0,0,0,0.06)",
  overflow: "hidden",
};

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ backgroundColor: "#fff", border: "1px solid #e5e7eb", borderRadius: "0.5rem", padding: "0.5rem 0.75rem", fontSize: "0.8125rem", boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}>
      {label && <p style={{ margin: "0 0 0.25rem", fontWeight: 600, color: "#374151" }}>{label}</p>}
      {payload.map((p) => (
        <p key={p.name} style={{ margin: 0, color: p.fill || "#6b7280" }}>
          {p.name}: <strong>{p.value}%</strong>
        </p>
      ))}
    </div>
  );
};

const InsightHubDashboard = ({ adviserId = null }) => {
  const [stage, setStage] = useState("");
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    const loadInsights = async () => {
      setLoading(true);
      setError("");
      try {
        const result = await apiService.getInsightAnalytics({ adviserId: adviserId || undefined, stage });
        if (mounted) setData(result);
      } catch {
        if (mounted) setError("Unable to load submission insights right now.");
      } finally {
        if (mounted) setLoading(false);
      }
    };
    loadInsights();
    return () => { mounted = false; };
  }, [adviserId, stage]);

  return (
    <div style={cardStyle}>
      {/* Panel header */}
      <div style={{ padding: "1.25rem 1.5rem", background: "linear-gradient(135deg,#065f46 0%,#10b981 100%)", borderRadius: "0.75rem 0.75rem 0 0", display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "0.75rem" }}>
        <div>
          <h2 style={{ margin: 0, color: "#fff", fontSize: "1.0625rem", fontWeight: 700 }}>InsightHub Analytics</h2>
          <p style={{ margin: "0.2rem 0 0", color: "rgba(255,255,255,0.8)", fontSize: "0.8125rem" }}>On-time, late, and pending rates across milestone stages.</p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          <label style={{ fontSize: "0.75rem", color: "rgba(255,255,255,0.85)", fontWeight: 600 }}>Milestone</label>
          <select
            value={stage}
            onChange={(e) => setStage(e.target.value)}
            style={{ padding: "0.35rem 0.65rem", borderRadius: "0.375rem", border: "1px solid rgba(255,255,255,0.35)", backgroundColor: "rgba(255,255,255,0.15)", color: "#fff", fontSize: "0.8125rem", cursor: "pointer", outline: "none" }}
          >
            <option value="" style={{ color: "#111" }}>All</option>
            <option value="Proposal" style={{ color: "#111" }}>Proposal</option>
            <option value="Midterm" style={{ color: "#111" }}>Midterm</option>
            <option value="Final" style={{ color: "#111" }}>Final</option>
          </select>
        </div>
      </div>

      <div style={{ padding: "1.5rem", display: "flex", flexDirection: "column", gap: "1.5rem" }}>
        {loading && <div style={{ textAlign: "center", padding: "3rem", color: "#6b7280" }}>Loading insights…</div>}
        {error && (
          <div style={{ padding: "0.75rem 1rem", backgroundColor: "#fef2f2", border: "1px solid #fecaca", borderRadius: "0.5rem", color: "#b91c1c", fontSize: "0.875rem" }}>{error}</div>
        )}

        {!loading && !error && data && (
          <>
            {/* Metric cards */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(150px,1fr))", gap: "1rem" }}>
              {data.metricCards.map((card) => (
                <MetricCard key={card.label} label={card.label} value={card.value} tone={card.tone} suffix="%" />
              ))}
            </div>

            {/* Charts row */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(300px,1fr))", gap: "1.25rem" }}>

              {/* Stage Progress Trend */}
              <div style={cardStyle}>
                <div style={{ padding: "1rem 1.25rem", borderBottom: "1px solid #f3f4f6" }}>
                  <h3 style={{ margin: 0, fontSize: "0.9375rem", fontWeight: 700, color: "#111827" }}>Stage Progress Trend</h3>
                </div>
                <div style={{ padding: "1.25rem 1.25rem 0" }}>
                  <ResponsiveContainer width="100%" height={240}>
                    <BarChart data={data.trendSeries} margin={{ top: 4, right: 8, left: 0, bottom: 4 }} barSize={36}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                      <XAxis dataKey="label" tick={{ fontSize: 12, fill: "#6b7280" }} axisLine={false} tickLine={false} />
                      <YAxis domain={[0, 100]} tick={{ fontSize: 12, fill: "#6b7280" }} axisLine={false} tickLine={false} tickFormatter={(v) => `${v}%`} />
                      <Tooltip content={<CustomTooltip />} formatter={(v) => [`${v}%`, "On-time"]} />
                      <Bar dataKey="value" name="On-time" radius={[6, 6, 0, 0]}>
                        {data.trendSeries.map((entry, i) => (
                          <Cell key={entry.label} fill={BAR_COLORS[i % BAR_COLORS.length]} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
                <div style={{ height: "1.25rem" }} />
              </div>

              {/* Status Mix */}
              <div style={cardStyle}>
                <div style={{ padding: "1rem 1.25rem", borderBottom: "1px solid #f3f4f6" }}>
                  <h3 style={{ margin: 0, fontSize: "0.9375rem", fontWeight: 700, color: "#111827" }}>Status Mix</h3>
                </div>
                <div style={{ padding: "1.25rem", display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                  {data.statusBreakdown.map((item, i) => {
                    const color = STATUS_COLORS[i % STATUS_COLORS.length];
                    const total = data.statusBreakdown.reduce((s, x) => s + (x.value || 0), 0);
                    const pct = total > 0 ? Math.round((item.value / total) * 100) : 0;
                    return (
                      <div key={item.label}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "0.3rem" }}>
                          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                            <span style={{ width: "10px", height: "10px", borderRadius: "50%", backgroundColor: color, display: "inline-block", flexShrink: 0 }} />
                            <span style={{ fontSize: "0.8125rem", color: "#374151", fontWeight: 500 }}>{item.label}</span>
                          </div>
                          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                            <span style={{ fontSize: "0.8125rem", fontWeight: 700, color }}>{item.value}</span>
                            <span style={{ fontSize: "0.75rem", color: "#9ca3af" }}>({pct}%)</span>
                          </div>
                        </div>
                        <div style={{ height: "6px", backgroundColor: "#e5e7eb", borderRadius: "9999px", overflow: "hidden" }}>
                          <div style={{ height: "100%", width: `${pct}%`, backgroundColor: color, borderRadius: "9999px", transition: "width 0.4s ease" }} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default InsightHubDashboard;
