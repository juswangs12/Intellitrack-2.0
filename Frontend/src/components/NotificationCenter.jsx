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
import { useAuth } from "../context/AuthContext";
import apiService from "../services/ApiService";

const NotificationCenter = () => {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [showPanel, setShowPanel] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    const fetchNotifications = async () => {
      if (!user?.id) return;
      try {
        const response = await apiService.requestJson(`/notifications/${user.id}`);
        const data = Array.isArray(response) ? response : [];
        setNotifications(data);
        setUnreadCount(data.filter((n) => !n.read && !n.isRead).length);
      } catch (err) {
        console.error("Failed to fetch notifications", err);
      }
    };

    fetchNotifications();
    const interval = setInterval(fetchNotifications, 30000);
    return () => clearInterval(interval);
  }, [user]);

  const markAsRead = async (id) => {
    try {
      await apiService.apiCall(`/notifications/${id}/read`, { method: "POST" });
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n)),
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      console.error("Failed to mark notification as read", err);
    }
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

  const getIcon = (message) => {
    if (!message) return Info;
    const msg = message.toLowerCase();
    if (msg.includes("urgent") || msg.includes("deadline")) return AlertCircle;
    if (msg.includes("complete") || msg.includes("reviewed")) return CheckCircle;
    return Info;
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
            <h3 style={{ fontWeight: "600", fontSize: "1.125rem", color: "#111827", margin: 0 }}>
              Notifications
            </h3>
            <button onClick={() => setShowPanel(false)} style={{ border: "none", background: "none", cursor: "pointer", color: "#6b7280" }}>
              <X size={20} />
            </button>
          </div>

          <div style={{ overflowY: "auto", flex: 1 }}>
            {notifications.length === 0 ? (
              <div style={{ padding: "2rem", textAlign: "center", color: "#6b7280" }}>
                No notifications
              </div>
            ) : (
              notifications.map((n) => {
                const Icon = getIcon(n.message);
                const isRead = n.read || n.isRead;
                return (
                  <div
                    key={n.id}
                    onClick={() => !isRead && markAsRead(n.id)}
                    style={{
                      padding: "1rem",
                      borderBottom: "1px solid #f3f4f6",
                      cursor: "pointer",
                      background: isRead ? "transparent" : "#f9fafb",
                      transition: "background 0.2s",
                      display: "flex",
                      gap: "0.75rem",
                    }}
                  >
                    <div style={{ color: isRead ? "#9ca3af" : "#800020", marginTop: "0.25rem" }}>
                      <Icon size={18} />
                    </div>
                    <div style={{ flex: 1 }}>
                      <p style={{ margin: 0, fontSize: "0.875rem", color: isRead ? "#6b7280" : "#111827", fontWeight: isRead ? "400" : "500" }}>
                        {n.message}
                      </p>
                      <p style={{ margin: "0.25rem 0 0 0", fontSize: "0.75rem", color: "#9ca3af" }}>
                        {getTimeAgo(n.timestamp)}
                      </p>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}
    </>
  );
};

export default NotificationCenter;
