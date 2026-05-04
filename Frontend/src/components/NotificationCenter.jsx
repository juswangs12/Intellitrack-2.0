import React, { useState, useEffect } from "react";
import {
  Bell,
  X,
  Clock,
  AlertCircle,
  Info,
  CheckCircle,
  BellRing,
} from "lucide-react";

const NotificationCenter = ({ userRole }) => {
  const [notifications, setNotifications] = useState([]);
  const [showPanel, setShowPanel] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    const generated = generateNotifications(userRole);
    setNotifications(generated);
    setUnreadCount(generated.filter((n) => !n.read).length);
    const interval = setInterval(() => {
      const updated = generateNotifications(userRole);
      setNotifications(updated);
      setUnreadCount(updated.filter((n) => !n.read).length);
    }, 30000);
    return () => clearInterval(interval);
  }, [userRole]);

  const generateNotifications = (role) => {
    const now = new Date();
    const list = [];
    if (role === "student") {
      list.push({
        id: 1,
        type: "deadline-urgent",
        title: "Urgent: Chapter 3 Deadline Tomorrow",
        message:
          "Chapter 3 submission is due tomorrow at 11:59 PM. You haven't submitted yet.",
        priority: "high",
        timestamp: new Date(now - 1000 * 60 * 15).toISOString(),
        read: false,
        icon: AlertCircle,
        color: "#dc2626",
      });
      list.push({
        id: 2,
        type: "review-complete",
        title: "Adviser Reviewed Your Submission",
        message: "Your adviser has completed reviewing your last submission.",
        priority: "medium",
        timestamp: new Date(now - 1000 * 60 * 60 * 4).toISOString(),
        read: false,
        icon: CheckCircle,
        color: "#10b981",
      });
      list.push({
        id: 3,
        type: "deadline-info",
        title: "Upcoming: SDD Due Next Week",
        message: "Start working on SDD now to avoid last-minute rush.",
        priority: "low",
        timestamp: new Date(now - 1000 * 60 * 60 * 6).toISOString(),
        read: true,
        icon: Info,
        color: "#3b82f6",
      });
    } else if (role === "adviser" || role === "coordinator") {
      list.push({
        id: 4,
        type: "submission",
        title: "New Submission Pending Review",
        message:
          "A team submitted a document for review. AI Pre-Analysis: Structure complete.",
        priority: "high",
        timestamp: new Date(now - 1000 * 60 * 10).toISOString(),
        read: false,
        icon: BellRing,
        color: "#800020",
      });
      list.push({
        id: 5,
        type: "deadline-alert",
        title: "Reminder: Review Due Today",
        message:
          "A team is waiting for your feedback on their submission from 3 days ago.",
        priority: "high",
        timestamp: new Date(now - 1000 * 60 * 30).toISOString(),
        read: false,
        icon: Clock,
        color: "#dc2626",
      });
    } else if (role === "administrator") {
      list.push({
        id: 6,
        type: "system",
        title: "3 New User Registrations",
        message: "3 new users registered today. Review and assign roles.",
        priority: "medium",
        timestamp: new Date(now - 1000 * 60 * 20).toISOString(),
        read: false,
        icon: BellRing,
        color: "#800020",
      });
      list.push({
        id: 7,
        type: "deadline-alert",
        title: "Deadline Alert: Groups Due Tomorrow",
        message:
          "Some groups haven't submitted yet. Consider sending a reminder.",
        priority: "medium",
        timestamp: new Date(now - 1000 * 60 * 60).toISOString(),
        read: false,
        icon: AlertCircle,
        color: "#f59e0b",
      });
    }
    return list.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
  };

  const markAsRead = (id) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n)),
    );
    setUnreadCount((prev) => Math.max(0, prev - 1));
  };
  const markAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);
  };
  const deleteNotification = (id) => {
    const n = notifications.find((x) => x.id === id);
    setNotifications((prev) => prev.filter((x) => x.id !== id));
    if (n && !n.read) setUnreadCount((prev) => Math.max(0, prev - 1));
  };
  const getTimeAgo = (ts) => {
    const diff = Math.floor((new Date() - new Date(ts)) / (1000 * 60));
    if (diff < 1) return "Just now";
    if (diff < 60) return `${diff} min ago`;
    const h = Math.floor(diff / 60);
    if (h < 24) return `${h} hour${h > 1 ? "s" : ""} ago`;
    const d = Math.floor(h / 24);
    return `${d} day${d > 1 ? "s" : ""} ago`;
  };
  const getPriority = (p) =>
    ({
      high: { label: "Urgent", cls: "danger" },
      medium: { label: "Important", cls: "warning" },
    })[p] || { label: "Info", cls: "info" };

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
              notifications.map((notification) => {
                const Icon = notification.icon;
                const priority = getPriority(notification.priority);
                return (
                  <div
                    key={notification.id}
                    onClick={() => markAsRead(notification.id)}
                    style={{
                      padding: "1rem",
                      borderBottom: "1px solid #f3f4f6",
                      background: notification.read ? "white" : "#fef3f2",
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
                          background: `${notification.color}20`,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                        }}
                      >
                        <Icon
                          style={{
                            width: "1.25rem",
                            height: "1.25rem",
                            color: notification.color,
                          }}
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
                            {notification.title}
                          </p>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              deleteNotification(notification.id);
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
                          {notification.message}
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
                            {getTimeAgo(notification.timestamp)}
                          </span>
                          <span
                            className={`badge ${priority.cls}`}
                            style={{ fontSize: "0.65rem" }}
                          >
                            {priority.label}
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
