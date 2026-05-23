package com.intellitrack.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * DEPRECATED: Use Deliverable directly!
 * This class remains only for backward compatibility!
 */
@Deprecated
@Entity
@Table(name = "deadlines")
public class Deadline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deliverable_id")
    private Deliverable deliverable;

    @Column(nullable = false)
    private LocalDateTime dueAt;

    @Column(nullable = false)
    private String academicTerm;

    // Backward compatibility constructor
    @Deprecated
    public Deadline() {
    }

    @Deprecated
    public Deadline(Deliverable deliverable) {
        this.deliverable = deliverable;
        this.dueAt = deliverable.getDueAt();
        this.academicTerm = deliverable.getAcademicTerm();
    }

    @Deprecated
    public Long getId() {
        return id;
    }

    @Deprecated
    public void setId(Long id) {
        this.id = id;
    }

    @Deprecated
    public Deliverable getDeliverable() {
        return deliverable;
    }

    @Deprecated
    public void setDeliverable(Deliverable deliverable) {
        this.deliverable = deliverable;
        if (deliverable != null) {
            this.dueAt = deliverable.getDueAt();
            this.academicTerm = deliverable.getAcademicTerm();
        }
    }

    @Deprecated
    public LocalDateTime getDueAt() {
        if (deliverable != null) {
            return deliverable.getDueAt();
        }
        return dueAt;
    }

    @Deprecated
    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
        if (deliverable != null) {
            deliverable.setDueAt(dueAt);
        }
    }

    @Deprecated
    public String getAcademicTerm() {
        if (deliverable != null) {
            return deliverable.getAcademicTerm();
        }
        return academicTerm;
    }

    @Deprecated
    public void setAcademicTerm(String academicTerm) {
        this.academicTerm = academicTerm;
        if (deliverable != null) {
            deliverable.setAcademicTerm(academicTerm);
        }
    }
}
