package com.intellitrack.service;

import java.util.Locale;
import java.util.Map;

/** University course catalogue. Section identifiers are never numerically normalized. */
public final class CapstoneCoursePolicy {
    private static final Map<String, String> COURSES = Map.of("IT332", "Capstone 1", "IT411", "Capstone 2");
    private CapstoneCoursePolicy() {}

    public static String code(String value) {
        String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!COURSES.containsKey(code))
            throw new IllegalArgumentException("Unknown subject code '" + code + "'. Use IT332 (Capstone 1) or IT411 (Capstone 2).");
        return code;
    }

    public static String name(String code, String suppliedName) {
        String expected = COURSES.get(code(code));
        if (suppliedName == null || !expected.equalsIgnoreCase(suppliedName.trim().replaceAll("\\s+", " ")))
            throw new IllegalArgumentException(code + " must be named " + expected);
        return expected;
    }

    public static boolean offSemester(String code, String term) {
        String normalized = TrackingPolicy.term(term);
        String regularSemester = code(code).equals("IT332") ? "2" : "1";
        return normalized != null && !normalized.endsWith(" / " + regularSemester);
    }
}
