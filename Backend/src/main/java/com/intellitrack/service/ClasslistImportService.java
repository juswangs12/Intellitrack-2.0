package com.intellitrack.service;

import com.intellitrack.dto.ClasslistImportResultDto;
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
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ClasslistImportService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;
    
    @Autowired
    private ClassSectionRepository classSectionRepository;
    
    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern STUDENT_ID_PATTERN_1 = Pattern.compile("^\\d{2}-\\d{4}-\\d{3}$"); // ##-####-###
    private static final Pattern STUDENT_ID_PATTERN_2 = Pattern.compile("^\\d{4}-\\d{5}$"); // ####-#####

    @Transactional
    public ClasslistImportResultDto importClasslist(MultipartFile file) throws Exception {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int totalRows = 0;
        int successfullyImported = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = getWorkbook(file, inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                totalRows++;

                try {
                    String studentId = getCellValueAsString(row.getCell(0));
                    String fullName = getCellValueAsString(row.getCell(1));
                    String email = getCellValueAsString(row.getCell(2));
                    String subjectName = getCellValueAsString(row.getCell(3));
                    String subjectCode = getCellValueAsString(row.getCell(4));
                    String section = getCellValueAsString(row.getCell(5));

                    List<String> rowErrors = new ArrayList<>();
                    
                    if (studentId == null || studentId.isBlank()) {
                        rowErrors.add("Student ID is required");
                    } else if (!STUDENT_ID_PATTERN_1.matcher(studentId).matches() && !STUDENT_ID_PATTERN_2.matcher(studentId).matches()) {
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
                        errors.add("Row " + (row.getRowNum() + 1) + ": " + String.join("; ", rowErrors));
                        continue;
                    }

                    Subject subject = subjectRepository.findByCode(subjectCode).orElseGet(() -> {
                        Subject newSubject = new Subject();
                        newSubject.setCode(subjectCode);
                        newSubject.setName(subjectName);
                        return subjectRepository.save(newSubject);
                    });

                    ClassSection classSection = classSectionRepository
                            .findBySubjectIdAndSection(subject.getId(), section)
                            .orElseGet(() -> {
                                ClassSection newSection = new ClassSection();
                                newSection.setSubject(subject);
                                newSection.setSection(section);
                                return classSectionRepository.save(newSection);
                            });

                    Optional<StudentEnrollment> existingEnrollment = studentEnrollmentRepository
                            .findByClassSectionIdAndStudentId(classSection.getId(), studentId);
                    
                    if (existingEnrollment.isPresent()) {
                        warnings.add("Row " + (row.getRowNum() + 1) + ": Student with ID " + studentId + " already enrolled in this section, skipping");
                        continue;
                    }

                    StudentEnrollment enrollment = new StudentEnrollment();
                    enrollment.setStudentId(studentId);
                    enrollment.setClassSection(classSection);
                    enrollment.setFullName(fullName);
                    enrollment.setEmail(email);
                    enrollment.setStudent(null);
                    studentEnrollmentRepository.save(enrollment);
                    
                    successfullyImported++;

                } catch (Exception e) {
                    errors.add("Row " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }
        }

        return new ClasslistImportResultDto(
                totalRows,
                successfullyImported,
                totalRows - successfullyImported,
                errors,
                warnings
        );
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
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}