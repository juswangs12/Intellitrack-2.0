import React from "react";
import { FileText, LayoutDashboard, Calendar, Users } from "lucide-react";
import Layout from "../components/Layout";
import "../styles/StudentWorkspace.css";
export default function StudentDashboard() {
  return <Layout navItems={[{id:"home",name:"Dashboard",icon:LayoutDashboard,path:"/student/home"},{id:"submissions",name:"My submissions",icon:FileText,path:"/student/submissions"},{id:"groups",name:"My groups & enrollment",icon:Users,path:"/student/groups"},{id:"calendar",name:"Deadline calendar",icon:Calendar,path:"/student/calendar"}]} title="IntelliTrack Submission Tracker" />;
}
