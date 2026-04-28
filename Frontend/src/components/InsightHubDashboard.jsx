import React, { useEffect, useState } from "react";
import {
  BarChart,
  Bar,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import MetricCard from "./MetricCard";
import apiService from "../services/ApiService";

const tones = ["#15803d", "#dc2626", "#d97706"];

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
        const result = await apiService.getInsightAnalytics({
          adviserId: adviserId || undefined,
          stage,
        });
        if (mounted) {
          setData(result);
        }
      } catch (err) {
        if (mounted) {
          setError("Unable to load submission insights right now.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadInsights();

    return () => {
      mounted = false;
    };
  }, [adviserId, stage]);

  return (
    <section className="analytics-panel">
      <div className="section-heading-row">
        <div>
          <h2>InsightHub Analytics</h2>
          <p>View on-time, late, and pending rates across milestone stages.</p>
        </div>
        <label className="analytics-filter">
          <span>Milestone</span>
          <select
            value={stage}
            onChange={(event) => setStage(event.target.value)}
          >
            <option value="">All</option>
            <option value="Proposal">Proposal</option>
            <option value="Midterm">Midterm</option>
            <option value="Final">Final</option>
          </select>
        </label>
      </div>

      {loading && (
        <p className="section-message">Loading adviser insights...</p>
      )}
      {error && <p className="section-message error">{error}</p>}

      {!loading && !error && data && (
        <div className="analytics-stack">
          <div className="metric-grid metric-grid-compact">
            {data.metricCards.map((card) => (
              <MetricCard
                key={card.label}
                label={card.label}
                value={card.value}
                tone={card.tone}
                suffix="%"
              />
            ))}
          </div>

          <div className="analytics-grid analytics-grid-two-up">
            <article className="chart-card">
              <h3>Stage Progress Trend</h3>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart
                  data={data.trendSeries}
                  margin={{ top: 8, right: 8, left: 0, bottom: 8 }}
                >
                  <XAxis dataKey="label" />
                  <YAxis domain={[0, 100]} />
                  <Tooltip
                    formatter={(value) => [`${value}%`, "On-time completion"]}
                  />
                  <Bar dataKey="value" radius={[8, 8, 0, 0]}>
                    {data.trendSeries.map((entry, index) => (
                      <Cell
                        key={entry.label}
                        fill={tones[index % tones.length]}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </article>

            <article className="chart-card">
              <h3>Status Mix</h3>
              <div className="status-breakdown-list">
                {data.statusBreakdown.map((item, index) => (
                  <div className="status-breakdown-item" key={item.label}>
                    <span
                      className="status-breakdown-dot"
                      style={{ backgroundColor: tones[index % tones.length] }}
                    />
                    <span>{item.label}</span>
                    <strong>{item.value}</strong>
                  </div>
                ))}
              </div>
            </article>
          </div>
        </div>
      )}
    </section>
  );
};

export default InsightHubDashboard;
