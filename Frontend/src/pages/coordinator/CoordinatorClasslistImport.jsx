import { useState, useRef } from "react";
import GoogleClasslistBrowser from '../../components/GoogleClasslistBrowser';
import WorkspaceDialog from '../../components/WorkspaceDialog';
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

const CoordinatorClasslistImport = () => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState(null);
  const [term,setTerm]=useState(initialTerm);
  const [source,setSource]=useState(''),[sheetUrl,setSheetUrl]=useState(''),[worksheet,setWorksheet]=useState('');
  const [sheetName,setSheetName]=useState('');
  const fileInput=useRef(null);
  const [chooser,setChooser]=useState(false),[cloudOpen,setCloudOpen]=useState(false);
  const chooseSource=next=>{setSource(next);setError(null);setImportResult(null);setChooser(false);setCloudOpen(next==='google');};

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
        <div style={{padding:24}}><AcademicTermFields value={term} onChange={setTerm} disabled={importing}/><p>Use each student's Google sign-in email. Accepted courses: IT332 — Capstone 1, IT411 — Capstone 2.</p><p>IT332 normally runs in semester 2; IT411 in semester 1. G01/G02 are off-semester sections; G1/G2 are on-semester sections. Section identifiers stay distinct.</p></div>
        <div className="card-header">
          <h2 className="card-title">Upload Classlist</h2>
        </div>
        <div className="card-content">
          <input ref={fileInput} type="file" accept=".xlsx,.xls" disabled={importing} onChange={handleFileChange} hidden id="classlist-file"/>
          <button type="button" className="btn btn-primary" disabled={importing} onClick={()=>setChooser(true)}>{source?'Change import file':'Import classlist'}</button>
          {chooser&&<WorkspaceDialog title="Import classlist" onClose={()=>setChooser(false)}><p>Where is your classlist?</p><div className="import-source-choices">
            <button type="button" className="btn btn-secondary" onClick={()=>{chooseSource('local');fileInput.current.click();}}>Upload from this device</button>
            <button type="button" className="btn btn-secondary" onClick={()=>chooseSource('google')}>Choose from Google Drive</button>
          </div></WorkspaceDialog>}
          {cloudOpen&&<WorkspaceDialog title="Choose from Google Drive" onClose={()=>setCloudOpen(false)}><GoogleClasslistBrowser disabled={importing} onSelect={file=>{setSheetUrl(file.url);setSheetName(file.name);setCloudOpen(false);}}/></WorkspaceDialog>}
          {source==='google'?<div className="tracking-form">
            {sheetUrl?<p>Attached: <strong>{sheetName}</strong></p>:<p>No spreadsheet selected.</p>}
            <label>Worksheet name (optional)<input value={worksheet} onChange={e=>setWorksheet(e.target.value)} placeholder="Leave blank to import the first worksheet" disabled={importing}/></label>
            <p className="tracking-help">Uses columns A-G in the format below, with a header in row 1. Maximum 5,000 student rows.</p>
          </div>:source==='local'?<>
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
          </>:<p>Attach a Google Sheets classlist or an Excel workbook from your device.</p>}
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
                  Group Code{" "}
                  <span style={{ color: "#9ca3af" }}>
                    (optional — students with the same group code will be
                    auto-grouped)
                  </span>
                </li>
              </ul>
            </div>
          </div>

          <button
            className="btn btn-primary"
            onClick={handleImport}
            disabled={importing || !source || (source==='local'?!selectedFile:!sheetUrl.trim())}
          >
            <Upload size={16} />{" "}
            {importing ? "Importing..." : "Import selected classlist"}
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
