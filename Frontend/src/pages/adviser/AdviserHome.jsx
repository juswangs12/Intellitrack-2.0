import React from "react";
import { Users, FileText, CheckCircle, Clock } from "lucide-react";

const assignedStudents = [
  {
    id: 1,
    name: "Alice Santos",
    studentId: "2021-0001",
    project: "AI Grading System",
    lastSubmission: "2025-01-15",
    status: "on-track",
  },
  {
    id: 2,
    name: "Bob Reyes",
    studentId: "2021-0002",
    project: "Smart Inventory",
    lastSubmission: "2025-01-14",
    status: "needs-review",
  },
  {
    id: 3,
    name: "Carol Lim",
    studentId: "2021-0003",
    project: "LMS Portal",
    lastSubmission: "2025-01-10",
    status: "overdue",
  },
  {
    id: 4,
    name: "Dan Cruz",
    studentId: "2021-0004",
    project: "Blockchain Voting",
    lastSubmission: "2025-01-16",
    status: "on-track",
  },
];

const statusBadge = {
  "on-track": "success",
  "needs-review": "warning",
  overdue: "danger",
};

const AdviserHome = () => (
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
          <div className="stat-value">4</div>
        </div>
      </div>
      <div className="stat-card">
        <div className="stat-icon gold">
          <FileText size={22} />
        </div>
        <div className="stat-content">
          <div className="stat-label">Pending Reviews</div>
          <div className="stat-value">1</div>
        </div>
      </div>
      <div className="stat-card">
        <div className="stat-icon green">
          <CheckCircle size={22} />
        </div>
        <div className="stat-content">
          <div className="stat-label">On Track</div>
          <div className="stat-value">2</div>
        </div>
      </div>
      <div className="stat-card">
        <div className="stat-icon blue">
          <Clock size={22} />
        </div>
        <div className="stat-content">
          <div className="stat-label">Overdue</div>
          <div className="stat-value">1</div>
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
              {assignedStudents.map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td>{s.studentId}</td>
                  <td>{s.project}</td>
                  <td>{s.lastSubmission}</td>
                  <td>
                    <span className={`badge ${statusBadge[s.status]}`}>
                      {s.status.replace("-", " ")}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
);

export default AdviserHome;
