import React from "react";
import { Home, User } from "lucide-react";
import Layout from "../components/Layout";

const navItems = [
  { label: "Home", icon: Home, path: "/adviser/home" },
  { label: "Profile", icon: User, path: "/adviser/profile" },
];

const AdviserDashboard = () => (
  <Layout navItems={navItems} title="Capstone Management System" />
);

export default AdviserDashboard;
