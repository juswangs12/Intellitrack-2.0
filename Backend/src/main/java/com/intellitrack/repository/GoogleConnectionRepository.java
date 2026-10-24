package com.intellitrack.repository;
import com.intellitrack.entity.GoogleConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface GoogleConnectionRepository extends JpaRepository<GoogleConnection,String> {
    void deleteByUserId(Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GoogleConnection> findByStateHash(String stateHash);
}
