import React, { useEffect, useMemo, useState } from "react";
import { Home, FileText, User } from "lucide-react";
import Layout from "../components/Layout";
import apiService from "../services/ApiService";

const StudentDashboard = () => {
  const [deliverables, setDeliverables] = useState([]);

  useEffect(() => {
    let mounted = true;

    const loadDeliverables = async () => {
      try {
        const data = await apiService.requestJson("/deliverables/active");
        if (mounted) {
          setDeliverables(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (mounted) {
          setDeliverables([]);
        }
      }
    };

    loadDeliverables();

    return () => {
      mounted = false;
    };
  }, []);

  const navItems = useMemo(() => {
    const submissionChildren =
      deliverables.length > 0
        ? deliverables.map((deliverable) => ({
            id: String(deliverable.id),
            name: deliverable.name,
            path: `/student/submissions/${deliverable.id}`,
          }))
        : [
            {
              id: "none",
              name: "No deliverables available",
              path: "/student/home",
            },
          ];

    return [
      { id: "home", name: "Home", icon: Home, path: "/student/home" },
      {
        id: "submissions",
        name: "Submissions",
        icon: FileText,
        dropdown: submissionChildren,
      },
      { id: "profile", name: "Profile", icon: User, path: "/student/profile" },
    ];
  }, [deliverables]);

  return <Layout navItems={navItems} title="Capstone Management System" />;
};

export default StudentDashboard;
