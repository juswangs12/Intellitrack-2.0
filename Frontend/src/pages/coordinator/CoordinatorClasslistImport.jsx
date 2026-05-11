import { useState } from "react";
import { Upload, FileSpreadsheet, CheckCircle2, AlertCircle, XCircle } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import apiService from "../../services/ApiService";

const CoordinatorClasslistImport = () => {
  const { user } = useAuth();
  const [selectedFile, setSelectedFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState(null);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (!file.name.toLowerCase().endsWith(".xlsx") && !file.name.toLowerCase().endsWith(".xls")) {
        setError("Please select an Excel file (.xlsx or .xls)");
        return;
      }
      setSelectedFile(file);
      setImportResult(null);
      setError(null);
    }
  };

  const handleImport = async () => {
    if (!selectedFile) {
      setError("Please select a file first");
      return;
    }

    try {
      setImporting(true);
      setError(null);
      setImportResult(null);

      const formData = new FormData();
      formData.append("file", selectedFile);

      const response = await apiService.requestJson("/classlist/import", {
        method: "POST",
        body: formData,
      });

      setImportResult(response);
    } catch (err) {
      console.error("Failed to import classlist", err);
      setError(err.message || "Failed to import classlist");
    } finally {
      setImporting(false);
    }
  };

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Classlist Import</h1>
        <p className="page-description">
          Upload Excel classlists to enroll students for grading.
        </p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", color: "#b91c1c" }}>
          {error}
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h2 className="card-title">Upload Classlist</h2>
        </div>
        <div className="card-content">
          <div
            style={{
              border: "2px dashed #d1d5db",
              borderRadius: "0.5rem",
              padding: "2rem",
              textAlign: "center",
              marginBottom: "1rem",
            }}
          >
            <FileSpreadsheet size={48} style={{ color: "#9ca3af", marginBottom: "0.5rem" }} />
            <input
              type="file"
              accept=".xlsx,.xls"
              onChange={handleFileChange}
              style={{ display: "none" }}
              id="classlist-file"
            />
            <label
              htmlFor="classlist-file"
              style={{
                display: "inline-block",
                cursor: "pointer",
                color: "#7f1d1d",
                fontWeight: 600,
              }}
            >
              {selectedFile ? selectedFile.name : "Click to select Excel file"}
            </label>
            <p style={{ color: "#6b7280", fontSize: "0.875rem", marginTop: "0.5rem" }}>
              Supported formats: .xlsx, .xls
            </p>
          </div>

          <div className="card" style={{ marginBottom: "1rem" }}>
            <div className="card-content" style={{ padding: "1rem" }}>
              <h3 style={{ margin: "0 0 0.5rem 0", fontSize: "1rem" }}>Expected Excel Format</h3>
              <p style={{ margin: "0 0 0.5rem 0", color: "#6b7280", fontSize: "0.875rem" }}>
                Columns (in order):
              </p>
              <ul style={{ margin: 0, paddingLeft: "1.25rem", fontSize: "0.875rem" }}>
                <li>Student ID (##-####-### or ####-#####)</li>
                <li>Full Name</li>
                <li>Email</li>
                <li>Subject Name</li>
                <li>Subject Code</li>
                <li>Section</li>
              </ul>
            </div>
          </div>

          <button
            className="btn btn-primary"
            onClick={handleImport}
            disabled={importing || !selectedFile}
          >
            <Upload size={16} /> {importing ? "Importing..." : "Import Classlist"}
          </button>
        </div>
      </div>

      {importResult && (
        <div className="card" style={{ marginTop: "1.5rem" }}>
          <div className="card-header">
            <h2 className="card-title">Import Summary</h2>
          </div>
          <div className="card-content">
            <div className="stats-grid" style={{ marginBottom: "1rem" }}>
              <div className="stat-card">
                <div className="stat-icon blue">
                  <FileSpreadsheet size={20} />
                </div>
                <div className="stat-content">
                  <p className="stat-label">Total Rows</p>
                  <p className="stat-value">{importResult.totalRows || 0}</p>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon green">
                  <CheckCircle2 size={20} />
                </div>
                <div className="stat-content">
                  <p className="stat-label">Successfully Imported</p>
                  <p className="stat-value">{importResult.successfullyImported || 0}</p>
                </div>
              </div>
              <div className="stat-card">
                <div className="stat-icon maroon">
                  <XCircle size={20} />
                </div>
                <div className="stat-content">
                  <p className="stat-label">Failed Rows</p>
                  <p className="stat-value">{importResult.failedRows || 0}</p>
                </div>
              </div>
            </div>

            {importResult.errors && importResult.errors.length > 0 && (
              <div style={{ marginBottom: "1rem" }}>
                <h3 style={{ margin: "0 0 0.5rem 0", fontSize: "1rem" }}>
                  <AlertCircle size={16} style={{ marginRight: "0.25rem", display: "inline" }} />
                  Errors
                </h3>
                <ul style={{ margin: 0, paddingLeft: "1.25rem", color: "#b91c1c" }}>
                  {importResult.errors.map((err, index) => (
                    <li key={index}>{err}</li>
                  ))}
                </ul>
              </div>
            )}

            {importResult.warnings && importResult.warnings.length > 0 && (
              <div>
                <h3 style={{ margin: "0 0 0.5rem 0", fontSize: "1rem" }}>
                  <AlertCircle size={16} style={{ marginRight: "0.25rem", display: "inline" }} />
                  Warnings
                </h3>
                <ul style={{ margin: 0, paddingLeft: "1.25rem", color: "#d97706" }}>
                  {importResult.warnings.map((warn, index) => (
                    <li key={index}>{warn}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default CoordinatorClasslistImport;