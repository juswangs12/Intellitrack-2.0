import React, { useState } from "react";
import { Upload, CheckCircle } from "lucide-react";

const DocumentSubmission = ({ docType, title, description }) => {
  const [file, setFile] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [comments, setComments] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) return;
    setSubmitting(true);
    // Simulate API call
    await new Promise((r) => setTimeout(r, 1000));
    setSubmitting(false);
    setSubmitted(true);
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">{title}</h1>
        <p className="page-description">{description}</p>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "2fr 1fr",
          gap: "1.5rem",
        }}
      >
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Submit Document</h2>
          </div>
          {submitted ? (
            <div className="empty-state">
              <CheckCircle
                style={{
                  width: "3rem",
                  height: "3rem",
                  color: "#10b981",
                  margin: "0 auto 1rem",
                  display: "block",
                }}
              />
              <h3 style={{ color: "#10b981", marginBottom: "0.5rem" }}>
                Submitted Successfully!
              </h3>
              <p>
                Your document has been submitted for review. Your adviser will
                review it soon.
              </p>
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setSubmitted(false);
                  setFile(null);
                  setComments("");
                }}
              >
                Submit Another Version
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="card-content">
              <div className="form-group" style={{ marginBottom: "1rem" }}>
                <label className="form-label">Upload Document (PDF)</label>
                <div
                  style={{
                    border: "2px dashed #d1d5db",
                    borderRadius: "0.5rem",
                    padding: "2rem",
                    textAlign: "center",
                    cursor: "pointer",
                    background: file ? "#f0fdf4" : "#f9fafb",
                  }}
                  onClick={() =>
                    document.getElementById(`file-${docType}`).click()
                  }
                >
                  <Upload
                    style={{
                      width: "2rem",
                      height: "2rem",
                      margin: "0 auto 0.5rem",
                      display: "block",
                      color: file ? "#10b981" : "#9ca3af",
                    }}
                  />
                  <p
                    style={{
                      margin: 0,
                      color: file ? "#10b981" : "#6b7280",
                      fontSize: "0.875rem",
                    }}
                  >
                    {file ? file.name : "Click or drag to upload PDF"}
                  </p>
                  <input
                    id={`file-${docType}`}
                    type="file"
                    accept=".pdf,.doc,.docx"
                    style={{ display: "none" }}
                    onChange={(e) => setFile(e.target.files[0])}
                  />
                </div>
              </div>
              <div className="form-group" style={{ marginBottom: "1.5rem" }}>
                <label className="form-label">
                  Additional Comments (optional)
                </label>
                <textarea
                  className="form-input"
                  rows={4}
                  placeholder="Add any notes for your adviser..."
                  value={comments}
                  onChange={(e) => setComments(e.target.value)}
                  style={{ resize: "vertical" }}
                />
              </div>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={!file || submitting}
              >
                {submitting ? "Submitting..." : "Submit Document"}
              </button>
            </form>
          )}
        </div>

        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Submission History</h2>
          </div>
          <div
            style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}
          >
            {[
              {
                version: "v1.0",
                date: "Nov 15, 2025",
                status: "revision",
                feedback: "Needs more detail in methodology",
              },
              {
                version: "v0.9",
                date: "Nov 1, 2025",
                status: "reviewed",
                feedback: "Initial draft reviewed",
              },
            ].map((h, i) => (
              <div
                key={i}
                style={{
                  padding: "0.75rem",
                  border: "1px solid #e5e7eb",
                  borderRadius: "0.5rem",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    marginBottom: "0.25rem",
                  }}
                >
                  <span style={{ fontWeight: "600", fontSize: "0.875rem" }}>
                    {h.version}
                  </span>
                  <span
                    className={`badge ${h.status === "revision" ? "danger" : "info"}`}
                  >
                    {h.status}
                  </span>
                </div>
                <p
                  style={{
                    fontSize: "0.75rem",
                    color: "#6b7280",
                    margin: "0.25rem 0 0 0",
                  }}
                >
                  {h.feedback}
                </p>
                <p
                  style={{
                    fontSize: "0.7rem",
                    color: "#9ca3af",
                    margin: "0.25rem 0 0 0",
                  }}
                >
                  {h.date}
                </p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export const ProjectProposal = () => (
  <DocumentSubmission
    docType="proposal"
    title="Project Proposal"
    description="Submit your capstone project proposal for adviser review."
  />
);

export const SRSDocument = () => (
  <DocumentSubmission
    docType="srs"
    title="SRS Document"
    description="Submit your Software Requirements Specification document."
  />
);

export const SDDDocument = () => (
  <DocumentSubmission
    docType="sdd"
    title="SDD Document"
    description="Submit your Software Design Document for review."
  />
);

export default DocumentSubmission;
