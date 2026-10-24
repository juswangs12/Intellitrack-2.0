package com.intellitrack;

import com.intellitrack.entity.User;
import com.intellitrack.repository.UserRepository;
import com.intellitrack.security.JwtTokenProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:security;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa","spring.datasource.password=","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","jwt.secret=test-only-signing-secret-at-least-thirty-two-characters",
    "spring.security.oauth2.client.registration.google.client-id=test-client","spring.security.oauth2.client.registration.google.client-secret=test-secret",
    "ai.api.key=","google.cloud.service-account-json=","google.token-encryption-key=","spring.mail.test-connection=false"
})
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class TrackingSecurityTest {
    @Autowired MockMvc mvc;@Autowired UserRepository users;@Autowired JwtTokenProvider jwt;
    @Autowired com.intellitrack.repository.SubjectRepository subjects;
    @Autowired com.intellitrack.repository.ClassSectionRepository sections;
    @Autowired com.intellitrack.repository.StudentEnrollmentRepository enrollments;
    User student,other,coordinator;
    @BeforeEach void setup(){
        student=users.save(new User("Student","Test","student@test.invalid","hash","student"));
        other=users.save(new User("Other","Test","other@test.invalid","hash","student"));
        coordinator=users.save(new User("Coordinator","Test","coordinator@test.invalid","hash","coordinator"));
    }
    @Autowired com.intellitrack.service.AuthService authService;
    @Autowired com.intellitrack.repository.NotificationRepository notifications;
    @Test void passwordResetRevokesBothTokensAndAllowsFreshLogin() throws Exception {
        String old=bearer(student), refresh=jwt.generateRefreshToken(student.getId());
        student.setPasswordResetToken("reset-test");student.setPasswordResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(5));users.saveAndFlush(student);
        authService.resetPassword("reset-test","replacement123");users.flush();
        mvc.perform(get("/api/users/me").header("Authorization",old)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh-token").contentType("application/json").content("{\"refreshToken\":\""+refresh+"\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization",bearer(users.findById(student.getId()).orElseThrow()))).andExpect(status().isOk());
    }
    @Test void notificationsCannotBeReadOrChangedByAnotherStudent() throws Exception {
        var note=notifications.saveAndFlush(new com.intellitrack.entity.Notification(other.getId(),"Private reminder"));
        mvc.perform(get("/api/notifications/"+other.getId()).header("Authorization",bearer(student))).andExpect(status().isForbidden());
        mvc.perform(post("/api/notifications/"+note.getId()+"/read").header("Authorization",bearer(student))).andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertFalse(notifications.findById(note.getId()).orElseThrow().isRead());
        mvc.perform(post("/api/notifications/"+note.getId()+"/read").header("Authorization",bearer(other))).andExpect(status().isOk());
    }
    @Autowired com.intellitrack.service.UserService userService;
    @Test void passwordChangeRevokesExistingAccessAndRefresh() throws Exception {
        student.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("original123"));users.saveAndFlush(student);
        String old=bearer(student), refresh=jwt.generateRefreshToken(student.getId());
        var change=new com.intellitrack.dto.PasswordChangeRequest();change.setCurrentPassword("original123");change.setNewPassword("changed123");change.setConfirmPassword("changed123");
        userService.changePassword(student.getId(),change);users.flush();
        mvc.perform(get("/api/users/me").header("Authorization",old)).andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,()->authService.refreshToken(refresh));
    }
    @Test void adminCreationDoesNotAcceptGoogleIdentityOrResetToken() throws Exception {
        var admin=users.save(new User("Admin","Test","create-admin@test.invalid","hash","administrator"));
        mvc.perform(post("/api/users").header("Authorization",bearer(admin)).contentType("application/json")
          .content("{\"firstName\":\"Created\",\"lastName\":\"Account\",\"email\":\"created@test.invalid\",\"password\":\"password123\",\"role\":\"adviser\",\"googleSub\":\"forged\",\"passwordResetToken\":\"forged\"}"))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
        var created=users.findByEmailIgnoreCase("created@test.invalid").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNull(created.getGoogleSub());
        org.junit.jupiter.api.Assertions.assertNull(created.getPasswordResetToken());
        org.junit.jupiter.api.Assertions.assertNotEquals("password123",created.getPassword());
        mvc.perform(delete("/api/users/"+admin.getId()).header("Authorization",bearer(admin))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/users/"+created.getId()).header("Authorization",bearer(admin))).andExpect(status().isOk());
    }
    String bearer(User user){return "Bearer "+jwt.generateToken(user);}
    @Test void websiteAnalyticsAreAdminOnlyAndExcludeSecrets() throws Exception {
        mvc.perform(get("/api/admin/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/overview").header("Authorization",bearer(coordinator))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/overview").header("Authorization",bearer(student))).andExpect(status().isForbidden());
        var admin=users.saveAndFlush(new User("Admin","Analytics","analytics@test.invalid","hash","administrator"));
        student.recordSignIn();users.saveAndFlush(student);
        mvc.perform(get("/api/admin/overview").header("Authorization",bearer(admin)))
          .andExpect(status().isOk()).andExpect(jsonPath("$.roles.student").value(2))
          .andExpect(jsonPath("$.registrations.length()").value(7))
          .andExpect(jsonPath("$.recentSignIns[0].name").value("Student Test"))
          .andExpect(jsonPath("$.recentSignIns[0].password").doesNotExist())
          .andExpect(jsonPath("$.integrations[0].lastSuccess").isEmpty())
          .andExpect(jsonPath("$.database").value("Reachable"));
    }
    @Test void semesterArchiveIsStaffOnlyAndCopyEndpointsAreRetired()throws Exception{
        mvc.perform(put("/api/academic/semesters").header("Authorization",bearer(student)).contentType("application/json").content("{\"term\":\"2026-2027 / 1\",\"archived\":true}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/academic/groups/1/carry").header("Authorization",bearer(coordinator)).contentType("application/json").content("{\"subjectId\":1,\"term\":\"2026-2027 / 2\"}"))
            .andExpect(status().isGone());
    }
    @Test void coordinatorCalendarSyncIsRoleProtected()throws Exception{
        mvc.perform(post("/api/google/calendar/coordinator/sync")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/google/calendar/coordinator/sync").header("Authorization",bearer(student))).andExpect(status().isForbidden());
        var adviser=users.save(new User("Adviser","Test","calendar-adviser@test.invalid","hash","adviser"));
        mvc.perform(post("/api/google/calendar/coordinator/sync").header("Authorization",bearer(adviser))).andExpect(status().isForbidden());
        mvc.perform(post("/api/google/calendar/coordinator/sync").header("Authorization",bearer(coordinator))).andExpect(status().isConflict());
    }
    @Test void awaitingListIncludesOnlyUnenrolledGoogleStudentsAndClearsAfterEnrollment()throws Exception{
        student.setGoogleId("google-student");coordinator.setGoogleId("google-staff");users.saveAndFlush(student);users.saveAndFlush(coordinator);
        mvc.perform(get("/api/student-enrollments/awaiting").header("Authorization",bearer(student))).andExpect(status().isForbidden());
        var adviser=users.save(new User("Adviser","Test","pending-adviser@test.invalid","hash","adviser"));
        mvc.perform(get("/api/student-enrollments/awaiting").header("Authorization",bearer(adviser))).andExpect(status().isForbidden());
        mvc.perform(get("/api/student-enrollments/awaiting").header("Authorization",bearer(coordinator)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].id").value(student.getId()))
            .andExpect(jsonPath("$.data[0].password").doesNotExist()).andExpect(jsonPath("$.data[0].googleId").doesNotExist());
        var subject=new com.intellitrack.entity.Subject();subject.setCode("PENDING");subject.setName("Pending test");subjects.save(subject);
        var section=new com.intellitrack.entity.ClassSection();section.setSubject(subject);section.setSection("A");section.setAcademicTerm("2026-2027 / 1");sections.save(section);
        mvc.perform(post("/api/subjects/sections/"+section.getId()+"/enroll").header("Authorization",bearer(coordinator)).contentType("application/json").content("{\"userId\":"+student.getId()+"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/student-enrollments/awaiting").header("Authorization",bearer(coordinator))).andExpect(jsonPath("$.data.length()").value(0));
        other.setGoogleSub("google-other");users.saveAndFlush(other);
        var imported=new com.intellitrack.entity.StudentEnrollment();imported.setClassSection(section);imported.setFullName("Other Test");imported.setEmail(other.getEmail());enrollments.save(imported);
        mvc.perform(get("/api/student-enrollments/awaiting").header("Authorization",bearer(coordinator))).andExpect(jsonPath("$.data[0].id").value(other.getId()));
        mvc.perform(post("/api/student-enrollments/"+imported.getId()+"/link-user").header("Authorization",bearer(coordinator)).contentType("application/json").content("{\"userId\":"+other.getId()+"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/student-enrollments/awaiting").header("Authorization",bearer(coordinator))).andExpect(jsonPath("$.data.length()").value(0));
    }
    @Test void coordinatorCanCreateSubjectButStudentsCannotAndDuplicatesAreRejected()throws Exception{
        String body="{\"code\":\" cap-new \",\"name\":\"Capstone Setup\"}";
        mvc.perform(post("/api/academic/subjects").header("Authorization",bearer(student)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/academic/subjects").header("Authorization",bearer(coordinator)).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber());
        mvc.perform(post("/api/academic/subjects").header("Authorization",bearer(coordinator)).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/academic").header("Authorization",bearer(coordinator))).andExpect(status().isOk()).andExpect(jsonPath("$.subjects[0].code").value("CAP-NEW"));
    }
    @Test void anonymousCannotReadTracking()throws Exception{mvc.perform(get("/api/tracking")).andExpect(status().isUnauthorized());}
    @Test void studentsCannotCreateAccountsOrReadOtherProfiles()throws Exception{
        mvc.perform(post("/api/users").header("Authorization",bearer(student)).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/users/"+other.getId()).header("Authorization",bearer(student))).andExpect(status().isForbidden());
        mvc.perform(get("/api/users/"+student.getId()).header("Authorization",bearer(student))).andExpect(status().isOk());
    }
    @Test void refreshTokensCannotAuthenticateApiRequests()throws Exception{
        mvc.perform(get("/api/tracking").header("Authorization","Bearer "+jwt.generateRefreshToken(student.getId()))).andExpect(status().isUnauthorized());
    }
    @Test void retiredCommentAndUploadRoutesStayDisabled()throws Exception{
        mvc.perform(post("/api/comments/submission/1").header("Authorization",bearer(coordinator)).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/submissions/google-doc").header("Authorization",bearer(student)).contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test void currentDatabaseRoleOverridesStaleTokenRole()throws Exception{
        String old=jwt.generateToken(coordinator);coordinator.setRole("student");users.saveAndFlush(coordinator);
        mvc.perform(get("/api/academic").header("Authorization","Bearer "+old)).andExpect(status().isForbidden());
    }
    @Test void administratorAccountEditorSavesRoleWithoutErasingOtherFields()throws Exception{
        var admin=users.save(new User("Admin","Test","admin@test.invalid","hash","administrator"));
        other.setPhone("12345");users.saveAndFlush(other);
        mvc.perform(put("/api/users/"+other.getId()+"/account").header("Authorization",bearer(admin)).contentType("application/json")
            .content("{\"firstName\":\"Other\",\"lastName\":\"Test\",\"department\":\"IT\",\"role\":\"adviser\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("adviser"));
        org.junit.jupiter.api.Assertions.assertEquals("12345",users.findById(other.getId()).orElseThrow().getPhone());
        mvc.perform(put("/api/users/"+other.getId()+"/account").header("Authorization",bearer(student)).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }
    @Test void refreshUsesJsonBodyAndReadinessIsPublic()throws Exception{
        mvc.perform(post("/api/auth/refresh-token").contentType("application/json").content("{\"refreshToken\":\""+jwt.generateRefreshToken(student.getId())+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value(student.getId()));
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
    @Test void legacyMetricsAreDisabledAndRuntimeConfigIsTruthful()throws Exception{
        mvc.perform(get("/api/analytics").header("Authorization",bearer(coordinator))).andExpect(status().isForbidden());
        var admin=users.save(new User("Admin","Test","admin@test.invalid","hash","administrator"));
        mvc.perform(get("/api/system-config").header("Authorization",bearer(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.settings['Academic timezone']").value("Asia/Manila"));
    }

    @Test void currentUserReturnsServerIdentityWithoutSecrets()throws Exception{
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization",bearer(student)))
          .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(student.getId()))
          .andExpect(jsonPath("$.role").value("student")).andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void accountSearchWithoutRoleIncludesStaff()throws Exception{
        mvc.perform(get("/api/users").param("q","Coordinator").header("Authorization",bearer(coordinator)))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].id").value(coordinator.getId()));
    }
}
