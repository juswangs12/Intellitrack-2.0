import React from "react";
import { Users } from "lucide-react";
import Layout from "../components/Layout";
export default function AdviserDashboard(){return <Layout navItems={[{id:"home",name:"My groups & submissions",icon:Users,path:"/adviser/home"}]} title="IntelliTrack Submission Tracker" />;}
