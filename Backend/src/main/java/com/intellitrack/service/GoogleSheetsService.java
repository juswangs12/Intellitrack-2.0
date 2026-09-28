package com.intellitrack.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.intellitrack.config.google.GoogleCloudProperties;
import com.intellitrack.dto.coordinator.*;
import com.intellitrack.exception.GoogleSheetsExportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Production Google Workspace Service Layer.
 *
 * <p>Credential loading priority (no hard-coded paths, no source-code changes required):
 * <ol>
 *   <li>GOOGLE_APPLICATION_CREDENTIALS environment variable (file path)</li>
 *   <li>GOOGLE_SERVICE_ACCOUNT_JSON environment variable (base64 or raw JSON)</li>
 *   <li>google.cloud.service-account-json application property (base64 or raw JSON)</li>
 *   <li>classpath:google-service-account.json</li>
 * </ol>
 */
@Service
public class GoogleSheetsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);

    /** Number of fixed columns before per-deliverable columns in the Students sheet. */
    private static final int FIXED_STUDENT_COLUMNS = 11;

    private static final List<String> SCOPES = List.of(
            SheetsScopes.SPREADSHEETS,
            DriveScopes.DRIVE_FILE,
            DriveScopes.DRIVE);

    private static final DateTimeFormatter DISPLAY_DATETIME =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm:ss");
    private static final DateTimeFormatter SHORT_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GoogleCloudProperties properties;
    private final MetricsCalculationService metricsCalculationService;

    // Thread-safe lazy singletons (double-checked locking with volatile fields)
    private final Object initLock = new Object();
    private volatile GoogleCredentials cachedCredentials;
    private volatile Sheets sheetsService;
    private volatile Drive driveService;

    public GoogleSheetsService(GoogleCloudProperties properties,
                               MetricsCalculationService metricsCalculationService) {
        this.properties = properties;
        this.metricsCalculationService = metricsCalculationService;
    }

    // -------------------------------------------------------------------------
    //  Public API
    // -------------------------------------------------------------------------

    public boolean isConfigured() {
        try {
            return resolveCredentials() != null;
        } catch (Exception e) {
            logger.debug("Google Sheets integration not configured: {}", e.getMessage());
            return false;
        }
    }

    @Retryable(
            retryFor = {IOException.class, GoogleSheetsExportException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public String generateInstitutionalReport(String coordinatorEmail) {
        try {
            logger.info("Starting institutional report generation for coordinator: {}", coordinatorEmail);

            CoordinatorAnalyticsV2Dto analytics = metricsCalculationService.buildCoordinatorAnalyticsV2();
            String folderId      = findOrCreateDriveFolder();
            String spreadsheetId = findOrCreateSpreadsheet(folderId);
            shareWithUser(spreadsheetId, coordinatorEmail);
            populateAllSheets(spreadsheetId, analytics);

            String url = "https://docs.google.com/spreadsheets/d/" + spreadsheetId;
            logger.info("Institutional report generated successfully: {}", url);
            return url;

        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            logger.error("=== Google API Error Details ===");
            logger.error("Status Code: {}", e.getStatusCode());
            logger.error("Status: {}", e.getStatusMessage());
            logger.error("Error Details: {}", e.getDetails());
            logger.error("Full Response: {}", e.getContent());
            logger.error("=== End Google API Error Details ===");
            throw new GoogleSheetsExportException(
                    "Google API Error: " + e.getDetails() + " (Code " + e.getStatusCode() + ")", e);
        } catch (GoogleSheetsExportException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to generate institutional report", e);
            throw new GoogleSheetsExportException(
                    "Failed to generate institutional report: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    //  Credential & service resolution
    // -------------------------------------------------------------------------

    private GoogleCredentials resolveCredentials() throws IOException {
        if (cachedCredentials != null) return cachedCredentials;
        synchronized (initLock) {
            if (cachedCredentials != null) return cachedCredentials;

            InputStream credStream = null;
            String source = null;

            // Priority 1: GOOGLE_APPLICATION_CREDENTIALS (file path)
            String gacPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (gacPath != null && !gacPath.isBlank()) {
                java.io.File credFile = new java.io.File(gacPath);
                if (credFile.isFile()) {
                    credStream = new FileInputStream(credFile);
                    source = "GOOGLE_APPLICATION_CREDENTIALS: " + gacPath;
                    logger.info("Google credentials loaded from {}", source);
                } else {
                    logger.warn("GOOGLE_APPLICATION_CREDENTIALS path does not exist: {}", gacPath);
                }
            }

            // Priority 2: GOOGLE_SERVICE_ACCOUNT_JSON env var (base64 or raw JSON)
            if (credStream == null) {
                String jsonEnv = System.getenv("GOOGLE_SERVICE_ACCOUNT_JSON");
                if (jsonEnv != null && !jsonEnv.isBlank()) {
                    credStream = decodeCredentialString(jsonEnv, "GOOGLE_SERVICE_ACCOUNT_JSON env var");
                    source = "GOOGLE_SERVICE_ACCOUNT_JSON env var";
                }
            }

            // Priority 3: google.cloud.service-account-json property (base64 or raw JSON)
            if (credStream == null) {
                String jsonProp = properties.getServiceAccountJson();
                if (jsonProp != null && !jsonProp.isBlank()) {
                    credStream = decodeCredentialString(jsonProp, "google.cloud.service-account-json property");
                    source = "google.cloud.service-account-json property";
                }
            }

            // Priority 4: classpath:google-service-account.json
            if (credStream == null) {
                try {
                    ClassPathResource resource = new ClassPathResource("google-service-account.json");
                    if (resource.exists()) {
                        credStream = resource.getInputStream();
                        source = "classpath:google-service-account.json";
                        logger.info("Google credentials loaded from {}", source);
                    }
                } catch (Exception e) {
                    logger.debug("No classpath google-service-account.json found: {}", e.getMessage());
                }
            }

            if (credStream == null) {
                throw new IOException(
                        "Google Service Account credentials not found. Set GOOGLE_APPLICATION_CREDENTIALS "
                        + "(file path), GOOGLE_SERVICE_ACCOUNT_JSON (base64/JSON), "
                        + "google.cloud.service-account-json property, "
                        + "or place google-service-account.json on the classpath.");
            }

            try (InputStream stream = credStream) {
                cachedCredentials = GoogleCredentials.fromStream(stream).createScoped(SCOPES);
                logger.info("=== Google Credential Details ===");
                logger.info("Credential Source: {}", source);
                if (cachedCredentials instanceof com.google.auth.oauth2.ServiceAccountCredentials) {
                    com.google.auth.oauth2.ServiceAccountCredentials saCreds = 
                            (com.google.auth.oauth2.ServiceAccountCredentials) cachedCredentials;
                    logger.info("Credential Type: Service Account");
                    logger.info("Project ID: {}", saCreds.getProjectId());
                    logger.info("Client Email: {}", saCreds.getClientEmail());
                    logger.info("Client ID: {}", saCreds.getClientId());
                    logger.info("Scopes: {}", SCOPES);
                } else {
                    logger.info("Credential Type: {}", cachedCredentials.getClass().getSimpleName());
                }
                logger.info("=== End Credential Details ===");
            }
            return cachedCredentials;
        }
    }

    private InputStream decodeCredentialString(String value, String source) {
        try {
            byte[] decoded = Base64.getDecoder().decode(value.trim());
            logger.info("Google credentials loaded from {} (base64-encoded)", source);
            return new ByteArrayInputStream(decoded);
        } catch (IllegalArgumentException e) {
            logger.info("Google credentials loaded from {} (raw JSON)", source);
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        if (sheetsService == null) {
            synchronized (initLock) {
                if (sheetsService == null) {
                    GoogleCredentials credentials = resolveCredentials();
                    logger.info("Refreshing Google credentials for Sheets service...");
                    credentials.refreshIfExpired();
                    com.google.auth.oauth2.AccessToken token = credentials.getAccessToken();
                    if (token != null) {
                        logger.info("Access token obtained, expires at: {}", token.getExpirationTime());
                    } else {
                        logger.warn("No access token found after refresh!");
                    }
                    sheetsService = new Sheets.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            GsonFactory.getDefaultInstance(),
                            new HttpCredentialsAdapter(credentials))
                            .setApplicationName(properties.getApplicationName())
                            .build();
                    logger.info("Sheets service initialized successfully.");
                }
            }
        }
        return sheetsService;
    }

    private Drive getDriveService() throws IOException, GeneralSecurityException {
        if (driveService == null) {
            synchronized (initLock) {
                if (driveService == null) {
                    GoogleCredentials credentials = resolveCredentials();
                    logger.info("Refreshing Google credentials for Drive service...");
                    credentials.refreshIfExpired();
                    com.google.auth.oauth2.AccessToken token = credentials.getAccessToken();
                    if (token != null) {
                        logger.info("Access token obtained, expires at: {}", token.getExpirationTime());
                    } else {
                        logger.warn("No access token found after refresh!");
                    }
                    driveService = new Drive.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            GsonFactory.getDefaultInstance(),
                            new HttpCredentialsAdapter(credentials))
                            .setApplicationName(properties.getApplicationName())
                            .build();
                    logger.info("Drive service initialized successfully.");
                }
            }
        }
        return driveService;
    }

    // -------------------------------------------------------------------------
    //  Drive / Spreadsheet lifecycle
    // -------------------------------------------------------------------------

    private String findOrCreateDriveFolder() throws IOException, GeneralSecurityException {
        try {
            String folderName = properties.getDriveFolderName();
            String escaped = folderName.replace("'", "\\'");
            String query = String.format(
                    "mimeType='application/vnd.google-apps.folder' and name='%s' and trashed=false", escaped);

            logger.info("Searching for Drive folder: {}", folderName);
            FileList result = getDriveService().files().list()
                    .setQ(query).setSpaces("drive").setFields("files(id,name)").execute();

            if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                String folderId = result.getFiles().get(0).getId();
                logger.info("Found existing Drive folder '{}': {}", folderName, folderId);
                return folderId;
            }

            logger.info("Creating new Drive folder: {}", folderName);
            File meta = new File().setName(folderName).setMimeType("application/vnd.google-apps.folder");
            File created = getDriveService().files().create(meta).setFields("id").execute();
            logger.info("Created Drive folder '{}': {}", folderName, created.getId());
            return created.getId();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            logger.error("=== Google Drive API Error (findOrCreateDriveFolder) ===");
            logger.error("Status Code: {}", e.getStatusCode());
            logger.error("Status Message: {}", e.getStatusMessage());
            logger.error("Error Details: {}", e.getDetails());
            logger.error("Full Response: {}", e.getContent());
            logger.error("===================================================");
            throw new GoogleSheetsExportException("Google Drive API error: " + e.getMessage(), e);
        }
    }

    private String findOrCreateSpreadsheet(String folderId) throws IOException, GeneralSecurityException {
        try {
            String name = properties.getSpreadsheetName();
            String escaped = name.replace("'", "\\'");
            String query = String.format(
                    "mimeType='application/vnd.google-apps.spreadsheet' and name='%s' and trashed=false", escaped);

            logger.info("Searching for existing spreadsheet: {}", name);
            FileList result = getDriveService().files().list()
                    .setQ(query).setSpaces("drive").setFields("files(id,name)").execute();

            if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                String existingId = result.getFiles().get(0).getId();
                logger.info("Reusing existing spreadsheet '{}': {}", name, existingId);
                return existingId;
            }

            logger.info("Creating new spreadsheet: {}", name);
            Spreadsheet spreadsheet = new Spreadsheet()
                    .setProperties(new SpreadsheetProperties().setTitle(name))
                    .setSheets(buildRequiredTabs());
            Spreadsheet created = getSheetsService().spreadsheets().create(spreadsheet).execute();
            String spreadsheetId = created.getSpreadsheetId();
            logger.info("Created new spreadsheet '{}': {}", name, spreadsheetId);

            logger.info("Moving spreadsheet {} into folder {}", spreadsheetId, folderId);
            getDriveService().files().update(spreadsheetId, new File())
                    .setAddParents(folderId).setRemoveParents("root").setFields("id,parents").execute();
            logger.info("Moved spreadsheet {} into Drive folder {}", spreadsheetId, folderId);

            return spreadsheetId;
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            logger.error("=== Google API Error (findOrCreateSpreadsheet) ===");
            logger.error("Status Code: {}", e.getStatusCode());
            logger.error("Status Message: {}", e.getStatusMessage());
            logger.error("Error Details: {}", e.getDetails());
            logger.error("Full Response: {}", e.getContent());
            logger.error("==================================================");
            throw new GoogleSheetsExportException("Google API error: " + e.getMessage(), e);
        }
    }

    private List<Sheet> buildRequiredTabs() {
        String[] tabs = {
                "Overview", "Students", "Groups", "Sections", "Subjects",
                "Advisers", "Deadlines", "Late Submissions", "Risk Assessment", "AI Summary",
                "Flat Analytics"
        };
        List<Sheet> sheets = new ArrayList<>();
        for (int i = 0; i < tabs.length; i++) {
            sheets.add(new Sheet().setProperties(new SheetProperties().setTitle(tabs[i]).setIndex(i)));
        }
        return sheets;
    }

    private void shareWithUser(String fileId, String email) throws IOException, GeneralSecurityException {
        try {
            if (email == null || email.isBlank()) {
                logger.warn("Coordinator email is blank - skipping share step");
                return;
            }
            logger.info("Sharing file {} with user {}", fileId, email);
            Permission permission = new Permission().setType("user").setRole("writer").setEmailAddress(email);
            getDriveService().permissions().create(fileId, permission).setSendNotificationEmail(false).execute();
            logger.info("Shared spreadsheet {} with {}", fileId, email);
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            logger.error("=== Google Drive API Error (shareWithUser) ===");
            logger.error("Status Code: {}", e.getStatusCode());
            logger.error("Status Message: {}", e.getStatusMessage());
            logger.error("Error Details: {}", e.getDetails());
            logger.error("Full Response: {}", e.getContent());
            logger.error("=================================================");
            throw new GoogleSheetsExportException("Failed to share spreadsheet: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    //  Sheet population orchestration
    // -------------------------------------------------------------------------

    private void populateAllSheets(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {
        populateOverviewSheet(spreadsheetId, analytics);
        populateStudentsSheet(spreadsheetId, analytics);
        populateGroupsSheet(spreadsheetId, analytics);
        populateSectionsSheet(spreadsheetId, analytics);
        populateSubjectsSheet(spreadsheetId, analytics);
        populateAdvisersSheet(spreadsheetId, analytics);
        populateDeadlinesSheet(spreadsheetId, analytics);
        populateLateSubmissionsSheet(spreadsheetId, analytics);
        populateRiskAssessmentSheet(spreadsheetId, analytics);
        populateAiSummarySheet(spreadsheetId, analytics);
        populateFlatAnalyticsSheet(spreadsheetId, analytics);
    }

    private void populateFlatAnalyticsSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {
        if (analytics.students() == null || analytics.students().isEmpty()) {
            clearAndWrite(spreadsheetId, "Flat Analytics", List.of(List.of("No data available")));
            return;
        }

        Map<Long, com.intellitrack.dto.coordinator.DeliverableMetaDto> deliverableMap =
            analytics.deliverables() == null ? Map.of() :
            analytics.deliverables().stream()
                .collect(java.util.stream.Collectors.toMap(
                    com.intellitrack.dto.coordinator.DeliverableMetaDto::id, d -> d));

        List<List<Object>> values = new java.util.ArrayList<>();
        values.add(List.of(
            "Student Name", "Student ID", "Email", "Linked Status", "Subject", "Section",
            "Group", "Adviser", "Deliverable", "Deadline", "Submission Status", "Submitted Date",
            "Days Late", "Completion %", "AI Status", "Risk Level", "Remarks"
        ));

        for (var student : analytics.students()) {
            if (student.deliverableStatuses() == null) continue;
            var riskLevel = calculateStudentRiskLevel(student.completionRate(), student.lateCount(), student.missingCount());
            var studentEmail = getStudentEmail(student);
            
            for (var cell : student.deliverableStatuses()) {
                if ("NOT_YET_ASSIGNED".equals(cell.status())) continue;
                
                var deliverable = deliverableMap.get(cell.deliverableId());
                var deadline = deliverable != null && deliverable.dueAt() != null ?
                    deliverable.dueAt().format(SHORT_DATETIME) : "";
                
                String remarks = generateRemarks(cell.status(), cell.lateDays());
                
                values.add(List.of(
                    student.fullName(),
                    nvl(student.studentId()),
                    studentEmail,
                    student.accountStatus(),
                    nvl(student.subject()),
                    nvl(student.section()),
                    student.groupName() != null ? student.groupName() : "Unassigned",
                    nvl(student.adviserName()),
                    deliverable != null ? deliverable.name() : "",
                    deadline,
                    cell.displayText() != null ? cell.displayText() : statusLabel(cell.status()),
                    cell.submittedAt() != null ? cell.submittedAt().format(SHORT_DATETIME) : "",
                    cell.lateDays() != null ? cell.lateDays() : "",
                    student.completionRate() != null ? String.format("%.1f%%", student.completionRate()) : "0%",
                    nvl(cell.aiStatus()),
                    riskLevel,
                    remarks
                ));
            }
        }

        clearAndWrite(spreadsheetId, "Flat Analytics", values);
        applyFlatAnalyticsSheetFormatting(spreadsheetId, analytics, deliverableMap);
    }

    private String calculateStudentRiskLevel(Double completionRate, Long lateCount, Long missingCount) {
        double rate = completionRate != null ? completionRate : 0.0;
        long missing = missingCount != null ? missingCount : 0;
        if (rate < 50 || missing > 3) return "HIGH";
        if (rate < 75 || (lateCount != null && lateCount > 1)) return "MEDIUM";
        return "LOW";
    }

    private String getStudentEmail(com.intellitrack.dto.coordinator.StudentAnalyticsRowDto student) {
        // Since we don't have email in the DTO currently, leave blank for now
        return "";
    }

    private String generateRemarks(String status, Integer lateDays) {
        if (status == null) return "-";
        return switch (status) {
            case "SUBMITTED_ON_TIME" -> "Submitted on time";
            case "SUBMITTED_LATE" -> "Late by " + (lateDays != null ? lateDays : 0) + " days";
            case "DID_NOT_SUBMIT" -> "Missing submission";
            case "DUE_TODAY" -> "Due today";
            case "UPCOMING" -> "Pending deadline";
            default -> "-";
        };
    }

    private void applyFlatAnalyticsSheetFormatting(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics,
            Map<Long, com.intellitrack.dto.coordinator.DeliverableMetaDto> deliverableMap)
            throws IOException, GeneralSecurityException {
        int sheetId = resolveSheetId(spreadsheetId, "Flat Analytics");
        if (sheetId < 0) return;

        List<Request> requests = new java.util.ArrayList<>();
        
        // Freeze first row
        requests.add(new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
            .setProperties(new SheetProperties().setSheetId(sheetId).setGridProperties(new GridProperties().setFrozenRowCount(1)))
            .setFields("gridProperties.frozenRowCount")));
        
        // Bold header
        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
            .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(0).setEndRowIndex(1))
            .setCell(new CellData().setUserEnteredFormat(new CellFormat().setTextFormat(new TextFormat().setBold(true))))
            .setFields("userEnteredFormat.textFormat.bold")));
        
        // Auto resize columns
        requests.add(new Request().setAutoResizeDimensions(new AutoResizeDimensionsRequest()
            .setDimensions(new DimensionRange().setSheetId(sheetId).setDimension("COLUMNS").setStartIndex(0).setEndIndex(17))));
        
        // Alternating row colors and status colors
        if (analytics.students() != null) {
            int rowIndex = 1; // Skip header
            for (var student : analytics.students()) {
                if (student.deliverableStatuses() == null) continue;
                var riskLevel = calculateStudentRiskLevel(student.completionRate(), student.lateCount(), student.missingCount());
                Color riskColor = riskLevelToColor(riskLevel);
                
                for (var cell : student.deliverableStatuses()) {
                    if ("NOT_YET_ASSIGNED".equals(cell.status())) continue;
                    
                    Color rowColor = rowIndex % 2 == 0 ? rgb(1.0f, 1.0f, 1.0f) : rgb(0.98f, 0.98f, 0.98f);
                    // Apply row background
                    requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                        .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(rowIndex).setEndRowIndex(rowIndex+1))
                        .setCell(new CellData().setUserEnteredFormat(new CellFormat().setBackgroundColor(rowColor)))
                        .setFields("userEnteredFormat.backgroundColor")));
                    
                    // Apply status color for submission status column
                    Color statusColor = statusToColor(cell.status());
                    if (statusColor != null) {
                        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                            .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(rowIndex).setEndRowIndex(rowIndex+1).setStartColumnIndex(10).setEndColumnIndex(11))
                            .setCell(new CellData().setUserEnteredFormat(new CellFormat().setBackgroundColor(statusColor)))
                            .setFields("userEnteredFormat.backgroundColor")));
                    }
                    
                    // Apply risk color
                    if (riskColor != null) {
                        requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                            .setRange(new GridRange().setSheetId(sheetId).setStartRowIndex(rowIndex).setEndRowIndex(rowIndex+1).setStartColumnIndex(15).setEndColumnIndex(16))
                            .setCell(new CellData().setUserEnteredFormat(new CellFormat().setBackgroundColor(riskColor)))
                            .setFields("userEnteredFormat.backgroundColor")));
                    }
                    
                    rowIndex++;
                }
            }
        }
        
        if (!requests.isEmpty()) {
            try {
                logger.info("Applying formatting to Flat Analytics sheet with {} requests", requests.size());
                getSheetsService().spreadsheets().batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests)).execute();
                logger.info("Successfully applied formatting to Flat Analytics sheet");
            } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                logger.error("=== Google Sheets API Error (applyFlatAnalyticsSheetFormatting) ===");
                logger.error("Status Code: {}", e.getStatusCode());
                logger.error("Status Message: {}", e.getStatusMessage());
                logger.error("Error Details: {}", e.getDetails());
                logger.error("Full Response: {}", e.getContent());
                logger.error("===============================================================");
                // Don't fail the whole export just for formatting
                logger.warn("Failed to apply formatting to Flat Analytics sheet, but continuing with export", e);
            }
        }
    }

    private Color riskLevelToColor(String riskLevel) {
        if (riskLevel == null) return null;
        return switch (riskLevel) {
            case "HIGH" -> rgb(1.0f, 0.8f, 0.8f);
            case "MEDIUM" -> rgb(1.0f, 0.95f, 0.8f);
            case "LOW" -> rgb(0.8f, 1.0f, 0.8f);
            default -> null;
        };
    }

    // -------------------------------------------------------------------------
    //  Individual sheet writers
    // -------------------------------------------------------------------------

    private void populateOverviewSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        long totalOnTime  = 0, totalLate = 0, totalMissing = 0;
        double avgCompletion = 0;
        if (analytics.students() != null && !analytics.students().isEmpty()) {
            avgCompletion = analytics.students().stream()
                    .mapToDouble(s -> s.completionRate() != null ? s.completionRate() : 0.0)
                    .average().orElse(0.0);
            totalOnTime  = analytics.students().stream().mapToLong(StudentAnalyticsRowDto::onTimeCount).sum();
            totalLate    = analytics.students().stream().mapToLong(StudentAnalyticsRowDto::lateCount).sum();
            totalMissing = analytics.students().stream().mapToLong(StudentAnalyticsRowDto::missingCount).sum();
        }

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("IntelliTrack Institutional Analytics Report"));
        values.add(List.of(""));
        values.add(List.of("Report Generated",          LocalDateTime.now().format(DISPLAY_DATETIME)));
        values.add(List.of("System",                    "IntelliTrack 2.0"));
        values.add(List.of(""));
        values.add(List.of("INSTITUTIONAL SUMMARY"));
        values.add(List.of("Total Students",            safeSize(analytics.students())));
        values.add(List.of("Total Groups",              safeSize(analytics.groups())));
        values.add(List.of("Total Advisers",            safeSize(analytics.advisers())));
        values.add(List.of("Total Subjects",            safeSize(analytics.subjects())));
        values.add(List.of("Total Deliverables",        safeSize(analytics.deliverables())));
        values.add(List.of("Average Completion Rate",   String.format("%.1f%%", avgCompletion)));
        values.add(List.of("Total On-Time Submissions", totalOnTime));
        values.add(List.of("Total Late Submissions",    totalLate));
        values.add(List.of("Total Missing Submissions", totalMissing));

        clearAndWrite(spreadsheetId, "Overview", values);
    }

    private void populateStudentsSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        List<List<Object>> values = new ArrayList<>();

        List<Object> headers = new ArrayList<>(Arrays.asList(
                "Student Name", "Student ID", "Subject", "Section", "Group",
                "Adviser", "Account Status", "Completion Rate", "On Time", "Late", "Missing"));
        if (analytics.deliverables() != null) {
            for (DeliverableMetaDto d : analytics.deliverables()) headers.add(d.name());
        }
        values.add(headers);

        if (analytics.students() != null) {
            for (StudentAnalyticsRowDto s : analytics.students()) {
                List<Object> row = new ArrayList<>();
                row.add(s.fullName());
                row.add(nvl(s.studentId()));
                row.add(nvl(s.subject()));
                row.add(nvl(s.section()));
                row.add(s.groupName() != null ? s.groupName() : "Unassigned");
                row.add(nvl(s.adviserName()));
                row.add(nvl(s.accountStatus()));
                row.add(s.completionRate() != null ? String.format("%.1f%%", s.completionRate()) : "0%");
                row.add(s.onTimeCount());
                row.add(s.lateCount());
                row.add(s.missingCount());
                if (s.deliverableStatuses() != null) {
                    for (StudentDeliverableCellDto cell : s.deliverableStatuses()) {
                        String display = cell.displayText() != null ? cell.displayText() : statusLabel(cell.status());
                        // Append AI status when submission exists and AI has been run
                        if (cell.aiStatus() != null && !"PENDING".equals(cell.aiStatus())) {
                            String aiLabel = switch (cell.aiStatus()) {
                                case "COMPLETED"    -> "AI: Analyzed";
                                case "PROCESSING"   -> "AI: Processing";
                                case "FAILED",
                                     "RATE_LIMITED" -> "AI: Failed";
                                default             -> null;
                            };
                            if (aiLabel != null) display = display + " \u00b7 " + aiLabel;
                        }
                        row.add(display);
                    }
                }
                values.add(row);
            }
        }

        clearAndWrite(spreadsheetId, "Students", values);
        applyStudentColorFormatting(spreadsheetId, analytics);
    }

    private void populateGroupsSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        Map<Long, String> adviserNames = new HashMap<>();
        if (analytics.advisers() != null) {
            for (AdviserMetaDto a : analytics.advisers()) adviserNames.put(a.id(), a.name());
        }
        Map<Long, String> subjectLabels = new HashMap<>();
        if (analytics.subjects() != null) {
            for (SubjectMetaDto sub : analytics.subjects()) {
                subjectLabels.put(sub.id(), sub.code() + " - " + sub.name());
            }
        }
        Map<String, List<StudentAnalyticsRowDto>> studentsByGroup = new HashMap<>();
        if (analytics.students() != null) {
            for (StudentAnalyticsRowDto s : analytics.students()) {
                String key = s.groupName() != null ? s.groupName() : "Unassigned";
                studentsByGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }
        }

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("Group Name", "Group Code", "Adviser", "Subject",
                "Members", "Completion %", "On-Time", "Late", "Missing", "Risk Level"));

        if (analytics.groups() != null) {
            for (GroupMetaDto g : analytics.groups()) {
                List<StudentAnalyticsRowDto> members = studentsByGroup.getOrDefault(g.title(), List.of());
                int memberCount = members.size();
                double avgCompletion = members.stream()
                        .mapToDouble(s -> s.completionRate() != null ? s.completionRate() : 0.0)
                        .average().orElse(0.0);
                long onTime  = members.stream().mapToLong(StudentAnalyticsRowDto::onTimeCount).sum();
                long late    = members.stream().mapToLong(StudentAnalyticsRowDto::lateCount).sum();
                long missing = members.stream().mapToLong(StudentAnalyticsRowDto::missingCount).sum();

                String risk;
                if (avgCompletion < 50 || missing > (long) memberCount * 2) risk = "HIGH";
                else if (avgCompletion < 75 || late > memberCount) risk = "MEDIUM";
                else risk = "LOW";

                values.add(List.of(
                        g.title(), nvl(g.code()),
                        g.adviserId() != null ? adviserNames.getOrDefault(g.adviserId(), "ID:" + g.adviserId()) : "Unassigned",
                        g.subjectId() != null ? subjectLabels.getOrDefault(g.subjectId(), "ID:" + g.subjectId()) : "",
                        memberCount, String.format("%.1f%%", avgCompletion),
                        onTime, late, missing, risk));
            }
        }

        clearAndWrite(spreadsheetId, "Groups", values);
    }

    private void populateSectionsSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        // Build subject label lookup
        Map<Long, String> subjectLabels = new HashMap<>();
        if (analytics.subjects() != null) {
            for (SubjectMetaDto sub : analytics.subjects()) {
                subjectLabels.put(sub.id(), sub.code() + " - " + sub.name());
            }
        }

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("Section", "Subject", "Total Students", "Total Groups",
                "Avg Completion %", "On-Time", "Late", "Missing", "Risk Level"));

        if (analytics.sections() != null) {
            for (SectionMetaDto sec : analytics.sections()) {
                // Students in this section
                List<StudentAnalyticsRowDto> secStudents = analytics.students() == null ? List.of() :
                        analytics.students().stream()
                                .filter(s -> sec.section().equals(s.section()))
                                .collect(Collectors.toList());

                // Groups cannot be directly counted from SectionMetaDto alone;
                // count distinct group names from section students
                long groupCount = secStudents.stream()
                        .map(StudentAnalyticsRowDto::groupName)
                        .filter(g -> g != null && !"Unassigned".equals(g))
                        .distinct()
                        .count();

                double avgCompletion = secStudents.stream()
                        .mapToDouble(s -> s.completionRate() != null ? s.completionRate() : 0.0)
                        .average().orElse(0.0);
                long onTime  = secStudents.stream().mapToLong(StudentAnalyticsRowDto::onTimeCount).sum();
                long late    = secStudents.stream().mapToLong(StudentAnalyticsRowDto::lateCount).sum();
                long missing = secStudents.stream().mapToLong(StudentAnalyticsRowDto::missingCount).sum();

                String risk;
                if (avgCompletion < 50 || (secStudents.size() > 0 && missing > (long) secStudents.size() * 2))
                    risk = "HIGH";
                else if (avgCompletion < 75 || late > secStudents.size())
                    risk = "MEDIUM";
                else
                    risk = "LOW";

                values.add(List.of(
                        nvl(sec.section()),
                        sec.subjectId() != null ? subjectLabels.getOrDefault(sec.subjectId(), "ID:" + sec.subjectId()) : "",
                        secStudents.size(), groupCount,
                        String.format("%.1f%%", avgCompletion),
                        onTime, late, missing, risk));
            }
        }

        clearAndWrite(spreadsheetId, "Sections", values);
    }

    private void populateSubjectsSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("Subject Code", "Subject Name", "Total Students", "Total Groups",
                "Avg Completion %", "Total On-Time", "Total Late", "Total Missing"));

        if (analytics.subjects() != null) {
            for (SubjectMetaDto sub : analytics.subjects()) {
                List<StudentAnalyticsRowDto> subStudents = analytics.students() == null ? List.of() :
                        analytics.students().stream()
                                .filter(s -> sub.code().equals(s.subject()) || sub.name().equals(s.subject()))
                                .collect(Collectors.toList());
                long groupCount = analytics.groups() == null ? 0 :
                        analytics.groups().stream().filter(g -> sub.id().equals(g.subjectId())).count();
                double avgCompletion = subStudents.stream()
                        .mapToDouble(s -> s.completionRate() != null ? s.completionRate() : 0.0)
                        .average().orElse(0.0);
                long onTime  = subStudents.stream().mapToLong(StudentAnalyticsRowDto::onTimeCount).sum();
                long late    = subStudents.stream().mapToLong(StudentAnalyticsRowDto::lateCount).sum();
                long missing = subStudents.stream().mapToLong(StudentAnalyticsRowDto::missingCount).sum();

                values.add(List.of(nvl(sub.code()), nvl(sub.name()),
                        subStudents.size(), groupCount,
                        String.format("%.1f%%", avgCompletion), onTime, late, missing));
            }
        }

        clearAndWrite(spreadsheetId, "Subjects", values);
    }

    private void populateAdvisersSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("Adviser Name", "Groups", "Students",
                "Avg Completion %", "Total On-Time", "Total Late", "Total Missing", "Risk Level"));

        if (analytics.advisers() != null) {
            Map<String, List<StudentAnalyticsRowDto>> studentsByAdviser =
                    analytics.students() == null ? Map.of() :
                    analytics.students().stream()
                            .filter(s -> s.adviserName() != null)
                            .collect(Collectors.groupingBy(StudentAnalyticsRowDto::adviserName));
            Map<Long, Long> groupsByAdviser =
                    analytics.groups() == null ? Map.of() :
                    analytics.groups().stream()
                            .filter(g -> g.adviserId() != null)
                            .collect(Collectors.groupingBy(GroupMetaDto::adviserId, Collectors.counting()));

            for (AdviserMetaDto adviser : analytics.advisers()) {
                List<StudentAnalyticsRowDto> advStudents =
                        studentsByAdviser.getOrDefault(adviser.name(), List.of());
                long groupCount = groupsByAdviser.getOrDefault(adviser.id(), 0L);
                double avgCompletion = advStudents.stream()
                        .mapToDouble(s -> s.completionRate() != null ? s.completionRate() : 0.0)
                        .average().orElse(0.0);
                long onTime  = advStudents.stream().mapToLong(StudentAnalyticsRowDto::onTimeCount).sum();
                long late    = advStudents.stream().mapToLong(StudentAnalyticsRowDto::lateCount).sum();
                long missing = advStudents.stream().mapToLong(StudentAnalyticsRowDto::missingCount).sum();
                String risk  = avgCompletion < 50 ? "HIGH" : avgCompletion < 75 ? "MEDIUM" : "LOW";

                values.add(List.of(adviser.name(), groupCount, advStudents.size(),
                        String.format("%.1f%%", avgCompletion), onTime, late, missing, risk));
            }
        }

        clearAndWrite(spreadsheetId, "Advisers", values);
    }

    private void populateDeadlinesSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("Deliverable Name", "Stage", "Due Date", "Status",
                "On-Time", "Late", "Not Submitted"));

        if (analytics.deliverables() != null) {
            for (DeliverableMetaDto d : analytics.deliverables()) {
                long onTime = 0, late = 0, missing = 0;
                if (analytics.students() != null) {
                    for (StudentAnalyticsRowDto s : analytics.students()) {
                        if (s.deliverableStatuses() == null) continue;
                        for (StudentDeliverableCellDto cell : s.deliverableStatuses()) {
                            if (!d.id().equals(cell.deliverableId())) continue;
                            switch (nvl(cell.status())) {
                                case "SUBMITTED_ON_TIME" -> onTime++;
                                case "SUBMITTED_LATE"    -> late++;
                                case "NOT_SUBMITTED",
                                     "DID_NOT_SUBMIT"   -> missing++;
                            }
                        }
                    }
                }
                String deadlineStatus = (d.dueAt() != null && LocalDateTime.now().isAfter(d.dueAt()))
                        ? "Past Due" : "Upcoming";
                values.add(List.of(d.name(), nvl(d.stage()),
                        d.dueAt() != null ? d.dueAt().format(SHORT_DATETIME) : "",
                        deadlineStatus, onTime, late, missing));
            }
        }

        clearAndWrite(spreadsheetId, "Deadlines", values);
    }

    private void populateLateSubmissionsSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("Student Name", "Student ID", "Group", "Deliverable",
                "Submitted At", "Days Late"));

        if (analytics.students() != null && analytics.deliverables() != null) {
            Map<Long, String> delivNames = analytics.deliverables().stream()
                    .collect(Collectors.toMap(DeliverableMetaDto::id, DeliverableMetaDto::name));

            for (StudentAnalyticsRowDto s : analytics.students()) {
                if (s.deliverableStatuses() == null) continue;
                for (StudentDeliverableCellDto cell : s.deliverableStatuses()) {
                    if (!"SUBMITTED_LATE".equals(cell.status())) continue;
                    values.add(List.of(
                            s.fullName(), nvl(s.studentId()),
                            s.groupName() != null ? s.groupName() : "Unassigned",
                            delivNames.getOrDefault(cell.deliverableId(), String.valueOf(cell.deliverableId())),
                            cell.submittedAt() != null ? cell.submittedAt().format(SHORT_DATETIME) : "",
                            cell.lateDays() != null ? cell.lateDays() : 0));
                }
            }
        }

        clearAndWrite(spreadsheetId, "Late Submissions", values);
    }

    private void populateRiskAssessmentSheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("Student Name", "Student ID", "Group", "Adviser",
                "Completion %", "On-Time", "Late", "Missing", "Risk Level", "Risk Reason"));

        if (analytics.students() != null) {
            for (StudentAnalyticsRowDto s : analytics.students()) {
                double completion = s.completionRate() != null ? s.completionRate() : 0.0;
                long missing = s.missingCount();
                long late    = s.lateCount();

                String risk, reason;
                if (completion < 50 || missing > 3) {
                    risk   = "HIGH";
                    reason = completion < 50 ? "Completion below 50%" : "More than 3 missing deliverables";
                } else if (completion < 75 || missing > 1 || late > 3) {
                    risk   = "MEDIUM";
                    reason = "Moderate compliance issues detected";
                } else {
                    risk   = "LOW";
                    reason = "On track";
                }

                values.add(List.of(
                        s.fullName(), nvl(s.studentId()),
                        s.groupName() != null ? s.groupName() : "Unassigned",
                        nvl(s.adviserName()),
                        String.format("%.1f%%", completion),
                        s.onTimeCount(), late, missing, risk, reason));
            }
        }

        clearAndWrite(spreadsheetId, "Risk Assessment", values);
    }

    private void populateAiSummarySheet(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        long totalLate    = analytics.students() == null ? 0 :
                analytics.students().stream().mapToLong(StudentAnalyticsRowDto::lateCount).sum();
        long totalMissing = analytics.students() == null ? 0 :
                analytics.students().stream().mapToLong(StudentAnalyticsRowDto::missingCount).sum();
        double avgCompletion = (analytics.students() == null || analytics.students().isEmpty()) ? 0.0 :
                analytics.students().stream()
                        .mapToDouble(s -> s.completionRate() != null ? s.completionRate() : 0.0)
                        .average().orElse(0.0);

        List<List<Object>> values = new ArrayList<>();
        values.add(List.of("IntelliTrack AI Institutional Summary"));
        values.add(List.of(""));
        values.add(List.of("Generated",   LocalDateTime.now().format(DISPLAY_DATETIME)));
        values.add(List.of("Powered By",  "Google Gemini"));
        values.add(List.of(""));
        values.add(List.of("Institution Snapshot"));
        values.add(List.of("Total Students",        safeSize(analytics.students())));
        values.add(List.of("Total Groups",          safeSize(analytics.groups())));
        values.add(List.of("Average Completion",    String.format("%.1f%%", avgCompletion)));
        values.add(List.of("Total Late",            totalLate));
        values.add(List.of("Total Missing",         totalMissing));
        values.add(List.of(""));
        values.add(List.of("AI-Generated Analysis (Google Gemini)"));
        values.add(List.of(""));

        try {
            String aiSummary = metricsCalculationService.generateCoordinatorAiSummary();
            if (aiSummary != null && !aiSummary.isBlank()) {
                for (String line : aiSummary.split("\n")) {
                    if (!line.isBlank()) values.add(List.of(line.trim()));
                }
            } else {
                values.add(List.of("AI summary not available. Configure AI_API_KEY to enable AI features."));
            }
        } catch (Exception e) {
            logger.error("Error generating AI summary for export", e);
            values.add(List.of("AI summary generation failed: " + e.getMessage()));
        }

        clearAndWrite(spreadsheetId, "AI Summary", values);
    }

    // -------------------------------------------------------------------------
    //  Color formatting via BatchUpdate
    // -------------------------------------------------------------------------

    private void applyStudentColorFormatting(String spreadsheetId, CoordinatorAnalyticsV2Dto analytics)
            throws IOException, GeneralSecurityException {

        if (analytics.students() == null || analytics.students().isEmpty()) return;
        if (analytics.deliverables() == null || analytics.deliverables().isEmpty()) return;

        int sheetId = resolveSheetId(spreadsheetId, "Students");
        if (sheetId < 0) {
            logger.warn("Students sheet not found for color formatting - skipping");
            return;
        }

        List<Request> requests = new ArrayList<>();
        int deliverableCount = analytics.deliverables().size();

        for (int rowIdx = 0; rowIdx < analytics.students().size(); rowIdx++) {
            StudentAnalyticsRowDto student = analytics.students().get(rowIdx);
            if (student.deliverableStatuses() == null) continue;

            int sheetRow = rowIdx + 1; // row 0 is the header

            for (int colIdx = 0; colIdx < Math.min(student.deliverableStatuses().size(), deliverableCount); colIdx++) {
                StudentDeliverableCellDto cell = student.deliverableStatuses().get(colIdx);
                Color bgColor = statusToColor(cell.status());
                if (bgColor == null) continue;

                int sheetCol = FIXED_STUDENT_COLUMNS + colIdx;
                requests.add(new Request().setRepeatCell(new RepeatCellRequest()
                        .setRange(new GridRange()
                                .setSheetId(sheetId)
                                .setStartRowIndex(sheetRow).setEndRowIndex(sheetRow + 1)
                                .setStartColumnIndex(sheetCol).setEndColumnIndex(sheetCol + 1))
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat().setBackgroundColor(bgColor)))
                        .setFields("userEnteredFormat.backgroundColor")));
            }
        }

        if (!requests.isEmpty()) {
            try {
                logger.info("Applying color formatting to {} cells in Students sheet", requests.size());
                getSheetsService().spreadsheets()
                        .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests))
                        .execute();
                logger.info("Successfully applied color formatting to Students sheet");
            } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
                logger.error("=== Google Sheets API Error (applyStudentColorFormatting) ===");
                logger.error("Status Code: {}", e.getStatusCode());
                logger.error("Status Message: {}", e.getStatusMessage());
                logger.error("Error Details: {}", e.getDetails());
                logger.error("Full Response: {}", e.getContent());
                logger.error("========================================================");
                // Don't fail the whole export just for color formatting
                logger.warn("Failed to apply color formatting, but continuing with export", e);
            }
        }
    }

    private int resolveSheetId(String spreadsheetId, String sheetName)
            throws IOException, GeneralSecurityException {
        try {
            logger.info("Resolving sheet ID for: {} in spreadsheet: {}", sheetName, spreadsheetId);
            Spreadsheet spreadsheet = getSheetsService().spreadsheets()
                    .get(spreadsheetId).setFields("sheets.properties").execute();
            if (spreadsheet.getSheets() == null) return -1;
            int sheetId = spreadsheet.getSheets().stream()
                    .filter(s -> sheetName.equals(s.getProperties().getTitle()))
                    .mapToInt(s -> s.getProperties().getSheetId())
                    .findFirst().orElse(-1);
            logger.info("Resolved sheet ID for {}: {}", sheetName, sheetId);
            return sheetId;
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            logger.error("=== Google Sheets API Error (resolveSheetId) ===");
            logger.error("Sheet Name: {}", sheetName);
            logger.error("Status Code: {}", e.getStatusCode());
            logger.error("Status Message: {}", e.getStatusMessage());
            logger.error("Error Details: {}", e.getDetails());
            logger.error("Full Response: {}", e.getContent());
            logger.error("==================================================");
            throw new GoogleSheetsExportException("Failed to resolve sheet ID for " + sheetName + ": " + e.getMessage(), e);
        }
    }

    private Color statusToColor(String status) {
        if (status == null) return null;
        return switch (status) {
            case "SUBMITTED_ON_TIME" -> rgb(0.78f, 0.93f, 0.78f); // pastel green
            case "APPROVED"          -> rgb(0.56f, 0.88f, 0.56f); // green
            case "SUBMITTED_LATE"    -> rgb(1.00f, 0.93f, 0.60f); // amber
            case "NOT_SUBMITTED",
                 "DID_NOT_SUBMIT"   -> rgb(0.96f, 0.78f, 0.78f); // pastel red
            case "REJECTED"         -> rgb(0.93f, 0.56f, 0.56f); // red
            case "PENDING"          -> rgb(0.78f, 0.88f, 1.00f); // light blue
            case "NEEDS_REVISION"   -> rgb(0.90f, 0.78f, 1.00f); // light purple
            case "UPCOMING"         -> rgb(0.95f, 0.95f, 0.95f); // light gray
            default                 -> null;
        };
    }

    private Color rgb(float r, float g, float b) {
        return new Color().setRed(r).setGreen(g).setBlue(b);
    }

    // -------------------------------------------------------------------------
    //  Utilities
    // -------------------------------------------------------------------------

    private void clearAndWrite(String spreadsheetId, String sheetName, List<List<Object>> values)
            throws IOException, GeneralSecurityException {
        try {
            logger.info("Clearing and writing to sheet: {} in spreadsheet: {}", sheetName, spreadsheetId);
            getSheetsService().spreadsheets().values()
                    .clear(spreadsheetId, sheetName + "!A1:ZZ", new ClearValuesRequest())
                    .execute();
            getSheetsService().spreadsheets().values()
                    .update(spreadsheetId, sheetName + "!A1", new ValueRange().setValues(values))
                    .setValueInputOption("RAW")
                    .execute();
            logger.info("Successfully wrote data to sheet: {}", sheetName);
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            logger.error("=== Google Sheets API Error (clearAndWrite) ===");
            logger.error("Sheet Name: {}", sheetName);
            logger.error("Status Code: {}", e.getStatusCode());
            logger.error("Status Message: {}", e.getStatusMessage());
            logger.error("Error Details: {}", e.getDetails());
            logger.error("Full Response: {}", e.getContent());
            logger.error("==================================================");
            throw new GoogleSheetsExportException("Failed to write to sheet " + sheetName + ": " + e.getMessage(), e);
        }
    }

    private String statusLabel(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "SUBMITTED_ON_TIME" -> "Submitted On Time";
            case "SUBMITTED_LATE"    -> "Late";
            case "NOT_SUBMITTED",
                 "DID_NOT_SUBMIT"   -> "Not Submitted";
            case "PENDING"           -> "Waiting Review";
            case "NEEDS_REVISION"    -> "Needs Revision";
            case "APPROVED"          -> "Approved";
            case "REJECTED"          -> "Rejected";
            case "UPCOMING"          -> "Upcoming";
            default                  -> status;
        };
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
