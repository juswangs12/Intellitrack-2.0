import React, { useEffect, useState } from "react";
import ActivityFeed from "./ActivityFeed";
import apiService from "../services/ApiService";

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
        const tracking = await apiService.getTrackingAnalytics(
          adviserId || undefined,
        );
        if (!mounted) {
          return;
        }

        setGroups(tracking.groupProgress || []);
        if ((tracking.groupProgress || []).length > 0) {
          setSelectedGroupId(String(tracking.groupProgress[0].groupId));
        }
      } catch (err) {
        if (mounted) {
          setError("Unable to load groups for summary view.");
        }
      }
    };

    loadGroups();

    return () => {
      mounted = false;
    };
  }, [adviserId]);

  useEffect(() => {
    if (!selectedGroupId) {
      setSummary(null);
      return;
    }

    let mounted = true;

    const loadSummary = async () => {
      setLoading(true);
      setError("");

      try {
        const result = await apiService.getSubmissionSummary(selectedGroupId);
        if (mounted) {
          setSummary(result);
        }
      } catch (err) {
        if (mounted) {
          setError("Unable to load submission summary right now.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadSummary();

    return () => {
      mounted = false;
    };
  }, [selectedGroupId]);

  return (
    <section className="analytics-panel">
      <div className="section-heading-row">
        <div>
          <h2>Institutional Oversight Dashboard</h2>
          <p>
            Review AI-generated submission summaries, revision history, and
            current deliverable status.
          </p>
        </div>
        <label className="analytics-filter">
          <span>Student Group</span>
          <select
            value={selectedGroupId}
            onChange={(event) => setSelectedGroupId(event.target.value)}
          >
            {groups.map((group) => (
              <option key={group.groupId} value={group.groupId}>
                {group.groupCode} - {group.groupTitle}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loading && (
        <p className="section-message">Loading submission summary...</p>
      )}
      {error && <p className="section-message error">{error}</p>}

      {!loading && !error && summary && (
        <div className="analytics-stack">
          <article className="summary-hero-card">
            <span className="status-stage">{summary.groupCode}</span>
            <h3>{summary.groupTitle}</h3>
            <p>{summary.headlineSummary}</p>
            <span className="summary-detail-copy">{summary.detailSummary}</span>
          </article>

          <div className="analytics-grid analytics-grid-two-up">
            <article className="chart-card">
              <h3>Deliverable Summary</h3>
              <div className="summary-table">
                <div className="summary-table-header">
                  <span>Deliverable</span>
                  <span>Status</span>
                  <span>Submitted</span>
                  <span>Revisions</span>
                </div>
                {summary.deliverables.map((item) => (
                  <div className="summary-table-row" key={item.deliverableId}>
                    <span>
                      <strong>{item.deliverableName}</strong>
                      <small>{item.stage}</small>
                    </span>
                    <span
                      className={`status-chip ${item.status.toLowerCase()}`}
                    >
                      {item.status}
                    </span>
                    <span>
                      {item.submittedAt
                        ? new Date(item.submittedAt).toLocaleString()
                        : "Not yet submitted"}
                    </span>
                    <span>{item.revisionCount}</span>
                  </div>
                ))}
              </div>
            </article>

            <article className="chart-card">
              <h3>Submission Timeline</h3>
              <ActivityFeed items={summary.timeline} />
            </article>
          </div>
        </div>
      )}
    </section>
  );
};

export default InstitutionalOversightDashboard;
