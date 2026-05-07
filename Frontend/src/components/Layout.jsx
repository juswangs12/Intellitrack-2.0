import React from "react";
import { Outlet } from "react-router-dom";
import Navbar from "./Navbar";
import Sidebar from "./Sidebar";

const Layout = ({ user, title, navItems, onLogout }) => {
  return (
    <div className="layout">
      <Navbar title={title} user={user} onLogout={onLogout} />
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
