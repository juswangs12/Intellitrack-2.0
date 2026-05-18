import { useEffect, useState } from "react";
import { Users, FileText, Star } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const AdviserHome = () => {
  const { user } = useAuth();
  const [data, setData] = useState({
    assignedStudents: [],
    assignedCount: 0,
    pendingReview: 0,
    onTrack: 0,
    overdue: 0,
    aiInsight: ""
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await apiService.requestJson(`/dashboard/adviser/${user.id}`);
        setData(response);
      } catch (err) {
        console.error("Failed to fetch adviser dashboard", err);
      } finally {
        setLoading(false);
      }
    };

    if (user?.id) {
      fetchDashboard();
    }
  }, [user]);

  if (loading) return <div>Loading...</div>;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Adviser Dashboard</h1>
        <p>Overview of your assigned students.</p>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon maroon">
            <Users size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">Assigned Students</div>
            <div className="stat-value">{data.assignedCount}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon gold">
            <FileText size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">Pending Reviews</div>
            <div className="stat-value">{data.pendingReview}</div>
          </div>
        </div>
      </div>

      {data.aiInsight && (
        <div className="card" style={{ marginTop: "1.5rem", borderLeft: "4px solid var(--maroon)" }}>
          <div className="card-content" style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
            <div className="stat-icon maroon">
              <Star size={20} />
            </div>
            <div>
              <p>AI Advisory Insight</p>
              <p>{data.aiInsight}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdviserHome;
