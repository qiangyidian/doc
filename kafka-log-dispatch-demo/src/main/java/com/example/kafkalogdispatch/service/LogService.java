package com.example.kafkalogdispatch.service;

import com.example.kafkalogdispatch.dto.LogDispatchResponse;
import com.example.kafkalogdispatch.dto.SendLogRequest;
import com.example.kafkalogdispatch.entity.AlertLog;
import com.example.kafkalogdispatch.entity.AuditLog;

import java.util.List;

public interface LogService {

    LogDispatchResponse sendLog(SendLogRequest request);

    List<AuditLog> listAuditLogs();

    List<AlertLog> listAlertLogs();
}