import React from "react";
import { Home, Users, Settings, Calendar, BarChart2 } from "lucide-react";
import Layout from "../components/Layout";

const navItems = [
  { id: "home", name: "Home", icon: Home, path: "/admin/home" },
  { id: "users", name: "User Management", icon: Users, path: "/admin/users" },
  { id: "system", name: "System Config", icon: Settings, path: "/admin/system" },
  { id: "deadlines", name: "Deadlines", icon: Calendar, path: "/admin/deadlines" },
  { id: "analytics", name: "Analytics", icon: BarChart2, path: "/admin/analytics" },
];

const AdminDashboard = () => (
  <Layout navItems={navItems} title="Capstone Management System" />
);

export default AdminDashboard;
