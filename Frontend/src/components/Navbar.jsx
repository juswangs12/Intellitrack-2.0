import React, { useState } from "react";
import { LogOut, GraduationCap, User, ChevronDown, Settings } from "lucide-react";
import NotificationCenter from "./NotificationCenter";
import { useNavigate } from "react-router-dom";

const Navbar = ({ title, user, onLogout }) => {
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const navigate = useNavigate();

  const handleProfileClick = () => {
    navigate(`/${user?.role}/profile`);
    setShowProfileMenu(false);
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <div className="navbar-left">
          <div className="navbar-logo-container">
            <GraduationCap className="navbar-icon" />
          </div>
          <div className="navbar-title-group">
            <span className="navbar-title">{title || "IntelliTrack"}</span>
            <span className="navbar-subtitle">Capstone Management System</span>
          </div>
        </div>

        <div className="navbar-right">
          <NotificationCenter />
          
          <div className="navbar-user-section">
            <div 
              className="navbar-profile-trigger"
              onClick={() => setShowProfileMenu(!showProfileMenu)}
            >
              <div className="navbar-avatar">
                {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
              </div>
              <div className="navbar-user-info">
                <span className="navbar-user-name">{user?.firstName} {user?.lastName}</span>
                <span className="navbar-user-role">{user?.role}</span>
              </div>
              <ChevronDown size={16} className={`navbar-chevron ${showProfileMenu ? 'rotated' : ''}`} />
            </div>

            {showProfileMenu && (
              <div className="navbar-dropdown-menu">
                <div className="dropdown-header">
                  <strong>Account Settings</strong>
                </div>
                <button onClick={handleProfileClick} className="dropdown-item">
                  <User size={16} />
                  <span>My Profile</span>
                </button>
                <button onClick={() => { navigate(`/${user?.role}/profile`); setShowProfileMenu(false); }} className="dropdown-item">
                  <Settings size={16} />
                  <span>Preferences</span>
                </button>
                <div className="dropdown-divider"></div>
                <button onClick={onLogout} className="dropdown-item logout">
                  <LogOut size={16} />
                  <span>Logout</span>
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
