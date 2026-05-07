import React from "react";
import { Outlet } from "react-router-dom";
import Navbar from "./Navbar";
import Sidebar from "./Sidebar";
import { useAuth } from "../context/AuthContext";

const Layout = ({ title, navItems, onLogout }) => {
  const { user, logout } = useAuth();
  const handleLogout = onLogout || logout;

  return (
    <div className="layout">
      <Navbar title={title} user={user} onLogout={handleLogout} />
      <div className="layout-content">
        <Sidebar user={user} navItems={navItems} />
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
