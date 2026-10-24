package com.intellitrack.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name="deadline_emails") @Getter @Setter
public class DeadlineEmail {
 @Id private String id;
 private Long userId;
 private Long requirementId;
 private String kind;
 private String revision;
 private String status="PENDING";
 private Instant createdAt=Instant.now();
 private Instant attemptedAt;
 private Instant sentAt;
 private String failure;
}
