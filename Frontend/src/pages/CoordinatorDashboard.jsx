import React from "react";
import { Home, FileText, Calendar, User } from "lucide-react";
import Layout from "../components/Layout";

const navItems = [
  { label: "Home", icon: Home, path: "/coordinator/home" },
  {
    label: "Document Review",
    icon: FileText,
    path: "/coordinator/document-review",
  },
  { label: "Calendar", icon: Calendar, path: "/coordinator/calendar" },
  { label: "Profile", icon: User, path: "/coordinator/profile" },
];

const CoordinatorDashboard = () => (
  <Layout navItems={navItems} title="Capstone Management System" />
);

export default CoordinatorDashboard;
