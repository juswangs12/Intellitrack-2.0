import React, { useState, useEffect } from "react";
import { Bell, X, Clock, AlertCircle, Info } from "lucide-react";
import { useAuth } from "../context/AuthContext";

const NotificationCenter = () => {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [readIds, setReadIds] = useState(new Set());
  const [showPanel, setShowPanel] = useState(false);

  useEffect(() => {
    if (!user?.id) return;
    const token = localStorage.getItem("token");
    fetch(`http://localhost:8080/api/deadlines/reminders?userId=${user.id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((data) => {
        if (Array.isArray(data)) setNotifications(data);
      })
      .catch(() => setNotifications([]));
  }, [user]);

  const unreadCount = notifications.filter(
    (n) => !readIds.has(n.deliverableId),
  ).length;

  const markAsRead = (id) => setReadIds((prev) => new Set([...prev, id]));
  const markAllAsRead = () =>
    setReadIds(new Set(notifications.map((n) => n.deliverableId)));
  const dismiss = (id) =>
    setNotifications((prev) => prev.filter((n) => n.deliverableId !== id));

  const iconFor = (riskLevel) => {
    if (riskLevel === "CRITICAL" || riskLevel === "HIGH") return AlertCircle;
    if (riskLevel === "MEDIUM") return Clock;
    return Info;
  };

  const colorFor = (riskLevel) => {
    if (riskLevel === "CRITICAL") return "#dc2626";
    if (riskLevel === "HIGH") return "#f59e0b";
    if (riskLevel === "MEDIUM") return "#3b82f6";
    return "#6b7280";
  };

  const badgeClassFor = (riskLevel) => {
    if (riskLevel === "CRITICAL" || riskLevel === "HIGH") return "danger";
    if (riskLevel === "MEDIUM") return "warning";
    return "info";
  };

  const formatHours = (h) => {
    if (h < 0) return `${Math.abs(h)}h overdue`;
    if (h < 24) return `${h}h remaining`;
    return `${Math.floor(h / 24)}d remaining`;
  };

  return (
    <>
      <div style={{ position: "relative" }}>
        <button
          onClick={() => setShowPanel(!showPanel)}
          className="navbar-notification"
        >
          <Bell style={{ width: "1.25rem", height: "1.25rem" }} />
          {unreadCount > 0 && (
            <span className="notification-badge">
              {unreadCount > 9 ? "9+" : unreadCount}
            </span>
          )}
        </button>
      </div>

      {showPanel && (
        <div
          style={{
            position: "fixed",
            top: "4.5rem",
            right: "1rem",
            width: "400px",
            maxHeight: "560px",
            background: "white",
            border: "1px solid #e5e7eb",
            borderRadius: "0.75rem",
            boxShadow: "0 10px 25px rgba(0,0,0,0.1)",
            zIndex: 1000,
            display: "flex",
            flexDirection: "column",
          }}
        >
          <div
            style={{
              padding: "1rem",
              borderBottom: "1px solid #e5e7eb",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <div>
              <h3
                style={{
                  fontWeight: "600",
                  fontSize: "1.125rem",
                  color: "#111827",
                  margin: 0,
                }}
              >
                Notifications
              </h3>
              <p style={{ fontSize: "0.75rem", color: "#6b7280", margin: 0 }}>
                Smart deadline alerts
              </p>
            </div>
            {unreadCount > 0 && (
              <button
                onClick={markAllAsRead}
                style={{
                  padding: "0.25rem 0.75rem",
                  background: "#800020",
                  color: "white",
                  border: "none",
                  borderRadius: "0.375rem",
                  fontSize: "0.75rem",
                  cursor: "pointer",
                  fontWeight: "500",
                }}
              >
                Mark all read
              </button>
            )}
          </div>
          <div style={{ overflowY: "auto", flex: 1 }}>
            {notifications.length === 0 ? (
              <div style={{ padding: "3rem 1rem", textAlign: "center" }}>
                <p style={{ color: "#6b7280", fontSize: "0.875rem" }}>
                  No notifications
                </p>
              </div>
            ) : (
              notifications.map((n) => {
                const Icon = iconFor(n.riskLevel);
                const color = colorFor(n.riskLevel);
                const isRead = readIds.has(n.deliverableId);
                return (
                  <div
                    key={n.deliverableId}
                    onClick={() => markAsRead(n.deliverableId)}
                    style={{
                      padding: "1rem",
                      borderBottom: "1px solid #f3f4f6",
                      background: isRead ? "white" : "#fef3f2",
                      cursor: "pointer",
                    }}
                  >
                    <div style={{ display: "flex", gap: "0.75rem" }}>
                      <div
                        style={{
                          flexShrink: 0,
                          width: "2.5rem",
                          height: "2.5rem",
                          borderRadius: "0.5rem",
                          background: `${color}20`,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                        }}
                      >
                        <Icon
                          style={{ width: "1.25rem", height: "1.25rem", color }}
                        />
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "start",
                            marginBottom: "0.25rem",
                          }}
                        >
                          <p
                            style={{
                              fontWeight: "600",
                              fontSize: "0.875rem",
                              color: "#111827",
                              margin: 0,
                            }}
                          >
                            {n.deliverableName}
                          </p>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              dismiss(n.deliverableId);
                            }}
                            style={{
                              padding: "0.25rem",
                              background: "transparent",
                              border: "none",
                              cursor: "pointer",
                            }}
                          >
                            <X
                              style={{
                                width: "1rem",
                                height: "1rem",
                                color: "#9ca3af",
                              }}
                            />
                          </button>
                        </div>
                        <p
                          style={{
                            fontSize: "0.75rem",
                            color: "#6b7280",
                            marginBottom: "0.5rem",
                            lineHeight: 1.4,
                          }}
                        >
                          {n.message}
                        </p>
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                          }}
                        >
                          <span
                            style={{ fontSize: "0.7rem", color: "#9ca3af" }}
                          >
                            {formatHours(n.hoursRemaining)}
                          </span>
                          <span
                            className={`badge ${badgeClassFor(n.riskLevel)}`}
                            style={{ fontSize: "0.65rem" }}
                          >
                            {n.riskLevel}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {showPanel && (
        <div
          onClick={() => setShowPanel(false)}
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            zIndex: 999,
          }}
        />
      )}
    </>
  );
};

export default NotificationCenter;
