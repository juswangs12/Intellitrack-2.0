import React, { useEffect, useState } from "react";
import apiService from "../services/ApiService";

const formatDeadline = (value) => {
  if (!value) {
    return "No deadline assigned";
  }

  return new Date(value).toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
};

const formatCountdown = (hoursRemaining) => {
  if (hoursRemaining === null || hoursRemaining === undefined) {
    return "Deadline unavailable";
  }

  if (hoursRemaining < 0) {
    return `${Math.abs(hoursRemaining)}h overdue`;
  }

  if (hoursRemaining < 24) {
    return `${hoursRemaining}h remaining`;
  }

  const daysRemaining = Math.floor(hoursRemaining / 24);
  return `${daysRemaining}d remaining`;
};

const StatusMonitoringPanel = ({ groupId }) => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!groupId) {
      setItems([]);
      return;
    }

    let mounted = true;

    const loadStatuses = async () => {
      setLoading(true);
      setError("");

      try {
        const data = await apiService.getGroupStatuses(groupId);
        if (mounted) {
          setItems(data);
        }
      } catch (err) {
        if (mounted) {
          setError("Unable to load deliverable status right now.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadStatuses();

    return () => {
      mounted = false;
    };
  }, [groupId]);

  return (
    <section className="status-monitoring-panel">
      <div className="section-heading-row">
        <div>
          <h2>Status Monitoring</h2>
          <p>Track each deliverable against its current deadline window.</p>
        </div>
      </div>

      {loading && (
        <p className="section-message">Loading submission status...</p>
      )}
      {error && <p className="section-message error">{error}</p>}

      {!loading && !error && (
        <div className="status-monitoring-grid">
          {items.map((item) => (
            <article
              key={item.deliverableId}
              className={`status-monitoring-card ${item.status.toLowerCase()}`}
            >
              <div className="status-monitoring-card-header">
                <div>
                  <span className="status-stage">{item.stage}</span>
                  <h3>{item.deliverableName}</h3>
                </div>
                <span className={`status-chip ${item.status.toLowerCase()}`}>
                  {item.status}
                </span>
              </div>

              <dl className="status-monitoring-meta">
                <div>
                  <dt>Deadline</dt>
                  <dd>{formatDeadline(item.dueAt)}</dd>
                </div>
                <div>
                  <dt>Countdown</dt>
                  <dd>{formatCountdown(item.hoursRemaining)}</dd>
                </div>
                <div>
                  <dt>Submitted</dt>
                  <dd>
                    {item.submittedAt
                      ? formatDeadline(item.submittedAt)
                      : "Not yet submitted"}
                  </dd>
                </div>
                <div>
                  <dt>Revisions</dt>
                  <dd>{item.revisionCount}</dd>
                </div>
              </dl>
            </article>
          ))}
        </div>
      )}
    </section>
  );
};

export default StatusMonitoringPanel;
