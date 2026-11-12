import React, { useState } from "react";
import {
  User,
  ChevronDown,
} from "lucide-react";
import NotificationCenter from "./NotificationCenter";
import BrandLogo from "./BrandLogo";
import { useNavigate } from "react-router-dom";

const Navbar = ({ title, user }) => {
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const navigate = useNavigate();

  const handleProfileClick = () => {
    navigate(`/${user?.role==='administrator'?'admin':user?.role}/profile`);
    setShowProfileMenu(false);
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <div className="navbar-left">
          <BrandLogo />
          <div className="navbar-title-group">
            <span className="navbar-title">{title || "IntelliTrack"}</span>
            <span className="navbar-subtitle">Capstone Submission Tracking System</span>
          </div>
        </div>

        <div className="navbar-right">
          <NotificationCenter />

          <div className="navbar-user-section">
            <button
              type="button" aria-expanded={showProfileMenu} aria-label="Account menu"
              className="navbar-profile-trigger"
              onClick={() => setShowProfileMenu(!showProfileMenu)}
            >
              <div className="navbar-avatar">
                {user?.firstName?.charAt(0)}
                {user?.lastName?.charAt(0)}
              </div>
              <div className="navbar-user-info">
                <span className="navbar-user-name">
                  {user?.firstName} {user?.lastName}
                </span>
                <span className="navbar-user-role">{user?.role}</span>
              </div>
              <ChevronDown
                size={16}
                className={`navbar-chevron ${showProfileMenu ? "rotated" : ""}`}
              />
            </button>

            {showProfileMenu && (
              <div className="navbar-dropdown-menu">
                <button onClick={handleProfileClick} className="dropdown-item">
                  <User size={16} />
                  <span>My Profile</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
