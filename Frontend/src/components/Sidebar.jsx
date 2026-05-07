import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ChevronDown, ChevronRight } from "lucide-react";

const Sidebar = ({ user, navItems }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [openDropdowns, setOpenDropdowns] = useState({});

  const toggleDropdown = (id) => {
    setOpenDropdowns((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const getInitials = (u) => {
    if (!u) return "U";
    const first = u.firstName || (u.name ? u.name.split(" ")[0] : "");
    const last = u.lastName || (u.name ? u.name.split(" ").slice(-1)[0] : "");
    return `${first[0] || ""}${last[0] || ""}`.toUpperCase() || "U";
  };

  const getDisplayName = (u) => {
    if (!u) return "User";
    return (
      u.name || `${u.firstName || ""} ${u.lastName || ""}`.trim() || "User"
    );
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-profile">
        <div className="profile-avatar">
          <span style={{ color: "white", fontWeight: "600", fontSize: "1rem" }}>
            {getInitials(user)}
          </span>
        </div>
        <div>
          <p className="profile-name">{getDisplayName(user)}</p>
          <p className="profile-role">{user?.role || "User"}</p>
        </div>
      </div>

      <nav className="sidebar-nav">
        {navItems &&
          navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;

            if (item.dropdown) {
              const isOpen = openDropdowns[item.label];
              const hasActiveChild = item.dropdown.some(
                (child) => location.pathname === child.path,
              );
              return (
                <div key={item.label}>
                  <button
                    className={`sidebar-dropdown-toggle ${hasActiveChild ? "active" : ""}`}
                    onClick={() => toggleDropdown(item.label)}
                  >
                    <div className="sidebar-item-content">
                      {Icon && (
                        <Icon style={{ width: "1.25rem", height: "1.25rem" }} />
                      )}
                      <span>{item.label}</span>
                    </div>
                    {isOpen ? (
                      <ChevronDown style={{ width: "1rem", height: "1rem" }} />
                    ) : (
                      <ChevronRight style={{ width: "1rem", height: "1rem" }} />
                    )}
                  </button>
                  {isOpen && (
                    <div className="sidebar-dropdown">
                      {item.dropdown.map((child) => (
                        <button
                          key={child.label}
                          className={`sidebar-subitem ${location.pathname === child.path ? "active" : ""}`}
                          onClick={() => navigate(child.path)}
                        >
                          {child.label}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              );
            }

            return (
              <button
                key={item.label}
                className={`sidebar-item ${isActive ? "active" : ""}`}
                onClick={() => navigate(item.path)}
              >
                {Icon && (
                  <Icon style={{ width: "1.25rem", height: "1.25rem" }} />
                )}
                <span>{item.label}</span>
              </button>
            );
          })}
      </nav>
    </aside>
  );
};

export default Sidebar;
