import React, { useEffect, useState } from "react";
import {
  BarChart,
  Bar,
  Cell,
  PieChart,
  Pie,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import ActivityFeed from "./ActivityFeed";
import MetricCard from "./MetricCard";
import ProgressList from "./ProgressList";
import apiService from "../services/ApiService";

const barColors = ["#0f766e", "#d97706", "#dc2626"];

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
        const result = await apiService.getTrackingAnalytics(
          adviserId || undefined,
        );
        if (mounted) {
          setData(result);
        }
      } catch (err) {
        if (mounted) {
          setError("Unable to load tracking analytics right now.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadAnalytics();

    return () => {
      mounted = false;
    };
  }, [adviserId]);

  return (
    <section className="analytics-panel">
      <div className="section-heading-row">
        <div>
          <h2>{title}</h2>
          <p>
            Monitor submission volume, queue status, and per-group completion.
          </p>
        </div>
      </div>

      {loading && <p className="section-message">Loading analytics...</p>}
      {error && <p className="section-message error">{error}</p>}

      {!loading && !error && data && (
        <div className="analytics-stack">
          <div className="metric-grid">
            {data.metricCards.map((card) => (
              <MetricCard
                key={card.label}
                label={card.label}
                value={card.value}
                tone={card.tone}
              />
            ))}
          </div>

          <div className="analytics-grid analytics-grid-two-up">
            <article className="chart-card">
              <h3>Status Distribution</h3>
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie
                    data={data.statusDistribution}
                    dataKey="value"
                    nameKey="label"
                    outerRadius={90}
                    innerRadius={45}
                  >
                    {data.statusDistribution.map((entry, index) => (
                      <Cell
                        key={entry.label}
                        fill={barColors[index % barColors.length]}
                      />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </article>

            <article className="chart-card">
              <h3>Recent Activity</h3>
              <ActivityFeed items={data.activityFeed} />
            </article>
          </div>

          <article className="chart-card">
            <h3>Group Completion</h3>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart
                data={data.groupProgress}
                margin={{ top: 8, right: 8, left: 0, bottom: 8 }}
              >
                <XAxis dataKey="groupCode" />
                <YAxis domain={[0, 100]} />
                <Tooltip formatter={(value) => [`${value}%`, "Completion"]} />
                <Bar dataKey="completionRate" radius={[8, 8, 0, 0]}>
                  {data.groupProgress.map((entry, index) => (
                    <Cell
                      key={entry.groupId}
                      fill={barColors[index % barColors.length]}
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
            <ProgressList items={data.groupProgress} />
          </article>
        </div>
      )}
    </section>
  );
};

export default SubmissionTrackingDashboard;
