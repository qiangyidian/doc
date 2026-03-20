package com.example.kafkalogdispatch.repository;

import com.example.kafkalogdispatch.entity.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    List<AlertLog> findAllByOrderByIdDesc();
}