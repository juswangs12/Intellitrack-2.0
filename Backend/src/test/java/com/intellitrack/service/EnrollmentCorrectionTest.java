package com.intellitrack.service;
import com.intellitrack.controller.SubjectController;
import com.intellitrack.entity.*;
import com.intellitrack.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class EnrollmentCorrectionTest {
 SubjectController controller;StudentEnrollmentRepository enrollments;ClassSectionRepository sections;ProjectGroupRepository groups;StudentEnrollment enrollment;ClassSection target;
 ClassSection section(long id,long subjectId,String code,String name){var subject=new Subject();subject.setId(subjectId);subject.setCode(code);subject.setName(name);var s=new ClassSection();s.setId(id);s.setSubject(subject);s.setSection("G1");s.setAcademicTerm("2026-2027 / 1");return s;}
 @BeforeEach void setup(){controller=new SubjectController();enrollments=mock(StudentEnrollmentRepository.class);sections=mock(ClassSectionRepository.class);groups=mock(ProjectGroupRepository.class);
 ReflectionTestUtils.setField(controller,"studentEnrollmentRepository",enrollments);ReflectionTestUtils.setField(controller,"classSectionRepository",sections);ReflectionTestUtils.setField(controller,"groupRepository",groups);ReflectionTestUtils.setField(controller,"semesters",mock(SemesterService.class));
 enrollment=new StudentEnrollment();enrollment.setId(1L);enrollment.setEmail("student@example.test");enrollment.setClassSection(section(1,1,"IT322","Capstone 1"));target=section(2,2,"IT332","Capstone 1");
 when(enrollments.findByIdWithSectionAndSubject(1L)).thenReturn(Optional.of(enrollment));when(sections.findById(2L)).thenReturn(Optional.of(target));when(enrollments.save(any())).thenAnswer(i->i.getArgument(0));}
 @Test void correctsInvalidSubjectWithoutRelabelingHistory(){assertEquals(200,controller.moveStudent(1L,2L).getStatusCode().value());assertSame(target,enrollment.getClassSection());}
 @Test void rejectsDuplicateInAnotherSectionOfTargetSubject(){var other=new StudentEnrollment();other.setId(9L);other.setEmail("STUDENT@example.test");other.setClassSection(section(9,2,"IT332","Capstone 1"));when(enrollments.findAllWithSectionAndSubject()).thenReturn(List.of(other));assertEquals(409,controller.moveStudent(1L,2L).getStatusCode().value());verify(enrollments,never()).save(any());}
 @Test void protectsGroupAndFormerGroupHistory(){when(groups.existsByStudents_Id(1L)).thenReturn(true);assertThrows(IllegalArgumentException.class,()->controller.moveStudent(1L,2L));when(groups.existsByStudents_Id(1L)).thenReturn(false);enrollment.getFormerGroupIds().add(5L);assertThrows(IllegalArgumentException.class,()->controller.moveStudent(1L,2L));verify(enrollments,never()).save(any());}
}
