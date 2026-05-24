import React from "react";

const ActivityFeed = ({ items = [] }) => {
  if (items.length === 0) {
    return (
      <p style={{ textAlign: "center", color: "#9ca3af", fontSize: "0.875rem", padding: "1rem 0", margin: 0 }}>
        No recent activity available.
      </p>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "0" }}>
      {items.map((item, index) => (
        <div
          key={`${item.title}-${index}`}
          style={{
            display: "flex",
            gap: "0.75rem",
            padding: "0.75rem 0",
            borderBottom: index < items.length - 1 ? "1px solid #f3f4f6" : "none",
            alignItems: "flex-start",
          }}
        >
          <div style={{
            width: "2rem", height: "2rem", borderRadius: "50%",
            backgroundColor: "#fef2f2", border: "1px solid #fecaca",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: "0.875rem", flexShrink: 0,
          }}>📌</div>
          <div style={{ minWidth: 0 }}>
            <p style={{ margin: 0, fontSize: "0.8125rem", fontWeight: 600, color: "#111827", lineHeight: 1.4 }}>{item.title}</p>
            {item.subtitle && <p style={{ margin: "0.1rem 0 0", fontSize: "0.75rem", color: "#6b7280" }}>{item.subtitle}</p>}
            {item.timestamp && <p style={{ margin: "0.1rem 0 0", fontSize: "0.7rem", color: "#9ca3af" }}>{item.timestamp}</p>}
          </div>
        </div>
      ))}
    </div>
  );
};

export default ActivityFeed;
