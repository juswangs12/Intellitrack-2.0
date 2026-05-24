import React, { useState } from "react";
import { ChevronDown, ChevronRight, Users } from "lucide-react";

const rateColor = (pct) => pct >= 80 ? "#10b981" : pct >= 50 ? "#f59e0b" : "#ef4444";

const statusColor = (status) => {
  switch (status) {
    case "ON_TIME":
      return "#10b981";
    case "LATE":
      return "#f59e0b";
    case "NOT_SUBMITTED":
      return "#ef4444";
    default:
      return "#6b7280";
  }
};

const statusLabel = (status) => {
  switch (status) {
    case "ON_TIME":
      return "On Time";
    case "LATE":
      return "Late";
    case "NOT_SUBMITTED":
      return "Not Submitted";
    default:
      return "Unknown";
  }
};

const ProgressList = ({ items = [] }) => {
  const [expandedGroups, setExpandedGroups] = useState({});

  const toggleGroup = (groupId) => {
    setExpandedGroups((prev) => ({ ...prev, [groupId]: !prev[groupId] }));
  };

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
        const isExpanded = expandedGroups[item.groupId];

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
            {/* Group header - clickable to expand */}
            <div
              onClick={() => toggleGroup(item.groupId)}
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-start",
                marginBottom: "0.5rem",
                cursor: "pointer",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                {isExpanded ? (
                  <ChevronDown size={16} style={{ color: "#6b7280" }} />
                ) : (
                  <ChevronRight size={16} style={{ color: "#6b7280" }} />
                )}
                <div>
                  <p style={{ margin: 0, fontWeight: 700, fontSize: "0.875rem", color: "#111827" }}>
                    {item.groupTitle}
                  </p>
                  <p style={{ margin: 0, fontSize: "0.75rem", color: "#6b7280" }}>
                    {item.groupCode}
                  </p>
                </div>
              </div>
              <div style={{ textAlign: "right" }}>
                <span style={{ fontWeight: 800, fontSize: "1rem", color }}>
                  {item.completionRate}%
                </span>
                <p style={{ margin: 0, fontSize: "0.73rem", color: "#9ca3af" }}>
                  {item.submittedCount} of {item.totalDeliverables}
                </p>
              </div>
            </div>

            {/* Completion bar */}
            <div style={{ height: "6px", backgroundColor: "#e5e7eb", borderRadius: "9999px", overflow: "hidden", marginBottom: "0.375rem" }}>
              <div style={{ height: "100%", width: `${Math.min(item.completionRate, 100)}%`, backgroundColor: color, borderRadius: "9999px", transition: "width 0.4s ease" }} />
            </div>

            {/* Student participation summary */}
            <div style={{ display: "flex", gap: "1rem", marginBottom: "0.5rem", flexWrap: "wrap" }}>
              <span style={{ display: "flex", alignItems: "center", gap: "0.375rem", fontSize: "0.75rem", color: "#10b981" }}>
                <Users size={14} /> On Time: {item.onTimeStudents || 0}
              </span>
              <span style={{ display: "flex", alignItems: "center", gap: "0.375rem", fontSize: "0.75rem", color: "#f59e0b" }}>
                Late: {item.lateStudents || 0}
              </span>
              <span style={{ display: "flex", alignItems: "center", gap: "0.375rem", fontSize: "0.75rem", color: "#ef4444" }}>
                Missing: {item.missingStudents || 0}
              </span>
            </div>

            {/* Student participation details (expanded) */}
            {isExpanded && item.studentParticipations && item.studentParticipations.length > 0 && (
              <div style={{ marginTop: "1rem", paddingTop: "1rem", borderTop: "1px solid #e5e7eb" }}>
                <h4 style={{ margin: "0 0 0.5rem 0", fontSize: "0.8125rem", fontWeight: 600, color: "#374151" }}>
                  Student Participation
                </h4>
                <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                  {item.studentParticipations.map((student) => (
                    <div
                      key={student.studentId}
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        padding: "0.5rem 0.75rem",
                        backgroundColor: "#fff",
                        borderRadius: "0.375rem",
                        border: "1px solid #e5e7eb",
                      }}
                    >
                      <div>
                        <p style={{ margin: 0, fontSize: "0.8125rem", fontWeight: 500, color: "#1f2937" }}>
                          {student.studentName}
                        </p>
                        {(student.subjectCode || student.sectionName) && (
                          <p style={{ margin: 0, fontSize: "0.7rem", color: "#9ca3af" }}>
                            {student.subjectCode && student.sectionName
                              ? `${student.subjectCode} - ${student.sectionName}`
                              : student.subjectCode || student.sectionName}
                          </p>
                        )}
                      </div>
                      <span
                        style={{
                          padding: "0.25rem 0.625rem",
                          borderRadius: "9999px",
                          fontSize: "0.7rem",
                          fontWeight: 600,
                          backgroundColor: `${statusColor(student.status)}15`,
                          color: statusColor(student.status),
                        }}
                      >
                        {statusLabel(student.status)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default ProgressList;
