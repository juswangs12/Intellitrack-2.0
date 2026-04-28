import React, { useEffect, useState } from "react";
import apiService from "../services/ApiService";

const NotificationCenter = ({ userId }) => {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!userId) {
      setMessages([]);
      return;
    }

    let mounted = true;

    const loadMessages = async () => {
      setLoading(true);
      setError("");

      try {
        const data = await apiService.getDeadlineReminders(userId);
        if (mounted) {
          setMessages(data);
        }
      } catch (err) {
        if (mounted) {
          setError("Unable to load reminder feed right now.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadMessages();

    return () => {
      mounted = false;
    };
  }, [userId]);

  return (
    <section className="analytics-panel">
      <div className="section-heading-row">
        <div>
          <h2>Notification Center</h2>
          <p>
            Personalized alerts for approaching deadlines and AI-detected
            delivery risk.
          </p>
        </div>
      </div>

      {loading && <p className="section-message">Loading reminders...</p>}
      {error && <p className="section-message error">{error}</p>}

      {!loading && !error && (
        <div className="notification-list">
          {messages.map((message, index) => (
            <article
              className={`notification-card ${message.riskLevel.toLowerCase()}`}
              key={`${message.deliverableId}-${index}`}
            >
              <div className="notification-card-header">
                <strong>{message.deliverableName}</strong>
                <span
                  className={`risk-pill ${message.riskLevel.toLowerCase()}`}
                >
                  {message.riskLevel}
                </span>
              </div>
              <p>{message.message}</p>
              <span className="activity-time">
                {message.hoursRemaining >= 0
                  ? `${message.hoursRemaining}h remaining`
                  : `${Math.abs(message.hoursRemaining)}h overdue`}
              </span>
            </article>
          ))}
        </div>
      )}
    </section>
  );
};

export default NotificationCenter;
