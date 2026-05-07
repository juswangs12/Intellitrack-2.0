import React from "react";
import { LogOut, GraduationCap } from "lucide-react";
import NotificationCenter from "./NotificationCenter";

const Navbar = ({ title, user, onLogout }) => {
  return (
    <nav className="navbar">
      <div className="navbar-container">
        <div className="navbar-left">
          <GraduationCap className="navbar-icon" />
          <span className="navbar-title">{title || "IntelliTrack"}</span>
        </div>
        <div className="navbar-right">
          <NotificationCenter userRole={user?.role} />
          <button onClick={onLogout} className="navbar-logout">
            <LogOut style={{ width: "1rem", height: "1rem" }} />
            Logout
          </button>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
