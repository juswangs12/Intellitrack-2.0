import React, { useState } from "react";
import { FileText, CheckCircle, XCircle, Eye } from "lucide-react";

const DocumentReview = () => {
  const [selectedDoc, setSelectedDoc] = useState(null);
  const [feedback, setFeedback] = useState("");

  const pendingDocuments = [
    {
      id: 1,
      team: "Group Alpha",
      document: "Project Proposal",
      version: "v2.0",
      submitted: "Dec 1, 2025",
      size: "2.4 MB",
    },
    {
      id: 2,
      team: "Group Beta",
      document: "SRS Document",
      version: "v1.0",
      submitted: "Dec 3, 2025",
      size: "3.1 MB",
    },
    {
      id: 3,
      team: "Group Gamma",
      document: "SDD Chapter 2",
      version: "v1.5",
      submitted: "Dec 5, 2025",
      size: "1.8 MB",
    },
    {
      id: 4,
      team: "Group Delta",
      document: "Project Proposal",
      version: "v1.0",
      submitted: "Dec 6, 2025",
      size: "2.0 MB",
    },
  ];

  const handleApprove = (doc) => {
    alert(`Approved: ${doc.document} for ${doc.team}`);
    setSelectedDoc(null);
    setFeedback("");
  };

  const handleRevision = (doc) => {
    if (!feedback.trim()) {
      alert("Please provide feedback before requesting revision.");
      return;
    }
    alert(`Revision requested for: ${doc.document}`);
    setSelectedDoc(null);
    setFeedback("");
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Document Review</h1>
        <p className="page-description">
          Review and provide feedback on team submissions.
        </p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: selectedDoc ? "1fr 1fr" : "1fr",
          gap: "1.5rem",
        }}
      >
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">
              Pending Documents ({pendingDocuments.length})
            </h2>
          </div>
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Team</th>
                  <th>Document</th>
                  <th>Version</th>
                  <th>Submitted</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pendingDocuments.map((doc) => (
                  <tr
                    key={doc.id}
                    style={{
                      background:
                        selectedDoc?.id === doc.id ? "rgba(128,0,32,0.05)" : "",
                    }}
                  >
                    <td>
                      <strong>{doc.team}</strong>
                    </td>
                    <td>{doc.document}</td>
                    <td>
                      <span className="badge info">{doc.version}</span>
                    </td>
                    <td>{doc.submitted}</td>
                    <td>
                      <button
                        className="btn btn-primary"
                        style={{
                          padding: "0.25rem 0.75rem",
                          fontSize: "0.75rem",
                        }}
                        onClick={() => setSelectedDoc(doc)}
                      >
                        <Eye
                          style={{ width: "0.875rem", height: "0.875rem" }}
                        />{" "}
                        Review
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {selectedDoc && (
          <div className="card">
            <div
              className="card-header"
              style={{ display: "flex", justifyContent: "space-between" }}
            >
              <div>
                <h2 className="card-title">{selectedDoc.document}</h2>
                <p className="card-description">
                  {selectedDoc.team} — {selectedDoc.version}
                </p>
              </div>
              <button
                className="btn btn-secondary"
                style={{ padding: "0.25rem 0.75rem" }}
                onClick={() => setSelectedDoc(null)}
              >
                ✕
              </button>
            </div>
            <div
              style={{
                background: "#f9fafb",
                border: "1px solid #e5e7eb",
                borderRadius: "0.5rem",
                padding: "2rem",
                textAlign: "center",
                marginBottom: "1rem",
                minHeight: "200px",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
              }}
            >
              <div>
                <FileText
                  style={{
                    width: "3rem",
                    height: "3rem",
                    color: "#9ca3af",
                    margin: "0 auto 0.5rem",
                    display: "block",
                  }}
                />
                <p
                  style={{ color: "#6b7280", fontSize: "0.875rem", margin: 0 }}
                >
                  Document Preview
                </p>
                <p
                  style={{
                    color: "#9ca3af",
                    fontSize: "0.75rem",
                    margin: "0.25rem 0 0 0",
                  }}
                >
                  {selectedDoc.size}
                </p>
              </div>
            </div>
            <div className="form-group" style={{ marginBottom: "1rem" }}>
              <label className="form-label">Feedback / Comments</label>
              <textarea
                className="form-input"
                rows={5}
                placeholder="Write your feedback here..."
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
                style={{ resize: "vertical" }}
              />
            </div>
            <div style={{ display: "flex", gap: "0.75rem" }}>
              <button
                className="btn btn-success"
                onClick={() => handleApprove(selectedDoc)}
              >
                <CheckCircle style={{ width: "1rem", height: "1rem" }} />{" "}
                Approve
              </button>
              <button
                className="btn btn-danger"
                onClick={() => handleRevision(selectedDoc)}
              >
                <XCircle style={{ width: "1rem", height: "1rem" }} /> Request
                Revision
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default DocumentReview;
