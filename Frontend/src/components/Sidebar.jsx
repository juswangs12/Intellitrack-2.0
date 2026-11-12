import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ChevronDown, ChevronRight, LogOut } from "lucide-react";

const Sidebar = ({ navItems, onLogout }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [openDropdowns, setOpenDropdowns] = useState({});

  const toggleDropdown = (id) => {
    setOpenDropdowns((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <aside className="sidebar">
      <nav className="sidebar-nav" aria-label="Workspace navigation">
        {navItems &&
          navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path || location.pathname.startsWith(item.path + "/");

            if (item.dropdown) {
              const hasActiveChild = item.dropdown.some(
                (child) => location.pathname === child.path || location.pathname.startsWith(child.path + "/"),
              );
              const isOpen = openDropdowns[item.id] ?? hasActiveChild;
              return (
                <div key={item.id}>
                  <button
                    className={`sidebar-dropdown-toggle ${hasActiveChild ? "active" : ""}`}
                    aria-expanded={isOpen}
                    aria-controls={`navigation-${item.id}`}
                    onClick={() => toggleDropdown(item.id)}
                  >
                    <div className="sidebar-item-content">
                      {Icon && (
                        <Icon style={{ width: "1.25rem", height: "1.25rem" }} />
                      )}
                      <span>{item.name}</span>
                    </div>
                    {isOpen ? (
                      <ChevronDown style={{ width: "1rem", height: "1rem" }} />
                    ) : (
                      <ChevronRight style={{ width: "1rem", height: "1rem" }} />
                    )}
                  </button>
                  {isOpen && (
                    <div className="sidebar-dropdown" id={`navigation-${item.id}`}>
                      {item.dropdown.map((child) => (
                        <button
                          key={child.id}
                          aria-current={location.pathname === child.path ? "page" : undefined}
                          className={`sidebar-subitem ${location.pathname === child.path ? "active" : ""}${child.disabled ? " disabled" : ""}`}
                          onClick={() => !child.disabled && navigate(child.path)}
                          disabled={child.disabled}
                          style={child.disabled ? { opacity: 0.5, cursor: "default", fontStyle: "italic" } : {}}
                        >
                          {child.name}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              );
            }

            return (
              <button
                key={item.id}
                aria-current={isActive ? "page" : undefined}
                className={`sidebar-item ${isActive ? "active" : ""}`}
                onClick={() => navigate(item.path)}
              >
                {Icon && (
                  <Icon style={{ width: "1.25rem", height: "1.25rem" }} />
                )}
                <span>{item.name}</span>
              </button>
            );
          })}
      </nav>
      <div className="sidebar-footer">
        <button className="sidebar-item sidebar-logout" onClick={onLogout}>
          <LogOut aria-hidden="true" size={20} />
          <span>Log out</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
