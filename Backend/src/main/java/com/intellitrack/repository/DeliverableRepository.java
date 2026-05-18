package com.intellitrack.repository;

import com.intellitrack.entity.Deliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliverableRepository extends JpaRepository<Deliverable, Long> {
    Optional<Deliverable> findByName(String name);
    List<Deliverable> findByActiveTrue();
    boolean existsByName(String name);
}