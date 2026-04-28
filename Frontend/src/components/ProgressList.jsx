import React from "react";

const ProgressList = ({ items = [] }) => {
  if (items.length === 0) {
    return <p className="section-message">No group progress data available.</p>;
  }

  return (
    <div className="progress-list">
      {items.map((item) => (
        <div className="progress-row" key={item.groupId}>
          <div className="progress-row-header">
            <div>
              <h4>{item.groupTitle}</h4>
              <p>{item.groupCode}</p>
            </div>
            <strong>{item.completionRate}%</strong>
          </div>
          <div className="progress-track">
            <div
              className="progress-fill"
              style={{ width: `${item.completionRate}%` }}
            />
          </div>
          <span className="progress-caption">
            {item.submittedCount} of {item.totalDeliverables} deliverables on
            time
          </span>
        </div>
      ))}
    </div>
  );
};

export default ProgressList;
