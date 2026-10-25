import { useState } from "react";
import {
  Upload,
  FileSpreadsheet,
  CheckCircle2,
  AlertCircle,
  XCircle,
} from "lucide-react";
import apiService from "../../services/ApiService";
import {Link} from 'react-router-dom';
import AcademicTermFields,{initialTerm} from '../../components/AcademicTermFields';
import {connectGoogle} from '../TrackingDashboard';

const CoordinatorClasslistImport = () => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState(null);
  const [term,setTerm]=useState(initialTerm);
  const [source,setSource]=useState('local'),[sheetUrl,setSheetUrl]=useState(''),[worksheet,setWorksheet]=useState('');
  const connectSheets=async()=>{setError(null);try{await connectGoogle('imports');}catch(e){setError(e.message);}};

  const handleFileChange = (e) => {
    setSelectedFile(null);
    const file = e.target.files[0];
    if (file) {
      if (
        !file.name.toLowerCase().endsWith(".xlsx") &&
        !file.name.toLowerCase().endsWith(".xls")
      ) {
        setError("Please select an Excel file (.xlsx or .xls)");
        return;
      }
      setSelectedFile(file);
      setImportResult(null);
      setError(null);
    }
  };

  const handleImport = async () => {
    if (source==='local'&&!selectedFile) {
      setError("Please select a file first");
      return;
    }

    try {
      setImporting(true);
      setError(null);
      setImportResult(null);

      const formData = new FormData();
      formData.append("file", selectedFile);
      formData.append("term",term);

      const response = source==='google'?await apiService.requestJson('/classlist/import-google',{method:'POST',body:JSON.stringify({url:sheetUrl,worksheet,term})}):await apiService.requestJson("/classlist/import", {
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
    <div className="fade-in coordinator-import">
      <div className="page-header">
        <h1 className="page-title">Classlist Import</h1>
        <p className="page-description">
          Import student enrollments and groups for submission tracking.
        </p>
      </div>

      {error && (
        <div style={{ marginBottom: "1rem", color: "#b91c1c" }}>{error}</div>
      )}

      <div className="card">
        <div style={{padding:24}}><AcademicTermFields value={term} onChange={setTerm} disabled={importing}/><p>Use each student's Google sign-in email. The import creates subjects and sections when needed.</p></div>
        <div className="card-header">
          <h2 className="card-title">Upload Classlist</h2>
        </div>
        <div className="card-content">
          <div className="tracking-actions" role="group" aria-label="Import source">
            <button type="button" className={`btn ${source==='local'?'btn-primary':'btn-secondary'}`} disabled={importing} aria-pressed={source==='local'} onClick={()=>{setSource('local');setError(null);setImportResult(null);}}>Local Excel file</button>
            <button type="button" className={`btn ${source==='google'?'btn-primary':'btn-secondary'}`} disabled={importing} aria-pressed={source==='google'} onClick={()=>{setSource('google');setError(null);setImportResult(null);}}>Google Sheets</button>
          </div>
          {source==='google'?<div className="tracking-form">
            <p>Connect the Google account that owns or can read your classlist. The sheet can remain private. For an Excel file in Drive, open it in Google Sheets and save it as a Google spreadsheet first.</p>
            <button type="button" className="btn btn-secondary" onClick={connectSheets} disabled={importing}>Connect Google Sheets account</button>
            <label>Spreadsheet link<input type="url" value={sheetUrl} onChange={e=>setSheetUrl(e.target.value)} placeholder="https://docs.google.com/spreadsheets/d/..." disabled={importing}/></label>
            <label>Worksheet name (optional)<input value={worksheet} onChange={e=>setWorksheet(e.target.value)} placeholder="Leave blank to import the first worksheet" disabled={importing}/></label>
            <p className="tracking-help">Uses columns A-G in the format below, with a header in row 1. Maximum 5,000 student rows.</p>
          </div>:<>
          <div className="import-dropzone"
            style={{
              border: "2px dashed #d1d5db",
              borderRadius: "0.5rem",
              padding: "2rem",
              textAlign: "center",
              marginBottom: "1rem",
            }}
          >
            <FileSpreadsheet
              size={48}
              style={{ color: "#9ca3af", marginBottom: "0.5rem" }}
            />
            <input
              type="file"
              disabled={importing}
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
            <p
              style={{
                color: "#6b7280",
                fontSize: "0.875rem",
                marginTop: "0.5rem",
              }}
            >
              Supported formats: .xlsx, .xls
            </p>
          </div>
          </>}
          <div className="card import-format" style={{ marginBottom: "1rem" }}>
            <div className="card-content" style={{ padding: "1rem" }}>
              <h3 style={{ margin: "0 0 0.5rem 0", fontSize: "1rem" }}>
                Expected Excel Format
              </h3>
              <p
                style={{
                  margin: "0 0 0.5rem 0",
                  color: "#6b7280",
                  fontSize: "0.875rem",
                }}
              >
                Columns (in order):
              </p>
              <ul
                style={{
                  margin: 0,
                  paddingLeft: "1.25rem",
                  fontSize: "0.875rem",
                }}
              >
                <li>Student ID (##-####-### or ####-#####)</li>
                <li>Full Name</li>
                <li>Email</li>
                <li>Subject Name</li>
                <li>Subject Code</li>
                <li>Section</li>
                <li>
                  Group Name{" "}
                  <span style={{ color: "#9ca3af" }}>
                    (optional — students with the same group name will be
                    auto-grouped)
                  </span>
                </li>
              </ul>
            </div>
          </div>

          <button
            className="btn btn-primary"
            onClick={handleImport}
            disabled={importing || (source==='local'?!selectedFile:!sheetUrl.trim())}
          >
            <Upload size={16} />{" "}
            {importing ? "Importing..." : "Import Classlist"}
          </button>
        </div>
      </div>

      {importResult && (
        <div className="card" style={{ marginTop: "1.5rem" }}>
          <div className="card-header">
            <h2 className="card-title">Import Summary</h2>
            <Link to="/coordinator/classlist-view">Review students and sections →</Link>
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
                  <p className="stat-value">
                    {importResult.successfullyImported || 0}
                  </p>
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
                  <AlertCircle
                    size={16}
                    style={{ marginRight: "0.25rem", display: "inline" }}
                  />
                  Errors
                </h3>
                <ul
                  style={{
                    margin: 0,
                    paddingLeft: "1.25rem",
                    color: "#b91c1c",
                  }}
                >
                  {importResult.errors.map((err, index) => (
                    <li key={index}>{err}</li>
                  ))}
                </ul>
              </div>
            )}

            {importResult.warnings && importResult.warnings.length > 0 && (
              <div>
                <h3 style={{ margin: "0 0 0.5rem 0", fontSize: "1rem" }}>
                  <AlertCircle
                    size={16}
                    style={{ marginRight: "0.25rem", display: "inline" }}
                  />
                  Warnings
                </h3>
                <ul
                  style={{
                    margin: 0,
                    paddingLeft: "1.25rem",
                    color: "#d97706",
                  }}
                >
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
