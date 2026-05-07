import React, { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

const CoordinatorCalendar = () => {
  const today = new Date();
  const [currentMonth, setCurrentMonth] = useState(today.getMonth());
  const [currentYear, setCurrentYear] = useState(today.getFullYear());

  const deadlines = [
    {
      date: "2025-12-10",
      title: "Project Proposal Deadline",
      type: "deadline",
      teams: 12,
    },
    {
      date: "2025-12-20",
      title: "SRS Submission Deadline",
      type: "deadline",
      teams: 8,
    },
    {
      date: "2026-01-10",
      title: "SDD Submission Deadline",
      type: "deadline",
      teams: 10,
    },
    {
      date: "2025-12-15",
      title: "Group Alpha Defense",
      type: "defense",
      teams: 1,
    },
    {
      date: "2025-12-18",
      title: "Group Beta Defense",
      type: "defense",
      teams: 1,
    },
  ];

  const getDaysInMonth = (month, year) =>
    new Date(year, month + 1, 0).getDate();
  const getFirstDayOfMonth = (month, year) => new Date(year, month, 1).getDay();

  const prevMonth = () => {
    if (currentMonth === 0) {
      setCurrentMonth(11);
      setCurrentYear(currentYear - 1);
    } else setCurrentMonth(currentMonth - 1);
  };

  const nextMonth = () => {
    if (currentMonth === 11) {
      setCurrentMonth(0);
      setCurrentYear(currentYear + 1);
    } else setCurrentMonth(currentMonth + 1);
  };

  const getEventsForDay = (day) => {
    const dateStr = `${currentYear}-${String(currentMonth + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    return deadlines.filter((d) => d.date === dateStr);
  };

  const monthNames = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  ];
  const days = getDaysInMonth(currentMonth, currentYear);
  const firstDay = getFirstDayOfMonth(currentMonth, currentYear);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Calendar</h1>
        <p className="page-description">
          View upcoming deadlines, defenses, and events.
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "2fr 1fr",
          gap: "1.5rem",
        }}
      >
        <div className="card">
          <div
            className="card-header"
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <h2 className="card-title">
              {monthNames[currentMonth]} {currentYear}
            </h2>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <button
                className="btn btn-secondary"
                style={{ padding: "0.4rem" }}
                onClick={prevMonth}
              >
                <ChevronLeft style={{ width: "1rem", height: "1rem" }} />
              </button>
              <button
                className="btn btn-secondary"
                style={{ padding: "0.4rem" }}
                onClick={nextMonth}
              >
                <ChevronRight style={{ width: "1rem", height: "1rem" }} />
              </button>
            </div>
          </div>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(7, 1fr)",
              gap: "2px",
            }}
          >
            {["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"].map((d) => (
              <div
                key={d}
                style={{
                  padding: "0.5rem",
                  textAlign: "center",
                  fontWeight: "600",
                  fontSize: "0.75rem",
                  color: "#6b7280",
                }}
              >
                {d}
              </div>
            ))}
            {Array.from({ length: firstDay }).map((_, i) => (
              <div key={`empty-${i}`} />
            ))}
            {Array.from({ length: days }).map((_, i) => {
              const day = i + 1;
              const events = getEventsForDay(day);
              const isToday =
                day === today.getDate() &&
                currentMonth === today.getMonth() &&
                currentYear === today.getFullYear();
              return (
                <div
                  key={day}
                  style={{
                    padding: "0.5rem",
                    minHeight: "4rem",
                    border: "1px solid #e5e7eb",
                    borderRadius: "0.25rem",
                    background: isToday ? "rgba(128,0,32,0.05)" : "white",
                  }}
                >
                  <span
                    style={{
                      fontWeight: isToday ? "700" : "400",
                      color: isToday ? "var(--maroon)" : "#374151",
                      fontSize: "0.875rem",
                    }}
                  >
                    {day}
                  </span>
                  {events.map((ev, idx) => (
                    <div
                      key={idx}
                      style={{
                        marginTop: "2px",
                        padding: "1px 4px",
                        borderRadius: "2px",
                        fontSize: "0.65rem",
                        background:
                          ev.type === "deadline" ? "#fee2e2" : "#dbeafe",
                        color: ev.type === "deadline" ? "#991b1b" : "#1e40af",
                        overflow: "hidden",
                        whiteSpace: "nowrap",
                        textOverflow: "ellipsis",
                      }}
                    >
                      {ev.title}
                    </div>
                  ))}
                </div>
              );
            })}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Upcoming Events</h2>
          </div>
          <div
            style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}
          >
            {deadlines.map((ev, i) => (
              <div
                key={i}
                style={{
                  padding: "0.75rem",
                  border: "1px solid #e5e7eb",
                  borderRadius: "0.5rem",
                  borderLeft: `4px solid ${ev.type === "deadline" ? "var(--maroon)" : "#3b82f6"}`,
                }}
              >
                <p
                  style={{
                    fontWeight: "600",
                    fontSize: "0.875rem",
                    margin: "0 0 0.25rem",
                  }}
                >
                  {ev.title}
                </p>
                <p style={{ fontSize: "0.75rem", color: "#6b7280", margin: 0 }}>
                  {new Date(ev.date).toLocaleDateString("en-US", {
                    month: "long",
                    day: "numeric",
                    year: "numeric",
                  })}
                </p>
                <span
                  className={`badge ${ev.type === "deadline" ? "danger" : "info"}`}
                  style={{ fontSize: "0.65rem", marginTop: "0.25rem" }}
                >
                  {ev.type}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CoordinatorCalendar;
