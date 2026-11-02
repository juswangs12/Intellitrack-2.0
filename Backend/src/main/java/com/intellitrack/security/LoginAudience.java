package com.intellitrack.security;

import java.util.Set;

public final class LoginAudience {
    public static final String ATTRIBUTE = "intellitrack.loginAudience";
    private LoginAudience() {}
    public static String normalize(String value) {
        if (value == null || value.isBlank()) return "student";
        if (!Set.of("student", "staff").contains(value)) throw new IllegalArgumentException("Invalid sign-in portal");
        return value;
    }
    public static void requireRole(String audience, String role) {
        boolean allowed = normalize(audience).equals("student") ? "student".equals(role)
            : Set.of("adviser", "coordinator", "administrator").contains(role == null ? "" : role);
        if (!allowed) throw new IllegalArgumentException("This account cannot sign in through this portal. Choose the student or staff sign-in page for your account.");
    }
}
