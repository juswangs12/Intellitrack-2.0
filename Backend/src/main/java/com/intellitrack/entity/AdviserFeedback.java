package com.intellitrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "adviser_feedback")
@Data
@NoArgsConstructor
public class AdviserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adviser_id", nullable = false)
    private User adviser;

    @Column(columnDefinition = "TEXT")
    private String generalComments;

    @Column(nullable = false)
    private Double totalScore;

    @Column(nullable = false)
    private LocalDateTime evaluatedAt;

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CriterionEvaluation> criterionEvaluations = new ArrayList<>();

    public void addCriterionEvaluation(CriterionEvaluation evaluation) {
        criterionEvaluations.add(evaluation);
        evaluation.setFeedback(this);
    }
}
