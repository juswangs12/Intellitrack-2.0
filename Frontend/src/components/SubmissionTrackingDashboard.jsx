import React, { useEffect, useState } from "react";
import {
  BarChart,
  Bar,
  Cell,
  PieChart,
  Pie,
  ResponsiveContainer,
  Tooltip,
  Legend,
  XAxis,
  YAxis,
  CartesianGrid,
} from "recharts";
import ActivityFeed from "./ActivityFeed";
import MetricCard from "./MetricCard";
import ProgressList from "./ProgressList";
import apiService from "../services/ApiService";

const PIE_COLORS = ["#7f1d1d", "#d97706", "#b91c1c", "#065f46", "#1e3a8a"];
const BAR_COLORS = ["#7f1d1d", "#b91c1c", "#d97706", "#10b981", "#3b82f6", "#8b5cf6"];

const cardStyle = {
  backgroundColor: "#fff",
  borderRadius: "0.75rem",
  border: "1px solid #e5e7eb",
  boxShadow: "0 1px 6px rgba(0,0,0,0.06)",
  overflow: "hidden",
};

const cardHeaderStyle = {
  padding: "1rem 1.25rem",
  borderBottom: "1px solid #f3f4f6",
  display: "flex",
  alignItems: "center",
  gap: "0.5rem",
};

const cardTitleStyle = {
  margin: 0,
  fontSize: "0.9375rem",
  fontWeight: 700,
  color: "#111827",
};

const cardBodyStyle = { padding: "1.25rem" };

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ backgroundColor: "#fff", border: "1px solid #e5e7eb", borderRadius: "0.5rem", padding: "0.5rem 0.75rem", fontSize: "0.8125rem", boxShadow: "0 4px 12px rgba(0,0,0,0.1)" }}>
      {label && <p style={{ margin: "0 0 0.25rem", fontWeight: 600, color: "#374151" }}>{label}</p>}
      {payload.map((p) => (
        <p key={p.name} style={{ margin: 0, color: p.fill || p.color || "#6b7280" }}>
          {p.name}: <strong>{p.value}{typeof p.value === "number" && p.name === "Completion" ? "%" : ""}</strong>
        </p>
      ))}
    </div>
  );
};

const SubmissionTrackingDashboard = ({
  adviserId = null,
  title = "Submission Tracking Analytics",
}) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    const loadAnalytics = async () => {
      setLoading(true);
      setError("");
      try {
        const result = await apiService.getTrackingAnalytics(adviserId || undefined);
        if (mounted) setData(result);
      } catch {
        if (mounted) setError("Unable to load tracking analytics right now.");
      } finally {
        if (mounted) setLoading(false);
      }
    };
    loadAnalytics();
    return () => { mounted = false; };
  }, [adviserId]);

  return (
    <div style={cardStyle}>
      {/* Panel header */}
      <div style={{ padding: "1.25rem 1.5rem", background: "linear-gradient(135deg,#7f1d1d 0%,#b91c1c 100%)", borderRadius: "0.75rem 0.75rem 0 0" }}>
        <h2 style={{ margin: 0, color: "#fff", fontSize: "1.0625rem", fontWeight: 700 }}>{title}</h2>
        <p style={{ margin: "0.2rem 0 0", color: "rgba(255,255,255,0.8)", fontSize: "0.8125rem" }}>
          Monitor submission volume, queue status, and per-group completion.
        </p>
      </div>

      <div style={{ padding: "1.5rem", display: "flex", flexDirection: "column", gap: "1.5rem" }}>

        {loading && (
          <div style={{ textAlign: "center", padding: "3rem", color: "#6b7280" }}>Loading analytics…</div>
        )}
        {error && (
          <div style={{ padding: "0.75rem 1rem", backgroundColor: "#fef2f2", border: "1px solid #fecaca", borderRadius: "0.5rem", color: "#b91c1c", fontSize: "0.875rem" }}>{error}</div>
        )}

        {!loading && !error && data && (
          <>
            {/* Metric cards row */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(160px,1fr))", gap: "1rem" }}>
              {data.metricCards.map((card) => (
                <MetricCard key={card.label} label={card.label} value={card.value} tone={card.tone} />
              ))}
            </div>

            {/* Charts row */}
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(300px,1fr))", gap: "1.25rem" }}>

              {/* Status Distribution */}
              <div style={cardStyle}>
                <div style={cardHeaderStyle}>
                  <h3 style={cardTitleStyle}>Status Distribution</h3>
                </div>
                <div style={cardBodyStyle}>
                  <ResponsiveContainer width="100%" height={240}>
                    <PieChart>
                      <Pie data={data.statusDistribution} dataKey="value" nameKey="label" outerRadius={85} innerRadius={42} paddingAngle={3}>
                        {data.statusDistribution.map((entry, i) => (
                          <Cell key={entry.label} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip content={<CustomTooltip />} />
                      <Legend iconType="circle" iconSize={10} formatter={(v) => <span style={{ fontSize: "0.8rem", color: "#374151" }}>{v}</span>} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              </div>

              {/* Recent Activity */}
              <div style={cardStyle}>
                <div style={cardHeaderStyle}>
                  <h3 style={cardTitleStyle}>Recent Activity</h3>
                </div>
                <div style={{ ...cardBodyStyle, maxHeight: "300px", overflowY: "auto" }}>
                  <ActivityFeed items={data.activityFeed} />
                </div>
              </div>
            </div>

            {/* Group Completion */}
            <div style={cardStyle}>
              <div style={cardHeaderStyle}>
                <h3 style={cardTitleStyle}>Group Completion</h3>
              </div>
              <div style={{ padding: "1.25rem 1.25rem 0" }}>
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={data.groupProgress} margin={{ top: 4, right: 8, left: 0, bottom: 4 }} barSize={32}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                    <XAxis dataKey="groupCode" tick={{ fontSize: 12, fill: "#6b7280" }} axisLine={false} tickLine={false} />
                    <YAxis domain={[0, 100]} tick={{ fontSize: 12, fill: "#6b7280" }} axisLine={false} tickLine={false} tickFormatter={(v) => `${v}%`} />
                    <Tooltip content={<CustomTooltip />} formatter={(value) => [`${value}%`, "Completion"]} />
                    <Bar dataKey="completionRate" name="Completion" radius={[6, 6, 0, 0]}>
                      {data.groupProgress.map((entry, i) => (
                        <Cell key={entry.groupId} fill={BAR_COLORS[i % BAR_COLORS.length]} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <div style={cardBodyStyle}>
                <ProgressList items={data.groupProgress} />
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default SubmissionTrackingDashboard;
