package com.intellitrack.entity;

public enum UserRole {
    STUDENT("student"),
    ADVISER("adviser"),
    COORDINATOR("coordinator"),
    ADMINISTRATOR("administrator");

    private final String roleName;

    UserRole(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    public static UserRole fromRoleName(String roleName) {
        for (UserRole role : values()) {
            if (role.roleName.equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + roleName);
    }
}
