import React from "react";
import { Home, Users, Settings, Calendar, BarChart2 } from "lucide-react";
import Layout from "../components/Layout";

const navItems = [
  { label: "Home", icon: Home, path: "/admin/home" },
  { label: "User Management", icon: Users, path: "/admin/users" },
  { label: "System Config", icon: Settings, path: "/admin/system" },
  { label: "Deadlines", icon: Calendar, path: "/admin/deadlines" },
  { label: "Analytics", icon: BarChart2, path: "/admin/analytics" },
];

const AdminDashboard = () => (
  <Layout navItems={navItems} title="Capstone Management System" />
);

export default AdminDashboard;
