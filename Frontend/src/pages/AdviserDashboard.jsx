import React from "react";
import { Users, LayoutDashboard, FileCheck } from "lucide-react";
import Layout from "../components/Layout";
export default function AdviserDashboard(){return <Layout navItems={[{id:"home",name:"Dashboard",icon:LayoutDashboard,path:"/adviser/home"},{id:"groups",name:"My groups",icon:Users,path:"/adviser/groups"},{id:"review",name:"Review submissions",icon:FileCheck,path:"/adviser/review"}]} title="IntelliTrack Submission Tracker" />;}
