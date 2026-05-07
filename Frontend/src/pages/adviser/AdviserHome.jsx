import React, { useEffect, useState } from "react";
import { Users, FileText, CheckCircle, Clock } from "lucide-react";
import { useAuth } from "../../context/AuthContext";

const AdviserHome = () => {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.id) return;
    const token = localStorage.getItem("token");
    fetch(`http://localhost:8080/api/dashboard/adviser/${user.id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setData(d))
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, [user]);

  const assignedStudents = data?.assignedStudents ?? [];
  const assignedCount = loading ? "…" : (data?.assignedCount ?? 0);
  const pendingReview = loading ? "…" : (data?.pendingReview ?? 0);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Adviser Dashboard</h1>
        <p className="page-description">
          Overview of your assigned students and their capstone progress.
        </p>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon maroon">
            <Users size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">Assigned Students</div>
            <div className="stat-value">{assignedCount}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon gold">
            <FileText size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">Pending Reviews</div>
            <div className="stat-value">{pendingReview}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon green">
            <CheckCircle size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">Active Submissions</div>
            <div className="stat-value">
              {loading ? "…" : (data?.activeSubmissions ?? 0)}
            </div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon blue">
            <Clock size={22} />
          </div>
          <div className="stat-content">
            <div className="stat-label">Reviewed</div>
            <div className="stat-value">
              {loading ? "…" : (data?.reviewed ?? 0)}
            </div>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginTop: "1.5rem" }}>
        <div className="card-header">
          <h2 className="card-title">Assigned Students</h2>
        </div>
        <div className="card-content">
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Student ID</th>
                  <th>Project Title</th>
                  <th>Last Submission</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {assignedStudents.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5}
                      style={{
                        color: "#6b7280",
                        textAlign: "center",
                        padding: "1rem",
                      }}
                    >
                      No students assigned yet.
                    </td>
                  </tr>
                ) : (
                  assignedStudents.map((s) => (
                    <tr key={s.id}>
                      <td>
                        {s.firstName} {s.lastName}
                      </td>
                      <td>{s.studentId ?? "—"}</td>
                      <td>{s.group?.title ?? "—"}</td>
                      <td>{s.email}</td>
                      <td>
                        <span className="badge info">{s.role}</span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdviserHome;
