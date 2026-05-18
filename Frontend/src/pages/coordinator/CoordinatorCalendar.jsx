import React, { useEffect, useMemo, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import apiService from "../../services/ApiService";

const CoordinatorCalendar = () => {
  const today = new Date();
  const [currentMonth, setCurrentMonth] = useState(today.getMonth());
  const [currentYear, setCurrentYear] = useState(today.getFullYear());
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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
    return events.filter((d) => d.date === dateStr);
  };

  useEffect(() => {
    let mounted = true;

    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await apiService.getDeadlineCalendar(
          currentYear,
          currentMonth + 1,
        );
        if (!mounted) return;

        const mapped = (Array.isArray(data) ? data : [])
          .map((item) => {
            const dueAt = item.dueAt ? new Date(item.dueAt) : null;
            if (!dueAt || Number.isNaN(dueAt.getTime())) return null;
            const date = dueAt.toISOString().slice(0, 10);
            return {
              id: item.deadlineId,
              date,
              dueAt: item.dueAt,
              title: item.deliverableName,
              stage: item.stage,
              type: "deadline",
            };
          })
          .filter(Boolean);

        setEvents(mapped);
      } catch (err) {
        if (mounted) {
          setError("Failed to load calendar deadlines.");
          setEvents([]);
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };

    load();

    return () => {
      mounted = false;
    };
  }, [currentMonth, currentYear]);

  const upcoming = useMemo(() => {
    const now = Date.now();
    return events
      .filter((ev) => {
        const t = new Date(ev.dueAt).getTime();
        return !Number.isNaN(t) && t >= now;
      })
      .sort((a, b) => new Date(a.dueAt) - new Date(b.dueAt))
      .slice(0, 10);
  }, [events]);

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
          View upcoming deadlines and milestone events.
        </p>
      </div>

      {error && <div className="error-message">{error}</div>}

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
                      key={ev.id ?? idx}
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
            {loading ? (
              <div style={{ padding: "1rem", color: "#6b7280" }}>
                Loading events...
              </div>
            ) : upcoming.length === 0 ? (
              <div style={{ padding: "1rem", color: "#6b7280" }}>
                No upcoming deadlines scheduled.
              </div>
            ) : (
              upcoming.map((ev) => (
                <div
                  key={ev.id}
                  style={{
                    padding: "0.75rem",
                    border: "1px solid #e5e7eb",
                    borderRadius: "0.5rem",
                    borderLeft: "4px solid var(--maroon)",
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
                    {new Date(ev.dueAt).toLocaleString()}
                  </p>
                  <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.5rem" }}>
                    <span className="badge danger" style={{ fontSize: "0.65rem" }}>
                      deadline
                    </span>
                    <span className="badge info" style={{ fontSize: "0.65rem" }}>
                      {ev.stage}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CoordinatorCalendar;
