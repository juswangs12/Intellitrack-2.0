import React from "react";

const MetricCard = ({ label, value, tone = "neutral", suffix = "" }) => {
  const displayValue = Number.isInteger(value)
    ? value
    : (value?.toFixed?.(1) ?? value);

  return (
    <article className={`metric-card metric-card-${tone}`}>
      <span className="metric-card-label">{label}</span>
      <strong className="metric-card-value">
        {displayValue}
        {suffix}
      </strong>
    </article>
  );
};

export default MetricCard;
