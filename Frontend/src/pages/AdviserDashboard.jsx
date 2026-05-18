import React from "react";
import { Home, User, FileText, Users } from "lucide-react";
import Layout from "../components/Layout";

const navItems = [
  { id: "home", name: "Home", icon: Home, path: "/adviser/home" },
  { id: "groups", name: "My Groups", icon: Users, path: "/adviser/groups" },
  { id: "review", name: "Review Submissions", icon: FileText, path: "/adviser/review" },
  { id: "profile", name: "Profile", icon: User, path: "/adviser/profile" },
];

const AdviserDashboard = () => (
  <Layout navItems={navItems} title="Capstone Management System" />
);

export default AdviserDashboard;
