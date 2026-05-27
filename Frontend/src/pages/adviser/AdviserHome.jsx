import { useEffect, useState } from "react";
import { Users, FileText } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const AdviserHome = () => {
  const { user } = useAuth();
  const [data, setData] = useState({
    assignedStudents: [],
    assignedCount: 0,
    assignedGroupsCount: 0,
    totalSubmissions: 0,
    onTime: 0,
    late: 0,
    notSubmitted: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await apiService.requestJson(
          `/dashboard/adviser/${user.id}`,
        );
        const { aiInsight, ...restData } = response;
        setData(restData);
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
        <p>Overview of your assigned groups and submissions.</p>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon maroon">
            <Users size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">Assigned Groups</div>
            <div className="stat-value">{data.assignedGroupsCount}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon gold">
            <FileText size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">Total Submissions</div>
            <div className="stat-value">{data.totalSubmissions}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon green">
            <FileText size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">On Time</div>
            <div className="stat-value">{data.onTime}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon red">
            <FileText size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">Late</div>
            <div className="stat-value">{data.late}</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdviserHome;
