package com.intellitrack.service;

import com.intellitrack.dto.ClasslistImportResultDto;
import com.intellitrack.dto.ClasslistImportRowDto;
import com.intellitrack.dto.ManualEnrollmentRequest;
import com.intellitrack.entity.*;
import com.intellitrack.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@SuppressWarnings("null")
@Service
public class ClasslistImportService {
    @Autowired private org.springframework.context.ApplicationEventPublisher events;
    @org.springframework.beans.factory.annotation.Autowired private com.intellitrack.service.SemesterService semesters;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassSectionRepository classSectionRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private AuditService auditService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern STUDENT_ID_PATTERN_1 = Pattern.compile("^\\d{2}-\\d{4}-\\d{3}$"); // ##-####-###
    private static final Pattern STUDENT_ID_PATTERN_2 = Pattern.compile("^\\d{4}-\\d{5}$"); // ####-#####

    @Transactional
    public ClasslistImportResultDto importClasslist(MultipartFile file, String performedBy, String ipAddress, String inputTerm) throws Exception {
        final String term=TrackingPolicy.term(inputTerm);
        semesters.requireActive(term);
        if(term==null)throw new IllegalArgumentException("Select an academic term before importing");
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<ClasslistImportRowDto> rowResults = new ArrayList<>();
        int skippedRows = 0;
        int totalRows = 0;
        int successfullyImported = 0;
        
        // Helper class to track group data
        class GroupData {
            List<Long> enrollmentIds = new ArrayList<>();
            Subject subject;
            String title;
        }
        
        // groupName -> GroupData (enrollments + subject)
        Map<String, GroupData> groupDataMap = new HashMap<>();

        try (InputStream inputStream = file.getInputStream();
                Workbook workbook = getWorkbook(file, inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            sheet.getRow(0); // skip header row

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;
                totalRows++;
                String studentId = "", fullName = "", email = "";

                try {
                    studentId = getCellValueAsString(row.getCell(0));
                    fullName = getCellValueAsString(row.getCell(1));
                    email = getCellValueAsString(row.getCell(2));
                    String subjectCode = CapstoneCoursePolicy.code(getCellValueAsString(row.getCell(4)));
                    String subjectName = CapstoneCoursePolicy.name(subjectCode, getCellValueAsString(row.getCell(3)));
                    String section = getCellValueAsString(row.getCell(5));
                    String groupName = getCellValueAsString(row.getCell(6)); // optional

                    List<String> rowErrors = new ArrayList<>();

                    if (studentId == null || studentId.isBlank()) {
                        rowErrors.add("Student ID is required");
                    } else if (!STUDENT_ID_PATTERN_1.matcher(studentId).matches()
                            && !STUDENT_ID_PATTERN_2.matcher(studentId).matches()) {
                        rowErrors.add("Invalid student ID format. Expected ##-####-### or ####-#####");
                    }

                    if (fullName == null || fullName.isBlank()) {
                        rowErrors.add("Full name is required");
                    }

                    if (email == null || email.isBlank()) {
                        rowErrors.add("Email is required");
                    } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                        rowErrors.add("Invalid email format");
                    }

                    if (subjectName == null || subjectName.isBlank()) {
                        rowErrors.add("Subject name is required");
                    }

                    if (subjectCode == null || subjectCode.isBlank()) {
                        rowErrors.add("Subject code is required");
                    }

                    if (section == null || section.isBlank()) {
                        rowErrors.add("Section is required");
                    }

                    if (!rowErrors.isEmpty()) {
                        String message = String.join("; ", rowErrors);
                        errors.add("Row " + (row.getRowNum() + 1) + ": " + message);
                        rowResults.add(new ClasslistImportRowDto(row.getRowNum() + 1, studentId, fullName, email, "FAILED", message));
                        continue;
                    }

                    CapstoneCoursePolicy.section(subjectCode,term,section);

                    Subject subject = subjectRepository.findByCode(subjectCode).orElseGet(() -> {
                        Subject newSubject = new Subject();
                        newSubject.setCode(subjectCode);
                        newSubject.setName(subjectName);
                        return subjectRepository.save(newSubject);
                    });

                    // Reuse the subject ID and standardize legacy short names in place.
                    if (!subjectName.equals(subject.getName())) {
                        subject.setName(subjectName);
                        subjectRepository.save(subject);
                    }

                    ClassSection classSection = classSectionRepository
                            .findBySubjectIdAndSectionAndAcademicTerm(subject.getId(), section, term)
                            .orElseGet(() -> {
                                ClassSection newSection = new ClassSection();
                                newSection.setSubject(subject);
                                newSection.setAcademicTerm(term);
                                newSection.setSection(section);
                                return classSectionRepository.save(newSection);
                            });

                    Optional<StudentEnrollment> existingEnrollment = studentEnrollmentRepository
                            .findByClassSectionIdAndStudentId(classSection.getId(), studentId);

                    if (existingEnrollment.isPresent()) {
                        skippedRows++;
                        rowResults.add(new ClasslistImportRowDto(row.getRowNum() + 1, studentId, fullName, email, "SKIPPED", "Already enrolled in this section"));
                        continue;
                    }

                    // Subject-level duplicate check by email
                    final String rowStudentId = studentId;
                    final String rowEmail = email;
                    if (email != null && !email.isBlank() &&
                            studentEnrollmentRepository.findAll().stream().anyMatch(e->e.getClassSection().getSubject().getId().equals(subject.getId()) && java.util.Objects.equals(e.getClassSection().getAcademicTerm(),term) && (rowEmail.equalsIgnoreCase(e.getEmail()) || rowStudentId.equals(e.getStudentId())))) {
                        skippedRows++;
                        rowResults.add(new ClasslistImportRowDto(row.getRowNum() + 1, studentId, fullName, email, "SKIPPED", "Already enrolled in another section of " + subjectCode));
                        continue;
                    }

                    StudentEnrollment enrollment = new StudentEnrollment();
                    enrollment.setStudentId(studentId);
                    enrollment.setClassSection(classSection);
                    enrollment.setFullName(fullName);
                    enrollment.setEmail(email);
                    
                    // A self-edited student number must never transfer enrollment ownership.
                    Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email).filter(u->"student".equals(u.getRole()));
                    
                    if (existingUser.isPresent()) {
                        User user = existingUser.get();

                        // Subject-level duplicate check by user account ID
                        if (studentEnrollmentRepository.findAll().stream().anyMatch(e->e.getClassSection().getSubject().getId().equals(subject.getId()) && java.util.Objects.equals(e.getClassSection().getAcademicTerm(),term) && e.getStudent()!=null && e.getStudent().getId().equals(user.getId()))) {
                            skippedRows++;
                            rowResults.add(new ClasslistImportRowDto(row.getRowNum() + 1, studentId, fullName, email, "SKIPPED", "Account already linked to an enrollment in " + subjectCode));
                            continue;
                        }

                        enrollment.setStudent(user);
                        
                        // Update user's studentId if not set
                        if ((user.getStudentId() == null || user.getStudentId().isBlank()) && 
                            studentId != null && !studentId.isBlank()) {
                            user.setStudentId(studentId);
                            userRepository.save(user);
                        }
                    } else {
                        enrollment.setStudent(null);
                    }
                    
                    studentEnrollmentRepository.save(enrollment);
                    events.publishEvent(new ActivityEmailService.StudentImported(enrollment.getId()));

                    // If a group name is provided, track this enrollment for group assignment
                    if (groupName != null && !groupName.isBlank()) {
                        String normalizedGroupName = subjectCode + "-" + term.replace(" / ", "-S") + "-" + groupName.trim();
                        GroupData groupData = groupDataMap.computeIfAbsent(normalizedGroupName, k -> new GroupData());
                        groupData.title=groupName.trim();
                        groupData.enrollmentIds.add(enrollment.getId());
                        
                        // Set or validate the subject for this group
                        if (groupData.subject == null) {
                            groupData.subject = subject;
                        } else if (!groupData.subject.getId().equals(subject.getId())) {
                            warnings.add("Row " + (row.getRowNum() + 1) + ": Group '" + normalizedGroupName + 
                                    "' is already associated with subject " + groupData.subject.getCode() + 
                                    ", but this student is in subject " + subject.getCode() + 
                                    ". Group subject will remain " + groupData.subject.getCode());
                        }
                    }

                    successfullyImported++;
                    rowResults.add(new ClasslistImportRowDto(row.getRowNum() + 1, studentId, fullName, email, "IMPORTED", "New enrollment created"));

                } catch (Exception e) {
                    String message = e.getMessage() == null ? "Import failed" : e.getMessage();
                    errors.add("Row " + (row.getRowNum() + 1) + ": " + message);
                    rowResults.add(new ClasslistImportRowDto(row.getRowNum() + 1, studentId, fullName, email, "FAILED", message));
                }
            }
        }

        // Create groups for students sharing the same group name
        for (Map.Entry<String, GroupData> entry : groupDataMap.entrySet()) {
            String groupName = entry.getKey();
            GroupData groupData = entry.getValue();
            List<Long> enrollmentIds = groupData.enrollmentIds;
            Subject groupSubject = groupData.subject;
            
            try {
                // Find or create group with this name as both code and title
                ProjectGroup group = projectGroupRepository.findByCode(groupName).orElseGet(() -> {
                    ProjectGroup newGroup = new ProjectGroup();
                    newGroup.setCode(groupName);
                    newGroup.setTitle(groupData.title);
                    newGroup.setSubject(groupSubject);
                    newGroup.setAcademicTerm(term);
                    return projectGroupRepository.save(newGroup);
                });
                
                // If group already exists, validate and set subject if needed
                if (group.getSubject() == null) {
                    group.setSubject(groupSubject);
                } else if (!group.getSubject().getId().equals(groupSubject.getId())) {
                    warnings.add("Group '" + groupName + "' is already associated with subject " + 
                            group.getSubject().getCode() + ", will not change to " + groupSubject.getCode());
                }
                
                // Add enrollments to the group
                if(!java.util.Objects.equals(group.getAcademicTerm(),term) || !group.getSubject().getId().equals(groupSubject.getId()))
                    throw new IllegalArgumentException("Existing group code belongs to a different subject or term");
                List<StudentEnrollment> enrollmentsToAdd = new ArrayList<>();
                for (Long enrollmentId : enrollmentIds) {
                    studentEnrollmentRepository.findById(enrollmentId).ifPresent(e -> {
                        var assigned=projectGroupRepository.findByStudents_Id(enrollmentId);
                        if(assigned.isPresent()&&!assigned.get().getId().equals(group.getId()))
                            throw new IllegalArgumentException("Student already belongs to another group");
                        if (!group.getStudents().contains(e)) {
                            group.getStudents().add(e);
                            enrollmentsToAdd.add(e);
                        }
                    });
                }
                projectGroupRepository.save(group);
                
                // Also update the User.group field for each student (for backward compatibility)
                for (StudentEnrollment enrollment : enrollmentsToAdd) {
                    if (enrollment.getStudent() != null) {
                        User student = enrollment.getStudent();
                        student.setGroup(group);
                        userRepository.save(student);
                    }
                }
            } catch (Exception e) {
                warnings.add("Could not create/update group '" + groupName + "': " + e.getMessage());
            }
        }

        // Log the import operation to audit trail
        String details = String.format("Term: %s. File: %s. Imported %d of %d rows. Errors: %d, Warnings: %d",
                term, file.getOriginalFilename(),
                successfullyImported, totalRows, errors.size(), warnings.size());
        auditService.log("CLASSLIST_IMPORT", performedBy, "CLASSLIST", details, ipAddress);

        ClasslistImportResultDto result = new ClasslistImportResultDto(
                totalRows,
                successfullyImported,
                errors.size(),
                errors,
                warnings);
        result.setSkippedRows(skippedRows);
        result.setRows(rowResults);
        return result;
    }

    private Workbook getWorkbook(MultipartFile file, InputStream inputStream) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (filename != null && filename.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        }
        throw new IllegalArgumentException("Unsupported file format. Please use .xlsx or .xls");
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    /**
     * Manually enroll a student (by existing user account) into an existing class
     * section.
     */
    @Transactional
    public StudentEnrollment addStudent(ManualEnrollmentRequest req) {
        if (req.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (req.getClassSectionId() == null) {
            throw new IllegalArgumentException("Class section is required");
        }

        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.getUserId()));

        if (!"student".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Only users with the student role can be enrolled");
        }

        ClassSection section = classSectionRepository.findById(req.getClassSectionId())
                .orElseThrow(() -> new IllegalArgumentException("Class section not found: " + req.getClassSectionId()));
        semesters.requireActive(section.getAcademicTerm());

        String displayName = ((user.getLastName() != null && !user.getLastName().isBlank())
                ? user.getLastName() + ", " + user.getFirstName()
                : (user.getFirstName() != null ? user.getFirstName() : user.getEmail()));

        // Check by userId (covers Google OAuth users whose studentId differs from the
        // Excel-imported one)
        CapstoneCoursePolicy.section(section.getSubject().getCode(),section.getAcademicTerm(),section.getSection());
        if (studentEnrollmentRepository.existsByClassSectionIdAndStudent_Id(section.getId(), user.getId())) {
            throw new IllegalArgumentException(displayName + " is already enrolled in this section");
        }

        if(studentEnrollmentRepository.findAllWithSectionAndSubject().stream().anyMatch(e ->
            e.getClassSection().getSubject().getId().equals(section.getSubject().getId())
            && java.util.Objects.equals(e.getClassSection().getAcademicTerm(),section.getAcademicTerm())
            && ((e.getStudent()!=null && e.getStudent().getId().equals(user.getId()))
                || (e.getEmail()!=null && e.getEmail().equalsIgnoreCase(user.getEmail())))))
            throw new IllegalArgumentException("Student is already enrolled in this subject and academic term");

        // Also check by studentId string (for enrollments that were imported but not
        // yet linked to an account)
        String studentId = (user.getStudentId() != null && !user.getStudentId().isBlank())
                ? user.getStudentId()
                : user.getId().toString();
        if (studentEnrollmentRepository.existsByClassSectionIdAndStudentId(section.getId(), studentId)) {
            throw new IllegalArgumentException(displayName + " is already enrolled in this section (by student ID)");
        }

        String fullName = ((user.getLastName() != null && !user.getLastName().isBlank())
                ? user.getLastName() + ", " + user.getFirstName()
                : (user.getFirstName() != null ? user.getFirstName() : user.getEmail()));

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setFullName(fullName);
        enrollment.setEmail(user.getEmail());
        enrollment.setClassSection(section);
        enrollment.setStudent(user); // always linked because we look up by userId

        return studentEnrollmentRepository.save(enrollment);
    }
}
