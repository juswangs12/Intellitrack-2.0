import React from "react";
import { Users, LayoutDashboard } from "lucide-react";
import Layout from "../components/Layout";
export default function AdviserDashboard(){return <Layout navItems={[{id:"home",name:"Dashboard",icon:LayoutDashboard,path:"/adviser/home"},{id:"groups",name:"Groups & submissions",icon:Users,path:"/adviser/groups"}]} title="IntelliTrack Submission Tracker" />;}
