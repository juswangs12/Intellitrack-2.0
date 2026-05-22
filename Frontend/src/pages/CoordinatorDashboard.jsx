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
    name: "Submission Review",
    icon: FileText,
    path: "/coordinator/document-review",
  },
  {
    id: "submission-tracking",
    name: "Submission Tracking",
    icon: BarChart3,
    path: "/coordinator/submission-tracking",
  },
  {
    id: "groups",
    name: "Groups",
    icon: Users,
    path: "/coordinator/groups",
  },
  {
    id: "milestones",
    name: "Milestones",
    icon: FTIcon,
    path: "/coordinator/milestones",
  },
  {
    id: "analytics",
    name: "Analytics",
    icon: BarChart3,
    path: "/coordinator/analytics",
  },
  { id: "calendar", name: "Calendar", icon: Calendar, path: "/coordinator/calendar" },

];

const CoordinatorDashboard = () => {
  return (
    <Layout navItems={navItems} title="Capstone Management System" />
  );
};

export default CoordinatorDashboard;
