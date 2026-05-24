import React from "react";

const rateColor = (pct) => pct >= 80 ? "#10b981" : pct >= 50 ? "#f59e0b" : "#ef4444";

const ProgressList = ({ items = [] }) => {
  if (items.length === 0) {
    return (
      <p style={{ textAlign: "center", color: "#9ca3af", fontSize: "0.875rem", padding: "0.5rem 0", margin: 0 }}>
        No group progress data available.
      </p>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
      {items.map((item, i) => {
        const color = rateColor(item.completionRate);
        return (
          <div
            key={item.groupId}
            style={{
              padding: "0.875rem 1rem",
              backgroundColor: i % 2 === 0 ? "#f9fafb" : "#fff",
              borderRadius: "0.5rem",
              border: "1px solid #e5e7eb",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "0.5rem" }}>
              <div>
                <p style={{ margin: 0, fontWeight: 700, fontSize: "0.875rem", color: "#111827" }}>{item.groupTitle}</p>
                <p style={{ margin: 0, fontSize: "0.75rem", color: "#6b7280" }}>{item.groupCode}</p>
              </div>
              <span style={{ fontWeight: 800, fontSize: "1rem", color }}>{item.completionRate}%</span>
            </div>
            <div style={{ height: "6px", backgroundColor: "#e5e7eb", borderRadius: "9999px", overflow: "hidden", marginBottom: "0.375rem" }}>
              <div style={{ height: "100%", width: `${Math.min(item.completionRate, 100)}%`, backgroundColor: color, borderRadius: "9999px", transition: "width 0.4s ease" }} />
            </div>
            <p style={{ margin: 0, fontSize: "0.73rem", color: "#9ca3af" }}>
              {item.submittedCount} of {item.totalDeliverables} deliverables on time
            </p>
          </div>
        );
      })}
    </div>
  );
};

export default ProgressList;
