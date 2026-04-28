import React from "react";

const ActivityFeed = ({ items = [] }) => {
  if (items.length === 0) {
    return <p className="section-message">No recent activity available.</p>;
  }

  return (
    <div className="activity-list analytics-activity-list">
      {items.map((item, index) => (
        <div className="activity-item" key={`${item.title}-${index}`}>
          <div className="activity-icon">📌</div>
          <div className="activity-content">
            <p>{item.title}</p>
            <span className="activity-subtitle">{item.subtitle}</span>
            <span className="activity-time">{item.timestamp}</span>
          </div>
        </div>
      ))}
    </div>
  );
};

export default ActivityFeed;
