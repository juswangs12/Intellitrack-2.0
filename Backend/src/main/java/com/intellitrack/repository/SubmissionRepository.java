package com.intellitrack.repository;

import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    @EntityGraph(attributePaths = {"group", "group.students", "group.students.classSection", "group.students.classSection.subject", "deliverable"})
    Optional<Submission> findByGroupIdAndDeliverableId(Long groupId, Long deliverableId);

    @EntityGraph(attributePaths = {"group", "group.students", "group.students.classSection", "group.students.classSection.subject", "deliverable"})
    List<Submission> findByGroupId(Long groupId);

    @EntityGraph(attributePaths = {"group", "group.students", "group.students.classSection", "group.students.classSection.subject", "deliverable"})
    List<Submission> findByDeliverableId(Long deliverableId);

    @EntityGraph(attributePaths = {"group", "group.students", "group.students.classSection", "group.students.classSection.subject", "deliverable"})
    List<Submission> findByStatus(SubmissionStatus status);

    @EntityGraph(attributePaths = {"group", "group.students", "group.students.classSection", "group.students.classSection.subject", "deliverable"})
    List<Submission> findByStatusIn(List<SubmissionStatus> statuses);

    @EntityGraph(attributePaths = {"group", "group.students", "group.students.classSection", "group.students.classSection.subject", "deliverable"})
    @Query("SELECT s FROM Submission s WHERE s.group.adviser.id = :adviserId")
    List<Submission> findByGroupAdviserId(Long adviserId);
}
