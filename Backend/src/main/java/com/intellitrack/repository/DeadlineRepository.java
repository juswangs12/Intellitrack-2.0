package com.intellitrack.repository;

import com.intellitrack.entity.Deadline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeadlineRepository extends JpaRepository<Deadline, Long> {
    Optional<Deadline> findByDeliverableId(Long deliverableId);

    List<Deadline> findByDueAtBetween(LocalDateTime start, LocalDateTime end);
}