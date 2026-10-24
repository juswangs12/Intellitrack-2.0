package com.intellitrack.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="google_connections") @Getter @Setter
public class GoogleConnection {
    @Id private String id;
    private Long userId;
    private String purpose;
    private Instant lastSuccessAt;
    private String calendarFingerprint;
    private Instant nextAutoSyncAt;
    private String autoSyncError;
    private String stateHash;
    private Instant stateExpiresAt;
    @Column(columnDefinition="TEXT") private String encryptedVerifier;
    @Column(columnDefinition="TEXT") private String encryptedRefreshToken;
    private String googleEmail;
    private String calendarId;
    private String folderId;
    @Version private Long version;
}
