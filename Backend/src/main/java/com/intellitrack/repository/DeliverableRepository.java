package com.intellitrack.repository;

import com.intellitrack.entity.Deliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliverableRepository extends JpaRepository<Deliverable, Long> {
    Optional<Deliverable> findByName(String name);

    List<Deliverable> findByActiveTrue();

    boolean existsByName(String name);

    @Query("SELECT d FROM Deliverable d WHERE d.active = true AND (d.subject.id IN :subjectIds OR d.section.id IN :sectionIds)")
    List<Deliverable> findActiveBySubjectIdsOrSectionIds(List<Long> subjectIds, List<Long> sectionIds);

    @Query("SELECT d FROM Deliverable d LEFT JOIN FETCH d.subject LEFT JOIN FETCH d.section")
    List<Deliverable> findAllWithSubjectAndSection();

    @Query("SELECT d FROM Deliverable d LEFT JOIN FETCH d.subject LEFT JOIN FETCH d.section WHERE d.active = true")
    List<Deliverable> findActiveWithSubjectAndSection();
}