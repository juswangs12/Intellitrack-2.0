import React from "react";
import { BarChart3, Users, FileSpreadsheet, Calendar, LayoutDashboard } from "lucide-react";
import Layout from "../components/Layout";
import '../styles/Coordinator.css';
const navItems = [
  {id:"home",name:"Dashboard",icon:LayoutDashboard,path:"/coordinator/home"},
  {id:"analytics",name:"Submission analytics",icon:BarChart3,path:"/coordinator/analytics"},
  {id:"calendar",name:"Deadlines & calendar",icon:Calendar,path:"/coordinator/calendar"},
  {id:"groups",name:"Groups & advisers",icon:Users,path:"/coordinator/groups"},
  {id:"academic",name:"Academic records",icon:FileSpreadsheet,dropdown:[
    {id:"classlist",name:"Import classlist",path:"/coordinator/classlist"},
    {id:"classlist-view",name:"Students & sections",path:"/coordinator/classlist-view"},
    {id:"setup",name:"Academic setup",path:"/coordinator/setup"},
  ]},
];
export default function CoordinatorDashboard(){return <Layout navItems={navItems} title="IntelliTrack" />;}
