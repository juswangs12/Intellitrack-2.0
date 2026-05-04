import React from "react";
import { Home, FileText, User } from "lucide-react";
import Layout from "../components/Layout";

const navItems = [
  { label: "Home", icon: Home, path: "/student/home" },
  {
    label: "Submissions",
    icon: FileText,
    dropdown: [
      { label: "Project Proposal", path: "/student/submissions/proposal" },
      { label: "SRS Document", path: "/student/submissions/srs" },
      { label: "SDD Document", path: "/student/submissions/sdd" },
    ],
  },
  { label: "Profile", icon: User, path: "/student/profile" },
];

const StudentDashboard = () => (
  <Layout navItems={navItems} title="Capstone Management System" />
);

export default StudentDashboard;
