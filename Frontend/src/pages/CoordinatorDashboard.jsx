import React from "react";
import { Home, FileText, Calendar, User, Users, FileText as FTIcon, BarChart3, Clock, FileSpreadsheet, Users as UserGroupIcon } from "lucide-react";
import Layout from "../components/Layout";

const navItems = [
  { id: "home", name: "Home", icon: Home, path: "/coordinator/home" },
  {
    id: "classlist",
    name: "Classlist Import",
    icon: FileSpreadsheet,
    path: "/coordinator/classlist",
  },
  {
    id: "classlist-view",
    name: "Classlist View",
    icon: UserGroupIcon,
    path: "/coordinator/classlist-view",
  },
  {
    id: "document-review",
    name: "Document Review",
    icon: FileText,
    path: "/coordinator/document-review",
  },
  {
    id: "groups",
    name: "Groups",
    icon: Users,
    path: "/coordinator/groups",
  },
  {
    id: "deliverables",
    name: "Deliverables",
    icon: FTIcon,
    path: "/coordinator/deliverables",
  },
  {
    id: "deadlines",
    name: "Deadlines",
    icon: Clock,
    path: "/coordinator/deadlines",
  },
  {
    id: "analytics",
    name: "Analytics",
    icon: BarChart3,
    path: "/coordinator/analytics",
  },
  { id: "calendar", name: "Calendar", icon: Calendar, path: "/coordinator/calendar" },
  { id: "profile", name: "Profile", icon: User, path: "/coordinator/profile" },
];

const CoordinatorDashboard = () => {
  return (
    <Layout navItems={navItems} title="Capstone Management System" />
  );
};

export default CoordinatorDashboard;
