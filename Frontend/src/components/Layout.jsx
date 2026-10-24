import React, { useEffect, useRef } from "react";
import { Outlet, useLocation } from "react-router-dom";
import Navbar from "./Navbar";
import Sidebar from "./Sidebar";
import { useAuth } from "../context/AuthContext";

const Layout = ({ title, navItems }) => {
  const { user, logout } = useAuth();
  const { pathname } = useLocation();
  const contentRef = useRef(null);
  useEffect(() => {
    window.scrollTo(0, 0);
    if (contentRef.current) contentRef.current.scrollTop = 0;
  }, [pathname, user?.role]);
  
  return (
    <div className={`layout workspace-layout ${user?.role==='administrator'?'admin':user?.role||'student'}-layout`}>
      <Navbar title={title} user={user} onLogout={logout} />
      <div className="layout-content">
        <Sidebar user={user} navItems={navItems} />
        <main className="main-content" ref={contentRef}>
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
