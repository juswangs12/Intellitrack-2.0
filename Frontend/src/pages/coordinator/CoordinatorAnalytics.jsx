import { useState, useEffect, useCallback, useMemo } from "react";
import { Sparkles, RefreshCw, Filter, Search, Download, X, ChevronLeft, ChevronRight } from "lucide-react";
import apiService from "../../services/ApiService";

// ─── Status helpers ───────────────────────────────────────────────────────────

const STATUS_META = {
  SUBMITTED_ON_TIME: { label: "On Time",        bg: "#d1fae5", color: "#065f46", border: "#a7f3d0" },
  SUBMITTED_LATE:    { label: "Late",            bg: "#fee2e2", color: "#991b1b", border: "#fca5a5" },
  DID_NOT_SUBMIT:    { label: "Not Submitted",   bg: "#fef3c7", color: "#92400e", border: "#fcd34d" },
  DUE_TODAY:         { label: "Due Today",       bg: "#fef3c7", color: "#92400e", border: "#fcd34d" },
  UPCOMING:          { label: "Upcoming",        bg: "#dbeafe", color: "#1e40af", border: "#93c5fd" },
  NOT_YET_ASSIGNED:  { label: "N/A",             bg: "#f3f4f6", color: "#9ca3af", border: "#e5e7eb" },
  COMMENTED:         { label: "Commented",       bg: "#ede9fe", color: "#5b21b6", border: "#c4b5fd" },
};

const getStatusMeta = (status) =>
  STATUS_META[status] ?? { label: status ?? "-", bg: "#f3f4f6", color: "#6b7280", border: "#e5e7eb" };

const AI_STATUS_META = {
  COMPLETED:    { label: "Analyzed",    bg: "#d1fae5", color: "#065f46" },
  PROCESSING:   { label: "Processing",  bg: "#dbeafe", color: "#1e40af" },
  PENDING:      { label: "Pending",     bg: "#f3f4f6", color: "#6b7280" },
  FAILED:       { label: "Failed",      bg: "#fee2e2", color: "#991b1b" },
  RATE_LIMITED: { label: "Rate Ltd",    bg: "#fef3c7", color: "#92400e" },
};

const computeRiskLevel = (completionRate, lateCount, missingCount) => {
  if ((completionRate ?? 0) < 50 || (missingCount ?? 0) > 3) return "HIGH";
  if ((completionRate ?? 0) < 75 || (lateCount ?? 0) > 1)   return "MEDIUM";
  return "LOW";
};

const RISK_META = {
  HIGH:   { bg: "#fee2e2", color: "#991b1b", border: "#fca5a5" },
  MEDIUM: { bg: "#fef3c7", color: "#92400e", border: "#fcd34d" },
  LOW:    { bg: "#d1fae5", color: "#065f46", border: "#a7f3d0" },
};

