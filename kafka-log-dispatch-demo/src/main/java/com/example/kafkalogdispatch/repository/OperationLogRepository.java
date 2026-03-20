package com.example.kafkalogdispatch.repository;

import com.example.kafkalogdispatch.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
}