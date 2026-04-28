import React, { useEffect, useState } from "react";
import apiService from "../services/ApiService";

const SubmissionTrackerDashboard = ({ groupId }) => {
  const [deadlines, setDeadlines] = useState([]);
  const [calendar, setCalendar] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!groupId) {
      setDeadlines([]);
      setCalendar([]);
      return;
    }

    let mounted = true;

    const loadTracker = async () => {
      setLoading(true);
      setError("");

      try {
        const now = new Date();
        const [deadlineData, calendarData] = await Promise.all([
          apiService.getActiveDeadlines(groupId),
          apiService.getDeadlineCalendar(now.getFullYear(), now.getMonth() + 1),
        ]);

        if (mounted) {
          setDeadlines(deadlineData);
          setCalendar(calendarData);
        }
      } catch (err) {
        if (mounted) {
          setError("Unable to load deadline tracking right now.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadTracker();

    return () => {
      mounted = false;
    };
  }, [groupId]);

  return (
    <section className="analytics-panel">
      <div className="section-heading-row">
        <div>
          <h2>Submission Tracker Dashboard</h2>
          <p>
            Monitor active milestones, countdown windows, and AI-generated risk
            flags.
          </p>
        </div>
      </div>

      {loading && (
        <p className="section-message">Loading deadline tracker...</p>
      )}
      {error && <p className="section-message error">{error}</p>}

      {!loading && !error && (
        <div className="analytics-stack">
          <div className="deadline-grid">
            {deadlines.map((item) => (
              <article
                key={item.deadlineId ?? item.deliverableId}
                className={`deadline-card ${item.riskLevel.toLowerCase()}`}
              >
                <div className="deadline-card-header">
                  <div>
                    <span className="status-stage">{item.stage}</span>
                    <h3>{item.deliverableName}</h3>
                  </div>
                  <span className={`status-chip ${item.status.toLowerCase()}`}>
                    {item.status}
                  </span>
                </div>
                <strong className="deadline-countdown">
                  {item.countdownLabel}
                </strong>
                <p className="deadline-due-copy">
                  Due{" "}
                  {item.dueAt ? new Date(item.dueAt).toLocaleString() : "TBD"}
                </p>
                <div className="deadline-risk-row">
                  <span className={`risk-pill ${item.riskLevel.toLowerCase()}`}>
                    {item.riskLevel}
                  </span>
                  <span>Risk score: {item.riskScore}</span>
                </div>
                <p className="deadline-explanation">{item.riskExplanation}</p>
              </article>
            ))}
          </div>

          <article className="chart-card">
            <h3>Deadline Calendar</h3>
            <div className="calendar-list">
              {calendar.map((item) => (
                <div className="calendar-row" key={item.deadlineId}>
                  <div>
                    <strong>{item.deliverableName}</strong>
                    <small>{item.stage}</small>
                  </div>
                  <span>{new Date(item.dueAt).toLocaleDateString()}</span>
                </div>
              ))}
            </div>
          </article>
        </div>
      )}
    </section>
  );
};

export default SubmissionTrackerDashboard;