const computeRemarks = (status) => {
  switch (status) {
    case "SUBMITTED_ON_TIME": return "On track";
    case "SUBMITTED_LATE":    return "Submitted late — monitor";
    case "DID_NOT_SUBMIT":    return "Immediate attention needed";
    case "DUE_TODAY":         return "Deadline today";
    case "UPCOMING":          return "Upcoming deadline";
    default:                  return "-";
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  const d = new Date(dateStr);
  return d.toLocaleDateString([], { year: "numeric", month: "short", day: "numeric" });
};

const formatDateTime = (dateStr) => {
  if (!dateStr) return "-";
  const d = new Date(dateStr);
  return d.toLocaleDateString([], { year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
};

// ─── Card component ───────────────────────────────────────────────────────────

const SummaryCard = ({ label, value, valueColor, sub }) => (
  <div style={{
    padding: "20px",
    backgroundColor: "#fff",
    borderRadius: "12px",
    boxShadow: "0 1px 2px rgba(0,0,0,.05)",
    border: "1px solid #e5e7eb",
    minWidth: 0,
  }}>
    <p style={{ color: "#6b7280", margin: "0 0 6px", fontSize: "13px", fontWeight: 500 }}>{label}</p>
    <p style={{ fontSize: "26px", fontWeight: "700", color: valueColor ?? "#1f2937", margin: 0, lineHeight: 1 }}>
      {value ?? 0}
    </p>
    {sub && <p style={{ fontSize: "12px", color: "#9ca3af", margin: "4px 0 0" }}>{sub}</p>}
  </div>
);

// ─── Main component ───────────────────────────────────────────────────────────

const ROWS_PER_PAGE = 50;

const CoordinatorAnalytics = () => {
  const [analytics, setAnalytics]                 = useState(null);
  const [loading, setLoading]                     = useState(true);
  const [generatingAi, setGeneratingAi]           = useState(false);
  const [generatingGoogleSheet, setGeneratingGoogleSheet] = useState(false);
  const [googleSheetProgress, setGoogleSheetProgress]     = useState("");
  const [googleSheetUrl, setGoogleSheetUrl]       = useState(null);
  const [error, setError]                         = useState(null);
  const [aiSummary, setAiSummary]                 = useState(null);
  const [showAiSummary, setShowAiSummary]         = useState(false);
  const [showFilters, setShowFilters]             = useState(false);
  const [showExportMenu, setShowExportMenu]       = useState(false);
  const [page, setPage]                           = useState(0);
  const [viewMode, setViewMode]                   = useState("student"); // "flat" or "student"

  // Filters
  const [searchQuery, setSearchQuery]             = useState("");
  const [selectedSubject, setSelectedSubject]     = useState("");
  const [selectedSection, setSelectedSection]     = useState("");
  const [selectedGroup, setSelectedGroup]         = useState("");
  const [selectedAdviser, setSelectedAdviser]     = useState("");
  const [selectedDeliverable, setSelectedDeliverable] = useState("");
  const [statusFilter, setStatusFilter]           = useState("");

  // ── Fetch ──────────────────────────────────────────────────────────────────

  const fetchAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      setPage(0);
      const data = await apiService.requestJson("/analytics/coordinator-v2");
      setAnalytics(data);
    } catch (err) {
      console.error("Failed to fetch analytics:", err);
      setError("Failed to load analytics. Please try again.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchAnalytics(); }, [fetchAnalytics]);

  // Close export menu on outside click
  useEffect(() => {
    const handler = (e) => {
      const menu   = document.getElementById("export-menu");
      const button = document.getElementById("export-button");
      if (menu && button && !menu.contains(e.target) && !button.contains(e.target)) {
        setShowExportMenu(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  // ── Derived summary values (frontend-computed, no backend change needed) ──

  const totalDeliverables = analytics?.deliverables?.length ?? 0;

  const pendingCount = useMemo(() => {
    if (!analytics?.students) return 0;
    return analytics.students.filter(s =>
      s.deliverableStatuses?.some(c => c.status === "UPCOMING" || c.status === "DUE_TODAY")
    ).length;
  }, [analytics]);

  const institutionRiskScore = useMemo(() => {
    const rate = analytics?.summaryCards?.overallCompletionRate ?? 0;
    return rate < 50 ? "HIGH" : rate < 75 ? "MEDIUM" : "LOW";
  }, [analytics]);

  // ── Build flat rows ────────────────────────────────────────────────────────

  const allFlatRows = useMemo(() => {
    if (!analytics) return [];
    const deliverableMap = Object.fromEntries(
      (analytics.deliverables ?? []).map(d => [d.id, d])
    );
    return (analytics.students ?? []).flatMap(student => {
      const riskLevel = computeRiskLevel(student.completionRate, student.lateCount, student.missingCount);
      return (student.deliverableStatuses ?? [])
        .filter(cell => cell.status !== "NOT_YET_ASSIGNED")
        .map(cell => {
          const meta = deliverableMap[cell.deliverableId] ?? {};
          return {
            // student
            enrollmentId:  student.enrollmentId,
            fullName:      student.fullName,
            studentId:     student.studentId ?? "-",
            subject:       student.subject   ?? "-",
            section:       student.section   ?? "-",
            groupName:     student.groupName ?? "Unassigned",
            adviserName:   student.adviserName ?? "-",
            accountStatus: student.accountStatus,
            completionRate: student.completionRate ?? 0,
            riskLevel,
            // deliverable
            deliverableId:   cell.deliverableId,
            deliverableName: meta.name  ?? "-",
            stage:           meta.stage ?? "-",
            deadline:        meta.dueAt ?? null,
            // cell
            status:      cell.status,
            displayText: cell.displayText,
            submittedAt: cell.submittedAt ?? null,
            lateDays:    cell.lateDays    ?? null,
            aiStatus:    cell.aiStatus    ?? null,
            remarks:     computeRemarks(cell.status),
          };
        });
    });
  }, [analytics]);

  // ── Filter flat rows ───────────────────────────────────────────────────────

  const filteredFlatRows = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return allFlatRows.filter(row => {
      if (selectedSubject    && row.subject        !== selectedSubject)    return false;
      if (selectedSection    && row.section        !== selectedSection)    return false;
      if (selectedGroup      && row.groupName      !== selectedGroup)      return false;
      if (selectedAdviser    && row.adviserName    !== selectedAdviser)    return false;
      if (selectedDeliverable && row.deliverableName !== selectedDeliverable) return false;
      if (statusFilter) {
        if (statusFilter === "LATE_ONLY"    && row.status !== "SUBMITTED_LATE")  return false;
        if (statusFilter === "MISSING_ONLY" && row.status !== "DID_NOT_SUBMIT")  return false;
        if (statusFilter === "ON_TIME_ONLY" && row.status !== "SUBMITTED_ON_TIME") return false;
        if (statusFilter === "UPCOMING_ONLY" && row.status !== "UPCOMING" && row.status !== "DUE_TODAY") return false;
      }
      if (q && !row.fullName.toLowerCase().includes(q) && !row.studentId.toLowerCase().includes(q)) return false;
      return true;
    });
  }, [allFlatRows, searchQuery, selectedSubject, selectedSection, selectedGroup, selectedAdviser, selectedDeliverable, statusFilter]);

  const filteredStudents = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return (analytics?.students ?? []).filter(student => {
      if (selectedSubject    && student.subject        !== selectedSubject)    return false;
      if (selectedSection    && student.section        !== selectedSection)    return false;
      if (selectedGroup      && student.groupName      !== selectedGroup)      return false;
      if (selectedAdviser    && student.adviserName    !== selectedAdviser)    return false;
      if (q && !student.fullName.toLowerCase().includes(q) && !student.studentId.toLowerCase().includes(q)) return false;
      return true;
    });
  }, [analytics, searchQuery, selectedSubject, selectedSection, selectedGroup, selectedAdviser]);

  // Reset to page 0 when filters or viewMode changes
  useEffect(() => { setPage(0); }, [filteredFlatRows, filteredStudents, viewMode]);

  // Group students by subject for separate grids
  const studentsBySubject = useMemo(() => {
    const groups = {};
    filteredStudents.forEach(student => {
      const subject = student.subject || "Unassigned";
      if (!groups[subject]) groups[subject] = [];
      groups[subject].push(student);
    });
    return groups;
  }, [filteredStudents]);

  // Group flat rows by subject
  const flatRowsBySubject = useMemo(() => {
    const groups = {};
    filteredFlatRows.forEach(row => {
      const subject = row.subject || "Unassigned";
      if (!groups[subject]) groups[subject] = [];
      groups[subject].push(row);
    });
    return groups;
  }, [filteredFlatRows]);

  // Filter deliverables by subject (deliverable has subjectId or not)
  // First, let's get subject id map from analytics.subjects
  const subjectIdByCode = useMemo(() => {
    if (!analytics?.subjects) return {};
    return Object.fromEntries(
      analytics.subjects.map(s => [s.code, s.id])
    );
  }, [analytics]);

  const totalPages   = Math.max(1, Math.ceil(filteredFlatRows.length / ROWS_PER_PAGE));
  const totalStudentPages = Math.max(1, Math.ceil(filteredStudents.length / ROWS_PER_PAGE));
  const visibleRows  = filteredFlatRows.slice(page * ROWS_PER_PAGE, (page + 1) * ROWS_PER_PAGE);
  const visibleStudents = filteredStudents.slice(page * ROWS_PER_PAGE, (page + 1) * ROWS_PER_PAGE);

  // ── Filter options ─────────────────────────────────────────────────────────

  const subjectOptions    = useMemo(() => [...new Set((analytics?.students ?? []).map(s => s.subject).filter(Boolean))].sort(), [analytics]);
  const sectionOptions    = useMemo(() => [...new Set((analytics?.students ?? []).map(s => s.section).filter(Boolean))].sort(), [analytics]);
  const groupOptions      = useMemo(() => [...new Set((analytics?.students ?? []).map(s => s.groupName).filter(Boolean))].sort(), [analytics]);
  const adviserOptions    = useMemo(() => [...new Set((analytics?.students ?? []).map(s => s.adviserName).filter(Boolean))].sort(), [analytics]);
  const deliverableOptions = useMemo(() => (analytics?.deliverables ?? []).map(d => d.name).sort(), [analytics]);

  // ── AI Summary ────────────────────────────────────────────────────────────

  const generateAiSummary = useCallback(async () => {
    try {
      setGeneratingAi(true);
      const result = await apiService.requestJson("/analytics/generate-coordinator-ai-summary", { method: "POST" });
      if (result) { setAiSummary(result); setShowAiSummary(true); }
    } catch (err) {
      console.error("Failed to generate AI summary:", err);
      alert("Failed to generate AI summary. Please try again.");
    } finally {
      setGeneratingAi(false);
    }
  }, []);

  // ── Google Sheets export ───────────────────────────────────────────────────

  const exportToGoogleSheets = async () => {
    try {
      setGeneratingGoogleSheet(true);
      setGoogleSheetUrl(null);
      setGoogleSheetProgress("Collecting analytics...");
      const result = await apiService.requestJson("/export/analytics/google-sheets", { method: "POST" });
      if (typeof result === "string") {
        setGoogleSheetUrl(result);
        setGoogleSheetProgress("Open Google Spreadsheet");
      }
    } catch (err) {
      console.error("Failed to generate Google Sheet:", err);
      alert(err?.message ?? "Failed to generate Google Sheet. Please try again.");
    } finally {
      setGeneratingGoogleSheet(false);
    }
  };

  // ── CSV export ────────────────────────────────────────────────────────────

  const exportToCSV = () => {
    if (!filteredFlatRows.length) return;
    const headers = [
      "Student Name", "Student ID", "Subject", "Section", "Group", "Adviser",
      "Account Status", "Completion Rate", "Deliverable", "Stage", "Deadline",
      "Submission Status", "Submitted Date", "Days Late", "AI Status", "Risk Level", "Remarks",
    ];
    const escape = (v) => {
      const s = v == null ? "" : String(v);
      return s.includes(",") || s.includes('"') || s.includes("\n") ? `"${s.replace(/"/g, '""')}"` : s;
    };
    const lines = [headers.map(escape).join(",")];
    for (const row of filteredFlatRows) {
      lines.push([
        row.fullName, row.studentId, row.subject, row.section, row.groupName, row.adviserName,
        row.accountStatus, row.completionRate + "%", row.deliverableName, row.stage,
        formatDate(row.deadline), row.displayText ?? row.status,
        formatDateTime(row.submittedAt),
        row.lateDays != null ? row.lateDays + " days" : "-",
        row.aiStatus ? (AI_STATUS_META[row.aiStatus]?.label ?? row.aiStatus) : "-",
        row.riskLevel, row.remarks,
      ].map(escape).join(","));
    }
    const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8;" });
    const url  = URL.createObjectURL(blob);
    const link = Object.assign(document.createElement("a"), {
      href: url,
      download: `intellitrack-analytics-${new Date().toISOString().slice(0, 10)}.csv`,
      style: "display:none",
    });
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  // ── Shared button style ────────────────────────────────────────────────────

  const btnStyle = (primary = false) => ({
    display: "flex", alignItems: "center", gap: "8px",
    padding: "8px 16px", borderRadius: "8px", cursor: "pointer",
    fontWeight: 500, fontSize: "14px",
    border: primary ? "none" : "1px solid #e5e7eb",
    backgroundColor: primary ? "#7c3aed" : "#fff",
    color: primary ? "#fff" : "#1f2937",
  });

  // ── Render ─────────────────────────────────────────────────────────────────

  if (loading) {
    return (
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", minHeight: "400px", color: "#6b7280" }}>
        Loading institutional analytics…
      </div>
    );
  }

  const sc = analytics?.summaryCards;

  return (
    <div className="fade-in" style={{ padding: "24px" }}>

      {/* ── Header ── */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "24px", flexWrap: "wrap", gap: "12px" }}>
        <div>
          <h1 style={{ fontSize: "22px", fontWeight: "700", color: "#1f2937", margin: 0 }}>
            Institutional Analytics Dashboard
          </h1>
          <p style={{ color: "#6b7280", margin: "4px 0 0", fontSize: "14px" }}>
            {viewMode === "flat" 
              ? `${filteredFlatRows.length.toLocaleString()} submission records${allFlatRows.length !== filteredFlatRows.length ? ` (filtered from ${allFlatRows.length.toLocaleString()})` : ""}` 
              : `${filteredStudents.length.toLocaleString()} students${filteredStudents.length !== analytics?.students?.length ? ` (filtered from ${analytics.students.length.toLocaleString()})` : ""}`}
          </p>
        </div>
        <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", alignItems: "center" }}>
          <div style={{ display: "flex", gap: "4px", backgroundColor: "#f3f4f6", borderRadius: "8px", padding: "4px" }}>
            <button onClick={() => setViewMode("flat")} disabled={viewMode === "flat"} style={{ ...btnStyle(), padding: "6px 12px", fontSize: "13px", backgroundColor: viewMode === "flat" ? "#fff" : "transparent", boxShadow: viewMode === "flat" ? "0 1px 2px rgba(0,0,0,.05)" : "none" }}>
              Deliverable per Row
            </button>
            <button onClick={() => setViewMode("student")} disabled={viewMode === "student"} style={{ ...btnStyle(), padding: "6px 12px", fontSize: "13px", backgroundColor: viewMode === "student" ? "#fff" : "transparent", boxShadow: viewMode === "student" ? "0 1px 2px rgba(0,0,0,.05)" : "none" }}>
              Student per Row
            </button>
          </div>
          <button onClick={() => setShowFilters(f => !f)} style={btnStyle()}>
            <Filter size={15} /> Filters
          </button>
          <button onClick={generateAiSummary} disabled={generatingAi} style={{ ...btnStyle(true), opacity: generatingAi ? 0.7 : 1, cursor: generatingAi ? "not-allowed" : "pointer" }}>
            <Sparkles size={15} /> {generatingAi ? "Generating…" : "AI Summary"}
          </button>
          <div style={{ position: "relative" }}>
            <button id="export-button" onClick={() => setShowExportMenu(m => !m)} disabled={!analytics} style={{ ...btnStyle(), opacity: analytics ? 1 : 0.5, cursor: analytics ? "pointer" : "not-allowed" }}>
              <Download size={15} /> Export
            </button>
            {showExportMenu && (
              <div id="export-menu" style={{ position: "absolute", right: 0, top: "calc(100% + 4px)", backgroundColor: "#fff", borderRadius: "8px", boxShadow: "0 4px 12px rgba(0,0,0,.1)", border: "1px solid #e5e7eb", zIndex: 100, minWidth: "200px" }}>
                {[
                  { label: "Download CSV", icon: <Download size={14} />, action: () => { exportToCSV(); setShowExportMenu(false); } },
                  { label: "Open in Google Sheets", icon: <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>, action: () => { setShowExportMenu(false); exportToGoogleSheets(); } },
                ].map(({ label, icon, action }) => (
                  <button key={label} onClick={action} style={{ display: "flex", alignItems: "center", gap: "8px", width: "100%", padding: "10px 16px", border: "none", backgroundColor: "transparent", textAlign: "left", cursor: "pointer", fontSize: "14px", color: "#1f2937" }}>
                    {icon} {label}
                  </button>
                ))}
              </div>
            )}
          </div>
          <button onClick={fetchAnalytics} style={btnStyle()}>
            <RefreshCw size={15} /> Refresh
          </button>
        </div>
      </div>

      {/* ── Error ── */}
      {error && (
        <div style={{ padding: "12px 16px", backgroundColor: "#fef2f2", border: "1px solid #fecaca", borderRadius: "8px", color: "#991b1b", marginBottom: "24px", fontSize: "14px" }}>
          {error}
        </div>
      )}

      {/* ── Google Sheets progress ── */}
      {(generatingGoogleSheet || googleSheetUrl) && (
        <div style={{ padding: "20px", backgroundColor: "#fff", borderRadius: "12px", boxShadow: "0 1px 2px rgba(0,0,0,.05)", border: "1px solid #e5e7eb", marginBottom: "24px" }}>
          <h3 style={{ margin: "0 0 12px", fontSize: "16px", fontWeight: 600 }}>Generating Institutional Report</h3>
          <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
            <div style={{ width: 28, height: 28, borderRadius: "50%", border: "3px solid #e5e7eb", borderTopColor: generatingGoogleSheet ? "#7c3aed" : "#059669", animation: generatingGoogleSheet ? "spin 1s linear infinite" : "none" }} />
            <span style={{ fontSize: "14px", color: "#374151" }}>{googleSheetProgress}</span>
          </div>
          {googleSheetUrl && (
            <div style={{ display: "flex", gap: "12px", marginTop: "12px" }}>
              <button onClick={() => window.open(googleSheetUrl, "_blank")} style={{ ...btnStyle(true), backgroundColor: "#059669" }}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                Open Google Sheet
              </button>
              <button onClick={() => { setGoogleSheetUrl(null); setGoogleSheetProgress(""); }} style={btnStyle()}>
                Close
              </button>
            </div>
          )}
        </div>
      )}

      {/* ── Summary Cards (10) ── */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))", gap: "12px", marginBottom: "24px" }}>
        <SummaryCard label="Total Students"    value={sc?.totalStudents}                                                 />
        <SummaryCard label="Total Groups"      value={sc?.totalGroups}                                                   />
        <SummaryCard label="Total Advisers"    value={sc?.totalAdvisers}                                                 />
        <SummaryCard label="Total Deliverables" value={totalDeliverables}                                               />
        <SummaryCard label="Submitted"         value={sc?.studentsSubmitted}  valueColor="#059669"                       />
        <SummaryCard label="Pending"           value={pendingCount}           valueColor="#d97706"                       />
        <SummaryCard label="Late"              value={sc?.studentsLate}       valueColor="#dc2626"                       />
        <SummaryCard label="Missing"           value={sc?.studentsMissing}    valueColor="#dc2626"                       />
        <SummaryCard label="Avg Completion"    value={(sc?.overallCompletionRate ?? 0) + "%"} valueColor="#7c3aed"       />
        <SummaryCard
          label="Institution Risk"
          value={institutionRiskScore}
          valueColor={RISK_META[institutionRiskScore]?.color ?? "#6b7280"}
          sub={`Based on ${(sc?.overallCompletionRate ?? 0)}% avg completion`}
        />
      </div>

      {/* ── Filters ── */}
      {showFilters && (
        <div style={{ backgroundColor: "#fff", borderRadius: "12px", padding: "20px", marginBottom: "24px", boxShadow: "0 1px 2px rgba(0,0,0,.05)", border: "1px solid #e5e7eb" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
            <h3 style={{ margin: 0, fontSize: "15px", fontWeight: 600 }}>Filter Options</h3>
            <button onClick={() => setShowFilters(false)} style={{ border: "none", backgroundColor: "transparent", cursor: "pointer" }}>
              <X size={16} />
            </button>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))", gap: "16px" }}>
            {/* Search */}
            <div>
              <label style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "#374151", marginBottom: "6px" }}>Search</label>
              <div style={{ position: "relative" }}>
                <Search size={14} style={{ position: "absolute", left: "10px", top: "50%", transform: "translateY(-50%)", color: "#9ca3af" }} />
                <input type="text" value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                  placeholder="Name or student ID"
                  style={{ width: "100%", padding: "7px 10px 7px 32px", borderRadius: "8px", border: "1px solid #e5e7eb", fontSize: "13px", boxSizing: "border-box" }} />
              </div>
            </div>
            {/* Dropdowns */}
            {[
              { label: "Subject",    value: selectedSubject,    set: setSelectedSubject,    opts: subjectOptions,    all: "All Subjects"    },
              { label: "Section",    value: selectedSection,    set: setSelectedSection,    opts: sectionOptions,    all: "All Sections"    },
              { label: "Group",      value: selectedGroup,      set: setSelectedGroup,      opts: groupOptions,      all: "All Groups"      },
              { label: "Adviser",    value: selectedAdviser,    set: setSelectedAdviser,    opts: adviserOptions,    all: "All Advisers"    },
              { label: "Deliverable",value: selectedDeliverable,set: setSelectedDeliverable,opts: deliverableOptions, all: "All Deliverables" },
            ].map(({ label, value, set, opts, all }) => (
              <div key={label}>
                <label style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "#374151", marginBottom: "6px" }}>{label}</label>
                <select value={value} onChange={e => set(e.target.value)}
                  style={{ width: "100%", padding: "7px 10px", borderRadius: "8px", border: "1px solid #e5e7eb", fontSize: "13px" }}>
                  <option value="">{all}</option>
                  {opts.map(o => <option key={o} value={o}>{o}</option>)}
                </select>
              </div>
            ))}
            {/* Status */}
            <div>
              <label style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "#374151", marginBottom: "6px" }}>Status</label>
              <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}
                style={{ width: "100%", padding: "7px 10px", borderRadius: "8px", border: "1px solid #e5e7eb", fontSize: "13px" }}>
                <option value="">All Statuses</option>
                <option value="ON_TIME_ONLY">On Time Only</option>
                <option value="LATE_ONLY">Late Only</option>
                <option value="MISSING_ONLY">Not Submitted Only</option>
                <option value="UPCOMING_ONLY">Upcoming / Due Today</option>
              </select>
            </div>
          </div>
          {/* Clear filters */}
          {(searchQuery || selectedSubject || selectedSection || selectedGroup || selectedAdviser || selectedDeliverable || statusFilter) && (
            <button
              onClick={() => { setSearchQuery(""); setSelectedSubject(""); setSelectedSection(""); setSelectedGroup(""); setSelectedAdviser(""); setSelectedDeliverable(""); setStatusFilter(""); }}
              style={{ marginTop: "12px", padding: "6px 14px", borderRadius: "6px", border: "1px solid #e5e7eb", backgroundColor: "#fff", color: "#374151", fontSize: "13px", cursor: "pointer" }}>
              Clear all filters
            </button>
          )}
        </div>
      )}

      {/* ── Analytics Tables (split by subject) ── */}
      {analytics && (
        <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
          {Object.keys(viewMode === "flat" ? flatRowsBySubject : studentsBySubject).map(subject => {
            const subjectStudents = studentsBySubject[subject] || [];
            const subjectFlatRows = flatRowsBySubject[subject] || [];
            // Filter deliverables for this subject: deliverables that have subjectId matching the current subject, or no subjectId (if we want global)
            // Wait, let's get the subject id for this subject code
            const currentSubjectId = subjectIdByCode[subject];
            const subjectDeliverables = (analytics.deliverables || []).filter(d => 
              d.subjectId == null || d.subjectId === currentSubjectId
            );

            // Pagination per subject? Or keep global? Let's keep global for now, or maybe per subject?
            // Let's do per subject pagination for better UX! Wait, let's add state for per subject pages?
            // Wait, for simplicity, let's just show all for now, or keep global. Alternatively, let's just render all rows without pagination per subject for now!
            const rowsToRender = viewMode === "flat" ? subjectFlatRows : subjectStudents;

            if (rowsToRender.length === 0) return null;

            return (
              <div key={subject} style={{ backgroundColor: "#fff", borderRadius: "12px", boxShadow: "0 1px 2px rgba(0,0,0,.05)", border: "1px solid #e5e7eb", overflow: "hidden" }}>
                <div style={{ padding: "16px 20px", borderBottom: "1px solid #e5e7eb", backgroundColor: "#f9fafb" }}>
                  <h3 style={{ margin: 0, fontSize: "16px", fontWeight: 600, color: "#1f2937" }}>
                    {subject}
                  </h3>
                  <p style={{ margin: "4px 0 0", fontSize: "13px", color: "#6b7280" }}>
                    {viewMode === "flat" 
                      ? `${subjectFlatRows.length.toLocaleString()} records` 
                      : `${subjectStudents.length.toLocaleString()} students`}
                  </p>
                </div>
                <div style={{ overflowX: "auto" }}>
                  {viewMode === "flat" ? (
                    <table style={{ width: "100%", borderCollapse: "collapse", minWidth: "1400px" }}>
                      <thead>
                        <tr style={{ backgroundColor: "#f9fafb" }}>
                          {[
                            { label: "Student",        minW: 180 },
                            { label: "Student ID",     minW: 110 },
                            { label: "Section",        minW: 90  },
                            { label: "Group",          minW: 120 },
                            { label: "Adviser",        minW: 140 },
                            { label: "Account",        minW: 90  },
                            { label: "Deliverable",    minW: 150 },
                            { label: "Deadline",       minW: 110 },
                            { label: "Status",         minW: 140 },
                            { label: "Submitted",      minW: 140 },
                            { label: "Days Late",      minW: 90  },
                            { label: "AI Status",      minW: 100 },
                            { label: "Completion",     minW: 90  },
                            { label: "Risk",           minW: 80  },
                            { label: "Remarks",        minW: 180 },
                          ].map(({ label, minW }) => (
                            <th key={label} style={{
                              padding: "10px 14px", textAlign: "left", fontSize: "11px", fontWeight: 600,
                              color: "#6b7280", borderBottom: "1px solid #e5e7eb", minWidth: minW,
                              whiteSpace: "nowrap", letterSpacing: "0.03em", textTransform: "uppercase",
                            }}>
                              {label}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {subjectFlatRows.map((row, idx) => {
                          const statusM = getStatusMeta(row.status);
                          const riskM   = RISK_META[row.riskLevel] ?? {};
                          const aiM     = row.aiStatus ? (AI_STATUS_META[row.aiStatus] ?? { label: row.aiStatus, bg: "#f3f4f6", color: "#6b7280" }) : null;
                          const isLinked = row.accountStatus === "LINKED";
                          return (
                            <tr key={`${row.enrollmentId}-${row.deliverableId}-${idx}`}
                              style={{ backgroundColor: idx % 2 === 0 ? "#fff" : "#fafafa" }}
                              onMouseEnter={e => e.currentTarget.style.backgroundColor = "#f0f9ff"}
                              onMouseLeave={e => e.currentTarget.style.backgroundColor = idx % 2 === 0 ? "#fff" : "#fafafa"}
                            >
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", fontWeight: 500, color: "#1f2937", fontSize: "13px" }}>
                                {row.fullName}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", color: "#6b7280", fontSize: "12px" }}>
                                {row.studentId}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", color: "#6b7280", fontSize: "12px" }}>
                                {row.section}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", color: "#374151", fontSize: "12px" }}>
                                {row.groupName}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", color: "#374151", fontSize: "12px" }}>
                                {row.adviserName}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6" }}>
                                <span style={{
                                  display: "inline-block", padding: "3px 8px", borderRadius: "9999px", fontSize: "11px", fontWeight: 500,
                                  backgroundColor: isLinked ? "#d1fae5" : "#fef3c7",
                                  color:           isLinked ? "#059669" : "#92400e",
                                  border:          `1px solid ${isLinked ? "#a7f3d0" : "#fcd34d"}`,
                                }}>
                                  {row.accountStatus}
                                </span>
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", fontSize: "13px", color: "#1f2937" }}>
                                <div style={{ fontWeight: 500 }}>{row.deliverableName}</div>
                                <div style={{ fontSize: "11px", color: "#9ca3af" }}>{row.stage}</div>
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", fontSize: "12px", color: "#374151", whiteSpace: "nowrap" }}>
                                {formatDate(row.deadline)}
                              </td>
                              <td style={{ padding: "8px 14px", borderBottom: "1px solid #f3f4f6" }}>
                                <span style={{
                                  display: "inline-block", padding: "4px 10px", borderRadius: "6px", fontSize: "12px", fontWeight: 500,
                                  backgroundColor: statusM.bg, color: statusM.color, border: `1px solid ${statusM.border}`,
                                  whiteSpace: "nowrap",
                                }}>
                                  {row.displayText ?? statusM.label}
                                </span>
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", fontSize: "12px", color: "#374151", whiteSpace: "nowrap" }}>
                                {row.submittedAt ? formatDateTime(row.submittedAt) : "-"}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", fontSize: "12px", color: row.lateDays ? "#dc2626" : "#9ca3af", textAlign: "center" }}>
                                {row.lateDays != null ? `${row.lateDays}d` : "-"}
                              </td>
                              <td style={{ padding: "8px 14px", borderBottom: "1px solid #f3f4f6" }}>
                                {aiM ? (
                                  <span style={{ display: "inline-block", padding: "3px 8px", borderRadius: "6px", fontSize: "11px", fontWeight: 500, backgroundColor: aiM.bg, color: aiM.color }}>
                                    {aiM.label}
                                  </span>
                                ) : (
                                  <span style={{ color: "#9ca3af", fontSize: "12px" }}>-</span>
                                )}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", fontWeight: 600, fontSize: "12px", color: row.completionRate >= 80 ? "#059669" : row.completionRate >= 50 ? "#d97706" : "#dc2626" }}>
                                {row.completionRate}%
                              </td>
                              <td style={{ padding: "8px 14px", borderBottom: "1px solid #f3f4f6" }}>
                                <span style={{
                                  display: "inline-block", padding: "3px 8px", borderRadius: "6px", fontSize: "11px", fontWeight: 600,
                                  backgroundColor: riskM.bg, color: riskM.color, border: `1px solid ${riskM.border}`,
                                }}>
                                  {row.riskLevel}
                                </span>
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", fontSize: "12px", color: "#6b7280" }}>
                                {row.remarks}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  ) : (
                    <table style={{ width: "100%", borderCollapse: "collapse" }}>
                      <thead>
                        <tr style={{ backgroundColor: "#f9fafb" }}>
                          {[
                            { label: "Student",        minW: 180 },
                            { label: "Student ID",     minW: 110 },
                            { label: "Section",        minW: 90  },
                            { label: "Group",          minW: 120 },
                            { label: "Adviser",        minW: 140 },
                            { label: "Account",        minW: 90  },
                            { label: "Completion",     minW: 90  },
                            { label: "Risk",           minW: 80  },
                            ...(subjectDeliverables.map(d => ({ label: d.name, minW: 150 })) || []),
                          ].map(({ label, minW }) => (
                            <th key={label} style={{
                              padding: "10px 14px", textAlign: "left", fontSize: "11px", fontWeight: 600,
                              color: "#6b7280", borderBottom: "1px solid #e5e7eb", minWidth: minW,
                              whiteSpace: "nowrap", letterSpacing: "0.03em", textTransform: "uppercase",
                            }}>
                              {label}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {subjectStudents.map((student, idx) => {
                          const riskLevel = computeRiskLevel(student.completionRate, student.lateCount, student.missingCount);
                          const riskM     = RISK_META[riskLevel] ?? {};
                          const isLinked = student.accountStatus === "LINKED";

                          return (
                            <tr key={student.enrollmentId}
                              style={{ backgroundColor: idx % 2 === 0 ? "#fff" : "#fafafa" }}
                              onMouseEnter={e => e.currentTarget.style.backgroundColor = "#f0f9ff"}
                              onMouseLeave={e => e.currentTarget.style.backgroundColor = idx % 2 === 0 ? "#fff" : "#fafafa"}
                            >
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", fontWeight: 500, color: "#1f2937", fontSize: "13px" }}>
                                {student.fullName}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", color: "#6b7280", fontSize: "12px" }}>
                                {student.studentId}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", color: "#6b7280", fontSize: "12px" }}>
                                {student.section}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", color: "#374151", fontSize: "12px" }}>
                                {student.groupName}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", color: "#374151", fontSize: "12px" }}>
                                {student.adviserName}
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6" }}>
                                <span style={{
                                  display: "inline-block", padding: "3px 8px", borderRadius: "9999px", fontSize: "11px", fontWeight: 500,
                                  backgroundColor: isLinked ? "#d1fae5" : "#fef3c7",
                                  color:           isLinked ? "#059669" : "#92400e",
                                  border:          `1px solid ${isLinked ? "#a7f3d0" : "#fcd34d"}`,
                                }}>
                                  {student.accountStatus}
                                </span>
                              </td>
                              <td style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6", fontWeight: 600, fontSize: "12px", color: student.completionRate >= 80 ? "#059669" : student.completionRate >= 50 ? "#d97706" : "#dc2626" }}>
                                {student.completionRate}%
                              </td>
                              <td style={{ padding: "8px 14px", borderBottom: "1px solid #f3f4f6" }}>
                                <span style={{
                                  display: "inline-block", padding: "3px 8px", borderRadius: "6px", fontSize: "11px", fontWeight: 600,
                                  backgroundColor: riskM.bg, color: riskM.color, border: `1px solid ${riskM.border}`,
                                }}>
                                  {riskLevel}
                                </span>
                              </td>
                              {subjectDeliverables.map(deliverable => {
                                const cell = student.deliverableStatuses?.find(c => c.deliverableId === deliverable.id);
                                const statusM = cell ? getStatusMeta(cell.status) : { label: "-", bg: "#fff", color: "#6b7280", border: "#e5e7eb" };
                                const aiM = cell?.aiStatus ? (AI_STATUS_META[cell.aiStatus] ?? { label: cell.aiStatus, bg: "#f3f4f6", color: "#6b7280" }) : null;
                                return (
                                  <td key={deliverable.id} style={{ padding: "10px 14px", borderBottom: "1px solid #f3f4f6" }}>
                                    <div style={{ display: "flex", flexDirection: "column", gap: "2px" }}>
                                      <span style={{
                                        display: "inline-block", padding: "4px 10px", borderRadius: "6px", fontSize: "12px", fontWeight: 500,
                                        backgroundColor: statusM.bg, color: statusM.color, border: `1px solid ${statusM.border}`,
                                        whiteSpace: "nowrap", alignSelf: "flex-start",
                                      }}>
                                        {cell?.displayText ?? statusM.label}
                                      </span>
                                      {aiM && (
                                        <span style={{ display: "inline-block", padding: "2px 6px", borderRadius: "4px", fontSize: "10px", fontWeight: 500, backgroundColor: aiM.bg, color: aiM.color, alignSelf: "flex-start" }}>
                                          {aiM.label}
                                        </span>
                                      )}
                                    </div>
                                  </td>
                                );
                              })}
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </div>
              </div>
            );
          })}

          {Object.keys(viewMode === "flat" ? flatRowsBySubject : studentsBySubject).length === 0 && (
            <div style={{ backgroundColor: "#fff", borderRadius: "12px", boxShadow: "0 1px 2px rgba(0,0,0,.05)", border: "1px solid #e5e7eb", padding: "48px", textAlign: "center" }}>
              <p style={{ color: "#9ca3af", fontSize: "14px", margin: 0 }}>
                No records match the current filters.
              </p>
            </div>
          )}
        </div>
      )}

      {/* ── AI Summary Modal ── */}
      {showAiSummary && aiSummary && (
        <div
          style={{ position: "fixed", inset: 0, backgroundColor: "rgba(0,0,0,.5)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000, padding: "24px" }}
          onClick={() => setShowAiSummary(false)}>
          <div
            style={{ backgroundColor: "#fff", borderRadius: "12px", maxWidth: "800px", width: "100%", maxHeight: "80vh", overflow: "auto", padding: "24px", boxShadow: "0 10px 25px rgba(0,0,0,.2)" }}
            onClick={e => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
              <h2 style={{ margin: 0, fontSize: "18px", fontWeight: "700", color: "#1f2937" }}>
                AI Institutional Summary
              </h2>
              <button onClick={() => setShowAiSummary(false)} style={{ border: "none", backgroundColor: "transparent", cursor: "pointer" }}>
                <X size={20} />
              </button>
            </div>
            <div style={{ whiteSpace: "pre-wrap", fontSize: "14px", lineHeight: "1.7", color: "#374151" }}>
              {aiSummary}
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default CoordinatorAnalytics;
