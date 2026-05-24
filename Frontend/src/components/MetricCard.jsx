import React from "react";

const TONE_STYLES = {
  success: { gradient: "linear-gradient(135deg,#065f46,#10b981)", icon: "✓" },
  warning: { gradient: "linear-gradient(135deg,#92400e,#f59e0b)", icon: "⏳" },
  danger:  { gradient: "linear-gradient(135deg,#7f1d1d,#b91c1c)", icon: "!" },
  info:    { gradient: "linear-gradient(135deg,#1e3a8a,#3b82f6)", icon: "ℹ" },
  neutral: { gradient: "linear-gradient(135deg,#374151,#6b7280)", icon: "#" },
};

const MetricCard = ({ label, value, tone = "neutral", suffix = "" }) => {
  const displayValue = Number.isInteger(value) ? value : (value?.toFixed?.(1) ?? value);
  const { gradient, icon } = TONE_STYLES[tone] ?? TONE_STYLES.neutral;

  return (
    <div style={{
      background: gradient,
      borderRadius: "0.75rem",
      padding: "1.1rem 1rem",
      display: "flex",
      alignItems: "center",
      gap: "0.875rem",
      boxShadow: "0 3px 10px rgba(0,0,0,0.12)",
      color: "#fff",
      minWidth: 0,
    }}>
      <div style={{
        width: "2.25rem", height: "2.25rem",
        backgroundColor: "rgba(255,255,255,0.18)",
        borderRadius: "0.5rem",
        display: "flex", alignItems: "center", justifyContent: "center",
        fontSize: "1rem", flexShrink: 0,
      }}>
        {icon}
      </div>
      <div style={{ minWidth: 0 }}>
        <p style={{ margin: 0, fontSize: "0.7375rem", opacity: 0.82, fontWeight: 500, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{label}</p>
        <p style={{ margin: 0, fontSize: "1.6rem", fontWeight: 800, lineHeight: 1.15 }}>{displayValue}{suffix}</p>
      </div>
    </div>
  );
};

export default MetricCard;
