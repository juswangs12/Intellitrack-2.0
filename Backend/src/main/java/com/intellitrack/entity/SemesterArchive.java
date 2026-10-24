package com.intellitrack.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="semester_archives")
public class SemesterArchive {
    @Id private String term;
    private LocalDateTime archivedAt;
    public SemesterArchive() {}
    public SemesterArchive(String term,LocalDateTime at){this.term=term;this.archivedAt=at;}
    public String getTerm(){return term;}
    public LocalDateTime getArchivedAt(){return archivedAt;}
}
