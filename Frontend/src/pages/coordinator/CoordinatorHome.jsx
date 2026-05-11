import { useEffect, useState } from "react";
import { Users, Clock, AlertCircle, Sparkles } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const CoordinatorHome = () => {
  const { user } = useAuth();
  const [data, setData] = useState({
    totalStudents: 0,
    totalAdvisers: 0,
    submissionsPending: 0,
    submissionsLate: 0,
    aiInsight: ""
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await apiService.requestJson(`/dashboard/coordinator/${user.id}`);
        setData(response);
      } catch (err) {
        console.error("Failed to fetch coordinator dashboard", err);
      } finally {
        setLoading(false);
      }
    };

    if (user?.id) {
      fetchDashboard();
    }
  }, [user]);

  const stats = [
    { label: "Total Students", value: data.totalStudents, icon: Users, color: "maroon" },
    { label: "Total Advisers", value: data.totalAdvisers, icon: Users, color: "gold" },
    { label: "Pending Reviews", value: data.submissionsPending, icon: Clock, color: "green" },
    { label: "Late Submissions", value: data.submissionsLate, icon: AlertCircle, color: "blue" },
  ];

  if (loading) return <div>Loading...</div>;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Institutional Overview</h1>
        <p className="page-description">
          System-wide tracking and academic monitoring.
        </p>
      </div>

      <div className="stats-grid">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div className="stat-card" key={stat.label}>
              <div className={`stat-icon ${stat.color}`}>
                <Icon style={{ width: "1.5rem", height: "1.5rem" }} />
              </div>
              <div className="stat-content">
                <p className="stat-label">{stat.label}</p>
                <p className="stat-value">{stat.value}</p>
              </div>
            </div>
          );
        })}
      </div>

      {data.aiInsight && (
        <div className="card" style={{ marginTop: "1.5rem", borderLeft: "4px solid var(--maroon)" }}>
          <div className="card-content" style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <div className="stat-icon maroon">
              <Sparkles size={20} />
            </div>
            <div>
              <p>AI Strategic Insight</p>
              <p>{data.aiInsight}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CoordinatorHome;
