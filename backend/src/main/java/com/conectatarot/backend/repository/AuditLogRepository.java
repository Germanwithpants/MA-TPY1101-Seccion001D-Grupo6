package com.conectatarot.backend.repository;

import com.conectatarot.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime desde, LocalDateTime hasta);
    List<AuditLog> findAllByOrderByTimestampDesc();
}
