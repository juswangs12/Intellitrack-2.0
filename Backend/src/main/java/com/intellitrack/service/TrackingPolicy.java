package com.intellitrack.service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/** Shared rules. A late replacement never erases first-submission compliance. */
public final class TrackingPolicy {
    private TrackingPolicy() {}
    public static String status(LocalDateTime first, LocalDateTime due, LocalDateTime now) {
        if (first != null) return due != null && first.isAfter(due) ? "SUBMITTED_LATE" : "SUBMITTED_ON_TIME";
        if (due == null) return "AWAITING_SUBMISSION";
        if (now.isAfter(due)) return "OVERDUE";
        return now.toLocalDate().equals(due.toLocalDate()) ? "DUE_TODAY" : "AWAITING_SUBMISSION";
    }
    public static String term(String term) {
        if (term == null || term.isBlank()) return null;
        if (!Pattern.matches("\\d{4}-\\d{4} / [12]", term))
            throw new IllegalArgumentException("Term must use YYYY-YYYY / 1 or YYYY-YYYY / 2");
        if (Integer.parseInt(term.substring(5,9)) != Integer.parseInt(term.substring(0,4)) + 1)
            throw new IllegalArgumentException("School year must contain consecutive years");
        return term;
    }
}
