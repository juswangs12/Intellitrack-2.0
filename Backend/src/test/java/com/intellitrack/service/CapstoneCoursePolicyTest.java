package com.intellitrack.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CapstoneCoursePolicyTest {
    @Test void validatesCodeAndNameTogether(){
        assertEquals("IT332",CapstoneCoursePolicy.code(" it332 "));
        assertEquals("Capstone and Research 1",CapstoneCoursePolicy.name("IT332","Capstone  1"));
        assertEquals("Capstone and Research 1",CapstoneCoursePolicy.name("IT332"," capstone And Research  1 "));
        assertEquals("Capstone and Research 2",CapstoneCoursePolicy.name("IT411","Capstone 2"));
        assertEquals("Capstone and Research 2",CapstoneCoursePolicy.name("IT411","Capstone and Research 2"));
        assertThrows(IllegalArgumentException.class,()->CapstoneCoursePolicy.name("IT332","Capstone and Research 2"));
        assertThrows(IllegalArgumentException.class,()->CapstoneCoursePolicy.code("IT322"));
        assertThrows(IllegalArgumentException.class,()->CapstoneCoursePolicy.name("IT411","Capstone 1"));
    }
    @Test void semesterCycleCrossesSchoolYears(){
        assertFalse(CapstoneCoursePolicy.offSemester("IT332","2025-2026 / 2"));
        assertFalse(CapstoneCoursePolicy.offSemester("IT411","2026-2027 / 1"));
        assertTrue(CapstoneCoursePolicy.offSemester("IT332","2026-2027 / 1"));
        assertTrue(CapstoneCoursePolicy.offSemester("IT411","2026-2027 / 2"));
    }
}
